package com.proximichat.client;

import com.proximichat.network.PacketRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ProximiChatClient implements ClientModInitializer {

    public static KeyBinding pushToTalkKey;
    public static KeyBinding muteKey;
    public static KeyBinding voiceSettingsKey;

    @Override
    public void onInitializeClient() {
        // Register S2C packets on client
        PacketRegistry.registerClientbound();

        // Key bindings
        pushToTalkKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.proximichat.pushtotalk",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.proximichat"
        ));
        muteKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.proximichat.mute",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.proximichat"
        ));
        voiceSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.proximichat.settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.proximichat"
        ));

        // Tick handler — PTT detection & audio capture
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Toggle mute
            while (muteKey.wasPressed()) {
                VoiceClientHandler.toggleMute();
            }

            // Open settings screen
            while (voiceSettingsKey.wasPressed()) {
                client.setScreen(new VoiceSettingsScreen(null));
            }

            VoiceClientHandler.tick(client);
        });

        // HUD overlay (speaking icons)
        HudRenderCallback.EVENT.register((context, tickDelta) ->
                VoiceHudRenderer.render(context));

        // Networking: receive voice data from server
        ClientPlayNetworking.registerGlobalReceiver(
                com.proximichat.network.VoiceDataPayload.ID,
                VoiceClientHandler::onVoiceData);

        ClientPlayNetworking.registerGlobalReceiver(
                com.proximichat.network.VoiceStatePayload.ID,
                VoiceHudRenderer::onVoiceState);

        ClientPlayNetworking.registerGlobalReceiver(
                com.proximichat.network.GroupListPayload.ID,
                VoiceGroupScreen::onGroupList);

        // On join: announce we have the mod
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                VoiceClientHandler.onJoin());

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                VoiceClientHandler.onDisconnect());
    }
}
