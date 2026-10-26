package systemfetch;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

 /* USAGE:
Call create() with labels and real values collected by the existing
system information classes. The returned SVG string can be appended
directly to the HTML StringBuilder in htmlReport.java.

Example: CPU load per logical CPU

    cpuInfo.Load load = cpuInfo.findLoad(1000);
    double[] perCore = load.perCore();
    List<String> labels = new ArrayList<>();
    List<Double> values = new ArrayList<>();
    for (int i = 0; i < perCore.length; i++) {
        labels.add("CPU " + i);
        values.add(perCore[i] * 100);
    }
    b.append(barChart.create(labels, values, 100.0));

 */

public final class barChart {
    private static final int WIDTH = 800;
    private static final int LEFT = 240;
    private static final int RIGHT = 100;
    private static final int TOP = 24;
    private static final int ROW_HEIGHT = 34;
    private static final int BAR_HEIGHT = 18;
    private static final int BOTTOM = 18;

    private barChart() {
        // utility class; do not instantiate
    }

    // creates a chart scaled to the largest value in the data
    public static String create(List<String> labels, List<Double> values) {
        return create(labels, values, null);
    }

    public static String create(List<String> labels, List<Double> values, Double fixedMax){
        validateInputs(labels, values, fixedMax);

        int height = TOP + Math.max(labels.size(), 1) * ROW_HEIGHT + BOTTOM;
        double largestValue = 0.0;

        for (double value : values) {
            largestValue = Math.max(largestValue, value);
        }

        // all-zero data to use a scale of 1 to avoid zero division
        double scaleMax = fixedMax != null ? fixedMax : Math.max(largestValue, 1.0);

        int chartWidth = WIDTH - LEFT - RIGHT;

        StringBuilder sb = new StringBuilder();

        sb.append(String.format(
                Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" "
                        + "viewBox=\"0 0 %d %d\" width=\"100%%\" "
                        + "role=\"img\" aria-label=\"Horizontal bar chart\">\n",
                WIDTH, height));

        sb.append("""
                <style>
                  .chart-label { font: 14px system-ui, sans-serif; fill: #222; }
                  .chart-value { font: 13px system-ui, sans-serif; fill: #333; }
                  .chart-track { fill: #eeeeee; }
                  .chart-bar { fill: #3978c5; }
                  .chart-empty { font: 15px system-ui, sans-serif; fill: #555; }
                </style>
                """);

        if (labels.isEmpty()) {
            sb.append(String.format(
                    Locale.ROOT,
                    "<text class=\"chart-empty\" x=\"%d\" y=\"%d\">"
                            + "No data available</text>\n",
                    LEFT, TOP + ROW_HEIGHT / 2));
        } else {
            for (int i = 0; i < labels.size(); i++) {
                int y = TOP + i * ROW_HEIGHT;
                double value = values.get(i);
                double barWidth = chartWidth * Math.min(value / scaleMax, 1.0);

                sb.append(String.format(
                        Locale.ROOT,
                        "<text class=\"chart-label\" x=\"%d\" y=\"%d\" "
                                + "text-anchor=\"end\" dominant-baseline=\"middle\">%s</text>%n",
                        LEFT - 12, y + BAR_HEIGHT / 2, escapeXml(labels.get(i))));

                sb.append(String.format(
                        Locale.ROOT,
                        "<rect class=\"chart-track\" x=\"%d\" y=\"%d\" "
                                + "width=\"%d\" height=\"%d\" rx=\"3\"/>\n",
                        LEFT, y, chartWidth, BAR_HEIGHT));

                sb.append(String.format(
                        Locale.ROOT,
                        "<rect class=\"chart-bar\" x=\"%d\" y=\"%d\" "
                                + "width=\"%.2f\" height=\"%d\" rx=\"3\"/>\n",
                        LEFT, y, barWidth, BAR_HEIGHT));

                sb.append(String.format(
                        Locale.ROOT,
                        "<text class=\"chart-value\" x=\"%d\" y=\"%d\" "
                                + "dominant-baseline=\"middle\">%s</text>\n",
                        LEFT + chartWidth + 10, y + BAR_HEIGHT / 2,
                        formatValue(value)));
            }
        }

        sb.append("</svg>");
        return sb.toString();
    }

    private static void validateInputs(List<String> labels, List<Double> values, Double fixedMax) {
        Objects.requireNonNull(labels, "labels must not be null");
        Objects.requireNonNull(values, "values must not be null");

        if (labels.size() != values.size()) {
            throw new IllegalArgumentException("labels and values must have the same size");
        }

        if (fixedMax != null && (!Double.isFinite(fixedMax) || fixedMax <= 0)) {
            throw new IllegalArgumentException("fixedMax must be a positive finite number");
        }

        for (int i = 0; i < labels.size(); i++) {
            if (labels.get(i) == null) {
                throw new IllegalArgumentException("labels must not contain null values");
            }

            Double value = values.get(i);
            if (value == null || !Double.isFinite(value) || value < 0.0) {
                throw new IllegalArgumentException("values must not contain null or negative values");
            }
        }
    }

    private static String formatValue(double value) {
        return String.format(Locale.ROOT, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    // ensure labels cannot break svg/xml markup
    private static String escapeXml(String text){
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
