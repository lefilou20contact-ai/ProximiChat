package com.proximichat.network;

import com.proximichat.ProximiChat;
import com.proximichat.server.VoiceGroupManager;
import com.proximichat.server.VoiceServerHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class PacketRegistry {

    // ── Packet IDs ────────────────────────────────────────────────────────────
    public static final Identifier VOICE_DATA_ID        = id("voice_data");
    public static final Identifier CLIENT_CONNECTED_ID  = id("client_connected");
    public static final Identifier MUTE_PLAYER_ID       = id("mute_player");
    public static final Identifier CREATE_GROUP_ID      = id("create_group");
    public static final Identifier JOIN_GROUP_ID        = id("join_group");
    public static final Identifier LEAVE_GROUP_ID       = id("leave_group");
    public static final Identifier GROUP_LIST_S2C_ID    = id("group_list");
    public static final Identifier VOICE_STATE_S2C_ID   = id("voice_state");

    private static Identifier id(String path) {
        return Identifier.of(ProximiChat.MOD_ID, path);
    }

    /** Register packets received by the server (C2S). */
    public static void registerServerbound() {
        PayloadTypeRegistry.playC2S().register(VoiceDataPayload.ID, VoiceDataPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ClientConnectedPayload.ID, ClientConnectedPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(MutePlayerPayload.ID, MutePlayerPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GroupActionPayload.ID, GroupActionPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(VoiceDataPayload.ID,
                VoiceServerHandler::onVoiceData);

        ServerPlayNetworking.registerGlobalReceiver(ClientConnectedPayload.ID,
                VoiceServerHandler::onClientConnected);

        ServerPlayNetworking.registerGlobalReceiver(MutePlayerPayload.ID,
                VoiceServerHandler::onMutePlayer);

        ServerPlayNetworking.registerGlobalReceiver(GroupActionPayload.ID,
                VoiceGroupManager::onGroupAction);
    }

    /** Register packets received by the client (S2C). */
    public static void registerClientbound() {
        PayloadTypeRegistry.playS2C().register(VoiceDataPayload.ID, VoiceDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GroupListPayload.ID, GroupListPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(VoiceStatePayload.ID, VoiceStatePayload.CODEC);
    }
}
