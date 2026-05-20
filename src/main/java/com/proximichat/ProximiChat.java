package com.proximichat;

import com.proximichat.config.ProximiChatConfig;
import com.proximichat.network.PacketRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProximiChat implements ModInitializer {

    public static final String MOD_ID = "proximichat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[ProximiChat] Initializing...");

        // Load config
        ProximiChatConfig.load();

        // Register network packets
        PacketRegistry.registerServerbound();

        // Server lifecycle hooks
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("[ProximiChat] Server started — voice chat ready.");
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LOGGER.info("[ProximiChat] Server stopped.");
        });
    }
}
