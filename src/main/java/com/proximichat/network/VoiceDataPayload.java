package com.proximichat.network;

import com.proximichat.ProximiChat;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Carries a chunk of Opus-encoded audio from one player to the server,
 * which then forwards it to nearby players.
 */
public record VoiceDataPayload(
        UUID senderUuid,
        byte[] opusData,
        boolean whisper
) implements CustomPayload {

    public static final CustomPayload.Id<VoiceDataPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ProximiChat.MOD_ID, "voice_data"));

    public static final PacketCodec<PacketByteBuf, VoiceDataPayload> CODEC =
            PacketCodec.of(VoiceDataPayload::write, VoiceDataPayload::read);

    private static VoiceDataPayload read(PacketByteBuf buf) {
        UUID uuid     = buf.readUuid();
        byte[] data   = buf.readByteArray(4096); // max ~4 KB per frame
        boolean wh    = buf.readBoolean();
        return new VoiceDataPayload(uuid, data, wh);
    }

    private void write(PacketByteBuf buf) {
        buf.writeUuid(senderUuid);
        buf.writeByteArray(opusData);
        buf.writeBoolean(whisper);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
