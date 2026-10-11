package me.anchorhelper.many_utils;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import org.lwjgl.sdl.SDLMouse;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;

public class ManyUtils implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();
        Metrics.start();
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("many_utils", "hud"), (extractor, deltaTracker) -> Hud.draw(extractor));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Metrics.resetFps();
            Metrics.resetTps();
            if (FirstRun.markIfFirst()) {
                Minecraft mc = client;
                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal("Gpu Cpu Util"));
                        mc.player.sendSystemMessage(Component.literal("Open the settings in Mod Menu to change things."));
                        mc.player.sendSystemMessage(Component.literal("Without Shaders - recommended no animations"));
                        mc.player.sendSystemMessage(Component.literal("With Shaders - It doesn't matter, either one is fine"));
                    }
                });
            }
        });
        TestCommands.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null) return;
            Minecraft mc = client;
            if (mc == null || mc.level == null) return;
            int mouseMask = SDLMouse.SDL_GetMouseState((java.nio.FloatBuffer) null, (java.nio.FloatBuffer) null);
            boolean leftPressed = (mouseMask & SDLMouse.SDL_BUTTON_LMASK) != 0;
            boolean rightPressed = (mouseMask & SDLMouse.SDL_BUTTON_RMASK) != 0;
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
