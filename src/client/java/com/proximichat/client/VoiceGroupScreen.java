package com.proximichat.client;

import com.proximichat.network.GroupActionPayload;
import com.proximichat.network.GroupListPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen that lets players create, join, and leave voice groups.
 */
public class VoiceGroupScreen extends Screen {

    private final Screen parent;
    private static List<String> knownGroups = new ArrayList<>();
    private static String currentGroup = null;

    private TextFieldWidget groupNameField;

    public VoiceGroupScreen(Screen parent) {
        super(Text.literal("Voice Groups"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 4;

        groupNameField = new TextFieldWidget(this.textRenderer,
                cx - 100, y, 200, 20, Text.literal("Group name"));
        groupNameField.setMaxLength(32);
        addDrawableChild(groupNameField);

        // Create group
        addDrawableChild(ButtonWidget.builder(Text.literal("Create Group"), btn -> {
            String name = groupNameField.getText().trim();
            if (!name.isEmpty()) {
                ClientPlayNetworking.send(new GroupActionPayload(GroupActionPayload.Action.CREATE, name));
                currentGroup = name;
            }
        }).dimensions(cx - 100, y + 25, 95, 20).build());

        // Join group
        addDrawableChild(ButtonWidget.builder(Text.literal("Join Group"), btn -> {
            String name = groupNameField.getText().trim();
            if (!name.isEmpty()) {
                ClientPlayNetworking.send(new GroupActionPayload(GroupActionPayload.Action.JOIN, name));
                currentGroup = name;
            }
        }).dimensions(cx + 5, y + 25, 95, 20).build());

        // Leave group
        addDrawableChild(ButtonWidget.builder(Text.literal("Leave Group"), btn -> {
            ClientPlayNetworking.send(new GroupActionPayload(GroupActionPayload.Action.LEAVE, currentGroup != null ? currentGroup : ""));
            currentGroup = null;
        }).dimensions(cx - 100, y + 50, 200, 20).build());

        // Done
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE,
                btn -> this.client.setScreen(parent)
        ).dimensions(cx - 75, y + 80, 150, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, this.height / 4 - 20, 0xFFFFFF);

        // Draw group list
        int y = this.height / 4 + 110;
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Active groups:"), this.width / 2 - 100, y, 0xAAAAAA);
        for (String g : knownGroups) {
            y += 12;
            String label = g.equals(currentGroup) ? "▶ " + g : "  " + g;
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal(label), this.width / 2 - 100, y, 0xFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    // ── S2C: server sends updated group list ──────────────────────────────────

    public static void onGroupList(GroupListPayload payload,
                                   ClientPlayNetworking.Context ctx) {
        knownGroups = new ArrayList<>(payload.groupIds());
    }
}
