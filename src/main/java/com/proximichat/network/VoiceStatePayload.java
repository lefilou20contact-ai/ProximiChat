package com.proximichat.network;

import com.proximichat.ProximiChat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record VoiceStatePayload(UUID player, boolean speaking) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VoiceStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ProximiChat.id("voice_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceStatePayload> CODEC = StreamCodec.of(
            (buf, payload) -> { buf.writeUUID(payload.player()); buf.writeBoolean(payload.speaking()); },
            buf -> new VoiceStatePayload(buf.readUUID(), buf.readBoolean())
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
