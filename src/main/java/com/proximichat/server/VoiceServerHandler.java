package com.proximichat.server;

import com.proximichat.config.ProximiChatConfig;
import com.proximichat.network.ClientConnectedPayload;
import com.proximichat.network.MutePlayerPayload;
import com.proximichat.network.VoiceDataPayload;
import com.proximichat.network.VoiceStatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class VoiceServerHandler {
    private static final Set<UUID> connectedClients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<UUID, Set<UUID>> muteLists = new ConcurrentHashMap<>();
    private VoiceServerHandler() { }

    public static void onClientConnected(ClientConnectedPayload payload, ServerPlayNetworking.Context ctx) {
        connectedClients.add(ctx.player().getUUID());
        VoiceGroupManager.syncGroupList(ctx.server());
    }

    public static void onVoiceData(VoiceDataPayload payload, ServerPlayNetworking.Context ctx) {
        ServerPlayer sender = ctx.player();
        UUID senderUuid = sender.getUUID();
        byte[] audio = payload.opusData();
        if (!connectedClients.contains(senderUuid) || audio.length == 0
                || audio.length > VoiceDataPayload.MAX_FRAME_BYTES) return;

        ProximiChatConfig cfg = ProximiChatConfig.get();
        double maxDist = payload.whisper() ? cfg.whisperDistance : cfg.proximityDistance;
        if (!Double.isFinite(maxDist) || maxDist <= 0) return;
        Vec3 senderPos = sender.position();
        Optional<String> senderGroup = VoiceGroupManager.getGroupOf(senderUuid);
        VoiceDataPayload forwarded = new VoiceDataPayload(senderUuid, audio, payload.whisper());

        sender.level().players().forEach(target -> {
            UUID targetId = target.getUUID();
            if (targetId.equals(senderUuid) || !connectedClients.contains(targetId) || isMuted(senderUuid, targetId)) return;
            Optional<String> targetGroup = VoiceGroupManager.getGroupOf(targetId);
            boolean sameGroup = senderGroup.isPresent() && senderGroup.equals(targetGroup);
            if (!sameGroup && target.position().distanceTo(senderPos) > maxDist) return;
            ServerPlayNetworking.send(target, forwarded);
        });
        broadcastVoiceState(sender, true);
    }

    public static void onMutePlayer(MutePlayerPayload payload, ServerPlayNetworking.Context ctx) {
        UUID listener = ctx.player().getUUID();
        Set<UUID> muted = muteLists.computeIfAbsent(listener, k -> ConcurrentHashMap.newKeySet());
        if (payload.muted()) muted.add(payload.target());
        else muted.remove(payload.target());
    }

    private static boolean isMuted(UUID speaker, UUID listener) {
        Set<UUID> muted = muteLists.get(listener);
        return muted != null && muted.contains(speaker);
    }

    private static void broadcastVoiceState(ServerPlayer speaker, boolean speaking) {
        VoiceStatePayload state = new VoiceStatePayload(speaker.getUUID(), speaking);
        speaker.level().players().forEach(player -> {
            if (connectedClients.contains(player.getUUID())) ServerPlayNetworking.send(player, state);
        });
    }

    public static void playerDisconnected(UUID player, MinecraftServer server) {
        connectedClients.remove(player);
        muteLists.remove(player);
        muteLists.values().forEach(muted -> muted.remove(player));
        VoiceGroupManager.playerDisconnected(player, server);
    }

    public static boolean isConnected(UUID uuid) { return connectedClients.contains(uuid); }
}
