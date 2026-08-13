package me.anchorhelper.many_utils;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.lwjgl.glfw.GLFW;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class ManyUtils implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();
        Metrics.start();
        HudRenderCallback.EVENT.register((context, tickDelta) -> Hud.draw(context));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Metrics.resetFps();
            Metrics.resetTps();
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
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null) return;
            MinecraftClient mc = client;
            if (mc == null || mc.world == null) return;
            long window = mc.getWindow().getHandle();
            boolean leftPressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean rightPressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            if (leftPressed && !Metrics.prevLeftPressed) {
                Metrics.recordClick(true);
            }
            if (rightPressed && !Metrics.prevRightPressed) {
                Metrics.recordClick(false);
            }
            Metrics.prevLeftPressed = leftPressed;
            Metrics.prevRightPressed = rightPressed;
        });
    }
}
