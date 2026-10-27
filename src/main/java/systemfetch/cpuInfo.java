package systemfetch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


public class cpuInfo {

    private static final Path CPU_INFO_PATH = Path.of("/proc/cpuinfo");
    private static final Path STAT_PATH = Path.of("/proc/stat");

    public record Load(double all, double[] perCore) {
        public Load {
            perCore = perCore.clone(); // to prevent the caller from modifying the array
        }

        @Override
        public double[] perCore() {
            return perCore.clone();
        }
    }

    // Snapshot of the cumulative CPU counters read from /proc/stat
    private record CpuTimes(long busy, long idle) {}

    // Snapshot containing aggregate and per-logical-CPU counters
    private record CpuSnapshot(CpuTimes total, List<CpuTimes> cores) {}

    public static String getCpuModel () {
        List<String> lines = readLines(CPU_INFO_PATH);

        String model = firstField(lines, "model name");
        if (!model.isBlank()) { // if model is not blank
            return model;
        }

        String implementer = firstField(lines, "CPU implementer");
        String part = firstField(lines, "CPU part");

        if (!implementer.isBlank() || !part.isBlank()) {
            return "Model name unavailable"
                    + " (implementer: "
                    + (implementer.isBlank() ? "unknown" : implementer)
                    + ", part: "
                    + (part.isBlank() ? "unknown" : part)
                    + ")";
        }

        return "Model name unavailable";
    }

    public static int getLogicalCount() {
        int count = 0;
        for (String line : readLines(CPU_INFO_PATH)) {
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).trim().equals("processor")) { // looking for processor lines
                count++;
            }
        }
        if (count > 0) {
            return count;
        }
        try {
            int snapshotCores = readSnapshot().cores().size();
            if (snapshotCores > 0) {
                return snapshotCores;
            }
        } catch (IllegalStateException ignored) {
        }
        return Runtime.getRuntime().availableProcessors();
    }

    public static int getPhysicalCount() {
        List<String> lines = readLines(CPU_INFO_PATH);
        Set<String> physicalCores = new HashSet<>();

        String physicalId = null;
        String coreId = null;

        for (String line : lines) {
            if (line.isBlank()) {
                addPhysicalCore(physicalCores, physicalId, coreId);
                physicalId = null;
                coreId = null;
                continue;
            }

            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }

            String key = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();

            if (key.equals("physical id")) {
                physicalId = value;
            } else if (key.equals("core id")) {
                coreId = value;
            }
        }

        addPhysicalCore(physicalCores, physicalId, coreId);

        return physicalCores.isEmpty() ? -1 : physicalCores.size();
    }

    private static void addPhysicalCore(Set<String> cores, String physicalId, String coreId) {
        if (physicalId != null && coreId != null) {
            cores.add(physicalId + ":" + coreId);
        }
    }

    // reads cpu field from the first processor block that contains it
    // else return empty string
    private static String firstField(List<String> lines, String wantedField) {
        for (String line : lines) {
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0,colon).trim().equals(wantedField)) {
                return line.substring(colon+1).trim();
            }
        }
        return "";
    }

    /*
     * Measures CPU load over the requested interval.
     *
     * @param milliseconds time between readings; must be zero or greater
     * @return load fractions from 0.0 to 1.0 for the aggregate and each logical CPU
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public static Load findLoad(long milliseconds) throws InterruptedException {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("milliseconds must be zero or greater");
        }

        CpuSnapshot before;
        try {
            before = readSnapshot();
        } catch (IllegalStateException e) {
            return new Load(0.0, new double[0]);
        }

        Thread.sleep(milliseconds);

        CpuSnapshot after;
        try {
            after = readSnapshot();
        } catch (IllegalStateException e) {
            return new Load(0.0, new double[0]);
        }

        double overall = calculateLoad(before.total(), after.total());
        int count = Math.min(before.cores().size(), after.cores().size());
        double[] perCore = new double[count];

        for (int i = 0; i < count; i++){
            perCore[i] = calculateLoad(before.cores().get(i), after.cores().get(i));
        }

        return new Load(overall, perCore);
    }
/* OSHI BASED (maybe can use as a fallback for non-Linux systems if we feel fancy)

    public static String getCpuModel(CentralProcessor cpu){
        return cpu.getProcessorIdentifier().getName();
    }

    public static int getPhysicalCount(CentralProcessor cpu){
        return cpu.getPhysicalProcessorCount();
    }
    public static int getLogicalCount(CentralProcessor cpu){
        return cpu.getLogicalProcessorCount();
    }
    public static Load findLoad(CentralProcessor cpu, long mil) throws InterruptedException {
        long[] sysBefore = cpu.getSystemCpuLoadTicks();
        long[][] coresBefore = cpu.getProcessorCpuLoadTicks();
        Thread.sleep(mil);
        return new Load(cpu.getSystemCpuLoadBetweenTicks(sysBefore), cpu.getProcessorCpuLoadBetweenTicks(coresBefore));
    }
*/

    public static String report() throws InterruptedException {
        StringBuilder sb = new StringBuilder();
        int physical = getPhysicalCount();
        int logical = getLogicalCount();

        sb.append("Model: ").append(getCpuModel()).append("\n");
        sb.append("Physical Cores: ")
                .append(physical >= 0 ? physical : "unavailable")
                .append('\n');
        sb.append("Logical cores: ").append(logical).append("\n");

        Load load = findLoad(1000);
        sb.append("Load per logical CPU:\n");
        double[] perCore = load.perCore();
        if (perCore.length == 0) {
            sb.append("  unavailable\n");
        } else {
            for (int i = 0; i < perCore.length; i++)
                sb.append(String.format(Locale.ROOT, "  CPU %d: %.1f%%%n", i, perCore[i] * 100));
        }
        sb.append(String.format(Locale.ROOT, "Overall load: %.1f%%%n", load.all() * 100));
        return sb.toString();
    }
    //SystemInfo si = new SystemInfo();
    //HardwareAbstractionLayer hal = si.getHardware();
    //CentralProcessor cpu = hal.getProcessor();
    //String name = cpu.getProcessorIdentifier().getName();

    private static CpuSnapshot readSnapshot() {
        List<String> lines = readLines(STAT_PATH);
        CpuTimes total = null;
        List<CpuTimes> cores = new ArrayList<>();

        for (String line : lines){
            if (!line.startsWith("cpu"))
                continue;

            String[] fields = line.trim().split("\\s+");
            if (fields.length < 2) {
                continue;
            }

            String label = fields[0];
            if (label.equals("cpu")) {
                total = parseCpuTimes(fields);
            } else if (label.matches("cpu\\d+")) {
                int cpuNumber;
                try {
                    cpuNumber = Integer.parseInt(label.substring(3));
                } catch (NumberFormatException e) {
                    continue;
                }
                while (cores.size() <= cpuNumber)
                    cores.add(null);

                cores.set(cpuNumber, parseCpuTimes(fields));
            }
        }
        cores.removeIf(Objects::isNull); // value -> value == null (advised by autocomplete)
        if (total == null && cores.isEmpty()) {
            throw new IllegalStateException(
                    "Unable to read CPU counters from /proc/stat."
            );
        }

        if (total == null) {
            total = sumCores(cores);
        }

        return new CpuSnapshot(total, List.copyOf(cores));
    }

    private static CpuTimes parseCpuTimes(String[] fields) {
        long user = valueAt(fields, 1);
        long nice = valueAt(fields, 2);
        long system = valueAt(fields, 3);
        long idle = valueAt(fields, 4);
        long iowait = valueAt(fields,5);
        long irq = valueAt(fields, 6);
        long softirq = valueAt(fields, 7);
        long steal = valueAt(fields, 8);

        long busy = user + nice + system + irq + softirq + steal;
        return new CpuTimes(busy, idle + iowait);
    }

    private static long valueAt(String[] fields, int index) {
        if (index >= fields.length) {
            return 0L;
        }
        try {
            return Long.parseLong(fields[index]);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static CpuTimes sumCores(List<CpuTimes> cores) {
        long busy = 0;
        long idle = 0;
        for (CpuTimes core : cores) {
            busy += core.busy;
            idle += core.idle;
        }
        return new CpuTimes(busy, idle);
    }

    // calculate fraction of elapsed CPU time spent busy between snapshots
    private static double calculateLoad(CpuTimes before, CpuTimes after) {
        long busyDelta = after.busy() - before.busy();
        long idleDelta = after.idle() - before.idle();

        // Invalid counters or no elapsed CPU time.
        if (busyDelta < 0 || idleDelta < 0) {
            return 0.0;
        }

        long totalDelta = busyDelta + idleDelta;
        if (totalDelta <= 0) {
            return 0.0;
        }

        return (double) busyDelta / totalDelta;
    }

    private static List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException | SecurityException e) {
            return List.of();
        }
    }
}

