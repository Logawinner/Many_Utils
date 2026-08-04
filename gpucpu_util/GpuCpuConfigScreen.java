package me.anchorhelper.gpucpu_util;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.Screen;

public class GpuCpuConfigScreen {
    public static Screen create(Screen parent) {
        Config cfg = Config.INSTANCE;
        ConfigBuilder b = ConfigBuilder.create().setParentScreen(parent).setTitle(Text.literal("GpuCpu Util"));
        b.setSavingRunnable(Config::save);
        ConfigEntryBuilder e = b.entryBuilder();

        ConfigCategory template = b.getOrCreateCategory(Text.literal("Template"));
        template.addEntry(e.startTextDescription(Text.literal("Template file: config/gpucpu_util.txt")).build());
        template.addEntry(e.startTextDescription(Text.literal("Edit the file with any text editor, then use Reload Template (in-game: /gpucpu_reload) or restart.")).build());

        ConfigCategory anim = b.getOrCreateCategory(Text.literal("Animated"));
        anim.addEntry(e.startBooleanToggle(Text.literal("Animate FPS Text"), cfg.animateFps).setSaveConsumer(v -> cfg.animateFps = v).build());
        anim.addEntry(e.startBooleanToggle(Text.literal("Animate CPU Name"), cfg.animateCpuName).setSaveConsumer(v -> cfg.animateCpuName = v).build());
        anim.addEntry(e.startBooleanToggle(Text.literal("Animate GPU Name"), cfg.animateGpuName).setSaveConsumer(v -> cfg.animateGpuName = v).build());

        ConfigCategory toggles = b.getOrCreateCategory(Text.literal("Toggles"));
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show FPS"), cfg.showFps).setSaveConsumer(v -> cfg.showFps = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show CPU"), cfg.showCpu).setSaveConsumer(v -> cfg.showCpu = v).build());
        toggles.addEntry(e.startBooleanToggle(Text.literal("Show CPU Name"), cfg.showCpuName).setSaveConsumer(v -> cfg.showCpuName = v).build());
        // Only show CPU temp toggle if CPU temp is supported
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
        toggles.addEntry(e.startBooleanToggle(Text.literal("Text Shadow"), cfg.shadow).setSaveConsumer(v -> cfg.shadow = v).build());

        ConfigCategory colors = b.getOrCreateCategory(Text.literal("Colored %/C"));
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize CPU Usage"), cfg.colorizeCpuUsage).setSaveConsumer(v -> cfg.colorizeCpuUsage = v).build());
        if (CpuTempMonitor.isSupported()) {
            colors.addEntry(e.startBooleanToggle(Text.literal("Colorize CPU Temp"), cfg.colorizeCpuTemp).setSaveConsumer(v -> cfg.colorizeCpuTemp = v).build());
        }
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize GPU Usage"), cfg.colorizeGpuUsage).setSaveConsumer(v -> cfg.colorizeGpuUsage = v).build());
        colors.addEntry(e.startBooleanToggle(Text.literal("Colorize GPU Temp"), cfg.colorizeGpuTemp).setSaveConsumer(v -> cfg.colorizeGpuTemp = v).build());

        ConfigCategory display = b.getOrCreateCategory(Text.literal("Display"));
        display.addEntry(e.startIntField(Text.literal("X (right padding)"), cfg.x).setMin(0).setSaveConsumer(v -> cfg.x = v).build());
        display.addEntry(e.startIntField(Text.literal("Y (top padding)"), cfg.y).setMin(0).setSaveConsumer(v -> cfg.y = v).build());
        display.addEntry(e.startDoubleField(Text.literal("Scale"), cfg.scale).setMin(0.5).setMax(4.0).setSaveConsumer(v -> cfg.scale = v).build());
        display.addEntry(e.startColorField(Text.literal("Base Color"), cfg.color).setSaveConsumer(v -> cfg.color = v).build());

        return b.build();
    }
}
