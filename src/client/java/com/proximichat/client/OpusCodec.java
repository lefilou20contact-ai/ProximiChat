package com.proximichat.client;

import com.proximichat.ProximiChat;
import com.proximichat.config.ProximiChatConfig;

/**
 * Abstraction layer over Opus encode/decode.
 *
 * ── Integration guide ────────────────────────────────────────────────────────
 * Add one of these to your build.gradle dependencies:
 *
 *   // Option A — Concentus (pure Java, no native libs needed):
 *   implementation 'org.concentus:Concentus:1.0.2'
 *
 *   // Option B — opus-java (JNI, ships native libs for win/mac/linux):
 *   implementation 'de.maxhenkel.opus4j:opus4j:1.0.4'
 *
 * Then replace the stub bodies below with the real encoder/decoder calls.
 * ────────────────────────────────────────────────────────────────────────────
 */
public class OpusCodec {

    private static boolean initialized = false;

    // Concentus encoder/decoder would be kept here as static fields:
    // private static OpusEncoder encoder;
    // private static OpusDecoder decoder;

    static {
        try {
            initCodec();
        } catch (Exception e) {
            ProximiChat.LOGGER.error("[ProximiChat] Opus init failed — voice chat disabled", e);
        }
    }

    private static void initCodec() throws Exception {
        ProximiChatConfig cfg = ProximiChatConfig.get();
        /*
         * With Concentus:
         *
         *   encoder = OpusEncoder.create(cfg.sampleRate, 1, OpusApplication.OPUS_APPLICATION_VOIP);
         *   encoder.setBitrate(32000);
         *   encoder.setComplexity(5);
         *   decoder = OpusDecoder.create(cfg.sampleRate, 1);
         */
        initialized = true; // remove once real codec is wired
        ProximiChat.LOGGER.info("[ProximiChat] Opus codec ready (stub mode).");
    }

    /**
     * Encode a PCM buffer (16-bit signed LE mono) to Opus bytes.
     * @param pcm   raw PCM samples
     * @param length number of bytes to read from pcm
     * @return Opus-encoded packet, or null on error
     */
    public static byte[] encode(byte[] pcm, int length) {
        if (!initialized) return null;
        try {
            /*
             * With Concentus:
             *   int frameSize = length / 2; // 16-bit samples
             *   short[] samples = new short[frameSize];
             *   ByteBuffer.wrap(pcm, 0, length).order(ByteOrder.LITTLE_ENDIAN)
             *              .asShortBuffer().get(samples);
             *   byte[] output = new byte[4000];
             *   int encodedLen = encoder.encode(samples, 0, frameSize, output, 0, output.length);
             *   return Arrays.copyOf(output, encodedLen);
             */
            // STUB: pass raw PCM through (no compression) for testing
            return java.util.Arrays.copyOf(pcm, length);
        } catch (Exception e) {
            ProximiChat.LOGGER.error("[ProximiChat] Encode error", e);
            return null;
        }
    }

    /**
     * Decode an Opus packet back to PCM.
     * @param opus Opus-encoded bytes
     * @return decoded PCM (16-bit signed LE mono), or null on error
     */
    public static byte[] decode(byte[] opus) {
        if (!initialized || opus == null) return null;
        try {
            /*
             * With Concentus:
             *   short[] pcmShorts = new short[ProximiChatConfig.get().frameSize];
             *   int decoded = decoder.decode(opus, 0, opus.length, pcmShorts, 0,
             *                                pcmShorts.length, false);
             *   byte[] pcm = new byte[decoded * 2];
             *   ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
             *              .asShortBuffer().put(pcmShorts, 0, decoded);
             *   return pcm;
             */
            // STUB: pass-through
            return opus;
        } catch (Exception e) {
            ProximiChat.LOGGER.error("[ProximiChat] Decode error", e);
            return null;
        }
    }
}
