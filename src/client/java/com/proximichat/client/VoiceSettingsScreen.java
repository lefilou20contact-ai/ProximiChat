package com.proximichat.client;

import com.proximichat.config.ProximiChatConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * In-game settings screen for ProximiChat.
 * Accessible via the key binding (default: unbound).
 */
public class VoiceSettingsScreen extends Screen {

    private final Screen parent;

    public VoiceSettingsScreen(Screen parent) {
        super(Component.translatable("screen.proximichat.settings"));
        this.parent = parent;
    }

    private static String onOff(boolean b) { return b ? "ON" : "OFF"; }

    @Override
    protected void init() {
        ProximiChatConfig cfg = ProximiChatConfig.get();
        int centerX = this.width / 2;
        int startY = this.height / 4;

        // Push-to-Talk toggle
        addRenderableWidget(Button.builder(
                Component.literal("Mode: " + (cfg.pushToTalk ? "Push-to-Talk" : "Voice Activation")),
                btn -> {
                    cfg.pushToTalk = !cfg.pushToTalk;
                    btn.setMessage(Component.literal("Mode: " + (cfg.pushToTalk ? "Push-to-Talk" : "Voice Activation")));
                    ProximiChatConfig.save();
                }
        ).bounds(centerX - 100, startY, 200, 20).build());

        // Input gain slider
        addRenderableWidget(new AbstractSliderButton(centerX - 100, startY + 30, 200, 20,
                Component.literal("Mic Volume: " + (int) (cfg.inputGain * 100) + "%"),
                cfg.inputGain) {
            @Override protected void updateMessage() {
                setMessage(Component.literal("Mic Volume: " + (int) (this.value * 100) + "%"));
            }
            @Override protected void applyValue() {
                cfg.inputGain = (float) this.value;
                ProximiChatConfig.save();
            }
        });

        // Output gain slider
        addRenderableWidget(new AbstractSliderButton(centerX - 100, startY + 60, 200, 20,
                Component.literal("Speaker Volume: " + (int) (cfg.outputGain * 100) + "%"),
                cfg.outputGain) {
            @Override protected void updateMessage() {
                setMessage(Component.literal("Speaker Volume: " + (int) (this.value * 100) + "%"));
            }
            @Override protected void applyValue() {
                cfg.outputGain = (float) this.value;
                ProximiChatConfig.save();
            }
        });

        // Noise suppression toggle
        addRenderableWidget(Button.builder(
                Component.literal("Noise Suppression: " + onOff(cfg.enableNoiseSuppression)),
                btn -> {
                    cfg.enableNoiseSuppression = !cfg.enableNoiseSuppression;
                    btn.setMessage(Component.literal("Noise Suppression: " + onOff(cfg.enableNoiseSuppression)));
                    ProximiChatConfig.save();
                }
        ).bounds(centerX - 100, startY + 90, 200, 20).build());

        // Show player icons toggle
        addRenderableWidget(Button.builder(
                Component.literal("Player Icons: " + onOff(cfg.showPlayerIcons)),
                btn -> {
                    cfg.showPlayerIcons = !cfg.showPlayerIcons;
                    btn.setMessage(Component.literal("Player Icons: " + onOff(cfg.showPlayerIcons)));
                    ProximiChatConfig.save();
                }
        ).bounds(centerX - 100, startY + 120, 200, 20).build());

        // Open group screen
        addRenderableWidget(Button.builder(
                Component.literal("Voice Groups..."),
                btn -> this.minecraft.gui.setScreen(new VoiceGroupScreen(this))
        ).bounds(centerX - 100, startY + 155, 200, 20).build());

        // Done
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                btn -> this.minecraft.gui.setScreen(parent)
        ).bounds(centerX - 75, startY + 190, 150, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        String title = this.title.getString();
        graphics.text(this.font, title,
                this.width / 2 - this.font.width(title) / 2,
                this.height / 4 - 20, 0xFFFFFFFF, true);
    }
}
