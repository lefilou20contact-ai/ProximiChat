package com.proximichat.network;

import com.proximichat.server.VoiceGroupManager;
import com.proximichat.server.VoiceServerHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class PacketRegistry {

    /** Register payload types + handlers received by the server (C2S). */
    public static void registerServerbound() {
        PayloadTypeRegistry.serverboundPlay().register(VoiceDataPayload.TYPE, VoiceDataPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ClientConnectedPayload.TYPE, ClientConnectedPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MutePlayerPayload.TYPE, MutePlayerPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GroupActionPayload.TYPE, GroupActionPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(VoiceDataPayload.TYPE,
                VoiceServerHandler::onVoiceData);

        ServerPlayNetworking.registerGlobalReceiver(ClientConnectedPayload.TYPE,
                VoiceServerHandler::onClientConnected);

        ServerPlayNetworking.registerGlobalReceiver(MutePlayerPayload.TYPE,
                VoiceServerHandler::onMutePlayer);

        ServerPlayNetworking.registerGlobalReceiver(GroupActionPayload.TYPE,
                VoiceGroupManager::onGroupAction);
    }

    /** Register payload types received by the client (S2C). Must also be
     *  called on the dedicated server so it is able to *send* them. */
    public static void registerClientbound() {
        PayloadTypeRegistry.clientboundPlay().register(VoiceDataPayload.TYPE, VoiceDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GroupListPayload.TYPE, GroupListPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VoiceStatePayload.TYPE, VoiceStatePayload.CODEC);
    }
}
