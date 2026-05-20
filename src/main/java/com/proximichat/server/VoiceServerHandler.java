package com.proximichat.server;

import com.proximichat.config.ProximiChatConfig;
import com.proximichat.network.ClientConnectedPayload;
import com.proximichat.network.MutePlayerPayload;
import com.proximichat.network.VoiceDataPayload;
import com.proximichat.network.VoiceStatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central server-side voice routing.
 * When a player sends voice data, this class finds all players within
 * proximityDistance and forwards the Opus frame to each of them.
 */
public class VoiceServerHandler {

    /** Players that have confirmed they have the mod installed. */
    private static final Set<UUID> connectedClients =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    /** Per-player mute lists: muterUUID → set of muted UUIDs.
     *  These are server-side admin mutes; client-side mutes are handled locally. */
    private static final Map<UUID, Set<UUID>> muteLists = new ConcurrentHashMap<>();

    // ── Packet handlers ───────────────────────────────────────────────────────

    public static void onClientConnected(ClientConnectedPayload payload,
                                         ServerPlayNetworking.Context ctx) {
        UUID uuid = ctx.player().getUuid();
        connectedClients.add(uuid);
    }

    public static void onVoiceData(VoiceDataPayload payload,
                                   ServerPlayNetworking.Context ctx) {
        ServerPlayerEntity sender = ctx.player();
        UUID senderUuid = sender.getUuid();

        if (!connectedClients.contains(senderUuid)) return;

        double maxDist = payload.whisper()
                ? ProximiChatConfig.get().whisperDistance
                : ProximiChatConfig.get().proximityDistance;

        Vec3d senderPos = sender.getPos();

        // Broadcast to nearby players on the same dimension
        VoiceDataPayload outPayload = new VoiceDataPayload(senderUuid, payload.opusData(), payload.whisper());

        sender.getServerWorld().getPlayers().forEach(target -> {
            if (target.getUuid().equals(senderUuid)) return; // don't echo to self
            if (!connectedClients.contains(target.getUuid())) return;
            if (isMuted(senderUuid, target.getUuid())) return;

            // Check if target is in the same private group — if so, skip proximity check
            Optional<String> senderGroup = VoiceGroupManager.getGroupOf(senderUuid);
            Optional<String> targetGroup = VoiceGroupManager.getGroupOf(target.getUuid());
            boolean inSameGroup = senderGroup.isPresent() && senderGroup.equals(targetGroup);

            if (!inSameGroup) {
                double dist = target.getPos().distanceTo(senderPos);
                if (dist > maxDist) return;
            }

            ServerPlayNetworking.send(target, outPayload);
        });

        // Broadcast speaking state to all connected clients for overlay icons
        broadcastVoiceState(sender, true);
    }

    public static void onMutePlayer(MutePlayerPayload payload,
                                    ServerPlayNetworking.Context ctx) {
        UUID muter = ctx.player().getUuid();
        Set<UUID> muted = muteLists.computeIfAbsent(muter,
                k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
        if (payload.muted()) {
            muted.add(payload.target());
        } else {
            muted.remove(payload.target());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isMuted(UUID speaker, UUID listener) {
        Set<UUID> muted = muteLists.get(listener);
        return muted != null && muted.contains(speaker);
    }

    private static void broadcastVoiceState(ServerPlayerEntity speaker, boolean speaking) {
        VoiceStatePayload state = new VoiceStatePayload(speaker.getUuid(), speaking);
        speaker.getServerWorld().getPlayers().forEach(p -> {
            if (connectedClients.contains(p.getUuid())) {
                ServerPlayNetworking.send(p, state);
            }
        });
    }

    public static void playerDisconnected(UUID uuid) {
        connectedClients.remove(uuid);
        muteLists.remove(uuid);
    }

    public static boolean isConnected(UUID uuid) {
        return connectedClients.contains(uuid);
    }
}
