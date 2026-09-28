package com.proximichat.client;

import com.proximichat.ProximiChat;
import com.proximichat.network.GroupListPayload;
import com.proximichat.network.PacketRegistry;
import com.proximichat.network.VoiceDataPayload;
import com.proximichat.network.VoiceStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class ProximiChatClient implements ClientModInitializer {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.of(ProximiChat.MOD_ID, "voice"));

    public static KeyMapping pushToTalkKey;
    public static KeyMapping muteKey;
    public static KeyMapping voiceSettingsKey;

    @Override
    public void onInitializeClient() {
        // Register S2C payload types on the client
        PacketRegistry.registerClientbound();

        // Key bindings
        pushToTalkKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.proximichat.pushtotalk",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_V,
                CATEGORY
        ));
        muteKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.proximichat.mute",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_M,
                CATEGORY
        ));
        voiceSettingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.proximichat.settings",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN,
                CATEGORY
        ));

        // Tick handler — PTT detection & audio capture
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Toggle mute
            while (muteKey.consumeClick()) {
                VoiceClientHandler.toggleMute();
            }

            // Open settings screen
            while (voiceSettingsKey.consumeClick()) {
                client.gui.setScreen(new VoiceSettingsScreen(null));
            }

            VoiceClientHandler.tick(client);
        });

        // HUD overlay (speaking / muted icons), drawn just before the chat layer
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.of(ProximiChat.MOD_ID, "voice_overlay"),
                (graphics, tickCounter) -> VoiceHudRenderer.render(graphics));

        // Networking: receive voice data from server
        ClientPlayNetworking.registerGlobalReceiver(VoiceDataPayload.TYPE, VoiceClientHandler::onVoiceData);
        ClientPlayNetworking.registerGlobalReceiver(VoiceStatePayload.TYPE, VoiceHudRenderer::onVoiceState);
        ClientPlayNetworking.registerGlobalReceiver(GroupListPayload.TYPE, VoiceGroupScreen::onGroupList);

        // On join: announce we have the mod
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                VoiceClientHandler.onJoin());

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                VoiceClientHandler.onDisconnect());
    }
}
