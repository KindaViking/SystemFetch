package systemfetch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class memInfo {
    private static final Path MEM_INFO_PATH = Path.of("/proc/meminfo");
    private static final long BYTES_PER_KIB = 1024L;
    private static final double GIB = 1024.0 * 1024.0 * 1024.0; // gibibyte = 1024^3 bytes

    private memInfo() {} // Prevent instantiation

    // total memory in bytes or -1 if not available
    public static long getTotal() {
        long value = getFieldKiB("MemTotal");
        return value < 0 ? -1 : value * BYTES_PER_KIB; // return value * BYTES_PER_KIB or -1 if its value is less than 0
    }
    /*
    public static long getTotal(GlobalMemory memory) {
        return memory.getTotal();
    }*/

    // available memory in bytes or -1 if not available
    public static long getAvailable() {
        long value = getFieldKiB("MemAvailable");
        return value < 0 ? -1 : value * BYTES_PER_KIB;
    }

    // used memory in bytes: total minus available, or -1 if not available
    public static long getUsed(){
        long total = getTotal();
        long available = getAvailable();
        if (total < 0 || available < 0 || available > total) {
            return -1;
        }
        return total - available;
    }
    /*
    public static long getUsed(GlobalMemory memory) {
        return memory.getTotal() - memory.getAvailable();
    }*/

    // percentage used, or -1
    public static double getPercentUsed(){
        long total = getTotal();
        long used = getUsed();
        return total <= 0 || used < 0 ? -1.0 : 100.0 * used / total;
    }
    /*
    public static double getPercentUsed(GlobalMemory memory) {
        return 100.0 * getUsed(memory) / getTotal(memory);
    }*/

    // read named /proc/meminfo field in KiB, or -1; lovely helper
    public static long getFieldKiB(String name) {
        List<String> lines;

        try {
            lines = Files.readAllLines(MEM_INFO_PATH);
        } catch (IOException | SecurityException e) {
            return -1;
        }

        for (String line : lines) {
            if (line.startsWith(name + ":")) {
                String value = line.substring(name.length() + 1).trim();
                String[] parts = value.split("\\s+");

                if (parts.length == 0) {
                    return -1;
                }

                try {
                    return Long.parseLong(parts[0]);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    public static String report() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total memory: %.2f GiB%n", getTotal() / GIB));
        sb.append(String.format("Used memory: %.2f GiB%n", getUsed() / GIB));
        sb.append(String.format("Memory in use: %.1f%%%n", getPercentUsed()));
        return sb.toString();
    }
}
