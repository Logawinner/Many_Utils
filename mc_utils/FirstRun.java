package me.anchorhelper.mc_utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import net.fabricmc.loader.api.FabricLoader;

public class FirstRun {
    private static final String FLAG_NAME = "minecraft_utils_firstrun.flag";

    public static boolean markIfFirst() {
        Path cfgDir = FabricLoader.getInstance().getConfigDir();
        Path flag = cfgDir.resolve(FLAG_NAME);
        if (Files.exists(flag, new LinkOption[0])) {
            return false;
        }
        try {
            Files.createDirectories(cfgDir, new FileAttribute[0]);
            Files.write(flag, new byte[]{1}, new OpenOption[0]);
        }
        catch (IOException iOException) {
            // empty catch block
        }
        return true;
    }
}
