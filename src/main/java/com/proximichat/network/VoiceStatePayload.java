package com.proximichat.network;

import com.proximichat.ProximiChat;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record VoiceStatePayload(UUID player, boolean speaking) implements CustomPayload {

    public static final CustomPayload.Id<VoiceStatePayload> ID =
            new CustomPayload.Id<>(Identifier.of(ProximiChat.MOD_ID, "voice_state"));

    public static final PacketCodec<PacketByteBuf, VoiceStatePayload> CODEC =
            PacketCodec.of(
                    (p, buf) -> { buf.writeUuid(p.player()); buf.writeBoolean(p.speaking()); },
                    buf -> new VoiceStatePayload(buf.readUuid(), buf.readBoolean())
            );

    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
