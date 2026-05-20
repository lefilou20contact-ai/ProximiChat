package com.proximichat.network;

import com.proximichat.ProximiChat;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record GroupListPayload(List<String> groupIds) implements CustomPayload {

    public static final CustomPayload.Id<GroupListPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ProximiChat.MOD_ID, "group_list"));

    public static final PacketCodec<PacketByteBuf, GroupListPayload> CODEC =
            PacketCodec.of(
                    (p, buf) -> {
                        buf.writeVarInt(p.groupIds().size());
                        p.groupIds().forEach(g -> buf.writeString(g, 64));
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        List<String> list = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) list.add(buf.readString(64));
                        return new GroupListPayload(list);
                    }
            );

    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
