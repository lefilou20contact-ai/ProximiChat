package com.proximichat.client;

import com.proximichat.ProximiChat;
import com.proximichat.config.ProximiChatConfig;
import com.proximichat.network.ClientConnectedPayload;
import com.proximichat.network.VoiceDataPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import javax.sound.sampled.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles microphone capture → Opus encode → send,
 * and receive → Opus decode → spatial playback.
 *
 * NOTE: This implementation uses javax.sound for simplicity/portability.
 * For production, replace the encode/decode stubs with actual JNI Opus bindings
 * (e.g. concentus or opus-java).
 */
public class VoiceClientHandler {

    private static TargetDataLine microphone;
    private static boolean muted = false;
    private static boolean connected = false;

    /** Per-speaker audio output lines */
    private static final Map<UUID, SourceDataLine> speakers = new ConcurrentHashMap<>();

    private static final AudioFormat FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            48000f, 16, 1, 2, 48000f, false
    );

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public static void onJoin() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            if (!AudioSystem.isLineSupported(info)) {
                ProximiChat.LOGGER.warn("[ProximiChat] Microphone not supported.");
                return;
            }
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(FORMAT);
            microphone.start();
            connected = true;

            // Announce to server
            ClientPlayNetworking.send(new ClientConnectedPayload(1));
            ProximiChat.LOGGER.info("[ProximiChat] Microphone opened.");
        } catch (LineUnavailableException e) {
            ProximiChat.LOGGER.error("[ProximiChat] Cannot open microphone", e);
        }
    }

    public static void onDisconnect() {
        connected = false;
        if (microphone != null && microphone.isOpen()) {
            microphone.stop();
            microphone.close();
        }
        speakers.values().forEach(line -> {
            if (line.isOpen()) { line.stop(); line.close(); }
        });
        speakers.clear();
    }

    // ── Tick — PTT / voice-activation ────────────────────────────────────────

    public static void tick(MinecraftClient client) {
        if (!connected || microphone == null || muted) return;

        boolean shouldTransmit;
        if (ProximiChatConfig.get().pushToTalk) {
            shouldTransmit = ProximiChatClient.pushToTalkKey.isPressed();
        } else {
            // Voice-activation: check if the buffer has audio above threshold
            shouldTransmit = hasAudioActivity();
        }

        if (shouldTransmit) captureAndSend(client);
    }

    private static boolean hasAudioActivity() {
        if (microphone == null) return false;
        int available = microphone.available();
        if (available < FORMAT.getFrameSize()) return false;

        byte[] buf = new byte[Math.min(available, 960 * FORMAT.getFrameSize())];
        int read = microphone.read(buf, 0, buf.length);
        long rms = 0;
        for (int i = 0; i < read - 1; i += 2) {
            short sample = (short) ((buf[i + 1] << 8) | (buf[i] & 0xFF));
            rms += (long) sample * sample;
        }
        rms = (long) Math.sqrt((double) rms / (read / 2));
        return rms > 500; // simple threshold, make configurable
    }

    private static void captureAndSend(MinecraftClient client) {
        if (client.player == null) return;
        int frameBytes = ProximiChatConfig.get().frameSize * FORMAT.getFrameSize();
        byte[] pcm = new byte[frameBytes];
        int read = microphone.read(pcm, 0, frameBytes);
        if (read <= 0) return;

        byte[] encoded = OpusCodec.encode(pcm, read);
        if (encoded == null) return;

        UUID uuid = client.player.getUuid();
        boolean whisper = false; // TODO: add whisper key binding
        ClientPlayNetworking.send(new VoiceDataPayload(uuid, encoded, whisper));
    }

    // ── Receive audio from server ─────────────────────────────────────────────

    public static void onVoiceData(VoiceDataPayload payload,
                                   ClientPlayNetworking.Context ctx) {
        byte[] pcm = OpusCodec.decode(payload.opusData());
        if (pcm == null) return;

        UUID speaker = payload.senderUuid();
        SourceDataLine line = speakers.computeIfAbsent(speaker, k -> openSpeakerLine());
        if (line == null || !line.isOpen()) return;

        // Apply volume based on distance (proximity attenuation)
        float volume = computeVolume(speaker, ctx.client(), payload.whisper());
        byte[] attenuated = applyVolume(pcm, volume);

        line.write(attenuated, 0, attenuated.length);
    }

    private static float computeVolume(UUID speakerUuid, MinecraftClient client, boolean whisper) {
        if (client.player == null || client.world == null) return 0f;
        var speaker = client.world.getPlayers().stream()
                .filter(p -> p.getUuid().equals(speakerUuid))
                .findFirst().orElse(null);
        if (speaker == null) return 1f; // same group — full volume

        double dist = client.player.getPos().distanceTo(speaker.getPos());
        double max = whisper ? ProximiChatConfig.get().whisperDistance
                             : ProximiChatConfig.get().proximityDistance;
        return (float) Math.max(0, 1.0 - (dist / max));
    }

    private static byte[] applyVolume(byte[] pcm, float volume) {
        byte[] out = new byte[pcm.length];
        for (int i = 0; i < pcm.length - 1; i += 2) {
            short sample = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xFF));
            sample = (short) (sample * volume * ProximiChatConfig.get().outputGain);
            out[i]     = (byte) (sample & 0xFF);
            out[i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return out;
    }

    private static SourceDataLine openSpeakerLine() {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(FORMAT);
            line.start();
            return line;
        } catch (LineUnavailableException e) {
            ProximiChat.LOGGER.error("[ProximiChat] Cannot open speaker line", e);
            return null;
        }
    }

    // ── Misc ──────────────────────────────────────────────────────────────────

    public static void toggleMute() {
        muted = !muted;
        ProximiChat.LOGGER.info("[ProximiChat] Muted: {}", muted);
    }

    public static boolean isMuted() { return muted; }
    public static boolean isConnected() { return connected; }
}
