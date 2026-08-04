package me.anchorhelper.gpucpu_util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

public final class HudTemplate {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("gpucpu_util.txt");
    private static volatile List<Line> cached = Collections.emptyList();

    public static Path path() {
        return FILE;
    }

    public static void ensureDefault() {
        if (Files.exists(FILE, new LinkOption[0])) {
            return;
        }
        try {
            Files.createDirectories(FILE.getParent(), new FileAttribute[0]);
            Files.writeString(FILE, (CharSequence) HudTemplate.defaultText(), StandardCharsets.UTF_8, new OpenOption[0]);
        } catch (IOException iOException) {
            // empty catch block
        }
    }

    public static void reload() {
        List<String> lines;
        HudTemplate.ensureDefault();
        ArrayList<Line> out = new ArrayList<Line>();
        try {
            lines = Files.readAllLines(FILE, StandardCharsets.UTF_8);
        } catch (IOException e) {
            cached = Collections.emptyList();
            return;
        }
        for (String raw : lines) {
            if (raw == null) continue;
            String line = HudTemplate.stripLineComment(raw);
            if ((line = line.trim()).isEmpty()) continue;
            out.add(new Line(Mode.PLAIN, line));
        }
        cached = out;
    }

    public static List<Line> lines() {
        return cached;
    }

    private static String stripLineComment(String s) {
        int i = s.indexOf("//");
        if (i < 0) {
            return s;
        }
        return s.substring(0, i);
    }

    private static String defaultText() {
        return "@wave_cpu {cpu_label} {cpu_name}\n@wave_cpu {cpu_use}{cpu_use_unit} {cpu_temp}{cpu_temp_unit}\n@wave_gpu {gpu_label} {gpu_name}\n@wave_gpu {gpu_use}{gpu_use_unit}@wave_gpu {gpu_temp}{gpu_temp_unit} @wave_gpu {gpu_integrated}\n@wave_fps {fps_label} {fps}\n\n// Notes / usage:\n// - Anything after // is ignored (comments).\n// - Wave directives can appear ANYWHERE: @wave_cpu @wave_gpu @wave_fps\n//   They toggle \"wave mode\" for following text until a formatting code (&x) appears.\n// - Color/format codes: &0-&f, plus &l (bold) &o (italic) &n (underline) &m (strikethrough) &r (reset).\n// - Example: @wave_gpu% &cGpu   -> waves \"% \" then &c applies to \"Gpu\".\n// - Variables: {fps} {cpu_name} {cpu_use} {cpu_use_unit} {cpu_temp} {cpu_temp_unit} {gpu_name} {gpu_use} {gpu_use_unit} {gpu_temp} {gpu_temp_unit} {gpu_integrated}\n// - Section labels: {cpu_label} {gpu_label} {fps_label} (empty if section disabled)\n// - Unit variables ({cpu_use_unit} {cpu_temp_unit} {gpu_use_unit} {gpu_temp_unit}) are separate to avoid colorization\n// - Colorize toggles still work for {cpu_use} {cpu_temp} {gpu_use} {gpu_temp} and override wave.\n";
    }

    private HudTemplate() {
    }

    public record Line(Mode mode, String text) {
    }

    public static enum Mode {
        PLAIN,
        WAVE_CPU,
        WAVE_GPU,
        WAVE_FPS;
    }
}
