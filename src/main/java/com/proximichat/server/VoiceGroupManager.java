package com.proximichat.server;

import com.proximichat.config.ProximiChatConfig;
import com.proximichat.network.GroupActionPayload;
import com.proximichat.network.GroupListPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Manages private voice groups. */
public final class VoiceGroupManager {
    private static final int MAX_GROUP_LENGTH = 32;
    private static final Map<String, Set<UUID>> groups = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerGroup = new ConcurrentHashMap<>();

    private VoiceGroupManager() { }

    public static void onGroupAction(GroupActionPayload payload, ServerPlayNetworking.Context ctx) {
        if (!ProximiChatConfig.get().allowGroups) return;
        ServerPlayer player = ctx.player();
        UUID uuid = player.getUUID();
        String groupId = payload.groupId().trim();
        if (groupId.isEmpty() || groupId.length() > MAX_GROUP_LENGTH
                || groupId.chars().anyMatch(Character::isISOControl)) return;
        MinecraftServer server = ctx.server();
        switch (payload.action()) {
            case CREATE -> createGroup(groupId, uuid, server);
            case JOIN -> joinGroup(groupId, uuid, server);
            case LEAVE -> leaveGroup(uuid, server);
        }
    }

    public static void createGroup(String groupId, UUID creator, MinecraftServer server) {
        if (groupId == null || groupId.isBlank() || groupId.length() > MAX_GROUP_LENGTH || server == null) return;
        leaveGroup(creator, server);
        Set<UUID> members = Collections.newSetFromMap(new ConcurrentHashMap<>());
        members.add(creator);
        Set<UUID> existing = groups.putIfAbsent(groupId, members);
        if (existing != null) return;
        playerGroup.put(creator, groupId);
        syncGroupList(server);
    }

    public static void joinGroup(String groupId, UUID player, MinecraftServer server) {
        Set<UUID> members = groups.get(groupId);
        if (members == null || server == null) return;
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
            if (members.isEmpty()) groups.remove(current, members);
        }
        syncGroupList(server);
    }

    public static Optional<String> getGroupOf(UUID player) { return Optional.ofNullable(playerGroup.get(player)); }
    public static List<String> getAllGroupIds() { return new ArrayList<>(groups.keySet()); }

    public static void syncGroupList(MinecraftServer server) {
        if (server == null) return;
        GroupListPayload payload = new GroupListPayload(getAllGroupIds());
        server.getPlayerList().getPlayers().forEach(p -> {
            if (VoiceServerHandler.isConnected(p.getUUID())) ServerPlayNetworking.send(p, payload);
        });
    }

    public static void playerDisconnected(UUID player, MinecraftServer server) { leaveGroup(player, server); }
}
