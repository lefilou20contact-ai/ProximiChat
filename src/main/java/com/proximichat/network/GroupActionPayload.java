package com.proximichat.network;

import com.proximichat.ProximiChat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record GroupActionPayload(Action action, String groupId) implements CustomPacketPayload {

    public enum Action { CREATE, JOIN, LEAVE }

    private static final int MAX_GROUP_ID_LENGTH = 64;

    public static final CustomPacketPayload.Type<GroupActionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.of(ProximiChat.MOD_ID, "group_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GroupActionPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.action().ordinal());
                buf.writeUtf(payload.groupId(), MAX_GROUP_ID_LENGTH);
            },
            buf -> new GroupActionPayload(
                    Action.values()[buf.readVarInt()],
                    buf.readUtf(MAX_GROUP_ID_LENGTH)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
