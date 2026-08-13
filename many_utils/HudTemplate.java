package me.anchorhelper.many_utils;

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
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("many_utils.txt");
    private static volatile List<Line> cached = Collections.emptyList();

    public static Path path() {
        return FILE;
    }

    public static void ensureDefault() {
        try {
            if (Files.exists(FILE, new LinkOption[0])) {
                String current = Files.readString(FILE, StandardCharsets.UTF_8);
                if (current.equals(defaultText())) {
                    return;
                }
            }
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
        return "@wave_cpu{cpu_label} {cpu_name}\n@wave_cpu{cpu_use}{cpu_use_unit} {cpu_temp}{cpu_temp_unit}\n@wave_gpu{gpu_label} {gpu_name}\n@wave_gpu{gpu_use}{gpu_use_unit} @wave_gpu{gpu_temp}{gpu_temp_unit} @wave_gpu{gpu_integrated}\n@wave_battery{bat_label} {battery}{battery_charging}\n@wave_fps{fps_label} {fps} {fps_1min_label}{fps_1min} {fps_5min_label}{fps_5min} {fps_15min_label}{fps_15min}\n@wave_memory{memory_label} {ram_used}{slash_mem}{ram_total}\n@wave_vram{vram_label} {vram_used}{slash_vram}{vram_total}\n@wave_irl_time{time_irl_label} {irl_time}\n@wave_game_time{time_game_label} {game_time}\n@wave_ping{ping_label} {ping}{ping_unit} {ping_1min_label}{ping_1min} {ping_5min_label}{ping_5min} {ping_15min_label}{ping_15min}\n@wave_coords{coords_label} {coords_x}, {coords_y}, {coords_z}\n@wave_tps{tps_label} {tps} {tps_1min_label}{tps_1min} {tps_5min_label}{tps_5min} {tps_15min_label}{tps_15min}\n@wave_biome{biome_label} {biome}\n@wave_chunk{chunk_label} {chunk_x}, {chunk_y}, {chunk_z}\n@wave_entity{entity_label} {entity_count}\n@wave_direction{direction_label} {direction}\n@wave_light{light_label} {light_level}\n@wave_day{day_label} {in_game_day}\n@wave_age{age_label} {world_age}\n@wave_cps{cps_label} {cps} L: {cps_l} R: {cps_r} Tot: {cps}\n\n// Notes / usage:\n// - Anything after // is ignored (comments).\n// - Wave directives can appear ANYWHERE: @wave_cpu @wave_gpu @wave_fps\n//   They toggle \"wave mode\" for following text until a formatting code (&x) appears.\n// - Color/format codes: &0-&f, plus &l (bold) &o (italic) &n (underline) &m (strikethrough) &r (reset).\n// - Example: @wave_gpu% &cGpu   -> waves \"% \" then &c applies to \"Gpu\".\n// - Variables: {fps} {cpu_name} {cpu_use} {cpu_use_unit} {cpu_temp} {cpu_temp_unit} {gpu_name} {gpu_use} {gpu_use_unit} {gpu_temp} {gpu_temp_unit} {gpu_integrated} {battery} {battery_charging}\n// - Memory: {ram_used} {ram_total} {slash_mem} {vram_used} {vram_total} {slash_vram}\n// - Time: {irl_time} {game_time}\n// - Other: {ping} {ping_unit} {coords_x} {coords_y} {coords_z} {tps} {tps}\n// - Extra: {biome} {chunk_x} {chunk_y} {chunk_z} {entity_count} {direction} {light_level} {in_game_day} {world_age}\n// - Advanced FPS: {fps_1min} {fps_5min} {fps_15min}\n// - Advanced Ping: {ping_1min} {ping_5min} {ping_15min}\n// - Advanced TPS: {tps_1min} {tps_5min} {tps_15min}\n// - Section labels: {cpu_label} {gpu_label} {fps_label} {memory_label} {vram_label} {time_irl_label} {time_game_label} {ping_label} {coords_label} {tps_label} {biome_label} {chunk_label} {entity_label} {direction_label} {light_label} {day_label} {age_label} {bat_label}\n";
    }

    private HudTemplate() {
    }

    public record Line(Mode mode, String text) {
    }

public static enum Mode {
        PLAIN,
        WAVE_CPU,
        WAVE_GPU,
        WAVE_FPS,
        WAVE_CPS;
    }
}
