package com.proximichat.network;

import com.proximichat.ProximiChat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record MutePlayerPayload(UUID target, boolean muted) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MutePlayerPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.of(ProximiChat.MOD_ID, "mute_player"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MutePlayerPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.target());
                buf.writeBoolean(payload.muted());
            },
            buf -> new MutePlayerPayload(buf.readUUID(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
