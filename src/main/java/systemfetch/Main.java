package systemfetch;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

public class Main {
    public static void main(String[] args) {
        SystemInfo systemInfo = new SystemInfo();
        // CentralProcessor processor = systemInfo.getHardware().getProcessor();
        HardwareAbstractionLayer hal = new SystemInfo().getHardware();

        // String name = processor.getProcessorIdentifier().getName();
        try {
            String cpuName = cpuInfo.report(hal.getProcessor());
            System.out.println("Your CPU is: " + cpuName);
            System.out.print(memInfo.report(hal.getMemory()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Load measurament interrupted\n");
        }
        System.out.println("USB");
        System.out.print(usbInfo.report(usbInfo.readAll()));

        System.out.println("PCI");
        System.out.println(pciInfo.report(pciInfo.readAll()));
        idLookup lookup = new idLookup();

    }

}
