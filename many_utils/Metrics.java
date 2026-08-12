package me.anchorhelper.many_utils;

import com.sun.management.OperatingSystemMXBean;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.MinecraftClient;

public class Metrics {
    private static final OperatingSystemMXBean OS = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ManyUtils-Sampler");
        t.setDaemon(true);
        return t;
    });
    private static volatile boolean started = false;
    private static volatile int cpuPercent = 0;
    private static volatile int gpuUtilPercent = -1;
    private static volatile int gpuTempC = -1;
    private static volatile int cpuTempC = -1;
    private static volatile String cachedGpuName = null;
    private static volatile String cachedCpuName = null;

    private static volatile boolean nvmlChecked = false;
    private static volatile boolean nvmlAvailable = false;
    private static volatile Class<?> nvmlClass = null;
    private static volatile Method nvmlUtilMethod = null;
    private static volatile Method nvmlTempMethod = null;
    private static volatile Method nvmlNameMethod = null;

    private static volatile boolean nvsmiChecked = false;
    private static volatile boolean nvsmiAvailable = false;

    private static volatile long ramTotal = Runtime.getRuntime().maxMemory();
    private static volatile long ramUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    private static volatile long vramTotal = 0;
    private static volatile long vramUsed = 0;

    private static volatile int ping = -1;
    private static volatile double x = 0;
    private static volatile double y = 0;
    private static volatile double z = 0;
    private static volatile String biome = "";
    private static volatile int chunkX = 0;
    private static volatile int chunkY = 0;
    private static volatile int chunkZ = 0;
    private static volatile int entityCount = 0;
    private static volatile float direction = 0f;
    private static volatile int lightLevel = 0;
    private static volatile long inGameDay = 0;
    private static volatile long worldAge = 0;

    private static volatile long irlTime = System.currentTimeMillis();
    private static volatile long gameTime = 0;

    private static volatile float fps1Min = 0;
    private static volatile float fps5Min = 0;
    private static volatile float fps15Min = 0;
    private static final java.util.List<Sample> fpsSamples = new java.util.ArrayList<>();
    private static final int MAX_FPS_SAMPLES = 1800;
    private static final Object FPS_LOCK = new Object();

    private static volatile double tps = 0;
    private static volatile float tps1Min = 0;
    private static volatile float tps5Min = 0;
    private static volatile float tps15Min = 0;
    private static final java.util.List<Sample> tpsSamples = new java.util.ArrayList<>();
    private static final Object TPS_LOCK = new Object();
    private static final int MAX_TPS_SAMPLES = 1800;
    private static long lastGameTime = 0;
    private static long lastTpsSampleTime = System.nanoTime();

    private static volatile float ping1Min = -1;
    private static volatile float ping5Min = -1;
    private static volatile float ping15Min = -1;
    private static final java.util.Queue<Sample> pingSamples = new java.util.ArrayDeque<>();
    private static final Object PING_LOCK = new Object();
    private static long lastPingSampleTime = System.nanoTime();

    private record Sample(long time, float value) {
    }

    private static final java.util.ArrayDeque<Long> leftClicks = new java.util.ArrayDeque<>();
    private static final java.util.ArrayDeque<Long> rightClicks = new java.util.ArrayDeque<>();

    public static void recordClick(boolean left) {
        long now = System.currentTimeMillis();
        if (left) {
            leftClicks.addLast(now);
        } else {
            rightClicks.addLast(now);
        }
    }

    public static float leftCps() {
        return countClicks(leftClicks);
    }

    public static float rightCps() {
        return countClicks(rightClicks);
    }

    public static float totalCps() {
        return leftCps() + rightCps();
    }

    private static float countClicks(java.util.ArrayDeque<Long> deque) {
        long now = System.currentTimeMillis();
        long cutoff = now - 1000;
        while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
            deque.pollFirst();
        }
        return deque.size();
    }

    public static void start() {
        if (started) return;
        started = true;
        CpuTempMonitor.start();
        EXEC.scheduleAtFixedRate(() -> {
            sampleCpu();
            sampleGpu();
            sampleMemory();
            sampleTime();
            samplePingAndCoords();
            sampleFps();
            samplePingRolling();
            sampleTpsRolling();
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    private static void sampleCpu() {
        double v = -1.0;
        try { v = OS.getSystemCpuLoad(); } catch (Throwable ignored) {}
        if (v >= 0.0 && v <= 1.0) {
            cpuPercent = (int) Math.round(v * 100.0);
            return;
        }
        long proc = 0L, now = 0L;
        try {
            proc = OS.getProcessCpuTime();
            now = System.nanoTime();
        } catch (Throwable ignored) {}
        if (proc > 0 && now > 0) {
            int p = (int) Math.round((double) proc / (double) now * 100.0);
            cpuPercent = Math.max(0, Math.min(100, p));
        }
    }

    private static void sampleGpu() {
        Integer u = nvmlGpuUtil();
        Integer t = nvmlGpuTemp();
        if (u == null || t == null) {
            if (u == null) u = nvsmiUtil();
            if (t == null) t = nvsmiTemp();
        }
        if (u != null) gpuUtilPercent = u;
        if (t != null) gpuTempC = t;
        cpuTempC = (int) Math.round(CpuTempMonitor.getCpuTemp());
    }

    public static int cpu() { return cpuPercent; }
    public static int cpuTemp() { return cpuTempC; }
    public static String cpuName() {
        String v = cachedCpuName;
        if (v != null && !v.isEmpty()) return v;
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            v = os.contains("win") ? cpuNameWindows() : (os.contains("mac") || os.contains("darwin") ? cpuNameMac() : cpuNameLinux());
        } catch (Throwable ignored) {}
        if (v == null || v.isEmpty()) v = "CPU";
        cachedCpuName = v;
        return v;
    }

    public static int gpu() { return gpuUtilPercent; }
    public static int gpuTemp() { return gpuTempC; }
    public static String gpuName() {
        String v = cachedGpuName;
        if (v != null && !v.isEmpty()) return v;
        String n1 = nvmlGpuName();
        if (n1 != null && !n1.isEmpty()) v = n1;
        if ((v == null || v.isEmpty())) {
            String n2 = nvsmiName();
            if (n2 != null && !n2.isEmpty()) v = n2;
        }
        if (v == null || v.isEmpty()) {
            try {
                String renderer = glString(0x1F01); // GL_RENDERER
                if (renderer != null && !renderer.isEmpty()) {
                    int slash = renderer.indexOf('/');
                    if (slash > 0) renderer = renderer.substring(0, slash);
                    v = renderer.trim();
                }
            } catch (Throwable ignored) {}
        }
        if (v == null || v.isEmpty()) v = "GPU";
        cachedGpuName = v;
        return v;
    }

    private static String cpuNameWindows() throws Exception {
        Process p = new ProcessBuilder("reg", "query", "HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0", "/v", "ProcessorNameString").redirectErrorStream(true).start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                int idx = line.indexOf("ProcessorNameString");
                if (idx < 0) continue;
                String s = line.substring(idx).replace("ProcessorNameString", "").replace("REG_SZ", "").trim();
                if (s.isEmpty()) continue;
                return s;
            }
        }
        return null;
    }

    private static String cpuNameMac() throws Exception {
        Process p = new ProcessBuilder("sysctl", "-n", "machdep.cpu.brand_string").redirectErrorStream(true).start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String s = br.readLine();
            if (s != null && !s.isEmpty()) return s.trim();
        }
        return null;
    }

    private static String cpuNameLinux() throws Exception {
        File f = new File("/proc/cpuinfo");
        if (!f.exists()) return null;
        try (BufferedReader br = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                int idx = line.indexOf("model name");
                if (idx < 0) continue;
                int c = line.indexOf(':', idx);
                if (c < 0) continue;
                String s = line.substring(c + 1).trim();
                if (s.isEmpty()) continue;
                return s;
            }
        }
        return null;
    }

    private static void ensureNvml() {
        if (nvmlChecked) return;
        nvmlChecked = true;
        try {
            nvmlClass = Class.forName("me.anchorhelper.many_utils.Nvml");
            Method avail = null, init = null;
            try { avail = nvmlClass.getMethod("available"); } catch (Throwable t) {}
            try { init = nvmlClass.getMethod("init"); } catch (Throwable t) {}
            if (avail != null) {
                try {
                    Object ok = avail.invoke(null);
                    nvmlAvailable = ok instanceof Boolean && (Boolean)ok;
                } catch (Throwable t) { nvmlAvailable = false; }
            } else if (init != null) {
                try {
                    Object ok = init.invoke(null);
                    nvmlAvailable = ok == null || (ok instanceof Boolean && (Boolean)ok);
                } catch (Throwable t) { nvmlAvailable = false; }
            } else {
                nvmlAvailable = true;
            }
            try { nvmlUtilMethod = nvmlClass.getMethod("gpuUtil"); } catch (Throwable t) {}
            if (nvmlUtilMethod == null) try { nvmlUtilMethod = nvmlClass.getMethod("getUtilization"); } catch (Throwable t) {}
            try { nvmlTempMethod = nvmlClass.getMethod("gpuTemp"); } catch (Throwable t) {}
            if (nvmlTempMethod == null) try { nvmlTempMethod = nvmlClass.getMethod("getTemperature"); } catch (Throwable t) {}
            try { nvmlNameMethod = nvmlClass.getMethod("gpuName"); } catch (Throwable t) {}
            if (nvmlNameMethod == null) try { nvmlNameMethod = nvmlClass.getMethod("getName"); } catch (Throwable t) {}
        } catch (Throwable t) {
            nvmlAvailable = false;
            nvmlClass = null;
        }
    }

    private static Integer nvmlGpuUtil() {
        ensureNvml();
        if (!nvmlAvailable || nvmlUtilMethod == null) return null;
        try {
            Object v = nvmlUtilMethod.invoke(null);
            if (v instanceof Number) return ((Number) v).intValue();
        } catch (Throwable ignored) {}
        return null;
    }

    private static Integer nvmlGpuTemp() {
        ensureNvml();
        if (!nvmlAvailable || nvmlTempMethod == null) return null;
        try {
            Object v = nvmlTempMethod.invoke(null);
            if (v instanceof Number) return ((Number) v).intValue();
        } catch (Throwable ignored) {}
        return null;
    }

    private static String nvmlGpuName() {
        ensureNvml();
        if (!nvmlAvailable || nvmlNameMethod == null) return null;
        try {
            Object v = nvmlNameMethod.invoke(null);
            if (v != null) return v.toString();
        } catch (Throwable ignored) {}
        return null;
    }

    private static void ensureNvSmi() {
        if (nvsmiChecked) return;
        nvsmiChecked = true;
        try {
            Process p = new ProcessBuilder("nvidia-smi", "-L").redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                nvsmiAvailable = br.readLine() != null;
            }
        } catch (Throwable t) {
            nvsmiAvailable = false;
        }
    }

    private static String nvsmiName() {
        ensureNvSmi();
        if (!nvsmiAvailable) return null;
        try {
            Process p = new ProcessBuilder("nvidia-smi", "--query-gpu=name", "--format=csv,noheader").redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String s = br.readLine();
                if (s == null) return null;
                return s.trim();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Integer nvsmiUtil() {
        ensureNvSmi();
        if (!nvsmiAvailable) return null;
        try {
            Process p = new ProcessBuilder("nvidia-smi", "--query-gpu=utilization.gpu", "--format=csv,noheader,nounits").redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String s = br.readLine();
                if (s == null || s.isEmpty()) return null;
                return Integer.parseInt(s.trim());
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Integer nvsmiTemp() {
        ensureNvSmi();
        if (!nvsmiAvailable) return null;
        try {
            Process p = new ProcessBuilder("nvidia-smi", "--query-gpu=temperature.gpu", "--format=csv,noheader,nounits").redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String s = br.readLine();
                if (s == null || s.isEmpty()) return null;
                return Integer.parseInt(s.trim());
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static String glString(int param) {
        try {
            String s = org.lwjgl.opengl.GL11.glGetString(param);
            if (s != null) return s.trim();
        } catch (Throwable ignored) {}
        return null;
    }

    private static void sampleMemory() {
        try {
            Runtime rt = Runtime.getRuntime();
            ramUsed = rt.totalMemory() - rt.freeMemory();
            ramTotal = rt.maxMemory();
        } catch (Throwable ignored) {}

        try {
            if (vramTotal <= 0) {
                vramTotal = detectVramTotal();
            }
            vramUsed = detectVramUsed();
            if (vramUsed < 0) vramUsed = 0;
            if (vramTotal < 0) vramTotal = 0;
        } catch (Throwable ignored) {}
    }

    private static long detectVramTotal() {
        long vram = detectNvidiaVram();
        if (vram >= 0) return vram;
        vram = detectAmdVram();
        if (vram >= 0) return vram;
        vram = detectIntelVram();
        if (vram >= 0) return vram;
        return -1;
    }

    private static long detectNvidiaVram() {
        try {
            Process p = new ProcessBuilder("nvidia-smi", "--query-gpu=memory.total", "--format=csv,noheader,nounits").redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String s = br.readLine();
                if (s != null && !s.isEmpty()) {
                    long mb = Long.parseLong(s.trim());
                    return mb * 1024 * 1024;
                }
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    private static long detectAmdVram() {
        try {
            Process p = new ProcessBuilder("rocm-smi", "--showmeminfo", "vram", "--csv").redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("Total Memory")) {
                        String[] parts = line.split(",");
                        if (parts.length >= 2) {
                            String val = parts[1].trim().replaceAll("[^0-9]", "");
                            if (!val.isEmpty()) {
                                long mb = Long.parseLong(val);
                                return mb * 1024 * 1024;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    private static long detectIntelVram() {
        try {
            Process p = new ProcessBuilder("xpu-smi", "dump", "-d", "0", "-m", "0", "-u", "0").redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("Total Memory")) {
                        String[] parts = line.split(":");
                        if (parts.length >= 2) {
                            String val = parts[1].trim().replaceAll("[^0-9]", "");
                            if (!val.isEmpty()) {
                                long mb = Long.parseLong(val);
                                return mb * 1024 * 1024;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    private static long detectVramUsed() {
        try {
            Process p = new ProcessBuilder("nvidia-smi", "--query-gpu=memory.used", "--format=csv,noheader,nounits").redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String s = br.readLine();
                if (s != null && !s.isEmpty()) {
                    long mb = Long.parseLong(s.trim());
                    return mb * 1024 * 1024;
                }
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    private static void sampleTime() {
        irlTime = System.currentTimeMillis();
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.world != null) {
                long currentGameTime = mc.world.getTime();
                long now = System.nanoTime();
                if (lastGameTime > 0 && currentGameTime != lastGameTime) {
                    long timeDelta = now - lastTpsSampleTime;
                    long tickDelta = currentGameTime - lastGameTime;
                    if (timeDelta > 0 && tickDelta > 0) {
                        double seconds = (double) timeDelta / 1_000_000_000.0;
                        tps = tickDelta / seconds;
                    }
                }
                lastGameTime = currentGameTime;
                lastTpsSampleTime = now;
                gameTime = mc.world.getTimeOfDay();
            }
        } catch (Throwable ignored) {}
    }

    private static void samplePingAndCoords() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.player != null) {
                ping = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
                x = mc.player.getX();
                y = mc.player.getY();
                z = mc.player.getZ();
                chunkX = ((int) Math.floor(x) % 16 + 16) % 16;
                chunkY = ((int) Math.floor(y) % 16 + 16) % 16;
                chunkZ = ((int) Math.floor(z) % 16 + 16) % 16;
                direction = mc.player.getYaw();
                lightLevel = mc.world.getLightLevel(mc.player.getBlockPos());
                worldAge = mc.world.getTime();
                inGameDay = worldAge / 24000L + 1L;
                int count = 0;
                for (net.minecraft.entity.Entity e : mc.world.getEntities()) {
                    count++;
                }
                entityCount = count;
                String biomeId = mc.world.getBiome(mc.player.getBlockPos()).getKey().orElseThrow().getValue().toString();
                biome = biomeId.contains("/") ? biomeId.substring(biomeId.lastIndexOf('/') + 1).replace("_", " ") : biomeId;
            }
        } catch (Throwable ignored) {}
    }

    private static void sampleFps() {
        float currentFps = 0;
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) {
                currentFps = mc.getCurrentFps();
            }
        } catch (Throwable ignored) {}
        synchronized (FPS_LOCK) {
            fpsSamples.add(new Sample(System.nanoTime(), currentFps));
            while (fpsSamples.size() > MAX_FPS_SAMPLES) {
                fpsSamples.remove(0);
            }
            int size = fpsSamples.size();
            int count1m = Math.min(120, size);
            int count5m = Math.min(600, size);
            int count15m = Math.min(1800, size);
            float sum1m = 0, sum5m = 0, sum15m = 0;
            for (int i = size - count1m; i < size; i++) {
                sum1m += fpsSamples.get(i).value();
            }
            for (int i = size - count5m; i < size; i++) {
                sum5m += fpsSamples.get(i).value();
            }
            for (int i = size - count15m; i < size; i++) {
                sum15m += fpsSamples.get(i).value();
            }
            fps1Min = count1m > 0 ? sum1m / count1m : 0;
            fps5Min = count5m > 0 ? sum5m / count5m : 0;
            fps15Min = count15m > 0 ? sum15m / count15m : 0;
        }
    }

    private static void samplePingRolling() {
        long now = System.nanoTime();
        synchronized (PING_LOCK) {
            pingSamples.add(new Sample(now, ping >= 0 ? ping : 0));
            while (!pingSamples.isEmpty() && pingSamples.peek().time() < now - TimeUnit.MINUTES.toNanos(15)) {
                pingSamples.poll();
            }
            if (now - lastPingSampleTime >= TimeUnit.MILLISECONDS.toNanos(500)) {
                lastPingSampleTime = now;
                float oneMin = 0, fiveMin = 0, fifteenMin = 0;
                int oneCount = 0, fiveCount = 0, fifteenCount = 0;
                long oneMinAgo = now - TimeUnit.MINUTES.toNanos(1);
                long fiveMinAgo = now - TimeUnit.MINUTES.toNanos(5);
                long fifteenMinAgo = now - TimeUnit.MINUTES.toNanos(15);
                for (Sample s : pingSamples) {
                    if (s.time() >= oneMinAgo) {
                        oneMin += s.value();
                        oneCount++;
                    }
                    if (s.time() >= fiveMinAgo) {
                        fiveMin += s.value();
                        fiveCount++;
                    }
                    if (s.time() >= fifteenMinAgo) {
                        fifteenMin += s.value();
                        fifteenCount++;
                    }
                }
                ping1Min = oneCount > 0 ? oneMin / oneCount : -1;
                ping5Min = fiveCount > 0 ? fiveMin / fiveCount : -1;
                ping15Min = fifteenCount > 0 ? fifteenMin / fifteenCount : -1;
            }
        }
    }

    private static void sampleTpsRolling() {
        long now = System.nanoTime();
        synchronized (TPS_LOCK) {
            tpsSamples.add(new Sample(now, (float) tps));
            while (tpsSamples.size() > MAX_TPS_SAMPLES) {
                tpsSamples.remove(0);
            }
            int size = tpsSamples.size();
            int count1m = Math.min(120, size);
            int count5m = Math.min(600, size);
            int count15m = Math.min(1800, size);
            float sum1m = 0, sum5m = 0, sum15m = 0;
            for (int i = size - count1m; i < size; i++) {
                sum1m += tpsSamples.get(i).value();
            }
            for (int i = size - count5m; i < size; i++) {
                sum5m += tpsSamples.get(i).value();
            }
            for (int i = size - count15m; i < size; i++) {
                sum15m += tpsSamples.get(i).value();
            }
            tps1Min = count1m > 0 ? sum1m / count1m : 0;
            tps5Min = count5m > 0 ? sum5m / count5m : 0;
            tps15Min = count15m > 0 ? sum15m / count15m : 0;
        }
    }

    public static void recordTick() {
    }

    public static void resetFps() {
        synchronized (FPS_LOCK) {
            fpsSamples.clear();
            fps1Min = 0;
            fps5Min = 0;
            fps15Min = 0;
        }
    }

    public static long ramTotal() { return ramTotal; }
    public static long ramUsed() { return ramUsed; }
    public static long vramTotal() { return vramTotal; }
    public static long vramUsed() { return vramUsed; }
    public static int ping() { return ping; }
    public static double posX() { return x; }
    public static double posY() { return y; }
    public static double posZ() { return z; }
    public static long irlTime() { return irlTime; }
    public static long gameTime() { return gameTime; }
    public static float fps1Min() { return fps1Min; }
    public static float fps5Min() { return fps5Min; }
    public static float fps15Min() { return fps15Min; }
    public static double tps() { return tps; }
    public static float tps1Min() { return tps1Min; }
    public static float tps5Min() { return tps5Min; }
    public static float tps15Min() { return tps15Min; }
    public static float ping1Min() { return ping1Min; }
    public static float ping5Min() { return ping5Min; }
    public static float ping15Min() { return ping15Min; }
    public static String biome() { return biome; }
    public static int chunkX() { return chunkX; }
    public static int chunkY() { return chunkY; }
    public static int chunkZ() { return chunkZ; }
    public static int entityCount() { return entityCount; }
    public static float direction() { return direction; }
    public static int lightLevel() { return lightLevel; }
    public static long inGameDay() { return inGameDay; }
    public static long worldAge() { return worldAge; }
}
