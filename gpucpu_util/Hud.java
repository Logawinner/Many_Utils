package me.anchorhelper.gpucpu_util;

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
        // Section labels - empty if the entire section is disabled
        boolean cpuSectionEnabled = Config.INSTANCE.showCpu || Config.INSTANCE.showCpuTemp;
        boolean gpuSectionEnabled = Config.INSTANCE.showGpu || Config.INSTANCE.showGpuTemp;
        boolean fpsSectionEnabled = Config.INSTANCE.showFps;

        vars.put("cpu_label", cpuSectionEnabled ? "CPU:" : "");
        vars.put("gpu_label", gpuSectionEnabled ? "GPU:" : "");
        vars.put("fps_label", fpsSectionEnabled ? "FPS:" : "");

        vars.put("fps", Config.INSTANCE.showFps ? Integer.toString(fpsVal) : "");
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

        List<List<RenderToken>> parsedLines = new ArrayList<>();
        int maxW = 0;
        for (HudTemplate.Line l : lines) {
            String raw = l.text();
            List<RenderToken> tokens;
            if (raw == null || (tokens = Hud.parseLine(raw, vars, cpuUseVal, gpuUseVal, gpuTempVal, cpuTempVal)).isEmpty()) continue;
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
        int originX = screenW - Config.INSTANCE.x - maxW;
        int originY = Config.INSTANCE.y;
        Matrix3x2fStack m = context.getMatrices();
        m.pushMatrix();
        m.translate((float)originX, (float)originY);
        float sc = (float)Config.INSTANCE.scale;
        m.scale(sc, sc);
        int y = 0;
        int baseArgb = Hud.withFullAlpha(Config.INSTANCE.color);
        for (List<RenderToken> list : parsedLines) {
            int x = 0;
            for (RenderToken t : list) {
                if (t.text == null || t.text.isEmpty()) continue;
                if (t.waved) {
                    boolean animOn;
                    boolean bl = animOn = t.waveMode == WaveMode.FPS && Config.INSTANCE.animateFps || t.waveMode == WaveMode.CPU && Config.INSTANCE.animateCpuName || t.waveMode == WaveMode.GPU && Config.INSTANCE.animateGpuName;
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
                    } else {
                        c1 = gpuC1;
                        c2 = gpuC2;
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

    private static List<RenderToken> parseLine(String template, Map<String, String> vars, int cpuUse, int gpuUse, int gpuTemp, int cpuTemp) {
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
                int adv = wm == WaveMode.CPU ? "@wave_cpu".length() : (wm == WaveMode.GPU ? "@wave_gpu".length() : "@wave_fps".length());
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

    private record RenderToken(String text, Style style, boolean waved, WaveMode waveMode) {
    }

    private static enum WaveMode {
        NONE,
        CPU,
        GPU,
        FPS;
    }
}
