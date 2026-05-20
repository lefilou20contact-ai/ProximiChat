package com.proximichat.network;

import com.proximichat.ProximiChat;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GroupActionPayload(Action action, String groupId) implements CustomPayload {

    public enum Action { CREATE, JOIN, LEAVE }

    public static final CustomPayload.Id<GroupActionPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ProximiChat.MOD_ID, "group_action"));

    public static final PacketCodec<PacketByteBuf, GroupActionPayload> CODEC =
            PacketCodec.of(
                    (p, buf) -> {
                        buf.writeEnumConstant(p.action());
                        buf.writeString(p.groupId(), 64);
                    },
                    buf -> new GroupActionPayload(
                            buf.readEnumConstant(Action.class),
                            buf.readString(64)
                    )
            );

    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
