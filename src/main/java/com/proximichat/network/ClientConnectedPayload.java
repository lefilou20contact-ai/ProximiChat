package com.proximichat.network;

import com.proximichat.ProximiChat;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ClientConnectedPayload(int clientVersion) implements CustomPayload {

    public static final CustomPayload.Id<ClientConnectedPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ProximiChat.MOD_ID, "client_connected"));

    public static final PacketCodec<PacketByteBuf, ClientConnectedPayload> CODEC =
            PacketCodec.of(
                    (p, buf) -> buf.writeVarInt(p.clientVersion()),
                    buf -> new ClientConnectedPayload(buf.readVarInt())
            );

    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
