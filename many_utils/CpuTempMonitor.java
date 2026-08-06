package me.anchorhelper.many_utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CpuTempMonitor {
    private static volatile double cpuTemp = -1;
    private static volatile boolean supported = false;
    private static final List<String> CPU_VENDOR_NAMES = List.of(
        "x86_pkg_temp", "k10temp", "k8temp", "coretemp", "core_thermal",
        "acpi_thermal", "thermal", "it87", "nct6775", "nct6776",
        "nct6779", "fam15h_power", "asus", "acpi", "it86", "it8686",
        "nct6778", "nct6795", "nct6796", "nct6797", "nct6798", "nct6799",
        "f71882fg", "f71883", "f71889", "f71869", "f71882", "f71889_3",
        "acpi_thermal_zone", "acpi_thermal_rel", "thermal_zone", "cpu_cooling",
        "cpufreq", "cpufreq_thermal", "hwmon_vid", "lm69", "lm85", "lm86",
        "lm90", "lm9570", "lm9572", "lm9577", "lm9578", "lm9579", "lm96080",
        "pc87427", "pc87430", "pc87468", "pc87485", "pc87562", "pc87577",
        "sch5627", "sch5628", "intel_powerclamp", "intel_rapl"
    );
    private static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

    public static void start() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CpuTemp-Monitor");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                double temp = readCpuTemp();
                cpuTemp = temp;
                supported = temp >= 0;
            } catch (Exception e) {
                cpuTemp = -1;
                supported = false;
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    public static double getCpuTemp() {
        return cpuTemp;
    }

    public static boolean isSupported() {
        return supported;
    }

    private static double readCpuTemp() {
        if (OS.contains("linux")) {
            return readLinuxCpuTemp();
        } else if (OS.contains("win")) {
            return readWindowsCpuTemp();
        } else if (OS.contains("mac") || OS.contains("darwin")) {
            return readMacCpuTemp();
        }
        return -1;
    }

    private static double readLinuxCpuTemp() {
        Path hwmonDir = Path.of("/sys/class/hwmon");
        if (!Files.exists(hwmonDir)) {
            return -1;
        }

        double max = Double.NEGATIVE_INFINITY;
        boolean found = false;

        try (DirectoryStream<Path> hwmonStream = Files.newDirectoryStream(hwmonDir, "hwmon*")) {
            for (Path hwmon : hwmonStream) {
                String name;
                try {
                    name = Files.readString(hwmon.resolve("name")).trim().toLowerCase(Locale.ROOT);
                } catch (IOException e) {
                    continue;
                }

                if (!CPU_VENDOR_NAMES.contains(name)) {
                    continue;
                }

                found = true;
                try (DirectoryStream<Path> tempStream = Files.newDirectoryStream(hwmon, "temp*_input")) {
                    for (Path tempFile : tempStream) {
                        try {
                            double value = Integer.parseInt(Files.readString(tempFile).trim()) / 1000.0;
                            if (value > max) {
                                max = value;
                            }
                        } catch (NumberFormatException | IOException e) {
                            // Skip invalid readings
                        }
                    }
                }
            }
        } catch (IOException ignored) {}

        if (!found || max == Double.NEGATIVE_INFINITY) {
            // Fallback: scan all hwmon entries for any temperature
            try (DirectoryStream<Path> hwmonStream = Files.newDirectoryStream(hwmonDir, "hwmon*")) {
                for (Path hwmon : hwmonStream) {
                    try (DirectoryStream<Path> tempStream = Files.newDirectoryStream(hwmon, "temp*_input")) {
                        for (Path tempFile : tempStream) {
                            try {
                                double value = Integer.parseInt(Files.readString(tempFile).trim()) / 1000.0;
                                if (value > max) {
                                    max = value;
                                }
                                found = true;
                            } catch (NumberFormatException | IOException e) {
                                // Skip invalid readings
                            }
                        }
                    }
                }
            } catch (IOException ignored) {}
        }

        return found ? max : -1;
    }

    private static double readWindowsCpuTemp() {
        // Try WMI via PowerShell - get temperature from Win32_TemperatureProbe
        try {
            String[] commands = {
                "powershell", "-Command",
                "Get-CimInstance -Query \"SELECT * FROM Win32_TemperatureProbe\" | Select-Object -ExpandProperty CurrentReading | Where-Object {$_ -gt 0}"
            };
            ProcessBuilder pb = new ProcessBuilder(commands);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        try {
                            double temp = Double.parseDouble(line);
                            if (temp > 0 && temp < 150) {
                                return temp;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            p.destroy();
        } catch (Exception ignored) {}

        // Fallback: Try using MSAcpi_ThermalZoneTemperature via WMI
        try {
            String[] commands = {
                "powershell", "-Command",
                "Get-CimInstance -Query \"SELECT * FROM Win32_PerfFormattedData_CountersThermalZoneInformation\" | Select-Object -ExpandProperty Temperature | Where-Object {$_ -gt 0}"
            };
            ProcessBuilder pb = new ProcessBuilder(commands);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        try {
                            // Thermal zone temperature is in tenths of Kelvin
                            double tempK = Double.parseDouble(line);
                            if (tempK > 0) {
                                // Convert Kelvin to Celsius
                                double tempC = tempK / 10.0 - 273.15;
                                if (tempC > 0 && tempC < 150) {
                                    return tempC;
                                }
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            p.destroy();
        } catch (Exception ignored) {}

        return -1;
    }

    private static double readMacCpuTemp() {
        // Try powermetrics (requires sudo on older macOS, may work on newer versions)
        try {
            String[] commands = {"powermetrics", "-i", "1000", "-n", "1"};
            ProcessBuilder pb = new ProcessBuilder(commands);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Look for CPU Die temperature
                    if (line.contains("CPU Die") || line.contains("CPU die") || line.contains("cpu_die")) {
                        int degIdx = line.indexOf('°');
                        if (degIdx > 0) {
                            String numPart = line.substring(0, degIdx).trim();
                            int spaceIdx = numPart.lastIndexOf(' ');
                            if (spaceIdx > 0) {
                                numPart = numPart.substring(spaceIdx + 1);
                            }
                            try {
                                double temp = Double.parseDouble(numPart);
                                if (temp > 0 && temp < 150) {
                                    return temp;
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
            p.destroy();
        } catch (Exception ignored) {}

        // Fallback: Try sysctl
        try {
            String[] commands = {"sysctl", "-n", "machdep.cpu.temperature"};
            ProcessBuilder pb = new ProcessBuilder(commands);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line != null) {
                    line = line.trim();
                    try {
                        double temp = Double.parseDouble(line);
                        if (temp > 0 && temp < 150) {
                            return temp;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            p.destroy();
        } catch (Exception ignored) {}

        return -1;
    }
}
