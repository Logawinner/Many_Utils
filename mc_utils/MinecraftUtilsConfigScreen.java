package me.anchorhelper.mc_utils;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.Screen;

public class MinecraftUtilsConfigScreen {
    public static Screen create(Screen parent) {
        Config cfg = Config.INSTANCE;
        ConfigBuilder b = ConfigBuilder.create().setParentScreen(parent).setTitle(Text.literal("Minecraft Utils"));
        b.setSavingRunnable(Config::save);
        ConfigEntryBuilder e = b.entryBuilder();

        ConfigCategory template = b.getOrCreateCategory(Text.literal("Template"));
        template.addEntry(e.startTextDescription(Text.literal("Template file: config/minecraft_utils.txt")).build());
        template.addEntry(e.startTextDescription(Text.literal("Edit the file with any text editor, then use Reload Template (in-game: /mc_utils_reload) or restart.")).build());

        ConfigCategory anim = b.getOrCreateCategory(Text.literal("Animated"));
        anim.addEntry(e.startBooleanToggle(Text.literal("Animate FPS Text"), cfg.animateFps).setSaveConsumer(v -> cfg.animateFps = v).build());
        anim.addEntry(e.startBooleanToggle(Text.literal("Animate CPU Name"), cfg.animateCpuName).setSaveConsumer(v -> cfg.animateCpuName = v).build());
        anim.addEntry(e.startBooleanToggle(Text.literal("Animate GPU Name"), cfg.animateGpuName).setSaveConsumer(v -> cfg.animateGpuName = v).build());

        ConfigCategory toggles = b.getOrCreateCategory(Text.literal("Toggles"));
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show FPS"), cfg.showFps).setSaveConsumer(v -> cfg.showFps = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show CPU"), cfg.showCpu).setSaveConsumer(v -> cfg.showCpu = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show CPU Name"), cfg.showCpuName).setSaveConsumer(v -> cfg.showCpuName = v).build());
        if (CpuTempMonitor.isSupported()) {
            toggles.addEntry(e.startBooleanToggle(Text.literal("Show CPU Temp"), cfg.showCpuTemp).setSaveConsumer(v -> cfg.showCpuTemp = v).build());
        } else {
            toggles.addEntry(e.startBooleanToggle(Text.literal("Show CPU Temp (Not supported on this system)"), false)
                .setSaveConsumer(v -> {})
                .setDefaultValue(false)
                .build());
        }
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show GPU"), cfg.showGpu).setSaveConsumer(v -> cfg.showGpu = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show GPU Name"), cfg.showGpuName).setSaveConsumer(v -> cfg.showGpuName = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show GPU Temp"), cfg.showGpuTemp).setSaveConsumer(v -> cfg.showGpuTemp = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show Memory (RAM/VRAM)"), cfg.showMemory).setSaveConsumer(v -> cfg.showMemory = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show IRL Time"), cfg.showIrlTime).setSaveConsumer(v -> cfg.showIrlTime = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("12-Hour Time Format"), cfg.timeFormat12Hour).setSaveConsumer(v -> cfg.timeFormat12Hour = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show Game Time"), cfg.showGameTime).setSaveConsumer(v -> cfg.showGameTime = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show Ping"), cfg.showPing).setSaveConsumer(v -> cfg.showPing = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show Coords"), cfg.showCoords).setSaveConsumer(v -> cfg.showCoords = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show TPS"), cfg.showTps).setSaveConsumer(v -> cfg.showTps = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show Biome"), cfg.showBiome).setSaveConsumer(v -> cfg.showBiome = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show Chunk Coords"), cfg.showChunkCoords).setSaveConsumer(v -> cfg.showChunkCoords = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show Entity Count"), cfg.showEntityCount).setSaveConsumer(v -> cfg.showEntityCount = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show Direction"), cfg.showDirection).setSaveConsumer(v -> cfg.showDirection = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show Light Level"), cfg.showLightLevel).setSaveConsumer(v -> cfg.showLightLevel = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show In-Game Day"), cfg.showInGameDay).setSaveConsumer(v -> cfg.showInGameDay = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show World Age"), cfg.showWorldAge).setSaveConsumer(v -> cfg.showWorldAge = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show Bat"), cfg.showBattery).setSaveConsumer(v -> cfg.showBattery = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Text Shadow"), cfg.shadow).setSaveConsumer(v -> cfg.shadow = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Move Mode (Drag to reposition HUD)"), cfg.moveMode).setSaveConsumer(v -> cfg.moveMode = v).build());

        ConfigCategory colors = b.getOrCreateCategory(Text.literal("Colored Stats"));
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize CPU Usage"), cfg.colorizeCpuUsage).setSaveConsumer(v -> cfg.colorizeCpuUsage = v).build());
        if (CpuTempMonitor.isSupported()) {
            colors.addEntry(e.startBooleanToggle(Text.literal("Colorize CPU Temp"), cfg.colorizeCpuTemp).setSaveConsumer(v -> cfg.colorizeCpuTemp = v).build());
        }
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize GPU Usage"), cfg.colorizeGpuUsage).setSaveConsumer(v -> cfg.colorizeGpuUsage = v).build());
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize GPU Temp"), cfg.colorizeGpuTemp).setSaveConsumer(v -> cfg.colorizeGpuTemp = v).build());
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize FPS"), cfg.colorizeFps).setSaveConsumer(v -> cfg.colorizeFps = v).build());
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize Ping"), cfg.colorizePing).setSaveConsumer(v -> cfg.colorizePing = v).build());
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize TPS"), cfg.colorizeTps).setSaveConsumer(v -> cfg.colorizeTps = v).build());
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize Coords"), cfg.colorizeCoords).setSaveConsumer(v -> cfg.colorizeCoords = v).build());
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize RAM"), cfg.colorizeMemory).setSaveConsumer(v -> cfg.colorizeMemory = v).build());
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize VRAM"), cfg.colorizeVram).setSaveConsumer(v -> cfg.colorizeVram = v).build());
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize IRL Time"), cfg.colorizeIrlTime).setSaveConsumer(v -> cfg.colorizeIrlTime = v).build());
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize Game Time"), cfg.colorizeGameTime).setSaveConsumer(v -> cfg.colorizeGameTime = v).build());
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize Biome"), cfg.colorizeBiome).setSaveConsumer(v -> cfg.colorizeBiome = v).build());
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize Direction"), cfg.colorizeDirection).setSaveConsumer(v -> cfg.colorizeDirection = v).build());
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize Entity Count"), cfg.colorizeEntityCount).setSaveConsumer(v -> cfg.colorizeEntityCount = v).build());
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize Light Level"), cfg.colorizeLightLevel).setSaveConsumer(v -> cfg.colorizeLightLevel = v).build());

        ConfigCategory advanced = b.getOrCreateCategory(Text.literal("Advanced Stats"));
        advanced.addEntry(e.startBooleanToggle(Text.literal("Advanced FPS Stats (1/5/15 min)"), cfg.advancedFpsStats).setSaveConsumer(v -> cfg.advancedFpsStats = v).build());
        advanced.addEntry(e.startBooleanToggle(Text.literal("Advanced Ping Stats"), cfg.advancedPingStats).setSaveConsumer(v -> cfg.advancedPingStats = v).build());
        advanced.addEntry(e.startBooleanToggle(Text.literal("Advanced TPS Stats"), cfg.advancedTpsStats).setSaveConsumer(v -> cfg.advancedTpsStats = v).build());

        ConfigCategory display = b.getOrCreateCategory(Text.literal("Display"));
        display.addEntry(e.startIntField(Text.literal("X (left padding)"), cfg.x).setMin(0).setSaveConsumer(v -> cfg.x = v).build());
        display.addEntry(e.startIntField(Text.literal("Y (top padding)"), cfg.y).setMin(0).setSaveConsumer(v -> cfg.y = v).build());
        display.addEntry(e.startDoubleField(Text.literal("Scale"), cfg.scale).setMin(0.5).setMax(4.0).setSaveConsumer(v -> cfg.scale = v).build());
        display.addEntry(e.startColorField(Text.literal("Base Color"), cfg.color).setSaveConsumer(v -> cfg.color = v).build());

        return b.build();
    }
}
