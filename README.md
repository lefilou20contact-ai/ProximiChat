# ProximiChat — Fabric Voice Chat Mod

A proximity voice chat mod for Minecraft 1.20–1.21.x (Fabric).  
Inspired by Simple Voice Chat. Fully open-source.

---

## Features

| Feature | Status |
|---|---|
| Push-to-talk | ✅ |
| Voice activation (VAD) | ✅ |
| Proximity-based volume | ✅ |
| Whisper mode | ✅ |
| Per-player mute | ✅ |
| Private voice groups | ✅ |
| Speaking overlay icons | ✅ |
| In-game settings screen | ✅ |
| Opus audio codec | ⚠️ stub — wire up below |

---

## Project Structure

```
src/main/java/com/proximichat/
├── ProximiChat.java              ← Mod entrypoint
├── config/
│   └── ProximiChatConfig.java   ← Server + client defaults
├── network/
│   ├── PacketRegistry.java      ← All packet registrations
│   ├── VoiceDataPayload.java    ← Audio packet
│   └── Payloads.java            ← All other packets
├── server/
│   ├── ProximiChatServer.java   ← Server entrypoint
│   ├── VoiceServerHandler.java  ← Route audio between players
│   └── VoiceGroupManager.java  ← Private groups
└── client/
    ├── ProximiChatClient.java   ← Client entrypoint + keybindings
    ├── VoiceClientHandler.java  ← Mic capture, send, receive, playback
    ├── OpusCodec.java           ← Encode/decode (stub — see below)
    ├── VoiceHudRenderer.java    ← Speaking icons HUD
    ├── VoiceSettingsScreen.java ← Settings GUI
    └── VoiceGroupScreen.java    ← Group management GUI
```

---

## ⚠️ Wiring Up Opus (required for real use)

The `OpusCodec` class ships as a **stub** that passes PCM through uncompressed.  
For production quality audio, add one of these to `build.gradle`:

### Option A — Concentus (pure Java, zero native dependencies)
```gradle
dependencies {
    implementation 'org.concentus:Concentus:1.0.2'
}
```
Then in `OpusCodec.java`, uncomment the Concentus code blocks.

### Option B — opus4j (JNI, best performance)
```gradle
repositories {
    maven { url "https://maven.maxhenkel.de/repository/public" }
}
dependencies {
    implementation 'de.maxhenkel.opus4j:opus4j:1.0.4'
}
```

---

## Building

```bash
# macOS / Linux
./gradlew build

# Windows
gradlew.bat build
```

Output: `build/libs/proximichat-1.0.0.jar`

Requires: Java 21+

---

## Configuration

`config/proximichat.json` (auto-generated on first run):

```json
{
  "voicePort": 25565,
  "proximityDistance": 32.0,
  "whisperDistance": 6.0,
  "allowGroups": true,
  "pushToTalk": true,
  "inputGain": 1.0,
  "outputGain": 1.0,
  "sampleRate": 48000,
  "frameSize": 960,
  "enableNoiseSuppression": true,
  "showPlayerIcons": true
}
```

---

## Key Bindings (default)

| Action | Default key |
|---|---|
| Push to Talk | `V` |
| Toggle Mute | `M` |
| Voice Settings | *(unbound)* |

---

## Installing (players)

1. Install [Fabric Loader](https://fabricmc.net/) for your MC version
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Drop `proximichat-1.0.0.jar` in your `mods/` folder
4. **Both client and server** need the mod installed

---

## License

MIT
