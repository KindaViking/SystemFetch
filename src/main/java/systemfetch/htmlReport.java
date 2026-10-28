package systemfetch;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class htmlReport {
    //build makes the whole HTML page as one giant string
    public static String build(String cpuText, String memText,
                               List<usbInfo.UsbDevice> usbDevices,
                               List<pciInfo.PciDevice> pciDevices,
                               idLookup lookup) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>System Report</title>
                <style>
                """);
        sb.append(STYLE);
        sb.append("""
                  </style>
                  </head>
                  <body>
                <h1>System Report</h1>
                """);

        //CPU: <pre> keeps the formatting from cpuInfo.report
        sb.append("<h2>CPU</h2>\n");
        sb.append("<pre id=\"cpu\">").append(escape(cpuText)).append("</pre>\n");
        // filled in with a bar chart of each CPU's load when the page comes from liveServer
        sb.append("<div id=\"cpu-chart\"></div>\n");

        //memory
        sb.append("<h2>Memory</h2>\n");
        sb.append("<pre id=\"memory\">").append(escape(memText)).append("</pre>\n");

        //USB
        sb.append("<h2>USB devices</h2>\n");
        sb.append(usbTable(usbDevices, lookup));

        //PCI: the same table method is used for both sorts
        sb.append("<h2>PCI devices</h2>\n");
        sb.append("<h3>Sorted by vendor</h3>\n");
        sb.append(pciTable(pciInfo.sortByVendor(pciDevices, lookup), lookup));
        sb.append("<h3>Sorted by bus, device, function</h3>\n");
        sb.append(pciTable(pciInfo.sortByAddress(pciDevices), lookup));

        sb.append(LIVE_SCRIPT);
        sb.append("</body>\n");
        sb.append("</html>\n");

        return sb.toString();
    }
        //CSS for the pages style
    private static final String STYLE = """
            body {
                font-family: system-ui, sans-serif;
                margin: 0 auto;
                padding: 16px;
                max-width: 960px;
                line-height: 1.5;
                color: #222;
            }
            h1 { margin-bottom: 0.5em; }
            h2 { margin-top: 2em; border-bottom: 1px solid #ccc; }
            pre {
                background: #f4f4f4;
                padding: 12px;
                overflow-x: auto;
            }
            .table-wrap { overflow-x: auto; }
            table {
                border-collapse: collapse;
                width: 100%;
            }
            th, td {
                text-align: left;
                padding: 6px 10px;
                border-bottom: 1px solid #ddd;
            }
            th { background: #f4f4f4; }
            """;

    // when the page comes from liveServer, asks it for new CPU and memory text every second
    // does nothing when report.html is opened as a file
    private static final String LIVE_SCRIPT = """
            <script>
              async function update() {
                const response = await fetch("/data");
                const data = await response.json();
                document.getElementById("cpu").textContent = data.cpu;
                document.getElementById("memory").textContent = data.memory;
                document.getElementById("cpu-chart").innerHTML = data.cpuChart;
                setTimeout(update, 1000);
              }

              if (location.protocol.startsWith("http")) {
                update();
              }
            </script>
            """;

    // replaces characters that have special meaning in HTML
    // & must be replaced first, or the & in &lt; would get replaced too
    public static String escape(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    //one table row per PCI device, in whatever order the list is in
    private static String pciTable(List<pciInfo.PciDevice> devices, idLookup lookup) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"table-wrap\">\n<table>\n");
        sb.append("<tr><th>Address</th><th>Vendor</th><th>Device</th><th>Class</th></tr>\n");

        for (pciInfo.PciDevice d : devices) {
            String vendorName = lookup.pciVendorName(d.vendorId());
            String deviceName = lookup.pciDeviceName(d.vendorId(), d.deviceId());

            sb.append("<tr>");
            sb.append("<td>").append(escape(d.address())).append("</td>");
            sb.append("<td>").append(escape(vendorName)).append("</td>");
            sb.append("<td>").append(escape(deviceName)).append("</td>");
            sb.append("<td>").append(hex(d.classId(), 6)).append("</td>");
            sb.append("</tr>\n");
        }

        sb.append("</table>\n</div>\n");
        return sb.toString();
    }

        //one table row per USB device, with names from the lookup like the terminal report
    private static String usbTable(List<usbInfo.UsbDevice> devices, idLookup lookup) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"table-wrap\">\n<table>\n");
        sb.append("<tr><th>Port</th><th>Vendor</th><th>Product</th></tr>\n");

        for (usbInfo.UsbDevice d : devices) {
            String vendorName = lookup.usbVendorName(d.vendorId());
            String productName = lookup.usbProductName(d.vendorId(), d.productId());

            sb.append("<tr>");
            sb.append("<td>").append(escape(d.name())).append("</td>");
            sb.append("<td>").append(escape(vendorName)).append("</td>");
            sb.append("<td>").append(escape(productName)).append("</td>");
            sb.append("</tr>\n");
        }

        sb.append("</table>\n</div>\n");
        return sb.toString();
    }

    //formats an ID as hex with the given number of digits, or "unknown" if it's -1
    private static String hex(int id, int digits) {
        return id < 0 ? "unknown" : String.format("0x%0" + digits + "x", id);
    }
    public static Path write(String html) {
        Path reportPath = Path.of("report.html").toAbsolutePath();
        try {
            Files.writeString(reportPath, html);
            System.out.println("Report written to report.html");
            return reportPath;
        } catch (IOException e) {
            System.err.println("Couldn't write report: " + e.getMessage());
            return null;
        }
    }
    public static void open(Path reportPath) {
        if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(reportPath.toUri());
            } catch (IOException e) {
                System.out.println("Couldn't open browser, open report.html in the project folder");
            }
        } else {
            System.out.println("Open report.html from the project folder");
        }
    }
}
