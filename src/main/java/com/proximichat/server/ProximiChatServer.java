package com.proximichat.server;

import com.proximichat.network.PacketRegistry;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class ProximiChatServer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        // Register S2C packet types so the dedicated server is able to send them
        PacketRegistry.registerClientbound();

        // Clean up when a player disconnects
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                VoiceServerHandler.playerDisconnected(handler.player.getUUID()));
    }
}
