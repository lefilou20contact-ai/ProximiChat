package com.proximichat.network;

import com.proximichat.ProximiChat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** Carries a single encoded voice frame. */
public record VoiceDataPayload(UUID senderUuid, byte[] opusData, boolean whisper) implements CustomPacketPayload {

    public static final int MAX_FRAME_BYTES = 4096;
    public static final CustomPacketPayload.Type<VoiceDataPayload> TYPE =
            new CustomPacketPayload.Type<>(ProximiChat.id("voice_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceDataPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                if (payload.opusData().length > MAX_FRAME_BYTES) {
                    throw new IllegalArgumentException("Voice frame exceeds maximum size");
                }
                buf.writeUUID(payload.senderUuid());
                buf.writeByteArray(payload.opusData());
                buf.writeBoolean(payload.whisper());
            },
            buf -> new VoiceDataPayload(buf.readUUID(), buf.readByteArray(MAX_FRAME_BYTES), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
