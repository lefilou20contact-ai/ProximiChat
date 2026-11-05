package com.proximichat.client;

import com.proximichat.network.GroupActionPayload;
import com.proximichat.network.GroupListPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class VoiceGroupScreen extends Screen {
    private final Screen parent;
    private static List<String> knownGroups = new ArrayList<>();
    private static String currentGroup;
    private EditBox groupNameField;

    public VoiceGroupScreen(Screen parent) { super(Component.literal("Voice Groups")); this.parent = parent; }

    @Override protected void init() {
        int cx = width / 2, y = height / 4;
        groupNameField = new EditBox(font, cx - 100, y, 200, 20, Component.literal("Group name"));
        groupNameField.setMaxLength(32);
        addRenderableWidget(groupNameField);
        addRenderableWidget(Button.builder(Component.literal("Create Group"), button -> sendAction(GroupActionPayload.Action.CREATE))
                .bounds(cx - 100, y + 25, 95, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Join Group"), button -> sendAction(GroupActionPayload.Action.JOIN))
                .bounds(cx + 5, y + 25, 95, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Leave Group"), button -> {
            ClientPlayNetworking.send(new GroupActionPayload(GroupActionPayload.Action.LEAVE, ""));
            currentGroup = null;
        }).bounds(cx - 100, y + 50, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> minecraft.setScreen(parent))
                .bounds(cx - 75, y + 80, 150, 20).build());
    }

    private void sendAction(GroupActionPayload.Action action) {
        String name = groupNameField.getValue().trim();
        if (!name.isEmpty() && name.length() <= 32) {
            ClientPlayNetworking.send(new GroupActionPayload(action, name));
            currentGroup = name;
        }
    }

    @Override public void extractRenderState(net.fabricmc.fabric.api.client.rendering.v1.hud.GuiGraphicsExtractor graphics,
                                              int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        String title = this.title.getString();
        graphics.text(font, title, width / 2 - font.width(title) / 2, height / 4 - 20, 0xFFFFFFFF, true);
        int listY = height / 4 + 110;
        graphics.text(font, "Active groups:", width / 2 - 100, listY, 0xFFAAAAAA, true);
        for (String group : knownGroups) {
            listY += 12;
            graphics.text(font, group.equals(currentGroup) ? "> " + group : "  " + group,
                    width / 2 - 100, listY, 0xFFFFFFFF, true);
        }
    }

    public static void onGroupList(GroupListPayload payload, ClientPlayNetworking.Context ctx) {
        knownGroups = new ArrayList<>(payload.groupIds());
        if (currentGroup != null && !knownGroups.contains(currentGroup)) currentGroup = null;
    }
}
