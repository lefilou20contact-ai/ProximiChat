package com.proximichat.network;

import com.proximichat.ProximiChat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record GroupListPayload(List<String> groupIds) implements CustomPacketPayload {

    private static final int MAX_GROUP_ID_LENGTH = 64;

    public static final CustomPacketPayload.Type<GroupListPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.of(ProximiChat.MOD_ID, "group_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GroupListPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.groupIds().size());
                payload.groupIds().forEach(g -> buf.writeUtf(g, MAX_GROUP_ID_LENGTH));
            },
            buf -> {
                int size = buf.readVarInt();
                List<String> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) list.add(buf.readUtf(MAX_GROUP_ID_LENGTH));
                return new GroupListPayload(list);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
