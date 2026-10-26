package systemfetch;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import java.util.List;
import java.util.Scanner;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        // SystemInfo systemInfo = new SystemInfo();
        // CentralProcessor processor = systemInfo.getHardware().getProcessor();
        HardwareAbstractionLayer hal = new SystemInfo().getHardware();

        String cpuText = "";
        String memText = "";

        // String name = processor.getProcessorIdentifier().getName();
        try {
            // CPU now read directly from Linux /proc
            cpuText = cpuInfo.report();
            //System.out.println("Your CPU is: " + cpuText);

            memText = memInfo.report();
            //System.out.print(memText);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Load measurament interrupted\n");
        }
        List<usbInfo.UsbDevice> usbDevices = usbInfo.readAll();
        //System.out.println("USB");
        //System.out.print(usbInfo.report(usbDevices));
        idLookup lookup = new idLookup();

        //sorting pci
        List<pciInfo.PciDevice> pciDevices = pciInfo.readAll();

        //calling htmlReport to write data to html file and display it
        String html = htmlReport.build(cpuText, memText, usbDevices, pciDevices, lookup);
        Path reportPath = htmlReport.write(html);
        if (reportPath != null){
            htmlReport.open(reportPath);
        }
/*
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
*/
    }

}
