package me.anchorhelper.gpucpu_util;

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

    public boolean colorizeCpuUsage = true;
    public boolean colorizeGpuUsage = true;
    public boolean colorizeCpuTemp = true;
    public boolean colorizeGpuTemp = true;

    public boolean animateFps = false;
    public boolean animateCpuName = false;
    public boolean animateGpuName = false;

    public int x = 6;
    public int y = 6;
    public double scale = 1.0;
    public int color = 0xFFFFFF;
    public boolean shadow = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = FabricLoader.getInstance().getConfigDir().resolve("gpucpu_util.json").toFile();
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
