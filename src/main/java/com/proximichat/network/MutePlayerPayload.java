package com.proximichat.network;

import com.proximichat.ProximiChat;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record MutePlayerPayload(UUID target, boolean muted) implements CustomPayload {

    public static final CustomPayload.Id<MutePlayerPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ProximiChat.MOD_ID, "mute_player"));

    public static final PacketCodec<PacketByteBuf, MutePlayerPayload> CODEC =
            PacketCodec.of(
                    (p, buf) -> { buf.writeUuid(p.target()); buf.writeBoolean(p.muted()); },
                    buf -> new MutePlayerPayload(buf.readUuid(), buf.readBoolean())
            );

    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
