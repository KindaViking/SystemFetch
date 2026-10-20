package systemfetch;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import java.util.List;
import java.util.Scanner;

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

        //System.out.println("PCI");
        //System.out.println(pciInfo.report(pciInfo.readAll()));
        idLookup lookup = new idLookup();

        //sorting pci
        List<pciInfo.PciDevice> pciDevices = pciInfo.readAll();
        //System.out.println("PCI (By Vendor)");
        //System.out.println(pciInfo.report(pciInfo.sortByVendor(pciDevices, lookup)));

        //System.out.println("PCI (By Bus, Device or Function)");
        //System.out.println(pciInfo.report(pciInfo.sortByAddress(pciDevices)));

        //add a loop to ask which sort the user would rather see
        Scanner scanner = new Scanner(System.in);
        //keep asking until quit is received
        while (true){
            System.out.println("Sort PCI devices (type: unsorted/vendor/address/q)");
            String choice = scanner.nextLine().strip().toLowerCase();

            if (choice.equals("q")){
                break;
            }
            switch (choice){
                case "unsorted" ->{
                    System.out.println("Unsorted PCI devices");
                    System.out.println(pciInfo.report(pciInfo.readAll()));
                }
                case "vendor" ->{
                    System.out.println("PCI (By vendor)");
                    System.out.println(pciInfo.report(pciInfo.sortByVendor(pciDevices, lookup)));
                }
                case "address" -> {
                    System.out.println("PCI (By Bus, Device, Function)");
                    System.out.println(pciInfo.report(pciInfo.sortByAddress(pciDevices)));
                }
                default -> System.out.println("Unknown option, re-enter input");
            }
        }
        scanner.close();

    }

}
