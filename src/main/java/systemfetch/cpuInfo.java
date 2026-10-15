package systemfetch;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;


public class cpuInfo {
    public record Load(double all, double[] perCore) {}
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
    public static String report(CentralProcessor cpu) throws InterruptedException {
        StringBuilder sb = new StringBuilder();
        sb.append("Model: ").append(getCpuModel(cpu)).append("\n");
        sb.append("Physical Cores: ").append(getPhysicalCount(cpu)).append("\n");
        sb.append("Logical cores: ").append(getLogicalCount(cpu)).append("\n");

        Load load = findLoad(cpu, 1000);
        sb.append("Load per logical CPU:\n");
        for (int i = 0; i < load.perCore().length; i++)
            sb.append(String.format("  CPU %d: %.1f%%%n", i, load.perCore()[i] * 100));
        sb.append(String.format("Overall load: %.1f%%%n", load.all() * 100));
        return sb.toString();
    }
}
//SystemInfo si = new SystemInfo();
//HardwareAbstractionLayer hal = si.getHardware();
//CentralProcessor cpu = hal.getProcessor();
//String name = cpu.getProcessorIdentifier().getName();
