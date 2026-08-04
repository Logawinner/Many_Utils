package me.anchorhelper.gpucpu_util;

public class HudOverrides {
    private static volatile VendorChoice cpu = VendorChoice.DEFAULT;
    private static volatile VendorChoice gpu = VendorChoice.DEFAULT;

    public static void setCpu(VendorChoice v) {
        cpu = v == null ? VendorChoice.DEFAULT : v;
    }

    public static void setGpu(VendorChoice v) {
        gpu = v == null ? VendorChoice.DEFAULT : v;
    }

    public static VendorChoice getCpu() {
        return cpu;
    }

    public static VendorChoice getGpu() {
        return gpu;
    }
}
