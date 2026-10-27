package systemfetch;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

// a small web server so the report updates live in the browser
// run it from the project folder, then open http://localhost:8080
public class liveServer {

    private static final double GIB = 1024.0 * 1024.0 * 1024.0;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);

        // "/" sends the page
        server.createContext("/", exchange -> {
            String page = Files.readString(Path.of("web/live.html"));
            send(exchange, "text/html", page);
        });

        // "/data" sends the latest numbers, which the page asks for every second
        server.createContext("/data", exchange -> send(exchange, "application/json", dataJson()));

        server.start();
        System.out.println("Live report at http://localhost:8080 (Ctrl+C to stop)");
    }

    // builds the numbers as JSON, e.g.
    // {"overall": 12.5, "cores": [10.0, 15.0], "memUsed": 3.21, "memTotal": 7.84, "memPercent": 41.0}
    private static String dataJson() {
        cpuInfo.Load load;
        try {
            // measures the CPU load over half a second
            load = cpuInfo.findLoad(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            load = new cpuInfo.Load(0, new double[0]);
        }

        StringBuilder cores = new StringBuilder();
        double[] perCore = load.perCore();
        for (int i = 0; i < perCore.length; i++) {
            if (i > 0) {
                cores.append(", ");
            }
            cores.append(String.format(Locale.ROOT, "%.1f", perCore[i] * 100));
        }

        return String.format(Locale.ROOT,
                "{\"overall\": %.1f, \"cores\": [%s], \"memUsed\": %.2f, \"memTotal\": %.2f, \"memPercent\": %.1f}",
                load.all() * 100,
                cores,
                memInfo.getUsed() / GIB,
                memInfo.getTotal() / GIB,
                memInfo.getPercentUsed());
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
