package com.proximichat;

import com.proximichat.config.ProximiChatConfig;
import com.proximichat.network.PacketRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProximiChat implements ModInitializer {
    public static final String MOD_ID = "proximichat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }

    @Override public void onInitialize() {
        LOGGER.info("[ProximiChat] Initializing...");
        ProximiChatConfig.load();
        PacketRegistry.registerServerbound();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> LOGGER.info("[ProximiChat] Server started."));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LOGGER.info("[ProximiChat] Server stopped."));
    }
}
