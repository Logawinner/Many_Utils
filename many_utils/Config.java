package me.anchorhelper.many_utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class Config {
    public boolean showFps = true;
    public boolean showCpu = true;
    public boolean showGpu = true;
    public boolean showCpuName = true;
    public boolean showGpuName = true;
    public boolean showCpuTemp = true;
    public boolean showGpuTemp = true;
    public boolean showMemory = false;
    public boolean showIrlTime = false;
    public boolean showGameTime = false;
    public boolean showPing = false;
    public boolean showCoords = false;
    public boolean showTps = false;
    public boolean showBiome = false;
    public boolean showChunkCoords = false;
    public boolean showEntityCount = false;
    public boolean showDirection = false;
    public boolean showLightLevel = false;
    public boolean showInGameDay = false;
    public boolean showWorldAge = false;
    public boolean showBattery = false;

    public boolean advancedFpsStats = false;
    public boolean advancedPingStats = false;
    public boolean advancedTpsStats = false;

    public boolean timeFormat12Hour = false;

    public boolean colorizeCpuUsage = true;
    public boolean colorizeGpuUsage = true;
    public boolean colorizeCpuTemp = true;
    public boolean colorizeGpuTemp = true;
    public boolean colorizeFps = true;
    public boolean colorizePing = true;
    public boolean colorizeTps = true;
    public boolean colorizeCoords = false;
    public boolean colorizeMemory = false;
    public boolean colorizeVram = false;
    public boolean colorizeIrlTime = false;
    public boolean colorizeGameTime = false;
    public boolean colorizeBiome = false;
    public boolean colorizeDirection = false;
    public boolean colorizeEntityCount = false;
    public boolean colorizeLightLevel = false;

    public boolean animateFps = false;
    public boolean animateCpuName = false;
    public boolean animateGpuName = false;

    public int x = 6;
    public int y = 6;
    public double scale = 1.0;
    public int color = 0xFFFFFF;
    public boolean shadow = true;
    public boolean moveMode = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = FabricLoader.getInstance().getConfigDir().resolve("many_utils.json").toFile();
    public static Config INSTANCE = new Config();

    public static void load() {
        try {
            if (FILE.exists()) {
                try (FileReader r = new FileReader(FILE)) {
                    Config loaded = GSON.fromJson(r, Config.class);
                    if (loaded != null) {
                        INSTANCE = loaded;
                    }
                }
            } else save();
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            FILE.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(FILE)) {
                GSON.toJson(INSTANCE, w);
            }
        } catch (Exception ignored) {}
    }
}
