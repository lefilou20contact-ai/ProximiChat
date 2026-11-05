package com.proximichat.client;

import com.proximichat.ProximiChat;
import com.proximichat.config.ProximiChatConfig;
import com.proximichat.network.ClientConnectedPayload;
import com.proximichat.network.VoiceDataPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import javax.sound.sampled.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class VoiceClientHandler {
    private static TargetDataLine microphone;
    private static Thread captureThread;
    private static volatile boolean muted;
    private static volatile boolean connected;
    private static final AtomicBoolean pushToTalkHeld = new AtomicBoolean(false);
    private static final AtomicReference<UUID> localPlayerUuid = new AtomicReference<>();
    private static final Map<UUID, SourceDataLine> speakers = new ConcurrentHashMap<>();
    private static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000f, 16, 1, 2, 48000f, false);

    private VoiceClientHandler() { }

    public static synchronized void onJoin() {
        stopAudio();
        Minecraft client = Minecraft.getInstance();
        localPlayerUuid.set(client.player == null ? null : client.player.getUUID());
        ClientPlayNetworking.send(new ClientConnectedPayload(2));
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            if (!AudioSystem.isLineSupported(info)) {
                ProximiChat.LOGGER.warn("[ProximiChat] Microphone format is unsupported.");
                return;
            }
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(FORMAT);
            microphone.start();
            connected = true;
            captureThread = new Thread(VoiceClientHandler::captureLoop, "ProximiChat Mic Capture");
            captureThread.setDaemon(true);
            captureThread.start();
        } catch (LineUnavailableException | IllegalArgumentException e) {
            ProximiChat.LOGGER.warn("[ProximiChat] Cannot open microphone", e);
            stopAudio();
        }
    }

    public static synchronized void onDisconnect() {
        stopAudio();
        localPlayerUuid.set(null);
        OpusCodec.reset();
    }

    private static void stopAudio() {
        connected = false;
        pushToTalkHeld.set(false);
        Thread thread = captureThread;
        captureThread = null;
        if (thread != null) thread.interrupt();
        TargetDataLine mic = microphone;
        microphone = null;
        if (mic != null && mic.isOpen()) {
            mic.stop();
            mic.close();
        }
        speakers.values().forEach(line -> {
            if (line != null && line.isOpen()) { line.stop(); line.close(); }
        });
        speakers.clear();
    }

    public static void tick(Minecraft client) {
        localPlayerUuid.set(client.player == null ? null : client.player.getUUID());
        if (!connected || muted || ProximiChatClient.pushToTalkKey == null) {
            pushToTalkHeld.set(false);
            return;
        }
        pushToTalkHeld.set(ProximiChatClient.pushToTalkKey.isDown());
    }

    private static int frameSamples() {
        return switch (ProximiChatConfig.get().frameSize) {
            case 120, 240, 480, 960, 1920, 2880 -> ProximiChatConfig.get().frameSize;
            default -> 960;
        };
    }

    private static void captureLoop() {
        while (connected && !Thread.currentThread().isInterrupted()) {
            int bytes = frameSamples() * FORMAT.getFrameSize();
            byte[] pcm = new byte[bytes];
            TargetDataLine mic = microphone;
            if (mic == null || !readFully(mic, pcm)) break;
            if (muted) continue;
            ProximiChatConfig cfg = ProximiChatConfig.get();
            if (cfg.pushToTalk ? !pushToTalkHeld.get() : !hasAudioActivity(pcm)) continue;
            applyInputGain(pcm, cfg.inputGain);
            byte[] encoded = OpusCodec.encode(pcm, pcm.length);
            UUID sender = localPlayerUuid.get();
            if (encoded == null || sender == null) continue;
            Minecraft.getInstance().execute(() -> {
                if (connected && ClientPlayNetworking.canSend(VoiceDataPayload.TYPE)) {
                    ClientPlayNetworking.send(new VoiceDataPayload(sender, encoded, false));
                }
            });
        }
    }

    private static boolean readFully(TargetDataLine line, byte[] bytes) {
        int offset = 0;
        while (offset < bytes.length && connected && !Thread.currentThread().isInterrupted()) {
            try {
                int count = line.read(bytes, offset, bytes.length - offset);
                if (count <= 0) return false;
                offset += count;
            } catch (RuntimeException e) { return false; }
        }
        return offset == bytes.length;
    }

    private static void applyInputGain(byte[] pcm, float gain) {
        if (!Float.isFinite(gain) || gain == 1.0f) return;
        for (int i = 0; i < pcm.length - 1; i += 2) {
            int sample = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xFF));
            sample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample * gain)));
            pcm[i] = (byte) sample;
            pcm[i + 1] = (byte) (sample >> 8);
        }
    }

    private static boolean hasAudioActivity(byte[] pcm) {
        long squares = 0;
        int count = pcm.length / 2;
        for (int i = 0; i < pcm.length - 1; i += 2) {
            short sample = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xFF));
            squares += (long) sample * sample;
        }
        return count > 0 && Math.sqrt((double) squares / count) > 500;
    }

    public static void onVoiceData(VoiceDataPayload payload, ClientPlayNetworking.Context ctx) {
        byte[] pcm = OpusCodec.decode(payload.senderUuid(), payload.opusData());
        if (pcm == null) return;
        UUID speaker = payload.senderUuid();
        SourceDataLine line = speakers.computeIfAbsent(speaker, key -> openSpeakerLine());
        if (line == null || !line.isOpen()) return;
        float volume = computeVolume(speaker, ctx.client(), payload.whisper());
        byte[] audio = applyVolume(pcm, volume, ProximiChatConfig.get().outputGain);
        line.write(audio, 0, audio.length);
    }

    private static float computeVolume(UUID speakerUuid, Minecraft client, boolean whisper) {
        if (client.player == null || client.level == null) return 0;
        var speaker = client.level.players().stream().filter(p -> p.getUUID().equals(speakerUuid)).findFirst().orElse(null);
        if (speaker == null) return 1;
        double distance = client.player.position().distanceTo(speaker.position());
        double range = whisper ? ProximiChatConfig.get().whisperDistance : ProximiChatConfig.get().proximityDistance;
        if (!Double.isFinite(range) || range <= 0) return 0;
        return (float) Math.max(0, 1 - distance / range);
    }

    private static byte[] applyVolume(byte[] pcm, float volume, float outputGain) {
        byte[] out = new byte[pcm.length];
        float gain = Float.isFinite(outputGain) ? Math.max(0, Math.min(4, volume * outputGain)) : volume;
        for (int i = 0; i < pcm.length - 1; i += 2) {
            short sample = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xFF));
            int scaled = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample * gain)));
            out[i] = (byte) scaled;
            out[i + 1] = (byte) (scaled >> 8);
        }
        return out;
    }

    private static SourceDataLine openSpeakerLine() {
        try {
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, FORMAT));
            line.open(FORMAT);
            line.start();
            return line;
        } catch (LineUnavailableException | IllegalArgumentException e) {
            ProximiChat.LOGGER.error("[ProximiChat] Cannot open speaker line", e);
            return null;
        }
    }

    public static void toggleMute() { muted = !muted; }
    public static boolean isMuted() { return muted; }
    public static boolean isConnected() { return connected; }
}
