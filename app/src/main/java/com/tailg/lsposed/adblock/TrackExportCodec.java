package com.tailg.lsposed.adblock;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

final class TrackExportCodec {
    private static final DateTimeFormatter TAILG_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);

    private TrackExportCodec() {
    }

    static String toGpx(List<TrackPoint> points, String trackName) {
        StringBuilder out = new StringBuilder(Math.max(1024, points.size() * 220));
        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<gpx version=\"1.1\" creator=\"Tailg LSPosed\" ")
                .append("xmlns=\"http://www.topografix.com/GPX/1/1\" ")
                .append("xmlns:tailg=\"https://github.com/tailg-lsposed/export/1\" ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 ")
                .append("http://www.topografix.com/GPX/1/1/gpx.xsd\">\n")
                .append("  <trk>\n")
                .append("    <name>").append(xmlEscape(trackName)).append("</name>\n")
                .append("    <trkseg>\n");

        for (TrackPoint point : points) {
            out.append(String.format(
                    Locale.US,
                    "      <trkpt lat=\"%.8f\" lon=\"%.8f\">\n",
                    point.latitude,
                    point.longitude
            ));
            String time = normalizeTime(point.reportTime);
            if (time != null) {
                out.append("        <time>").append(time).append("</time>\n");
            }
            Integer satellites = parseInteger(point.satellites);
            if (satellites != null && satellites >= 0) {
                out.append("        <sat>").append(satellites).append("</sat>\n");
            }
            if (!point.speed.isEmpty() || !point.heading.isEmpty()) {
                out.append("        <extensions>\n");
                appendExtension(out, "speedRaw", point.speed);
                appendExtension(out, "headingRaw", point.heading);
                out.append("        </extensions>\n");
            }
            out.append("      </trkpt>\n");
        }
        return out.append("    </trkseg>\n  </trk>\n</gpx>\n").toString();
    }

    static String toCsv(List<TrackPoint> points) {
        StringBuilder out = new StringBuilder(Math.max(512, points.size() * 100));
        out.append("latitude,longitude,report_time,speed_raw,heading_raw,satellites\n");
        for (TrackPoint point : points) {
            out.append(String.format(Locale.US, "%.8f,%.8f,", point.latitude, point.longitude))
                    .append(csvEscape(point.reportTime)).append(',')
                    .append(csvEscape(point.speed)).append(',')
                    .append(csvEscape(point.heading)).append(',')
                    .append(csvEscape(point.satellites)).append('\n');
        }
        return out.toString();
    }

    private static void appendExtension(StringBuilder out, String name, String value) {
        if (!value.isEmpty()) {
            out.append("          <tailg:").append(name).append('>')
                    .append(xmlEscape(value))
                    .append("</tailg:").append(name).append(">\n");
        }
    }

    private static String normalizeTime(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(value).toString();
        } catch (DateTimeParseException ignored) {
            // Tailg 3.5.9 commonly uses offsets such as +0000.
        }
        try {
            return OffsetDateTime.parse(value, TAILG_TIME_FORMAT).toInstant().toString();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String csvEscape(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0
                && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static String xmlEscape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
