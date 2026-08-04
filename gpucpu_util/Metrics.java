package me.anchorhelper.gpucpu_util;

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

public class Metrics {
    private static final OperatingSystemMXBean OS = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "GpuCpu-Sampler");
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

    public static void start() {
        if (started) return;
        started = true;
        CpuTempMonitor.start();
        EXEC.scheduleAtFixedRate(() -> {
            sampleCpu();
            sampleGpu();
        }, 0, 1, TimeUnit.SECONDS);
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
            nvmlClass = Class.forName("me.anchorhelper.gpucpu_util.Nvml");
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
}
