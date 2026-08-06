package me.anchorhelper.mc_utils;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class MinecraftUtils implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();
        Metrics.start();
        HudRenderCallback.EVENT.register((context, tickDelta) -> Hud.draw(context));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (FirstRun.markIfFirst()) {
                MinecraftClient mc = client;
                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.player.sendMessage(Text.literal("Gpu Cpu Util"), false);
                        mc.player.sendMessage(Text.literal("Open the settings in Mod Menu to change things."), false);
                        mc.player.sendMessage(Text.literal("Without Shaders - recommended no animations"), false);
                        mc.player.sendMessage(Text.literal("With Shaders - It doesn't matter, either one is fine"), false);
                    }
                });
            }
        });
        TestCommands.register();
    }
}
