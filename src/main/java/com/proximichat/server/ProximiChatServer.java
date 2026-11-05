package com.proximichat.server;

import com.proximichat.network.PacketRegistry;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class ProximiChatServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        PacketRegistry.registerClientbound();
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                VoiceServerHandler.playerDisconnected(handler.player.getUUID(), server));
    }
}
