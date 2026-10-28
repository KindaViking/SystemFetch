package systemfetch;

import java.util.List;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        String os = System.getProperty("os.name", "");
        if (!os.toLowerCase().contains("linux")) {
            System.out.println("This system is not running Linux (detected: " + os + "). SystemFetch only supports Linux.");
            return;
        }

        String cpuText = "";
        String memText = "";

        // String name = processor.getProcessorIdentifier().getName();
        try {
            // CPU now read directly from Linux /proc
            cpuText = cpuInfo.report();
            System.out.println("Your CPU is: " + cpuText);

            memText = memInfo.report();
            System.out.print(memText);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Load measurament interrupted\n");
        }
        List<usbInfo.UsbDevice> usbDevices = usbInfo.readAll();
        System.out.println("USB");
        System.out.print(usbInfo.report(usbDevices));
        idLookup lookup = new idLookup();

        //sorting pci
        List<pciInfo.PciDevice> pciDevices = pciInfo.readAll();

        //print sorted PCI lists
        System.out.println("PCI (By vendor)");
        System.out.println(pciInfo.report(pciInfo.sortByVendor(pciDevices, lookup)));

        System.out.println("PCI (By Bus, Device, Function)");
        System.out.println(pciInfo.report(pciInfo.sortByAddress(pciDevices)));

        //PCI summary
        System.out.println("PCI Summary");
        System.out.print(pciInfo.vendorSummary(pciDevices, lookup));
        System.out.println();
        System.out.print(pciInfo.classSummary(pciDevices));

        //calling htmlReport to write data to html file and display it
        String html = htmlReport.build(cpuText, memText, usbDevices, pciDevices, lookup);
        Path reportPath = htmlReport.write(html);
        if (reportPath != null){
            htmlReport.open(reportPath);
        }

    }

}
