package com.proximichat.network;

import com.proximichat.ProximiChat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * Carries a chunk of Opus-encoded audio from one player to the server,
 * which then forwards it to nearby players.
 */
public record VoiceDataPayload(
        UUID senderUuid,
        byte[] opusData,
        boolean whisper
) implements CustomPacketPayload {

    private static final int MAX_FRAME_BYTES = 4096; // ~4 KB per frame, generous headroom

    public static final CustomPacketPayload.Type<VoiceDataPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.of(ProximiChat.MOD_ID, "voice_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceDataPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.senderUuid());
                buf.writeByteArray(payload.opusData());
                buf.writeBoolean(payload.whisper());
            },
            buf -> new VoiceDataPayload(
                    buf.readUUID(),
                    buf.readByteArray(MAX_FRAME_BYTES),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
