
package systemfetch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class barChartDemo {

    public static void main(String[] args) throws Exception {
        StringBuilder html = new StringBuilder();

        html.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>Bar Chart Test</title>
                    <style>
                        body {
                            font-family: sans-serif;
                            max-width: 800px;
                            margin: 30px auto;
                        }
                        section {
                            margin-bottom: 35px;
                        }
                    </style>
                </head>
                <body>
                    <h1>Bar Chart Generator Test</h1>
                    <section>
                        <h2>1. Normal data (fixed maximum 100)</h2>
                """);

        html.append(barChart.create(
                List.of("CPU 0", "CPU 1", "CPU 2"),
                List.of(25.0, 50.0, 100.0),
                100.0
        ));

        html.append("""
                    </section>
                    <section>
                        <h2>2. Empty data</h2>
                """);

        html.append(barChart.create(
                List.of(),
                List.of()
        ));

        html.append("""
                    </section>
                    <section>
                        <h2>3. All values zero</h2>
                """);

        html.append(barChart.create(
                List.of("CPU 0", "CPU 1", "CPU 2"),
                List.of(0.0, 0.0, 0.0)
        ));

        html.append("""
                    </section>
                </body>
                </html>
                """);

        Files.writeString(Path.of("barChart-test.html"), html.toString());

        System.out.println("Created barChart-test.html in the project folder.");
    }
}
