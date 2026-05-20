package com.proximichat.client;

import com.proximichat.config.ProximiChatConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * In-game settings screen for ProximiChat.
 * Accessible via the key binding (default: unbound).
 */
public class VoiceSettingsScreen extends Screen {

    private final Screen parent;

    public VoiceSettingsScreen(Screen parent) {
        super(Text.translatable("screen.proximichat.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ProximiChatConfig cfg = ProximiChatConfig.get();
        int centerX = this.width / 2;
        int startY = this.height / 4;

        // Push-to-Talk toggle
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Mode: " + (cfg.pushToTalk ? "Push-to-Talk" : "Voice Activation")),
                btn -> {
                    cfg.pushToTalk = !cfg.pushToTalk;
                    btn.setMessage(Text.literal("Mode: " + (cfg.pushToTalk ? "Push-to-Talk" : "Voice Activation")));
                    ProximiChatConfig.save();
                }
        ).dimensions(centerX - 100, startY, 200, 20).build());

        // Input gain slider
        addDrawableChild(new SliderWidget(centerX - 100, startY + 30, 200, 20,
                Text.literal("Mic Volume: " + (int)(cfg.inputGain * 100) + "%"),
                cfg.inputGain) {
            @Override protected void updateMessage() {
                setMessage(Text.literal("Mic Volume: " + (int)(this.value * 100) + "%"));
            }
            @Override protected void applyValue() {
                cfg.inputGain = (float) this.value;
                ProximiChatConfig.save();
            }
        });

        // Output gain slider
        addDrawableChild(new SliderWidget(centerX - 100, startY + 60, 200, 20,
                Text.literal("Speaker Volume: " + (int)(cfg.outputGain * 100) + "%"),
                cfg.outputGain) {
            @Override protected void updateMessage() {
                setMessage(Text.literal("Speaker Volume: " + (int)(this.value * 100) + "%"));
            }
            @Override protected void applyValue() {
                cfg.outputGain = (float) this.value;
                ProximiChatConfig.save();
            }
        });

        // Noise suppression toggle
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Noise Suppression: " + (cfg.enableNoiseSuppression ? "ON" : "OFF")),
                btn -> {
                    cfg.enableNoiseSuppression = !cfg.enableNoiseSuppression;
                    btn.setMessage(Text.literal("Noise Suppression: " + (cfg.enableNoiseSuppression ? "ON" : "OFF")));
                    ProximiChatConfig.save();
                }
        ).dimensions(centerX - 100, startY + 90, 200, 20).build());

        // Show player icons toggle
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Player Icons: " + (cfg.showPlayerIcons ? "ON" : "OFF")),
                btn -> {
                    cfg.showPlayerIcons = !cfg.showPlayerIcons;
                    btn.setMessage(Text.literal("Player Icons: " + (cfg.showPlayerIcons ? "ON" : "OFF")));
                    ProximiChatConfig.save();
                }
        ).dimensions(centerX - 100, startY + 120, 200, 20).build());

        // Open group screen
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Voice Groups..."),
                btn -> this.client.setScreen(new VoiceGroupScreen(this))
        ).dimensions(centerX - 100, startY + 155, 200, 20).build());

        // Done
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE,
                btn -> this.client.setScreen(parent)
        ).dimensions(centerX - 75, startY + 190, 150, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, this.height / 4 - 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}
