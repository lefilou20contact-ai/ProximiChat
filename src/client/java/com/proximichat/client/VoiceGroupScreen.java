package com.proximichat.client;

import com.proximichat.network.GroupActionPayload;
import com.proximichat.network.GroupListPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen that lets players create, join, and leave voice groups.
 */
public class VoiceGroupScreen extends Screen {

    private final Screen parent;
    private static List<String> knownGroups = new ArrayList<>();
    private static String currentGroup = null;

    private EditBox groupNameField;

    public VoiceGroupScreen(Screen parent) {
        super(Component.literal("Voice Groups"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 4;

        groupNameField = new EditBox(this.font,
                cx - 100, y, 200, 20, Component.literal("Group name"));
        groupNameField.setMaxLength(32);
        addRenderableWidget(groupNameField);

        // Create group
        addRenderableWidget(Button.builder(Component.literal("Create Group"), btn -> {
            String name = groupNameField.getValue().trim();
            if (!name.isEmpty()) {
                ClientPlayNetworking.send(new GroupActionPayload(GroupActionPayload.Action.CREATE, name));
                currentGroup = name;
            }
        }).bounds(cx - 100, y + 25, 95, 20).build());

        // Join group
        addRenderableWidget(Button.builder(Component.literal("Join Group"), btn -> {
            String name = groupNameField.getValue().trim();
            if (!name.isEmpty()) {
                ClientPlayNetworking.send(new GroupActionPayload(GroupActionPayload.Action.JOIN, name));
                currentGroup = name;
            }
        }).bounds(cx + 5, y + 25, 95, 20).build());

        // Leave group
        addRenderableWidget(Button.builder(Component.literal("Leave Group"), btn -> {
            ClientPlayNetworking.send(new GroupActionPayload(GroupActionPayload.Action.LEAVE, currentGroup != null ? currentGroup : ""));
            currentGroup = null;
        }).bounds(cx - 100, y + 50, 200, 20).build());

        // Done
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                btn -> this.minecraft.gui.setScreen(parent)
        ).bounds(cx - 75, y + 80, 150, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String title = this.title.getString();
        graphics.text(this.font, title,
                this.width / 2 - this.font.width(title) / 2,
                this.height / 4 - 20, 0xFFFFFFFF, true);

        // Draw group list
        int y = this.height / 4 + 110;
        graphics.text(this.font, "Active groups:", this.width / 2 - 100, y, 0xFFAAAAAA, true);
        for (String g : knownGroups) {
            y += 12;
            String label = g.equals(currentGroup) ? "> " + g : "  " + g;
            graphics.text(this.font, label, this.width / 2 - 100, y, 0xFFFFFFFF, true);
        }
    }

    // ── S2C: server sends updated group list ──────────────────────────────────

    public static void onGroupList(GroupListPayload payload,
                                   ClientPlayNetworking.Context ctx) {
        knownGroups = new ArrayList<>(payload.groupIds());
    }
}
