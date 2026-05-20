package com.proximichat.server;

import com.proximichat.network.GroupActionPayload;
import com.proximichat.network.GroupListPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages private voice groups.
 * Players inside the same group can always hear each other
 * regardless of distance, as long as they're on the same server.
 */
public class VoiceGroupManager {

    /** groupId → set of member UUIDs */
    private static final Map<String, Set<UUID>> groups = new ConcurrentHashMap<>();

    /** playerUUID → groupId (for quick reverse lookup) */
    private static final Map<UUID, String> playerGroup = new ConcurrentHashMap<>();

    // ── Packet handler ────────────────────────────────────────────────────────

    public static void onGroupAction(GroupActionPayload payload,
                                     ServerPlayNetworking.Context ctx) {
        ServerPlayerEntity player = ctx.player();
        UUID uuid = player.getUuid();
        String groupId = payload.groupId().trim();

        switch (payload.action()) {
            case CREATE -> createGroup(groupId, uuid, player.getServer());
            case JOIN   -> joinGroup(groupId, uuid, player.getServer());
            case LEAVE  -> leaveGroup(uuid, player.getServer());
        }
    }

    // ── Group operations ──────────────────────────────────────────────────────

    public static void createGroup(String groupId, UUID creator, MinecraftServer server) {
        if (groupId.isEmpty() || groups.containsKey(groupId)) return;

        leaveGroup(creator, server); // leave any existing group first
        Set<UUID> members = Collections.newSetFromMap(new ConcurrentHashMap<>());
        members.add(creator);
        groups.put(groupId, members);
        playerGroup.put(creator, groupId);
        syncGroupList(server);
    }

    public static void joinGroup(String groupId, UUID player, MinecraftServer server) {
        Set<UUID> members = groups.get(groupId);
        if (members == null) return;

        leaveGroup(player, server);
        members.add(player);
        playerGroup.put(player, groupId);
        syncGroupList(server);
    }

    public static void leaveGroup(UUID player, MinecraftServer server) {
        String current = playerGroup.remove(player);
        if (current == null) return;

        Set<UUID> members = groups.get(current);
        if (members != null) {
            members.remove(player);
            if (members.isEmpty()) groups.remove(current);
        }
        if (server != null) syncGroupList(server);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public static Optional<String> getGroupOf(UUID player) {
        return Optional.ofNullable(playerGroup.get(player));
    }

    public static List<String> getAllGroupIds() {
        return new ArrayList<>(groups.keySet());
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    private static void syncGroupList(MinecraftServer server) {
        GroupListPayload payload = new GroupListPayload(getAllGroupIds());
        server.getPlayerManager().getPlayerList().forEach(p -> {
            if (VoiceServerHandler.isConnected(p.getUuid())) {
                ServerPlayNetworking.send(p, payload);
            }
        });
    }
}
