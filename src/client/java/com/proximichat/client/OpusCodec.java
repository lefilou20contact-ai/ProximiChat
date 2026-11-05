package com.proximichat.client;

import com.proximichat.ProximiChat;
import org.concentus.OpusApplication;
import org.concentus.OpusDecoder;
import org.concentus.OpusEncoder;
import org.concentus.OpusException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Pure Java Opus codec. Encoder state is local; decoder state is per speaker. */
public final class OpusCodec {
    private static final int RATE = 48000;
    private static final int CHANNELS = 1;
    private static final int MAX_PACKET_BYTES = 1276;
    private static final int MAX_PACKET_SAMPLES = 5760;
    private static OpusEncoder encoder;
    private static final Map<UUID, OpusDecoder> decoders = new HashMap<>();

    private OpusCodec() { }

    private static OpusEncoder encoder() throws OpusException {
        if (encoder == null) {
            encoder = new OpusEncoder(RATE, CHANNELS, OpusApplication.OPUS_APPLICATION_VOIP);
            encoder.setBitrate(32000);
            encoder.setComplexity(5);
        }
        return encoder;
    }

    public static synchronized byte[] encode(byte[] pcm, int length) {
        if (pcm == null || length < 2 || length > pcm.length || (length & 1) != 0) return null;
        int frameSamples = length / 2;
        if (frameSamples != 120 && frameSamples != 240 && frameSamples != 480
                && frameSamples != 960 && frameSamples != 1920 && frameSamples != 2880) return null;
        try {
            byte[] out = new byte[MAX_PACKET_BYTES];
            int encoded = encoder().encode(pcm, 0, frameSamples, out, 0, out.length);
            return encoded > 0 ? Arrays.copyOf(out, encoded) : null;
        } catch (OpusException | RuntimeException e) {
            ProximiChat.LOGGER.error("[ProximiChat] Opus encoding failed", e);
            return null;
        }
    }

    public static synchronized byte[] decode(UUID speaker, byte[] packet) {
        if (speaker == null || packet == null || packet.length == 0 || packet.length > MAX_PACKET_BYTES) return null;
        try {
            OpusDecoder decoder = decoders.computeIfAbsent(speaker, id -> {
                try { return new OpusDecoder(RATE, CHANNELS); }
                catch (OpusException e) { throw new IllegalStateException(e); }
            });
            short[] samples = new short[MAX_PACKET_SAMPLES];
            int count = decoder.decode(packet, 0, packet.length, samples, 0, samples.length, false);
            if (count <= 0 || count > samples.length) return null;
            byte[] pcm = new byte[count * 2];
            for (int i = 0; i < count; i++) {
                pcm[2 * i] = (byte) samples[i];
                pcm[2 * i + 1] = (byte) (samples[i] >>> 8);
            }
            return pcm;
        } catch (OpusException | RuntimeException e) {
            ProximiChat.LOGGER.warn("[ProximiChat] Opus decoding failed for {}", speaker, e);
            return null;
        }
    }

    public static synchronized void forgetSpeaker(UUID speaker) { decoders.remove(speaker); }
    public static synchronized void reset() { encoder = null; decoders.clear(); }
}
