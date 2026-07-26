package com.tailg.lsposed.adblock;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertTrue;

public class TrackExportCodecTest {
    @Test
    public void gpx_normalizesTailgTimeAndEscapesMetadata() {
        TrackPoint point = new TrackPoint(
                31.12345678d,
                120.87654321d,
                "2023-10-25T03:07:22.000+0000",
                "12&3",
                "45<6",
                "28"
        );

        String gpx = TrackExportCodec.toGpx(Collections.singletonList(point), "A&B");

        assertTrue(gpx.contains("<name>A&amp;B</name>"));
        assertTrue(gpx.contains("<time>2023-10-25T03:07:22Z</time>"));
        assertTrue(gpx.contains("<sat>28</sat>"));
        assertTrue(gpx.contains("12&amp;3"));
        assertTrue(gpx.contains("45&lt;6"));
    }

    @Test
    public void csv_quotesCommaAndDoubleQuoteValues() {
        TrackPoint point = new TrackPoint(
                31.0d,
                120.0d,
                "time,value",
                "12\"3",
                "",
                "20"
        );

        String csv = TrackExportCodec.toCsv(Collections.singletonList(point));

        assertTrue(csv.contains("\"time,value\""));
        assertTrue(csv.contains("\"12\"\"3\""));
    }
}
