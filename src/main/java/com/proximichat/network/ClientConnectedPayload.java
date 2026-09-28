package com.proximichat.network;

import com.proximichat.ProximiChat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ClientConnectedPayload(int clientVersion) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientConnectedPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.of(ProximiChat.MOD_ID, "client_connected"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientConnectedPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.clientVersion()),
            buf -> new ClientConnectedPayload(buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
