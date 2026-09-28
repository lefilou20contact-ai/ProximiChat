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

/**
 * Handles microphone capture → Opus encode → send,
 * and receive → Opus decode → spatial playback.
 *
 * Mic capture happens on a dedicated background thread (started on join,
 * stopped on disconnect) rather than on the client render thread, since
 * TargetDataLine#read() blocks and would otherwise stall the game loop.
 *
 * NOTE: This implementation uses javax.sound for simplicity/portability.
 * For production, replace the encode/decode stubs with actual JNI Opus bindings
 * (e.g. concentus or opus-java).
 */
public class VoiceClientHandler {

    private static TargetDataLine microphone;
    private static Thread captureThread;
    private static volatile boolean muted = false;
    private static volatile boolean connected = false;

    /** Updated every client tick from the main thread; read by the capture thread. */
    private static final AtomicBoolean pushToTalkHeld = new AtomicBoolean(false);

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

            captureThread = new Thread(VoiceClientHandler::captureLoop, "ProximiChat Mic Capture");
            captureThread.setDaemon(true);
            captureThread.start();
        } catch (LineUnavailableException e) {
            ProximiChat.LOGGER.error("[ProximiChat] Cannot open microphone", e);
        }
    }

    public static void onDisconnect() {
        connected = false;

        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
        if (microphone != null && microphone.isOpen()) {
            microphone.stop();
            microphone.close(); // unblocks any in-progress read()
        }
        speakers.values().forEach(line -> {
            if (line.isOpen()) { line.stop(); line.close(); }
        });
        speakers.clear();
    }

    // ── Tick (main thread) — just samples key state, does no blocking I/O ──────

    public static void tick(Minecraft client) {
        if (!connected || muted) {
            pushToTalkHeld.set(false);
            return;
        }
        pushToTalkHeld.set(ProximiChatClient.pushToTalkKey.isDown());
    }

    // ── Capture loop (background thread) ───────────────────────────────────────

    private static void captureLoop() {
        while (connected && !Thread.currentThread().isInterrupted()) {
            UUID playerUuid = currentPlayerUuid();
            if (playerUuid == null) {
                sleepQuiet(20);
                continue;
            }

            int frameBytes = ProximiChatConfig.get().frameSize * FORMAT.getFrameSize();
            byte[] pcm = new byte[frameBytes];
            int read;
            try {
                read = microphone.read(pcm, 0, frameBytes); // blocks, but off the render thread
            } catch (Exception e) {
                break; // line was closed on disconnect
            }
            if (read <= 0) continue;
            if (muted) continue;

            boolean shouldTransmit = ProximiChatConfig.get().pushToTalk
                    ? pushToTalkHeld.get()
                    : hasAudioActivity(pcm, read);
            if (!shouldTransmit) continue;

            applyInputGain(pcm, read);
            byte[] encoded = OpusCodec.encode(pcm, read);
            if (encoded == null) continue;

            boolean whisper = false; // TODO: add whisper key binding
            ClientPlayNetworking.send(new VoiceDataPayload(playerUuid, encoded, whisper));
        }
    }

    private static void applyInputGain(byte[] pcm, int len) {
        float gain = ProximiChatConfig.get().inputGain;
        if (gain == 1.0f) return;
        for (int i = 0; i < len - 1; i += 2) {
            int sample = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xFF));
            sample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample * gain)));
            pcm[i] = (byte) (sample & 0xFF);
            pcm[i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
    }

    private static UUID currentPlayerUuid() {
        Minecraft client = Minecraft.getInstance();
        return client.player != null ? client.player.getUUID() : null;
    }

    private static void sleepQuiet(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean hasAudioActivity(byte[] buf, int read) {
        long rms = 0;
        for (int i = 0; i < read - 1; i += 2) {
            short sample = (short) ((buf[i + 1] << 8) | (buf[i] & 0xFF));
            rms += (long) sample * sample;
        }
        rms = (long) Math.sqrt((double) rms / (read / 2));
        return rms > 500; // simple threshold, make configurable
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

    private static float computeVolume(UUID speakerUuid, Minecraft client, boolean whisper) {
        if (client.player == null || client.level == null) return 0f;
        var speaker = client.level.players().stream()
                .filter(p -> p.getUUID().equals(speakerUuid))
                .findFirst().orElse(null);
        if (speaker == null) return 1f; // same group — full volume

        double dist = client.player.position().distanceTo(speaker.position());
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
