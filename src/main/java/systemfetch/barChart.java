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

    // vertical version of create(): one column per value, labels underneath
    // used for the live CPU usage chart, with fixedMax 100 for percentages
    public static String createVertical(List<String> labels, List<Double> values, Double fixedMax) {
        validateInputs(labels, values, fixedMax);

        int side = 20;
        // each value gets an equal slot across the width, with the bar in the middle of its slot
        double slotWidth = (double) (WIDTH - 2 * side) / Math.max(labels.size(), 1);

        // when there are too many bars for the labels to fit side by side, turn them sideways
        int longestLabel = 0;
        for (String label : labels) {
            longestLabel = Math.max(longestLabel, label.length());
        }
        boolean sidewaysLabels = longestLabel * 8 > slotWidth * 0.9;
        double sidewaysFont = Math.min(12, slotWidth * 0.75);

        // values above the bars only when there's room for them, otherwise hover a bar to see it
        boolean showValues = slotWidth >= 28;

        int top = 24;                                                      // room for the value above a full bar
        int bottom = sidewaysLabels ? (int) (longestLabel * sidewaysFont * 0.6) + 16 : 36;   // room for the labels
        int chartHeight = 200;
        int height = top + chartHeight + bottom;

        double largestValue = 0.0;
        for (double value : values) {
            largestValue = Math.max(largestValue, value);
        }

        // all-zero data to use a scale of 1 to avoid zero division
        double scaleMax = fixedMax != null ? fixedMax : Math.max(largestValue, 1.0);

        StringBuilder sb = new StringBuilder();

        sb.append(String.format(
                Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" "
                        + "viewBox=\"0 0 %d %d\" width=\"100%%\" "
                        + "role=\"img\" aria-label=\"Vertical bar chart\">\n",
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
                    "<text class=\"chart-empty\" x=\"%d\" y=\"%d\" text-anchor=\"middle\">"
                            + "No data available</text>\n",
                    WIDTH / 2, height / 2));
        } else {
            double barWidth = Math.min(slotWidth * 0.6, 80);

            for (int i = 0; i < labels.size(); i++) {
                double value = values.get(i);
                double barHeight = chartHeight * Math.min(value / scaleMax, 1.0);
                double centre = side + slotWidth * i + slotWidth / 2;
                double x = centre - barWidth / 2;
                double y = top + chartHeight - barHeight;

                sb.append(String.format(
                        Locale.ROOT,
                        "<rect class=\"chart-track\" x=\"%.2f\" y=\"%d\" "
                                + "width=\"%.2f\" height=\"%d\" rx=\"3\"/>\n",
                        x, top, barWidth, chartHeight));

                // the <title> shows the label and value when the mouse is over the bar
                sb.append(String.format(
                        Locale.ROOT,
                        "<rect class=\"chart-bar\" x=\"%.2f\" y=\"%.2f\" "
                                + "width=\"%.2f\" height=\"%.2f\" rx=\"3\"><title>%s: %s</title></rect>\n",
                        x, y, barWidth, barHeight, escapeXml(labels.get(i)), formatValue(value)));

                if (showValues) {
                    sb.append(String.format(
                            Locale.ROOT,
                            "<text class=\"chart-value\" x=\"%.2f\" y=\"%.2f\" "
                                    + "text-anchor=\"middle\">%s</text>\n",
                            centre, y - 6, formatValue(value)));
                }

                if (sidewaysLabels) {
                    // turned to read upwards, ending just under the middle of the bar
                    int labelY = top + chartHeight + 8;
                    sb.append(String.format(
                            Locale.ROOT,
                            "<text class=\"chart-label\" x=\"%.2f\" y=\"%d\" text-anchor=\"end\" "
                                    + "dominant-baseline=\"middle\" style=\"font-size: %.1fpx\" "
                                    + "transform=\"rotate(-90 %.2f %d)\">%s</text>%n",
                            centre, labelY, sidewaysFont, centre, labelY, escapeXml(labels.get(i))));
                } else {
                    sb.append(String.format(
                            Locale.ROOT,
                            "<text class=\"chart-label\" x=\"%.2f\" y=\"%d\" "
                                    + "text-anchor=\"middle\">%s</text>%n",
                            centre, top + chartHeight + 22, escapeXml(labels.get(i))));
                }
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
