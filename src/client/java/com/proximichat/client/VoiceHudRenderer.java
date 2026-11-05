package com.proximichat.client;

import com.proximichat.config.ProximiChatConfig;
import com.proximichat.network.VoiceStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceHudRenderer {
    private static final int MUTED_COLOR = 0xFFFF5555;
    private static final int SPEAKING_COLOR = 0xFF55FF55;
    private static final Map<UUID, Long> speakingTimestamps = new ConcurrentHashMap<>();
    private static final long ICON_LINGER_MS = 300;

    public static void onVoiceState(VoiceStatePayload payload, ClientPlayNetworking.Context ctx) {
        if (payload.speaking()) speakingTimestamps.put(payload.player(), System.currentTimeMillis());
        else speakingTimestamps.remove(payload.player());
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (!ProximiChatConfig.get().showPlayerIcons) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        int screenH = mc.getWindow().getGuiScaledHeight();
        if (VoiceClientHandler.isMuted()) graphics.fill(4, screenH - 20, 12, screenH - 12, MUTED_COLOR);
        else if (ProximiChatClient.pushToTalkKey != null && ProximiChatClient.pushToTalkKey.isDown()) {
            graphics.fill(4, screenH - 20, 12, screenH - 12, SPEAKING_COLOR);
        }
        long now = System.currentTimeMillis();
        speakingTimestamps.entrySet().removeIf(entry -> now - entry.getValue() > ICON_LINGER_MS);
    }
}
