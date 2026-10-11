package me.anchorhelper.many_utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class PositionScreen extends Screen {
    private final Screen parent;
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private int originalX = 0;
    private int originalY = 0;
    private boolean wasMouseDown = false;

    protected PositionScreen(Screen parent) {
        super(Component.literal("Reposition HUD"));
        this.parent = parent;
    }

    public static void open(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        PositionScreen screen = new PositionScreen(parent);
        screen.originalX = Config.INSTANCE.x;
        screen.originalY = Config.INSTANCE.y;
        mc.setScreenAndShow(screen);
    }

    @Override
    protected void init() {
        int screenW = this.width;
        int screenH = this.height;
        int btnW = 100;
        int btnH = 20;
        int gap = 10;
        int totalW = btnW * 2 + gap;
        int startX = (screenW - totalW) / 2;
        int startY = screenH - btnH - 10;

        this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> {
            Config.save();
            Minecraft mc = Minecraft.getInstance();
            if (parent != null) {
                mc.setScreenAndShow(parent);
            } else {
                mc.setScreenAndShow(null);
            }
        }).bounds(startX, startY, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            Config.INSTANCE.x = originalX;
            Config.INSTANCE.y = originalY;
            Minecraft mc = Minecraft.getInstance();
            if (parent != null) {
                mc.setScreenAndShow(parent);
            } else {
                mc.setScreenAndShow(null);
            }
        }).bounds(startX + btnW + gap, startY, btnW, btnH).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double delta = -scrollY * 0.1;
        Config.INSTANCE.scale = Math.max(0.5, Math.min(4.0, Config.INSTANCE.scale + delta));
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        int screenW = this.width;
        int screenH = this.height;

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.level != null) {
            int mouseMask = org.lwjgl.sdl.SDLMouse.SDL_GetMouseState((java.nio.FloatBuffer) null, (java.nio.FloatBuffer) null);
            boolean mouseDown = (mouseMask & org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LMASK) != 0;

            if (mouseDown && !wasMouseDown) {
                dragging = true;
                dragOffsetX = mouseX - Config.INSTANCE.x;
                dragOffsetY = mouseY - Config.INSTANCE.y;
            }
            if (!mouseDown) {
                dragging = false;
            }
            if (dragging && mouseDown) {
                Config.INSTANCE.x = mouseX - dragOffsetX;
                Config.INSTANCE.y = mouseY - dragOffsetY;
            }
            wasMouseDown = mouseDown;
        }

        String note = "Scroll to rescale  |  Left Click + Drag to move";
        int textWidth = mc.font.width(note);
        extractor.text(mc.font, Component.literal(note), screenW - textWidth - 2, 2, 0xFFFFFFFF, true);

        super.extractRenderState(extractor, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
