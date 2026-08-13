package me.anchorhelper.many_utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Matrix3x2fStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.lwjgl.glfw.GLFW;

public class Hud {
    private static final int GPU_LIGHT_DEFAULT = 0x86EFAC;
    private static final int GPU_DARK_DEFAULT = 0x16A34A;
    private static final int CPU_RED = 0xEF4444;
    private static final int CPU_ORANGE = 0xF59E0B;
    private static final int INTEL_LIGHT = 0x9440B1;
    private static final int INTEL_DARK = 0x1E0D06;
    private static final int NVIDIA_LIGHT = 0xAA4C10;
    private static final int NVIDIA_DARK = 0x164B04;
    private static final int AMD_LIGHT = 0xEF4444;
    private static final int AMD_DARK = 0xF59E0B;
    private static final int FPS_YELLOW = 0xFFCC00;
    private static final int FPS_GOLD = 0xFFD700;
    private static final int BATTERY_C1 = 0x00FF00;
    private static final int BATTERY_C2 = 0xFF0000;
    private static final int MEMORY_C1 = 0x00BFFF;
    private static final int MEMORY_C2 = 0xFF4500;
    private static final int VRAM_C1 = 0xFF69B4;
    private static final int VRAM_C2 = 0x00FF00;
    private static final int IRL_TIME_C1 = 0xFFFFFF;
    private static final int IRL_TIME_C2 = 0x000000;
    private static final int GAME_TIME_C1 = 0xFFA500;
    private static final int GAME_TIME_C2 = 0x0000FF;
    private static final int PING_C1 = 0x00FF00;
    private static final int PING_C2 = 0xFF00FF;
    private static final int COORDS_C1 = 0xFF5555;
    private static final int COORDS_C2 = 0x00FF00;
    private static final int TPS_C1 = 0x00FF00;
    private static final int TPS_C2 = 0x0000FF;
    private static final int CPS_C1 = 0xFF69B4;
    private static final int CPS_C2 = 0x00FFFF;
    private static final int BIOME_C1 = 0x00FF00;
    private static final int BIOME_C2 = 0xFF0000;
    private static final int CHUNK_C1 = 0xFF5555;
    private static final int CHUNK_C2 = 0x00FF00;
    private static final int ENTITY_C1 = 0x00FF00;
    private static final int ENTITY_C2 = 0xFF0000;
    private static final int DIRECTION_C1 = 0x00BFFF;
    private static final int DIRECTION_C2 = 0xFF4500;
    private static final int LIGHT_C1 = 0xFFFF00;
    private static final int LIGHT_C2 = 0x0000FF;
    private static final int DAY_C1 = 0xFFA500;
    private static final int DAY_C2 = 0x0000FF;
    private static final int AGE_C1 = 0xFFFFFF;
    private static final int AGE_C2 = 0x000000;
    private static final long WAVE_PERIOD_MS = 4000;
    private static final double PHASE_STEP = Math.PI / 10.0;

    public static void draw(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) {
            return;
        }
        if (mc.options.hudHidden) {
            return;
        }

        HudTemplate.ensureDefault();
        List<HudTemplate.Line> lines;
        if (HudTemplate.lines().isEmpty()) {
            HudTemplate.reload();
        }
        if ((lines = HudTemplate.lines()).isEmpty()) {
            return;
        }

        TextRenderer tr = mc.textRenderer;
        String cpuNameRaw = Hud.safe(Metrics.cpuName(), "CPU");
        String gpuNameRaw = Hud.safe(Metrics.gpuName(), "GPU");
        int fpsVal = mc.getCurrentFps();
        int cpuUseVal = Hud.clamp0_100(Math.round(Metrics.cpu()));
        int gpuUseVal = Metrics.gpu();
        int gpuTempVal = Metrics.gpuTemp();
        int cpuTempVal = Metrics.cpuTemp();
        String gpuVendor = Hud.detectGpuVendor(gpuNameRaw);
        String cpuVendor = Hud.detectCpuVendor(cpuNameRaw);
        boolean integrated = Hud.detectIntegratedGpu(gpuNameRaw);

        int cpuC1 = CPU_RED;
        int cpuC2 = CPU_ORANGE;
        int gpuC1 = GPU_LIGHT_DEFAULT;
        int gpuC2 = GPU_DARK_DEFAULT;

        if (cpuVendor.equals("intel")) {
            cpuC1 = INTEL_LIGHT;
            cpuC2 = INTEL_DARK;
        } else if (cpuVendor.equals("amd")) {
            cpuC1 = AMD_LIGHT;
            cpuC2 = AMD_DARK;
        }

        if (gpuVendor.equals("intel")) {
            gpuC1 = INTEL_LIGHT;
            gpuC2 = INTEL_DARK;
        } else if (gpuVendor.equals("amd")) {
            gpuC1 = AMD_LIGHT;
            gpuC2 = AMD_DARK;
        } else if (gpuVendor.equals("nvidia")) {
            gpuC1 = NVIDIA_LIGHT;
            gpuC2 = NVIDIA_DARK;
        }

        Map<String, String> vars = new java.util.HashMap<>();
        // Section labels - padded to 9 chars for alignment
        boolean cpuSectionEnabled = Config.INSTANCE.showCpu || Config.INSTANCE.showCpuTemp;
        boolean gpuSectionEnabled = Config.INSTANCE.showGpu || Config.INSTANCE.showGpuTemp;
        boolean fpsSectionEnabled = Config.INSTANCE.showFps;

        vars.put("cpu_label", cpuSectionEnabled ? "CPU:" : "");
        vars.put("gpu_label", gpuSectionEnabled ? "GPU:" : "");
        vars.put("fps_label", fpsSectionEnabled ? "FPS:" : "");
        
        // Memory labels - only show if memory data is available
        long ramTotalBytes = Metrics.ramTotal();
        long vramTotalBytes = Metrics.vramTotal();
        boolean memoryDataAvailable = ramTotalBytes > 0 || vramTotalBytes > 0;
        vars.put("memory_label", Config.INSTANCE.showMemory && memoryDataAvailable ? "RAM:" : "");
        vars.put("vram_label", Config.INSTANCE.showMemory && memoryDataAvailable ? "VRAM:" : "");
        
        vars.put("time_irl_label", Config.INSTANCE.showIrlTime ? "IRL:" : "");
        vars.put("time_game_label", Config.INSTANCE.showGameTime ? "Game:" : "");
        vars.put("ping_label", Config.INSTANCE.showPing ? "Ping:" : "");
        vars.put("coords_label", Config.INSTANCE.showCoords ? "XYZ:" : "");
        vars.put("biome_label", Config.INSTANCE.showBiome ? "Biome:" : "");
        vars.put("chunk_label", Config.INSTANCE.showChunkCoords ? "Chunk XYZ:" : "");
        vars.put("entity_label", Config.INSTANCE.showEntityCount ? "Entities:" : "");
        vars.put("direction_label", Config.INSTANCE.showDirection ? "Dir:" : "");
        vars.put("light_label", Config.INSTANCE.showLightLevel ? "Light:" : "");
        vars.put("day_label", Config.INSTANCE.showInGameDay ? "Day:" : "");
        vars.put("age_label", Config.INSTANCE.showWorldAge ? "Age:" : "");

        vars.put("fps", Config.INSTANCE.showFps ? Integer.toString(fpsVal) : "");
        vars.put("fps_1min", Config.INSTANCE.advancedFpsStats ? String.format("%.0f", Metrics.fps1Min()) : "");
        vars.put("fps_5min", Config.INSTANCE.advancedFpsStats ? String.format("%.0f", Metrics.fps5Min()) : "");
        vars.put("fps_15min", Config.INSTANCE.advancedFpsStats ? String.format("%.0f", Metrics.fps15Min()) : "");
        vars.put("fps_1min_label", Config.INSTANCE.advancedFpsStats ? "1m:" : "");
        vars.put("fps_5min_label", Config.INSTANCE.advancedFpsStats ? "5m:" : "");
        vars.put("fps_15min_label", Config.INSTANCE.advancedFpsStats ? "15m:" : "");

        // CPS labels (shown only when Config.showCps is true)
        boolean cpsEnabled = Config.INSTANCE.showCps;
        vars.put("cps_label", cpsEnabled ? "CPS:" : "");
        vars.put("cps", cpsEnabled ? String.format("%.0f", Metrics.totalCps()) : "");
        vars.put("cps_l_label", cpsEnabled ? "L:" : "");
        vars.put("cps_l", cpsEnabled ? Integer.toString((int) Math.round(Metrics.leftCps())) : "");
        vars.put("cps_r_label", cpsEnabled ? "R:" : "");
        vars.put("cps_r", cpsEnabled ? Integer.toString((int) Math.round(Metrics.rightCps())) : "");

        vars.put("cpu_name", Config.INSTANCE.showCpuName ? cpuNameRaw : "");
        vars.put("cpu_vendor", cpuVendor);
        String cpuUseStr = "";
        String cpuUseUnitStr = "%";
        if (Config.INSTANCE.showCpu) {
            cpuUseStr = Integer.toString(cpuUseVal);
        } else {
            cpuUseUnitStr = "";
        }
        vars.put("cpu_use", cpuUseStr);
        vars.put("cpu_use_unit", cpuUseUnitStr);
        boolean gpuOn = Config.INSTANCE.showGpu;
        vars.put("gpu_name", gpuOn && Config.INSTANCE.showGpuName ? gpuNameRaw : "");
        vars.put("gpu_vendor", gpuVendor);
        String gpuUseStr = "";
        String gpuUseUnitStr = "%";
        if (gpuOn && gpuUseVal >= 0) {
            gpuUseStr = Integer.toString(Hud.clamp0_100(gpuUseVal));
        } else {
            gpuUseUnitStr = "";
        }
        vars.put("gpu_use", gpuUseStr);
        vars.put("gpu_use_unit", gpuUseUnitStr);

        String gpuTempStr = "";
        String gpuTempUnit = "";
        if (gpuOn && Config.INSTANCE.showGpuTemp && gpuTempVal >= 0) {
            gpuTempStr = Integer.toString(gpuTempVal);
            gpuTempUnit = "°C";
        } else if (gpuOn && Config.INSTANCE.showGpuTemp) {
            gpuTempUnit = "°C";
        }
        vars.put("gpu_temp", gpuTempStr);
        vars.put("gpu_temp_unit", gpuTempUnit);
        vars.put("gpu_integrated", integrated ? "(iGPU)" : "");
        
        // Battery - under GPU section
        String batteryStr = "";
        String batteryCharging = "";
        if (Config.INSTANCE.showBattery) {
            BatteryInfo battery = getBatteryInfo();
            if (battery.percent >= 0) {
                batteryStr = battery.percent + "%";
                batteryCharging = battery.charging ? "⚡" : "";
            }
        }
        vars.put("battery", batteryStr);
        vars.put("battery_charging", batteryCharging);
        vars.put("bat_label", Config.INSTANCE.showBattery ? "Battery:" : "");
        
        String cpuTempStr = "";
        String cpuTempUnit = "";
        if (Config.INSTANCE.showCpuTemp && cpuTempVal >= 0 && CpuTempMonitor.isSupported()) {
            cpuTempStr = Integer.toString(cpuTempVal);
            cpuTempUnit = "°C";
        } else if (Config.INSTANCE.showCpuTemp && CpuTempMonitor.isSupported()) {
            cpuTempUnit = "°C";
        }
        vars.put("cpu_temp", cpuTempStr);
        vars.put("cpu_temp_unit", cpuTempUnit);

        // Memory
        long ramUsedBytes = Metrics.ramUsed();
        long vramUsedBytes = Metrics.vramUsed();

        String ramStr = "";
        String ramTotalStr = "";
        String slashMem = "";
        if (Config.INSTANCE.showMemory && Metrics.ramTotal() > 0) {
            ramStr = formatBytes(Metrics.ramUsed());
            ramTotalStr = formatBytes(Metrics.ramTotal());
            slashMem = "/";
        }
        vars.put("ram_used", ramStr);
        vars.put("ram_total", ramTotalStr);
        vars.put("slash_mem", slashMem);

        String vramStr = "";
        String vramTotalStr = "";
        String slashVram = "";
        if (Config.INSTANCE.showMemory && Metrics.vramTotal() > 0) {
            vramStr = formatBytes(Metrics.vramUsed());
            vramTotalStr = formatBytes(Metrics.vramTotal());
            slashVram = "/";
        }
        vars.put("vram_used", vramStr);
        vars.put("vram_total", vramTotalStr);
        vars.put("slash_vram", slashVram);

        // Time
        String irlTimeStr = "";
        if (Config.INSTANCE.showIrlTime) {
            irlTimeStr = formatTime(Metrics.irlTime(), Config.INSTANCE.timeFormat12Hour);
        }
        vars.put("irl_time", irlTimeStr);

        String gameTimeStr = "";
        if (Config.INSTANCE.showGameTime) {
            gameTimeStr = formatGameTime(Metrics.gameTime());
        }
        vars.put("game_time", gameTimeStr);

        // Ping
        String pingStr = "";
        String pingUnit = "";
        if (Config.INSTANCE.showPing && Metrics.ping() >= 0) {
            pingStr = Integer.toString(Metrics.ping());
            pingUnit = "ms";
        } else if (Config.INSTANCE.showPing) {
            pingUnit = "";
        }
        vars.put("ping", pingStr);
        vars.put("ping_unit", pingUnit);
        vars.put("ping_1min", Config.INSTANCE.advancedPingStats && Metrics.ping1Min() >= 0 ? String.format("%.0f", Metrics.ping1Min()) : "");
        vars.put("ping_5min", Config.INSTANCE.advancedPingStats && Metrics.ping5Min() >= 0 ? String.format("%.0f", Metrics.ping5Min()) : "");
        vars.put("ping_15min", Config.INSTANCE.advancedPingStats && Metrics.ping15Min() >= 0 ? String.format("%.0f", Metrics.ping15Min()) : "");
        vars.put("ping_1min_label", Config.INSTANCE.advancedPingStats ? "1m:" : "");
        vars.put("ping_5min_label", Config.INSTANCE.advancedPingStats ? "5m:" : "");
        vars.put("ping_15min_label", Config.INSTANCE.advancedPingStats ? "15m:" : "");

        // Coords
        String coordsX = "";
        String coordsY = "";
        String coordsZ = "";
        if (Config.INSTANCE.showCoords) {
            coordsX = String.format("%.1f", Metrics.posX());
            coordsY = String.format("%.1f", Metrics.posY());
            coordsZ = String.format("%.1f", Metrics.posZ());
        }
        vars.put("coords_x", coordsX);
        vars.put("coords_y", coordsY);
        vars.put("coords_z", coordsZ);

        // TPS
        String tpsStr = "";
        if (Config.INSTANCE.showTps) {
            tpsStr = String.format("%.1f", Metrics.tps());
        }
        vars.put("tps", tpsStr);
        vars.put("tps_label", Config.INSTANCE.showTps ? "TPS:" : "");
        vars.put("tps_1min", Config.INSTANCE.advancedTpsStats ? String.format("%.1f", Metrics.tps1Min()) : "");
        vars.put("tps_5min", Config.INSTANCE.advancedTpsStats ? String.format("%.1f", Metrics.tps5Min()) : "");
        vars.put("tps_15min", Config.INSTANCE.advancedTpsStats ? String.format("%.1f", Metrics.tps15Min()) : "");
        vars.put("tps_1min_label", Config.INSTANCE.advancedTpsStats ? "1m:" : "");
        vars.put("tps_5min_label", Config.INSTANCE.advancedTpsStats ? "5m:" : "");
        vars.put("tps_15min_label", Config.INSTANCE.advancedTpsStats ? "15m:" : "");

        // Biome
        vars.put("biome", Config.INSTANCE.showBiome ? Metrics.biome() : "");

        // Chunk Coords
        String chunkXStr = "";
        String chunkYStr = "";
        String chunkZStr = "";
        if (Config.INSTANCE.showChunkCoords) {
            chunkXStr = Integer.toString(Metrics.chunkX());
            chunkYStr = Integer.toString(Metrics.chunkY());
            chunkZStr = Integer.toString(Metrics.chunkZ());
        }
        vars.put("chunk_x", chunkXStr);
        vars.put("chunk_y", chunkYStr);
        vars.put("chunk_z", chunkZStr);

        // Entity Count
        vars.put("entity_count", Config.INSTANCE.showEntityCount ? Integer.toString(Metrics.entityCount()) : "");

        // Direction
        vars.put("direction", Config.INSTANCE.showDirection ? formatDirection(Metrics.direction()) : "");

        // Light Level
        vars.put("light_level", Config.INSTANCE.showLightLevel ? Integer.toString(Metrics.lightLevel()) : "");

        // In-Game Day
        vars.put("in_game_day", Config.INSTANCE.showInGameDay ? Long.toString(Metrics.inGameDay()) : "");

        // World Age
        String worldAgeStr = "";
        if (Config.INSTANCE.showWorldAge) {
            long age = Metrics.worldAge();
            long days = age / 24000L;
            long hours = (age % 24000L) / 1000L;
            worldAgeStr = String.format("%dd %dh", days, hours);
        }
        vars.put("world_age", worldAgeStr);

        List<List<RenderToken>> parsedLines = new ArrayList<>();
        int maxW = 0;
        for (HudTemplate.Line l : lines) {
            String raw = l.text();
            List<RenderToken> tokens;
            if (raw == null || (tokens = Hud.parseLine(raw, vars, cpuUseVal, gpuUseVal, gpuTempVal, cpuTempVal, fpsVal, Metrics.ping(), Metrics.tps(), Metrics.gameTime())).isEmpty()) continue;
            int w = Hud.tokensWidth(tr, tokens);
            if (w > maxW) {
                maxW = w;
            }
            parsedLines.add(tokens);
        }
        if (maxW <= 0 || parsedLines.isEmpty()) {
            return;
        }

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        
        int originX = Config.INSTANCE.x;
        int originY = Config.INSTANCE.y;
        
        int hudW = (int)(maxW * Config.INSTANCE.scale);
        int hudH = (int)(parsedLines.size() * 11 * Config.INSTANCE.scale);
        originX = Math.max(0, Math.min(originX, screenW - hudW));
        originY = Math.max(0, Math.min(originY, screenH - hudH));
        
        renderHudTokens(context, tr, parsedLines, originX, originY, Hud.withFullAlpha(Config.INSTANCE.color), cpuC1, cpuC2, gpuC1, gpuC2);
    }

    public static void drawDirect(DrawContext context, TextRenderer tr) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) {
            return;
        }

        HudTemplate.ensureDefault();
        List<HudTemplate.Line> lines;
        if (HudTemplate.lines().isEmpty()) {
            HudTemplate.reload();
        }
        if ((lines = HudTemplate.lines()).isEmpty()) {
            return;
        }

        String cpuNameRaw = Hud.safe(Metrics.cpuName(), "CPU");
        String gpuNameRaw = Hud.safe(Metrics.gpuName(), "GPU");
        int fpsVal = mc.getCurrentFps();
        int cpuUseVal = Hud.clamp0_100(Math.round(Metrics.cpu()));
        int gpuUseVal = Metrics.gpu();
        int gpuTempVal = Metrics.gpuTemp();
        int cpuTempVal = Metrics.cpuTemp();
        String gpuVendor = Hud.detectGpuVendor(gpuNameRaw);
        String cpuVendor = Hud.detectCpuVendor(cpuNameRaw);
        boolean integrated = Hud.detectIntegratedGpu(gpuNameRaw);

        int cpuC1 = CPU_RED;
        int cpuC2 = CPU_ORANGE;
        int gpuC1 = GPU_LIGHT_DEFAULT;
        int gpuC2 = GPU_DARK_DEFAULT;

        if (cpuVendor.equals("intel")) {
            cpuC1 = INTEL_LIGHT;
            cpuC2 = INTEL_DARK;
        } else if (cpuVendor.equals("amd")) {
            cpuC1 = AMD_LIGHT;
            cpuC2 = AMD_DARK;
        }

        if (gpuVendor.equals("intel")) {
            gpuC1 = INTEL_LIGHT;
            gpuC2 = INTEL_DARK;
        } else if (gpuVendor.equals("amd")) {
            gpuC1 = AMD_LIGHT;
            gpuC2 = AMD_DARK;
        } else if (gpuVendor.equals("nvidia")) {
            gpuC1 = NVIDIA_LIGHT;
            gpuC2 = NVIDIA_DARK;
        }

        Map<String, String> vars = buildVars();
        List<List<RenderToken>> parsedLines = new ArrayList<>();
        int maxW = 0;
        for (HudTemplate.Line l : lines) {
            String raw = l.text();
            List<RenderToken> tokens;
            if (raw == null || (tokens = Hud.parseLine(raw, vars, cpuUseVal, gpuUseVal, gpuTempVal, cpuTempVal, fpsVal, Metrics.ping(), Metrics.tps(), Metrics.gameTime())).isEmpty()) continue;
            int w = Hud.tokensWidth(tr, tokens);
            if (w > maxW) {
                maxW = w;
            }
            parsedLines.add(tokens);
        }
        if (maxW <= 0 || parsedLines.isEmpty()) {
            return;
        }

        renderHudTokens(context, tr, parsedLines, 0, 0, Hud.withFullAlpha(Config.INSTANCE.color), cpuC1, cpuC2, gpuC1, gpuC2);
    }

    private static Map<String, String> buildVars() {
        Map<String, String> vars = new java.util.HashMap<>();
        boolean cpuSectionEnabled = Config.INSTANCE.showCpu || Config.INSTANCE.showCpuTemp;
        boolean gpuSectionEnabled = Config.INSTANCE.showGpu || Config.INSTANCE.showGpuTemp;
        boolean fpsSectionEnabled = Config.INSTANCE.showFps;

        vars.put("cpu_label", cpuSectionEnabled ? "CPU:" : "");
        vars.put("gpu_label", gpuSectionEnabled ? "GPU:" : "");
        vars.put("fps_label", fpsSectionEnabled ? "FPS:" : "");
        
        long ramTotalBytes = Metrics.ramTotal();
        long vramTotalBytes = Metrics.vramTotal();
        boolean memoryDataAvailable = ramTotalBytes > 0 || vramTotalBytes > 0;
        vars.put("memory_label", Config.INSTANCE.showMemory && memoryDataAvailable ? "RAM:" : "");
        vars.put("vram_label", Config.INSTANCE.showMemory && memoryDataAvailable ? "VRAM:" : "");
        
        vars.put("time_irl_label", Config.INSTANCE.showIrlTime ? "IRL:" : "");
        vars.put("time_game_label", Config.INSTANCE.showGameTime ? "Game:" : "");
        vars.put("ping_label", Config.INSTANCE.showPing ? "Ping:" : "");
        vars.put("coords_label", Config.INSTANCE.showCoords ? "XYZ:" : "");
        vars.put("biome_label", Config.INSTANCE.showBiome ? "Biome:" : "");
        vars.put("chunk_label", Config.INSTANCE.showChunkCoords ? "Chunk XYZ:" : "");
        vars.put("entity_label", Config.INSTANCE.showEntityCount ? "Entities:" : "");
        vars.put("direction_label", Config.INSTANCE.showDirection ? "Dir:" : "");
        vars.put("light_label", Config.INSTANCE.showLightLevel ? "Light:" : "");
        vars.put("day_label", Config.INSTANCE.showInGameDay ? "Day:" : "");
        vars.put("age_label", Config.INSTANCE.showWorldAge ? "Age:" : "");

        vars.put("fps", Config.INSTANCE.showFps ? Integer.toString(MinecraftClient.getInstance().getCurrentFps()) : "");
        vars.put("fps_1min", Config.INSTANCE.advancedFpsStats ? String.format("%.0f", Metrics.fps1Min()) : "");
        vars.put("fps_5min", Config.INSTANCE.advancedFpsStats ? String.format("%.0f", Metrics.fps5Min()) : "");
        vars.put("fps_15min", Config.INSTANCE.advancedFpsStats ? String.format("%.0f", Metrics.fps15Min()) : "");
        vars.put("fps_1min_label", Config.INSTANCE.advancedFpsStats ? "1m:" : "");
        vars.put("fps_5min_label", Config.INSTANCE.advancedFpsStats ? "5m:" : "");
        vars.put("fps_15min_label", Config.INSTANCE.advancedFpsStats ? "15m:" : "");
        vars.put("cpu_name", Config.INSTANCE.showCpuName ? Hud.safe(Metrics.cpuName(), "CPU") : "");
        vars.put("cpu_vendor", Hud.detectCpuVendor(Hud.safe(Metrics.cpuName(), "CPU")));
        String cpuUseStr = "";
        String cpuUseUnitStr = "%";
        if (Config.INSTANCE.showCpu) {
            cpuUseStr = Integer.toString(Hud.clamp0_100(Math.round(Metrics.cpu())));
        } else {
            cpuUseUnitStr = "";
        }
        vars.put("cpu_use", cpuUseStr);
        vars.put("cpu_use_unit", cpuUseUnitStr);
        boolean gpuOn = Config.INSTANCE.showGpu;
        vars.put("gpu_name", gpuOn && Config.INSTANCE.showGpuName ? Hud.safe(Metrics.gpuName(), "GPU") : "");
        vars.put("gpu_vendor", Hud.detectGpuVendor(Hud.safe(Metrics.gpuName(), "GPU")));
        String gpuUseStr = "";
        String gpuUseUnitStr = "%";
        int gpuUseVal = Metrics.gpu();
        if (gpuOn && gpuUseVal >= 0) {
            gpuUseStr = Integer.toString(Hud.clamp0_100(gpuUseVal));
        } else {
            gpuUseUnitStr = "";
        }
        vars.put("gpu_use", gpuUseStr);
        vars.put("gpu_use_unit", gpuUseUnitStr);

        String gpuTempStr = "";
        String gpuTempUnit = "";
        int gpuTempVal = Metrics.gpuTemp();
        if (gpuOn && Config.INSTANCE.showGpuTemp && gpuTempVal >= 0) {
            gpuTempStr = Integer.toString(gpuTempVal);
            gpuTempUnit = "°C";
        } else if (gpuOn && Config.INSTANCE.showGpuTemp) {
            gpuTempUnit = "°C";
        }
        vars.put("gpu_temp", gpuTempStr);
        vars.put("gpu_temp_unit", gpuTempUnit);
        vars.put("gpu_integrated", Hud.detectIntegratedGpu(Hud.safe(Metrics.gpuName(), "GPU")) ? "(iGPU)" : "");
        
        String batteryStr = "";
        String batteryCharging = "";
        if (Config.INSTANCE.showBattery) {
            BatteryInfo battery = getBatteryInfo();
            if (battery.percent >= 0) {
                batteryStr = battery.percent + "%";
                batteryCharging = battery.charging ? "⚡" : "";
            }
        }
        vars.put("battery", batteryStr);
        vars.put("battery_charging", batteryCharging);
        vars.put("bat_label", Config.INSTANCE.showBattery ? "Battery:" : "");
        
        String cpuTempStr = "";
        String cpuTempUnit = "";
        int cpuTempVal = Metrics.cpuTemp();
        if (Config.INSTANCE.showCpuTemp && cpuTempVal >= 0 && CpuTempMonitor.isSupported()) {
            cpuTempStr = Integer.toString(cpuTempVal);
            cpuTempUnit = "°C";
        } else if (Config.INSTANCE.showCpuTemp && CpuTempMonitor.isSupported()) {
            cpuTempUnit = "°C";
        }
        vars.put("cpu_temp", cpuTempStr);
        vars.put("cpu_temp_unit", cpuTempUnit);

        String ramStr = "";
        String ramTotalStr = "";
        String slashMem = "";
        if (Config.INSTANCE.showMemory && Metrics.ramTotal() > 0) {
            ramStr = formatBytes(Metrics.ramUsed());
            ramTotalStr = formatBytes(Metrics.ramTotal());
            slashMem = "/";
        }
        vars.put("ram_used", ramStr);
        vars.put("ram_total", ramTotalStr);
        vars.put("slash_mem", slashMem);

        String vramStr = "";
        String vramTotalStr = "";
        String slashVram = "";
        if (Config.INSTANCE.showMemory && Metrics.vramTotal() > 0) {
            vramStr = formatBytes(Metrics.vramUsed());
            vramTotalStr = formatBytes(Metrics.vramTotal());
            slashVram = "/";
        }
        vars.put("vram_used", vramStr);
        vars.put("vram_total", vramTotalStr);
        vars.put("slash_vram", slashVram);

        String irlTimeStr = "";
        if (Config.INSTANCE.showIrlTime) {
            irlTimeStr = formatTime(Metrics.irlTime(), Config.INSTANCE.timeFormat12Hour);
        }
        vars.put("irl_time", irlTimeStr);

        String gameTimeStr = "";
        if (Config.INSTANCE.showGameTime) {
            gameTimeStr = formatGameTime(Metrics.gameTime());
        }
        vars.put("game_time", gameTimeStr);

        String pingStr = "";
        String pingUnit = "";
        int pingVal = Metrics.ping();
        if (Config.INSTANCE.showPing && pingVal >= 0) {
            pingStr = Integer.toString(pingVal);
            pingUnit = "ms";
        } else if (Config.INSTANCE.showPing) {
            pingUnit = "";
        }
        vars.put("ping", pingStr);
        vars.put("ping_unit", pingUnit);
        vars.put("ping_1min", Config.INSTANCE.advancedPingStats && Metrics.ping1Min() >= 0 ? String.format("%.0f", Metrics.ping1Min()) : "");
        vars.put("ping_5min", Config.INSTANCE.advancedPingStats && Metrics.ping5Min() >= 0 ? String.format("%.0f", Metrics.ping5Min()) : "");
        vars.put("ping_15min", Config.INSTANCE.advancedPingStats && Metrics.ping15Min() >= 0 ? String.format("%.0f", Metrics.ping15Min()) : "");
        vars.put("ping_1min_label", Config.INSTANCE.advancedPingStats ? "1m:" : "");
        vars.put("ping_5min_label", Config.INSTANCE.advancedPingStats ? "5m:" : "");
        vars.put("ping_15min_label", Config.INSTANCE.advancedPingStats ? "15m:" : "");

        String coordsX = "";
        String coordsY = "";
        String coordsZ = "";
        if (Config.INSTANCE.showCoords) {
            coordsX = String.format("%.1f", Metrics.posX());
            coordsY = String.format("%.1f", Metrics.posY());
            coordsZ = String.format("%.1f", Metrics.posZ());
        }
        vars.put("coords_x", coordsX);
        vars.put("coords_y", coordsY);
        vars.put("coords_z", coordsZ);

        String tpsStr = "";
        if (Config.INSTANCE.showTps) {
            tpsStr = String.format("%.1f", Metrics.tps());
        }
        vars.put("tps", tpsStr);
        vars.put("tps_label", Config.INSTANCE.showTps ? "TPS:" : "");
        vars.put("tps_1min", Config.INSTANCE.advancedTpsStats ? String.format("%.1f", Metrics.tps1Min()) : "");
        vars.put("tps_5min", Config.INSTANCE.advancedTpsStats ? String.format("%.1f", Metrics.tps5Min()) : "");
        vars.put("tps_15min", Config.INSTANCE.advancedTpsStats ? String.format("%.1f", Metrics.tps15Min()) : "");
        vars.put("tps_1min_label", Config.INSTANCE.advancedTpsStats ? "1m:" : "");
        vars.put("tps_5min_label", Config.INSTANCE.advancedTpsStats ? "5m:" : "");
        vars.put("tps_15min_label", Config.INSTANCE.advancedTpsStats ? "15m:" : "");

        vars.put("biome", Config.INSTANCE.showBiome ? Metrics.biome() : "");

        String chunkXStr = "";
        String chunkYStr = "";
        String chunkZStr = "";
        if (Config.INSTANCE.showChunkCoords) {
            chunkXStr = Integer.toString(Metrics.chunkX());
            chunkYStr = Integer.toString(Metrics.chunkY());
            chunkZStr = Integer.toString(Metrics.chunkZ());
        }
        vars.put("chunk_x", chunkXStr);
        vars.put("chunk_y", chunkYStr);
        vars.put("chunk_z", chunkZStr);

        vars.put("entity_count", Config.INSTANCE.showEntityCount ? Integer.toString(Metrics.entityCount()) : "");
        vars.put("direction", Config.INSTANCE.showDirection ? formatDirection(Metrics.direction()) : "");
        vars.put("light_level", Config.INSTANCE.showLightLevel ? Integer.toString(Metrics.lightLevel()) : "");
        vars.put("in_game_day", Config.INSTANCE.showInGameDay ? Long.toString(Metrics.inGameDay()) : "");

        String worldAgeStr = "";
        if (Config.INSTANCE.showWorldAge) {
            long age = Metrics.worldAge();
            long days = age / 24000L;
            long hours = (age % 24000L) / 1000L;
            worldAgeStr = String.format("%dd %dh", days, hours);
        }
        vars.put("world_age", worldAgeStr);

        return vars;
    }

    private static void renderHudTokens(DrawContext context, TextRenderer tr, List<List<RenderToken>> parsedLines, int originX, int originY, int baseArgb, int cpuC1, int cpuC2, int gpuC1, int gpuC2) {
        Matrix3x2fStack m = context.getMatrices();
        m.pushMatrix();
        m.translate((float)originX, (float)originY);
        float sc = (float)Config.INSTANCE.scale;
        m.scale(sc, sc);
        int y = 0;
        for (List<RenderToken> list : parsedLines) {
            int x = 0;
            for (RenderToken t : list) {
                if (t.text == null || t.text.isEmpty()) continue;
                if (t.waved) {
                    boolean animOn;
                    boolean bl = animOn = t.waveMode == WaveMode.FPS && Config.INSTANCE.animateFps
                        || t.waveMode == WaveMode.CPU && Config.INSTANCE.animateCpuName
                        || t.waveMode == WaveMode.GPU && Config.INSTANCE.animateGpuName
                        || t.waveMode == WaveMode.BATTERY && Config.INSTANCE.animateBattery
                        || t.waveMode == WaveMode.MEMORY && Config.INSTANCE.animateMemory
                        || t.waveMode == WaveMode.VRAM && Config.INSTANCE.animateVram
                        || t.waveMode == WaveMode.IRL_TIME && Config.INSTANCE.animateIrlTime
                        || t.waveMode == WaveMode.GAME_TIME && Config.INSTANCE.animateGameTime
                        || t.waveMode == WaveMode.PING && Config.INSTANCE.animatePing
                        || t.waveMode == WaveMode.COORDS && Config.INSTANCE.animateCoords
                        || t.waveMode == WaveMode.TPS && Config.INSTANCE.animateTps
                        || t.waveMode == WaveMode.BIOME && Config.INSTANCE.animateBiome
                        || t.waveMode == WaveMode.CHUNK && Config.INSTANCE.animateChunkCoords
                        || t.waveMode == WaveMode.ENTITY && Config.INSTANCE.animateEntityCount
                        || t.waveMode == WaveMode.DIRECTION && Config.INSTANCE.animateDirection
                        || t.waveMode == WaveMode.LIGHT && Config.INSTANCE.animateLightLevel
                        || t.waveMode == WaveMode.DAY && Config.INSTANCE.animateInGameDay
                        || t.waveMode == WaveMode.AGE && Config.INSTANCE.animateWorldAge || t.waveMode == WaveMode.CPS && Config.INSTANCE.animateCps;
                    if (!animOn) {
                        x += Hud.drawPlainToken(context, tr, t, x, y, baseArgb);
                        continue;
                    }
                    int c1, c2;
                    if (t.waveMode == WaveMode.FPS) {
                        c1 = FPS_YELLOW;
                        c2 = FPS_GOLD;
                    } else if (t.waveMode == WaveMode.CPU) {
                        c1 = cpuC1;
                        c2 = cpuC2;
                    } else if (t.waveMode == WaveMode.GPU) {
                        c1 = gpuC1;
                        c2 = gpuC2;
                    } else if (t.waveMode == WaveMode.BATTERY) {
                        c1 = BATTERY_C1;
                        c2 = BATTERY_C2;
                    } else if (t.waveMode == WaveMode.MEMORY) {
                        c1 = MEMORY_C1;
                        c2 = MEMORY_C2;
                    } else if (t.waveMode == WaveMode.VRAM) {
                        c1 = VRAM_C1;
                        c2 = VRAM_C2;
                    } else if (t.waveMode == WaveMode.IRL_TIME) {
                        c1 = IRL_TIME_C1;
                        c2 = IRL_TIME_C2;
                    } else if (t.waveMode == WaveMode.GAME_TIME) {
                        c1 = GAME_TIME_C1;
                        c2 = GAME_TIME_C2;
                    } else if (t.waveMode == WaveMode.PING) {
                        c1 = PING_C1;
                        c2 = PING_C2;
                    } else if (t.waveMode == WaveMode.COORDS) {
                        c1 = COORDS_C1;
                        c2 = COORDS_C2;
                    } else if (t.waveMode == WaveMode.TPS) {
                        c1 = TPS_C1;
                        c2 = TPS_C2;
                    } else if (t.waveMode == WaveMode.BIOME) {
                        c1 = BIOME_C1;
                        c2 = BIOME_C2;
                    } else if (t.waveMode == WaveMode.CHUNK) {
                        c1 = CHUNK_C1;
                        c2 = CHUNK_C2;
                    } else if (t.waveMode == WaveMode.ENTITY) {
                        c1 = ENTITY_C1;
                        c2 = ENTITY_C2;
                    } else if (t.waveMode == WaveMode.DIRECTION) {
                        c1 = DIRECTION_C1;
                        c2 = DIRECTION_C2;
                    } else if (t.waveMode == WaveMode.LIGHT) {
                        c1 = LIGHT_C1;
                        c2 = LIGHT_C2;
                    } else if (t.waveMode == WaveMode.DAY) {
                        c1 = DAY_C1;
                        c2 = DAY_C2;
                    } else if (t.waveMode == WaveMode.CPS) {
                        c1 = CPS_C1;
                        c2 = CPS_C2;
                    } else {
                        c1 = AGE_C1;
                        c2 = AGE_C2;
                    }
                    Hud.drawWaveText(context, tr, t.text, x, y, c1, c2);
                    x += tr.getWidth(t.text);
                    continue;
                }
                x += Hud.drawPlainToken(context, tr, t, x, y, baseArgb);
            }
            Objects.requireNonNull(tr);
            y += 9 + 2;
        }
        m.popMatrix();
    }

    private static int drawPlainToken(DrawContext context, TextRenderer tr, RenderToken t, int x, int y, int baseArgb) {
        Text text = Text.literal(t.text).setStyle(t.style);
        if (Config.INSTANCE.shadow) {
            context.drawTextWithShadow(tr, text, x, y, baseArgb);
        } else {
            context.drawText(tr, text, x, y, baseArgb, false);
        }
        return tr.getWidth(t.text);
    }

    private static List<RenderToken> parseLine(String template, Map<String, String> vars, int cpuUse, int gpuUse, int gpuTemp, int cpuTemp, int fps, int ping, double tps, long gameTime) {
        String noComment = Hud.stripLineComment(template);
        if (noComment == null) {
            return List.of();
        }
        template = noComment.trim();
        if (template.isEmpty()) {
            return List.of();
        }
        List<RenderToken> out = new ArrayList<>();
        int baseRgb = Config.INSTANCE.color & 0xFFFFFF;
        Style style = Style.EMPTY;
        WaveMode waveMode = WaveMode.NONE;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < template.length(); i++) {
            int end;
            WaveMode wm;
            char c = template.charAt(i);
            if (c == '@' && (wm = Hud.matchWave(template, i)) != null) {
                Hud.flush(out, buf, style, waveMode);
                waveMode = wm;
                int adv = switch (wm) {
                    case CPU -> "@wave_cpu".length();
                    case GPU -> "@wave_gpu".length();
                    case FPS -> "@wave_fps".length();
                    case BATTERY -> "@wave_battery".length();
                    case MEMORY -> "@wave_memory".length();
                    case VRAM -> "@wave_vram".length();
                    case IRL_TIME -> "@wave_irl_time".length();
                    case GAME_TIME -> "@wave_game_time".length();
                    case PING -> "@wave_ping".length();
                    case COORDS -> "@wave_coords".length();
                    case TPS -> "@wave_tps".length();
                    case BIOME -> "@wave_biome".length();
                    case CHUNK -> "@wave_chunk".length();
                    case ENTITY -> "@wave_entity".length();
                    case DIRECTION -> "@wave_direction".length();
                    case LIGHT -> "@wave_light".length();
                    case DAY -> "@wave_day".length();
                    case AGE -> "@wave_age".length();
                    case CPS -> "@wave_cps".length();
                    default -> 0;
                };
                i += adv - 1;
                continue;
            }
            if (c == '&' && i + 1 < template.length()) {
                char n = template.charAt(i + 1);
                if (n == '&') {
                    buf.append('&');
                    i++;
                    continue;
                }
                Formatting f = Formatting.byCode(n);
                if (f != null) {
                    Hud.flush(out, buf, style, waveMode);
                    waveMode = WaveMode.NONE;
                    style = style.isEmpty() ? style : (f == Formatting.RESET ? Style.EMPTY : style.withColor(f));
                    i++;
                    continue;
                }
            }
            if (c == '{' && (end = template.indexOf('}', i + 1)) > i) {
                String key = template.substring(i + 1, end);
                Hud.flush(out, buf, style, waveMode);
                String value = vars.getOrDefault(key, "");
                if (!value.isEmpty()) {
                    boolean userPickedColor;
                    boolean tokenWaved = waveMode != WaveMode.NONE;
                    Style tokenStyle = style;
                    boolean bl = userPickedColor = !Hud.isBaseColor(style, baseRgb);
                    if (!userPickedColor) {
                        if (key.equals("cpu_use") && Config.INSTANCE.colorizeCpuUsage) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.usageColor(cpuUse) & 0xFFFFFF);
                        } else if (key.equals("cpu_temp") && Config.INSTANCE.colorizeCpuTemp) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.tempColor(cpuTemp) & 0xFFFFFF);
                        } else if (key.equals("gpu_use") && Config.INSTANCE.colorizeGpuUsage) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.usageColor(gpuUse) & 0xFFFFFF);
                        } else if (key.equals("gpu_temp") && Config.INSTANCE.colorizeGpuTemp) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.tempColor(gpuTemp) & 0xFFFFFF);
                        } else if (key.equals("fps") && Config.INSTANCE.colorizeFps) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.fpsColor(fps) & 0xFFFFFF);
                        } else if (key.equals("ping") && Config.INSTANCE.colorizePing) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.pingColor(ping) & 0xFFFFFF);
                        } else if (key.equals("tps") && Config.INSTANCE.colorizeTps) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.tpsColor(tps) & 0xFFFFFF);
                        } else if (key.equals("coords_x") && Config.INSTANCE.colorizeCoords) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.coordXColor() & 0xFFFFFF);
                        } else if (key.equals("coords_y") && Config.INSTANCE.colorizeCoords) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.coordYColor() & 0xFFFFFF);
                        } else if (key.equals("coords_z") && Config.INSTANCE.colorizeCoords) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.coordZColor() & 0xFFFFFF);
                        } else if ((key.equals("ram_used") || key.equals("ram_total")) && Config.INSTANCE.colorizeMemory) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.memoryColor() & 0xFFFFFF);
                        } else if ((key.equals("vram_used") || key.equals("vram_total")) && Config.INSTANCE.colorizeVram) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.vramColor() & 0xFFFFFF);
                        } else if (key.equals("irl_time") && Config.INSTANCE.colorizeIrlTime) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.timeColor() & 0xFFFFFF);
                        } else if (key.equals("game_time") && Config.INSTANCE.colorizeGameTime) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.gameTimeColor(gameTime) & 0xFFFFFF);
                        } else if (key.equals("battery") && Config.INSTANCE.showBattery) {
                            tokenWaved = false;
                            try {
                                int batt = Integer.parseInt(value.replace("%", ""));
                                tokenStyle = tokenStyle.withColor(Hud.batteryColor(batt) & 0xFFFFFF);
                            } catch (Exception ignored) {}
                        } else if (key.equals("entity_count") && Config.INSTANCE.colorizeEntityCount) {
                            tokenWaved = false;
                            try {
                                int count = Integer.parseInt(value);
                                tokenStyle = tokenStyle.withColor(Hud.entityCountColor(count) & 0xFFFFFF);
                            } catch (Exception ignored) {}
                        } else if (key.equals("light_level") && Config.INSTANCE.colorizeLightLevel) {
                            tokenWaved = false;
                            try {
                                int light = Integer.parseInt(value);
                                tokenStyle = tokenStyle.withColor(Hud.lightLevelColor(light) & 0xFFFFFF);
                            } catch (Exception ignored) {}
                        } else if (key.equals("direction") && Config.INSTANCE.colorizeDirection) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.directionColor() & 0xFFFFFF);
                        } else if (key.equals("chunk_x") && Config.INSTANCE.colorizeCoords) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.coordXColor() & 0xFFFFFF);
                        } else if (key.equals("chunk_y") && Config.INSTANCE.colorizeCoords) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.coordYColor() & 0xFFFFFF);
                        } else if (key.equals("chunk_z") && Config.INSTANCE.colorizeCoords) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.coordZColor() & 0xFFFFFF);
                        } else if ((key.equals("fps_1min") || key.equals("fps_5min") || key.equals("fps_15min")) && Config.INSTANCE.colorizeFps) {
                            tokenWaved = false;
                            tokenStyle = tokenStyle.withColor(Hud.fpsColor((int) fps) & 0xFFFFFF);
                        } else if ((key.equals("ping_1min") || key.equals("ping_5min") || key.equals("ping_15min")) && Config.INSTANCE.colorizePing) {
                            tokenWaved = false;
                            try {
                                int p = Integer.parseInt(value);
                                tokenStyle = tokenStyle.withColor(Hud.pingColor(p) & 0xFFFFFF);
                            } catch (Exception ignored) {}
                        } else if ((key.equals("tps_1min") || key.equals("tps_5min") || key.equals("tps_15min")) && Config.INSTANCE.colorizeTps) {
                            tokenWaved = false;
                            try {
                                double t = Double.parseDouble(value);
                                tokenStyle = tokenStyle.withColor(Hud.tpsColor(t) & 0xFFFFFF);
                            } catch (Exception ignored) {}
                        } else if ((key.equals("cps") || key.equals("cps_l") || key.equals("cps_r")) && Config.INSTANCE.colorizeCps) {
                            tokenWaved = false;
                            try {
                                float cpsVal = Float.parseFloat(value);
                                tokenStyle = tokenStyle.withColor(Hud.withFullAlpha(CPS_C1) & 0xFFFFFF);
                            } catch (Exception ignored) {}
                        }
                    }
                    out.add(new RenderToken(value, tokenStyle, tokenWaved, waveMode));
                }
                i = end;
                continue;
            }
            buf.append(c);
        }
        Hud.flush(out, buf, style, waveMode);
        String plain = Hud.tokensToPlain(out).trim();
        if (plain.isEmpty()) {
            return List.of();
        }
        return out;
    }

    private static boolean isBaseColor(Style style, int baseRgb) {
        if (style.getColor() == null) {
            return true;
        }
        return (style.getColor().getRgb() & 0xFFFFFF) == (baseRgb & 0xFFFFFF);
    }

    private static String stripLineComment(String s) {
        if (s == null) {
            return null;
        }
        int i = s.indexOf("//");
        if (i < 0) {
            return s;
        }
        return s.substring(0, i);
    }

    private static WaveMode matchWave(String s, int at) {
        if (s.regionMatches(at, "@wave_cpu", 0, "@wave_cpu".length())) {
            return WaveMode.CPU;
        }
        if (s.regionMatches(at, "@wave_gpu", 0, "@wave_gpu".length())) {
            return WaveMode.GPU;
        }
        if (s.regionMatches(at, "@wave_fps", 0, "@wave_fps".length())) {
            return WaveMode.FPS;
        }
        if (s.regionMatches(at, "@wave_battery", 0, "@wave_battery".length())) {
            return WaveMode.BATTERY;
        }
        if (s.regionMatches(at, "@wave_memory", 0, "@wave_memory".length())) {
            return WaveMode.MEMORY;
        }
        if (s.regionMatches(at, "@wave_vram", 0, "@wave_vram".length())) {
            return WaveMode.VRAM;
        }
        if (s.regionMatches(at, "@wave_irl_time", 0, "@wave_irl_time".length())) {
            return WaveMode.IRL_TIME;
        }
        if (s.regionMatches(at, "@wave_game_time", 0, "@wave_game_time".length())) {
            return WaveMode.GAME_TIME;
        }
        if (s.regionMatches(at, "@wave_ping", 0, "@wave_ping".length())) {
            return WaveMode.PING;
        }
        if (s.regionMatches(at, "@wave_coords", 0, "@wave_coords".length())) {
            return WaveMode.COORDS;
        }
        if (s.regionMatches(at, "@wave_tps", 0, "@wave_tps".length())) {
            return WaveMode.TPS;
        }
        if (s.regionMatches(at, "@wave_biome", 0, "@wave_biome".length())) {
            return WaveMode.BIOME;
        }
        if (s.regionMatches(at, "@wave_chunk", 0, "@wave_chunk".length())) {
            return WaveMode.CHUNK;
        }
        if (s.regionMatches(at, "@wave_entity", 0, "@wave_entity".length())) {
            return WaveMode.ENTITY;
        }
        if (s.regionMatches(at, "@wave_direction", 0, "@wave_direction".length())) {
            return WaveMode.DIRECTION;
        }
        if (s.regionMatches(at, "@wave_light", 0, "@wave_light".length())) {
            return WaveMode.LIGHT;
        }
        if (s.regionMatches(at, "@wave_day", 0, "@wave_day".length())) {
            return WaveMode.DAY;
        }
        if (s.regionMatches(at, "@wave_age", 0, "@wave_age".length())) {
            return WaveMode.AGE;
        }
        if (s.regionMatches(at, "@wave_cps", 0, "@wave_cps".length())) {
            return WaveMode.CPS;
        }
        return null;
    }

    private static void flush(List<RenderToken> out, StringBuilder buf, Style style, WaveMode waveMode) {
        if (buf.length() == 0) {
            return;
        }
        String s = buf.toString();
        buf.setLength(0);
        if (s.isEmpty()) {
            return;
        }
        boolean waved = waveMode != WaveMode.NONE;
        out.add(new RenderToken(s, style, waved, waveMode));
    }

    private static String tokensToPlain(List<RenderToken> tokens) {
        StringBuilder sb = new StringBuilder();
        for (RenderToken t : tokens) {
            sb.append(t.text);
        }
        return sb.toString();
    }

    private static int tokensWidth(TextRenderer tr, List<RenderToken> tokens) {
        int w = 0;
        for (RenderToken t : tokens) {
            if (t.text == null || t.text.isEmpty()) continue;
            w += tr.getWidth(t.text);
        }
        return w;
    }

    private static String safe(String s, String fallback) {
        if (s == null) {
            return fallback;
        }
        String t = s.trim();
        return t.isEmpty() ? fallback : t;
    }

    private static int clamp0_100(int v) {
        if (v < 0) return 0;
        if (v > 100) return 100;
        return v;
    }

    private static String detectCpuVendor(String cpuName) {
        String a = cpuName.toLowerCase(Locale.ROOT);
        if (a.contains("intel")) {
            return "intel";
        }
        if (a.contains("amd") || a.contains("ryzen") || a.contains("epyc")) {
            return "amd";
        }
        return "unknown";
    }

    private static String detectGpuVendor(String gpuName) {
        String a = gpuName.toLowerCase(Locale.ROOT);
        if (a.contains("nvidia") || a.contains("geforce") || a.contains("quadro") || a.contains("rtx") || a.contains("gtx")) {
            return "nvidia";
        }
        if (a.contains("amd") || a.contains("radeon")) {
            return "amd";
        }
        if (a.contains("intel") || a.contains("arc") || a.contains("uhd") || a.contains("iris")) {
            return "intel";
        }
        return "unknown";
    }

    private static boolean detectIntegratedGpu(String gpuName) {
        String a = gpuName.toLowerCase(Locale.ROOT);
        if (a.contains("uhd") || a.contains("iris")) {
            return true;
        }
        if (a.contains("intel") && !a.contains("arc")) {
            return true;
        }
        return a.contains("radeon") && (a.contains("graphics") || a.contains("apu") || a.contains("vega"));
    }

    private static void drawWaveText(DrawContext ctx, TextRenderer tr, String text, int x, int y, int c1, int c2) {
        if (text == null || text.isEmpty()) return;
        long t = System.currentTimeMillis();
        double base = (double)(t % WAVE_PERIOD_MS) / (double)WAVE_PERIOD_MS * Math.PI * 2.0;
        int i = 0;
        int off = 0;
        while (off < text.length()) {
            int cp = text.codePointAt(off);
            String ch = new String(Character.toChars(cp));
            double s = (Math.sin(base + PHASE_STEP * (double)i) + 1.0) * 0.5;
            int col = Hud.withFullAlpha(Hud.lerpColor(c1, c2, s));
            Text t1 = Text.literal(ch);
            if (Config.INSTANCE.shadow) {
                ctx.drawTextWithShadow(tr, t1, x, y, col);
            } else {
                ctx.drawText(tr, t1, x, y, col, false);
            }
            x += tr.getWidth(ch);
            off += Character.charCount(cp);
            i++;
        }
    }

    private static int withFullAlpha(int rgb) {
        return 0xFF000000 | rgb & 0xFFFFFF;
    }

    private static int lerpColor(int a, int b, double t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int)Math.round(ar + (br - ar) * t);
        int g = (int)Math.round(ag + (bg - ag) * t);
        int bl = (int)Math.round(ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int usageColor(int v) {
        if (v < 0) return Hud.withFullAlpha(Config.INSTANCE.color);
        if (v >= 80) return Hud.withFullAlpha(0xFF3B30);
        if (v >= 70) return Hud.withFullAlpha(0xFF9500);
        if (v >= 55) return Hud.withFullAlpha(0xFFCC00);
        return Hud.withFullAlpha(0x34C759);
    }

    private static int tempColor(int c) {
        if (c < 0) return Hud.withFullAlpha(Config.INSTANCE.color);
        if (c >= 85) return Hud.withFullAlpha(0xFF3B30);
        if (c >= 75) return Hud.withFullAlpha(0xFF9500);
        if (c >= 60) return Hud.withFullAlpha(0xFFCC00);
        return Hud.withFullAlpha(0x34C759);
    }

    private static String formatBytes(long bytes) {
        if (bytes <= 0) return "0";
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + "KB";
        if (bytes < 1024 * 1024 * 1024) return String.format("%.0fMB", bytes / (1024.0 * 1024.0));
        return String.format("%.1fGB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private static String formatTime(long millis, boolean twelveHour) {
        java.util.Date date = new java.util.Date(millis);
        String pattern = twelveHour ? "hh:mm:ss a" : "HH:mm:ss";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(pattern, Locale.ROOT);
        return sdf.format(date);
    }

    private static String formatGameTime(long ticks) {
        long hours = (ticks / 1000 + 6) % 24;
        long minutes = (ticks % 1000) * 60 / 1000;
        return String.format("%02d:%02d", hours, minutes);
    }

    private static String formatDirection(float yaw) {
        while (yaw >= 180) yaw -= 360;
        while (yaw < -180) yaw += 360;
        String dir = "";
        if (yaw >= -45 && yaw < 45) dir = "S";
        else if (yaw >= 45 && yaw < 135) dir = "W";
        else if (yaw >= 135 || yaw < -135) dir = "N";
        else if (yaw >= -135 && yaw < -45) dir = "E";
        return dir;
    }

    private static BatteryInfo getBatteryInfo() {
        try {
            Process p = new ProcessBuilder("bash", "-c", "cat /sys/class/power_supply/BAT*/capacity 2>/dev/null || echo -1").redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String s = br.readLine();
                if (s != null && !s.isEmpty()) {
                    int percent = Integer.parseInt(s.trim());
                    boolean charging = false;
                    try {
                        Process p2 = new ProcessBuilder("bash", "-c", "cat /sys/class/power_supply/BAT*/status 2>/dev/null || echo 'Unknown'").redirectErrorStream(true).start();
                        try (BufferedReader br2 = new BufferedReader(new InputStreamReader(p2.getInputStream(), StandardCharsets.UTF_8))) {
                            String status = br2.readLine();
                            if (status != null && status.trim().equalsIgnoreCase("Charging")) {
                                charging = true;
                            }
                        }
                        p2.destroy();
                    } catch (Exception ignored) {}
                    return new BatteryInfo(percent, charging);
                }
            }
            p.destroy();
        } catch (Exception ignored) {}
        return new BatteryInfo(-1, false);
    }

    private record BatteryInfo(int percent, boolean charging) {
    }

    private static int fpsColor(int fps) {
        if (fps >= 55) return Hud.withFullAlpha(0x34C759);
        if (fps >= 30) return Hud.withFullAlpha(0xFFCC00);
        if (fps >= 15) return Hud.withFullAlpha(0xFF9500);
        return Hud.withFullAlpha(0xFF3B30);
    }

    private static int pingColor(int ping) {
        if (ping <= 50) return Hud.withFullAlpha(0x34C759);
        if (ping <= 100) return Hud.withFullAlpha(0xFFCC00);
        if (ping <= 200) return Hud.withFullAlpha(0xFF9500);
        return Hud.withFullAlpha(0xFF3B30);
    }

    private static int tpsColor(double tps) {
        if (tps >= 19.5) return Hud.withFullAlpha(0x34C759);
        if (tps >= 18.0) return Hud.withFullAlpha(0xFFCC00);
        if (tps >= 15.0) return Hud.withFullAlpha(0xFF9500);
        return Hud.withFullAlpha(0xFF3B30);
    }

    private static int coordsColor() {
        return Hud.withFullAlpha(0x00FF00);
    }

    private static int coordXColor() {
        return Hud.withFullAlpha(0xFF5555);
    }

    private static int coordYColor() {
        return Hud.withFullAlpha(0x55FF55);
    }

    private static int coordZColor() {
        return Hud.withFullAlpha(0x5555FF);
    }

    private static int memoryColor() {
        return Hud.withFullAlpha(0x00BFFF);
    }

    private static int vramColor() {
        return Hud.withFullAlpha(0xFF69B4);
    }

    private static int timeColor() {
        return Hud.withFullAlpha(0xFFA500);
    }

    private static int gameTimeColor(long ticks) {
        long timeOfDay = ticks % 24000;
        if (timeOfDay < 1000) {
            return Hud.withFullAlpha(0xFFAA00);
        } else if (timeOfDay < 12000) {
            return Hud.withFullAlpha(0xFFFF00);
        } else if (timeOfDay < 13000) {
            return Hud.withFullAlpha(0xFF6600);
        } else if (timeOfDay < 22000) {
            return Hud.withFullAlpha(0x4444FF);
        } else {
            return Hud.withFullAlpha(0x00BFFF);
        }
    }

    private static int batteryColor(int percent) {
        if (percent >= 80) return Hud.withFullAlpha(0x34C759);
        if (percent >= 50) return Hud.withFullAlpha(0xFFCC00);
        if (percent >= 20) return Hud.withFullAlpha(0xFF9500);
        return Hud.withFullAlpha(0xFF3B30);
    }

    private static int entityCountColor(int count) {
        if (count <= 50) return Hud.withFullAlpha(0x34C759);
        if (count <= 150) return Hud.withFullAlpha(0xFFCC00);
        if (count <= 300) return Hud.withFullAlpha(0xFF9500);
        return Hud.withFullAlpha(0xFF3B30);
    }

    private static int lightLevelColor(int light) {
        if (light >= 8) return Hud.withFullAlpha(0xFFFF00);
        if (light >= 5) return Hud.withFullAlpha(0xFFCC00);
        if (light >= 2) return Hud.withFullAlpha(0xFF9500);
        return Hud.withFullAlpha(0xFF3B30);
    }

    private static int directionColor() {
        return Hud.withFullAlpha(0x00FFFF);
    }

    private record RenderToken(String text, Style style, boolean waved, WaveMode waveMode) {
    }

    private static enum WaveMode {
        NONE,
        CPU,
        GPU,
        FPS,
        BATTERY,
        MEMORY,
        VRAM,
        IRL_TIME,
        GAME_TIME,
        PING,
        COORDS,
        TPS,
        BIOME,
        CHUNK,
        ENTITY,
        DIRECTION,
        LIGHT,
        DAY,
        AGE,
        CPS
    }
}
