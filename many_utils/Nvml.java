package me.anchorhelper.many_utils;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.Structure;

import java.util.List;

public final class Nvml {
    public static boolean isAvailable() {
        try {
            NativeLibrary.getInstance("nvml");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private Nvml() {}

    public static class Utilization extends Structure {
        public int gpu;
        public int memory;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("gpu", "memory");
        }
    }

    public static interface NVML extends Library {
        NVML INSTANCE = Native.load("nvml", NVML.class);

        int nvmlInit_v2();
        int nvmlShutdown();
        int nvmlDeviceGetCount_v2(IntByReference count);
        int nvmlDeviceGetHandleByIndex_v2(int index, PointerByReference device);
        int nvmlDeviceGetName(Pointer device, byte[] name, int length);
        int nvmlDeviceGetUtilizationRates(Pointer device, Utilization rates);
        int nvmlDeviceGetTemperature(Pointer device, int sensorType, IntByReference temp);
    }
}
