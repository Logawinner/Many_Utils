package me.anchorhelper.many_utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class PositionScreen extends Screen {
    private final Screen parent;
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private int originalX = 0;
    private int originalY = 0;
    private boolean wasMouseDown = false;

    protected PositionScreen(Screen parent) {
        super(Text.literal("Reposition HUD"));
        this.parent = parent;
    }

    public static void open(Screen parent) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PositionScreen screen = new PositionScreen(parent);
        screen.originalX = Config.INSTANCE.x;
        screen.originalY = Config.INSTANCE.y;
        mc.setScreen(screen);
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

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), btn -> {
            Config.save();
            MinecraftClient mc = MinecraftClient.getInstance();
            if (parent != null) {
                mc.setScreen(parent);
            } else {
                mc.setScreen(null);
            }
        }).dimensions(startX, startY, btnW, btnH).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> {
            Config.INSTANCE.x = originalX;
            Config.INSTANCE.y = originalY;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (parent != null) {
                mc.setScreen(parent);
            } else {
                mc.setScreen(null);
            }
        }).dimensions(startX + btnW + gap, startY, btnW, btnH).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double delta = -vertical * 0.1;
        Config.INSTANCE.scale = Math.max(0.5, Math.min(4.0, Config.INSTANCE.scale + delta));
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int screenW = this.width;
        int screenH = this.height;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.world != null) {
            long window = mc.getWindow().getHandle();
            boolean mouseDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            
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
        int textWidth = mc.textRenderer.getWidth(note);
        context.drawTextWithShadow(mc.textRenderer, Text.literal(note), screenW - textWidth - 2, 2, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
