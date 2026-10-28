package systemfetch;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// a small web server so the report updates live in the browser
// run it from the project folder, then open http://localhost:8080
public class liveServer {

    public static void main(String[] args) throws IOException {
        idLookup lookup = new idLookup();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // "/" sends the same page as report.html
        server.createContext("/", exchange -> {
            String html = htmlReport.build(withoutPerCpuLoad(cpuText()), memInfo.report(),
                    usbInfo.readAll(), pciInfo.readAll(), lookup);
            send(exchange, "text/html", html);
        });

        // "/data" sends fresh CPU and memory text plus the CPU bar chart,
        // which the page asks for every second
        server.createContext("/data", exchange -> {
            cpuInfo.Load load = measureLoad();
            String json = "{\"cpu\": " + jsonString(withoutPerCpuLoad(cpuInfo.report(load)))
                    + ", \"memory\": " + jsonString(memInfo.report())
                    + ", \"cpuChart\": " + jsonString(cpuChart(load)) + "}";
            send(exchange, "application/json", json);
        });

        server.start();
        System.out.println("Live report at http://localhost:8080 (Ctrl+C to stop)");
    }

    // the CPU text from cpuInfo.report(), which measures the load over one second
    private static String cpuText() {
        try {
            return cpuInfo.report();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }

    // measures the CPU load over one second, or no load if interrupted
    private static cpuInfo.Load measureLoad() {
        try {
            return cpuInfo.findLoad(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new cpuInfo.Load(0, new double[0]);
        }
    }

    // SVG bar chart of each CPU's load as a percentage, made with barChart
    private static String cpuChart(cpuInfo.Load load) {
        double[] perCore = load.perCore();
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < perCore.length; i++) {
            labels.add("CPU " + i);
            values.add(perCore[i] * 100);
        }
        return barChart.createVertical(labels, values, 100.0);
    }

    // the bar chart shows each CPU's load, so the live page leaves those lines out of the text
    // (the terminal and report.html still show them)
    private static String withoutPerCpuLoad(String cpuText) {
        StringBuilder sb = new StringBuilder();
        for (String line : cpuText.split("\n")) {
            if (line.equals("Load per logical CPU:") || line.matches("\\s+CPU \\d+: .*")) {
                continue;
            }
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    // puts text in quotes for JSON, escaping backslashes, quotes and line breaks
    private static String jsonString(String text) {
        return "\"" + text.replace("\\", "\\\\")
                          .replace("\"", "\\\"")
                          .replace("\n", "\\n")
                          .replace("\r", "") + "\"";
    }

    // sends text back to the browser
    private static void send(HttpExchange exchange, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
