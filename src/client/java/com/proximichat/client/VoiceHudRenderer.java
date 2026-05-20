package com.proximichat.client;

import com.proximichat.config.ProximiChatConfig;
import com.proximichat.network.VoiceStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws a small microphone icon above players who are currently speaking,
 * and a muted icon on the local player when they are muted.
 */
public class VoiceHudRenderer {

    private static final Identifier SPEAKING_ICON =
            Identifier.of("proximichat", "textures/gui/speaking.png");
    private static final Identifier MUTED_ICON =
            Identifier.of("proximichat", "textures/gui/muted.png");

    /** UUID → timestamp of last voice packet (for fade-out) */
    private static final Map<UUID, Long> speakingTimestamps = new ConcurrentHashMap<>();
    private static final long ICON_LINGER_MS = 300;

    // ── S2C packet handler ────────────────────────────────────────────────────

    public static void onVoiceState(VoiceStatePayload payload,
                                    ClientPlayNetworking.Context ctx) {
        if (payload.speaking()) {
            speakingTimestamps.put(payload.player(), System.currentTimeMillis());
        } else {
            speakingTimestamps.remove(payload.player());
        }
    }

    // ── HUD render ────────────────────────────────────────────────────────────

    public static void render(DrawContext context) {
        if (!ProximiChatConfig.get().showPlayerIcons) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        // Mute indicator for local player (bottom left)
        if (VoiceClientHandler.isMuted()) {
            context.drawTexture(MUTED_ICON, 4, screenH - 20, 0, 0, 16, 16, 16, 16);
        }

        // Speaking indicator for self (bottom left, green mic)
        if (!VoiceClientHandler.isMuted() && ProximiChatClient.pushToTalkKey.isPressed()) {
            context.drawTexture(SPEAKING_ICON, 4, screenH - 20, 0, 0, 16, 16, 16, 16);
        }

        // Clean up stale entries
        speakingTimestamps.entrySet().removeIf(e -> now - e.getValue() > ICON_LINGER_MS);
    }
}
