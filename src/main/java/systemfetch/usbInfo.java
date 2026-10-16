package systemfetch;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class usbInfo {
    private static final Path DEFAULT_ROOT = Path.of("/sys/bus/usb/devices");

    /** One USB device. IDs are -1 if they could not be read. */
    public record UsbDevice(String name, int vendorId, int productId) {}

    public static List<UsbDevice> readAll() {
        return readAll(DEFAULT_ROOT);
    }

    /** Takes the folder as a parameter so it can be tested with a fake one. */
    public static List<UsbDevice> readAll(Path root) {
        List<UsbDevice> devices = new ArrayList<>();

        if (!Files.isDirectory(root)) {
            return devices;
        }

        try (Stream<Path> entries = Files.list(root)) {
            for (Path entry : entries.sorted().toList()) {
                // Interface entries such as 1-2:1.0 have no idVendor file
                if (!Files.exists(entry.resolve("idVendor"))) {
                    continue;
                }
                int vendorId = readHex(entry.resolve("idVendor"));
                int productId = readHex(entry.resolve("idProduct"));
                devices.add(new UsbDevice(entry.getFileName().toString(), vendorId, productId));
            }
        } catch (IOException | UncheckedIOException e) {
            //keep whatever was there before failure
        }

        return devices;
    }

    /** Reads a hex number from a file, with or without a 0x prefix. Returns -1 on failure. */
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

    public static String report(List<UsbDevice> devices) {
        if (devices.isEmpty()) {
            return "No USB devices found.\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("USB devices: ").append(devices.size()).append("\n");
        for (UsbDevice d : devices) {
            sb.append(String.format("  %s  vendor %s  product %s%n",
                      d.name(), hex(d.vendorId()), hex(d.productId())));
        }
        return sb.toString();
    }

    private static String hex(int id) {
        return id < 0 ? "unknown" : String.format("0x%04x", id);
    }
}
