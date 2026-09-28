package com.proximichat.network;

import com.proximichat.ProximiChat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record VoiceStatePayload(UUID player, boolean speaking) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<VoiceStatePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.of(ProximiChat.MOD_ID, "voice_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceStatePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.player());
                buf.writeBoolean(payload.speaking());
            },
            buf -> new VoiceStatePayload(buf.readUUID(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
