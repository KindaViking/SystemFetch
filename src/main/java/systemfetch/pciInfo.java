package systemfetch;

import java.util.Comparator;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class pciInfo {
    private static final Path ROOT = Path.of("/sys/bus/pci/devices");

    // decalre immutable data holder
    public record PciDevice(String address, int vendorId, int deviceId, int classId) {
    }

    public static List<PciDevice> readAll() {
        return readAll(ROOT);
    }

    /** Takes the folder as a parameter so it can be tested with a fake one. */
    // makes empty list, if there usb device folder doesn't exist, return an empty
    // list
    public static List<PciDevice> readAll(Path root) {
        List<PciDevice> devices = new ArrayList<>();

        if (!Files.isDirectory(root)) {
            return devices;
        }

        // gives a stream of everything in the folder, sorts them and adds them as
        // strings to ArrayList devices
        try (Stream<Path> entries = Files.list(root)) {
            for (Path entry : entries.sorted().toList()) {
                // every pci device should have a vendor file but if not, skip
                if (!Files.exists(entry.resolve("vendor"))) {
                    continue;
                }
                int vendorId = readHex(entry.resolve("vendor"));
                int deviceId = readHex(entry.resolve("device"));
                int classId = readHex(entry.resolve("class"));
                devices.add(new PciDevice(entry.getFileName().toString(), vendorId, deviceId, classId));
            }
        } catch (IOException | UncheckedIOException e) {
            // keep whatever was there before failure
        }

        return devices;
    }

    // reads a hex number from file, removes 0x, if it fails it returns -1
    private static int readHex(Path file) {
        try {
            String text = Files.readString(file).strip();
            if (text.startsWith("0x") || text.startsWith("0X")) {
                text = text.substring(2);
            }
            return Integer.parseInt(text, 16);
        } catch (IOException | NumberFormatException e) {
            return -1;
        }
    }

    // public static String report(List<PciDevice> devices) {
    // if (devices.isEmpty()) {
    // return "No PCI devices found.\n";
    // }
    // StringBuilder sb = new StringBuilder();
    // sb.append("Pci devices: ").append(devices.size()).append("\n");
    // for (PciDevice d : devices) {
    // sb.append(String.format(" %s vendor %s device %s class %s%n",
    // d.address(), hex(d.vendorId()), hex(d.deviceId()), classHex(d.classId())));
    // }
    // return sb.toString();
    // }

    public static String report(List<PciDevice> devices) {
        if (devices.isEmpty()) {
            return "No PCI devices found.\n";
        }

        idLookup lookup = new idLookup();

        StringBuilder sb = new StringBuilder();
        sb.append("PCI devices: ").append(devices.size()).append("\n");

        for (PciDevice d : devices) {
            String vendorName = lookup.pciVendorName(d.vendorId());
            String deviceName = lookup.pciDeviceName(d.vendorId(), d.deviceId());

            sb.append(String.format(
                    "  %s  %s - %s  class %s%n",
                    d.address(),
                    vendorName,
                    deviceName,
                    classHex(d.classId())));
        }

        return sb.toString();
    }

    private static String hex(int id) {
        return id < 0 ? "unknown" : String.format("0x%04x", id);
    }

    private static String classHex(int id) {
        return id < 0 ? "unknown" : String.format("0x%06x", id);
    }

    /*
     *PCI Device sorting methods begin here
    */
    private static int addressPart(PciDevice device, int index){
        String[] parts = device.address().split("[:.]");
        return Integer.parseInt(parts[index], 16);
    }
    private static int bus(PciDevice device){
        return addressPart(device, 1);
    }
    private static int device(PciDevice device){
        return addressPart(device, 2);
    }
    private static int function(PciDevice device){
        return addressPart(device, 3);
    }
}
