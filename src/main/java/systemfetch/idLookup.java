
package systemfetch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class idLookup {

    private final Map<Integer, String> pciVendors = new HashMap<>();
    private final Map<String, String> pciDevices = new HashMap<>();
    private final Map<Integer, String> usbVendors = new HashMap<>();
    private final Map<String, String> usbProducts = new HashMap<>();

    public idLookup() {
        loadPciIds(Path.of("data/pci.ids"));
        loadUsbIds(Path.of("data/usb.ids"));
    }

    private void loadPciIds(Path file) {
        int currentVendorId = -1;

        try {
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }

                if (Character.isWhitespace(line.charAt(0))) {
                    if (line.startsWith("\t\t")) {
                        continue;
                    }

                    String[] parts = line.trim().split("\\s+", 2);

                    if (currentVendorId >= 0
                            && parts.length == 2
                            && parts[0].matches("[0-9a-fA-F]{4}")) {
                        String key = String.format(
                                "%04x:%s",
                                currentVendorId,
                                parts[0].toLowerCase());
                        pciDevices.put(key, parts[1]);
                    }
                } else {
                    String[] parts = line.trim().split("\\s+", 2);

                    if (parts.length == 2
                            && parts[0].matches("[0-9a-fA-F]{4}")) {
                        currentVendorId = Integer.parseInt(parts[0], 16);
                        pciVendors.put(currentVendorId, parts[1]);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read PCI ID file: " + file);
        }
    }

    private void loadUsbIds(Path file) {
        int currentVendorId = -1;

        try {
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }

                if (Character.isWhitespace(line.charAt(0))) {
                    if (line.startsWith("\t\t")) {
                        continue;
                    }

                    String[] parts = line.trim().split("\\s+", 2);

                    if (currentVendorId >= 0
                            && parts.length == 2
                            && parts[0].matches("[0-9a-fA-F]{4}")) {
                        String key = String.format(
                                "%04x:%s",
                                currentVendorId,
                                parts[0].toLowerCase());
                        usbProducts.put(key, parts[1]);
                    }
                } else {
                    String[] parts = line.trim().split("\\s+", 2);

                    if (parts.length == 2
                            && parts[0].matches("[0-9a-fA-F]{4}")) {
                        currentVendorId = Integer.parseInt(parts[0], 16);
                        usbVendors.put(currentVendorId, parts[1]);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read USB ID file: " + file);
        }
    }

    public String pciVendorName(int vendorId) {
        if (vendorId < 0) {
            return "unknown";
        }

        return pciVendors.getOrDefault(
                vendorId, String.format("0x%04x", vendorId));
    }

    public String pciDeviceName(int vendorId, int deviceId) {
        if (deviceId < 0) {
            return "unknown";
        }

        String key = String.format("%04x:%04x", vendorId, deviceId);

        return pciDevices.getOrDefault(
                key, String.format("0x%04x", deviceId));
    }

    public String usbVendorName(int vendorId) {
        if (vendorId < 0) {
            return "unknown";
        }

        return usbVendors.getOrDefault(
                vendorId, String.format("0x%04x", vendorId));
    }

    public String usbProductName(int vendorId, int productId) {
        if (productId < 0) {
            return "unknown";
        }

        String key = String.format("%04x:%04x", vendorId, productId);

        return usbProducts.getOrDefault(
                key, String.format("0x%04x", productId));
    }
}
