package systemfetch;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

// a small web server so the report updates live in the browser
// run it from the project folder, then open http://localhost:8080
public class liveServer {

    public static void main(String[] args) throws IOException {
        idLookup lookup = new idLookup();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // "/" sends the same page as report.html
        server.createContext("/", exchange -> {
            String html = htmlReport.build(cpuText(), memInfo.report(),
                    usbInfo.readAll(), pciInfo.readAll(), lookup);
            send(exchange, "text/html", html);
        });

        // "/data" sends fresh CPU and memory text, which the page asks for every second
        server.createContext("/data", exchange -> {
            String json = "{\"cpu\": " + jsonString(cpuText())
                    + ", \"memory\": " + jsonString(memInfo.report()) + "}";
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
