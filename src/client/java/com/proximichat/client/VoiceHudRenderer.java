package com.proximichat.client;

import com.proximichat.config.ProximiChatConfig;
import com.proximichat.network.VoiceStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws a small indicator above/below the hotbar when the local player is
 * muted or speaking.
 *
 * The original 1.21.x version drew custom PNG icons (textures/gui/speaking.png,
 * textures/gui/muted.png) but those texture files were never actually shipped
 * in resources/, so nothing rendered but the "missing texture" placeholder.
 * This version draws simple colored squares instead so it always renders
 * something, with no missing assets required.
 */
public class VoiceHudRenderer {

    private static final int MUTED_COLOR    = 0xFFFF5555; // red
    private static final int SPEAKING_COLOR = 0xFF55FF55; // green

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

    // ── HUD render (called from HudElementRegistry, see ProximiChatClient) ─────

    public static void render(GuiGraphicsExtractor graphics) {
        if (!ProximiChatConfig.get().showPlayerIcons) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        long now = System.currentTimeMillis();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // Mute indicator for local player (bottom left)
        if (VoiceClientHandler.isMuted()) {
            graphics.fill(4, screenH - 20, 12, screenH - 12, MUTED_COLOR);
        } else if (ProximiChatClient.pushToTalkKey.isDown()) {
            // Speaking indicator for self (bottom left, green square)
            graphics.fill(4, screenH - 20, 12, screenH - 12, SPEAKING_COLOR);
        }

        // Clean up stale entries
        speakingTimestamps.entrySet().removeIf(e -> now - e.getValue() > ICON_LINGER_MS);
    }
}
