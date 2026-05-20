package com.proximichat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.proximichat.ProximiChat;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class ProximiChatConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("proximichat.json");

    private static ProximiChatConfig INSTANCE = new ProximiChatConfig();

    // ── Server settings ──────────────────────────────────────────────────────
    public int voicePort = 25565;          // UDP port for voice data
    public double proximityDistance = 32.0; // Max blocks for voice to be heard
    public double whisperDistance = 6.0;    // Distance for whisper mode
    public boolean allowGroups = true;      // Allow private voice groups

    // ── Client defaults (saved per-player on client side) ────────────────────
    public boolean pushToTalk = true;
    public float inputGain = 1.0f;
    public float outputGain = 1.0f;
    public int sampleRate = 48000;          // Hz
    public int frameSize = 960;             // Opus frame size (~20ms at 48kHz)
    public boolean enableNoiseSuppression = true;
    public boolean showPlayerIcons = true;  // Overlay icons when someone talks

    public static ProximiChatConfig get() {
        return INSTANCE;
    }

    public static void load() {
        File file = CONFIG_PATH.toFile();
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                INSTANCE = GSON.fromJson(reader, ProximiChatConfig.class);
                ProximiChat.LOGGER.info("[ProximiChat] Config loaded.");
            } catch (IOException e) {
                ProximiChat.LOGGER.error("[ProximiChat] Failed to load config", e);
            }
        } else {
            save(); // write defaults
        }
    }

    public static void save() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            ProximiChat.LOGGER.error("[ProximiChat] Failed to save config", e);
        }
    }
}
