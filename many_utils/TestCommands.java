package me.anchorhelper.many_utils;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class TestCommands {
    private static final String ARG_VENDOR = "vendor";

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("cputest").then(ClientCommandManager.argument(ARG_VENDOR, StringArgumentType.word()).suggests(TestCommands.vendorSuggestions()).executes(ctx -> {
                String v = StringArgumentType.getString(ctx, ARG_VENDOR).toLowerCase();
                VendorChoice choice = TestCommands.parseVendor(v);
                HudOverrides.setCpu(choice);
                TestCommands.send("CPU test mode: " + choice.name().toLowerCase());
                return 1;
            })));
            dispatcher.register(ClientCommandManager.literal("gputest").then(ClientCommandManager.argument(ARG_VENDOR, StringArgumentType.word()).suggests(TestCommands.vendorSuggestions()).executes(ctx -> {
                String v = StringArgumentType.getString(ctx, ARG_VENDOR).toLowerCase();
                VendorChoice choice = TestCommands.parseVendor(v);
                HudOverrides.setGpu(choice);
                TestCommands.send("GPU test mode: " + choice.name().toLowerCase());
                return 1;
            })));
            dispatcher.register(ClientCommandManager.literal("many_utils_reload").executes(ctx -> {
                HudTemplate.reload();
                TestCommands.send("many Utils: reloaded template");
                return 1;
            }));
            dispatcher.register(ClientCommandManager.literal("many_utils_template").executes(ctx -> {
                TestCommands.send("Template file: " + String.valueOf(HudTemplate.path().toAbsolutePath()));
                return 1;
            }));
        });
    }

    private static SuggestionProvider<FabricClientCommandSource> vendorSuggestions() {
        return (ctx, builder) -> {
            builder.suggest("intel");
            builder.suggest("amd");
            builder.suggest("default");
            return builder.buildFuture();
        };
    }

    private static VendorChoice parseVendor(String s) {
        if (s.equals("intel")) {
            return VendorChoice.INTEL;
        }
        if (s.equals("amd")) {
            return VendorChoice.AMD;
        }
        return VendorChoice.DEFAULT;
    }

    private static void send(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.sendMessage(Text.literal(msg), false);
        }
    }
}
