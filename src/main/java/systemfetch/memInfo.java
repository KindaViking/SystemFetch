package systemfetch;

import oshi.hardware.GlobalMemory;

public class memInfo {
    private static final double GIB = 1024.0 * 1024.0 * 1024.0; // gibibyte = 1024^3 bytes

    public static long getTotal(GlobalMemory memory) {
        return memory.getTotal();
    }

    public static long getUsed(GlobalMemory memory) {
        return memory.getTotal() - memory.getAvailable();
    }

    public static double getPercentUsed(GlobalMemory memory) {
        return 100.0 * getUsed(memory) / getTotal(memory);
    }

    public static String report(GlobalMemory memory) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total memory: %.2f GiB%n", getTotal(memory) / GIB));
        sb.append(String.format("Used memory: %.2f GiB%n", getUsed(memory) / GIB));
        sb.append(String.format("Memory in use: %.1f%%%n", getPercentUsed(memory)));
        return sb.toString();
    }
}
