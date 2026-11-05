package com.proximichat.client;

import com.proximichat.config.ProximiChatConfig;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VoiceSettingsScreen extends Screen {
    private final Screen parent;
    public VoiceSettingsScreen(Screen parent) { super(Component.translatable("screen.proximichat.settings")); this.parent = parent; }
    private static String onOff(boolean enabled) { return enabled ? "ON" : "OFF"; }

    @Override protected void init() {
        ProximiChatConfig cfg = ProximiChatConfig.get();
        int x = width / 2, y = height / 4;
        addRenderableWidget(Button.builder(Component.literal("Mode: " + (cfg.pushToTalk ? "Push-to-Talk" : "Voice Activation")), button -> {
            cfg.pushToTalk = !cfg.pushToTalk;
            button.setMessage(Component.literal("Mode: " + (cfg.pushToTalk ? "Push-to-Talk" : "Voice Activation")));
            ProximiChatConfig.save();
        }).bounds(x - 100, y, 200, 20).build());
        addRenderableWidget(new AbstractSliderButton(x - 100, y + 30, 200, 20,
                Component.literal("Mic Volume: " + (int)(cfg.inputGain * 100) + "%"), Math.max(0, Math.min(1, cfg.inputGain))) {
            @Override protected void updateMessage() { setMessage(Component.literal("Mic Volume: " + (int)(value * 100) + "%")); }
            @Override protected void applyValue() { cfg.inputGain = (float)value; ProximiChatConfig.save(); }
        });
        addRenderableWidget(new AbstractSliderButton(x - 100, y + 60, 200, 20,
                Component.literal("Speaker Volume: " + (int)(cfg.outputGain * 100) + "%"), Math.max(0, Math.min(1, cfg.outputGain))) {
            @Override protected void updateMessage() { setMessage(Component.literal("Speaker Volume: " + (int)(value * 100) + "%")); }
            @Override protected void applyValue() { cfg.outputGain = (float)value; ProximiChatConfig.save(); }
        });
        addRenderableWidget(Button.builder(Component.literal("Noise Suppression: " + onOff(cfg.enableNoiseSuppression)), button -> {
            cfg.enableNoiseSuppression = !cfg.enableNoiseSuppression;
            button.setMessage(Component.literal("Noise Suppression: " + onOff(cfg.enableNoiseSuppression)));
            ProximiChatConfig.save();
        }).bounds(x - 100, y + 90, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Player Icons: " + onOff(cfg.showPlayerIcons)), button -> {
            cfg.showPlayerIcons = !cfg.showPlayerIcons;
            button.setMessage(Component.literal("Player Icons: " + onOff(cfg.showPlayerIcons)));
            ProximiChatConfig.save();
        }).bounds(x - 100, y + 120, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Voice Groups..."), button -> minecraft.setScreen(new VoiceGroupScreen(this)))
                .bounds(x - 100, y + 155, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> minecraft.setScreen(parent))
                .bounds(x - 75, y + 190, 150, 20).build());
    }
}
