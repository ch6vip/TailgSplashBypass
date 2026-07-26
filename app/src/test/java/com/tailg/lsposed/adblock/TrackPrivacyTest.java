package com.tailg.lsposed.adblock;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TrackPrivacyTest {
    @Test
    public void trimEndpoints_removesPointsWithinRadius() {
        List<TrackPoint> points = Arrays.asList(
                point(0.000d),
                point(0.001d),
                point(0.002d),
                point(0.008d),
                point(0.009d),
                point(0.010d)
        );

        List<TrackPoint> trimmed = TrackPrivacy.trimEndpoints(points, 200.0d);

        assertEquals(2, trimmed.size());
        assertEquals(0.002d, trimmed.get(0).latitude, 0.000001d);
        assertEquals(0.008d, trimmed.get(1).latitude, 0.000001d);
    }

    @Test
    public void trimEndpoints_returnsEmptyForShortPrivateRoute() {
        List<TrackPoint> points = Arrays.asList(point(0.000d), point(0.001d));

        assertTrue(TrackPrivacy.trimEndpoints(points, 200.0d).isEmpty());
    }

    private static TrackPoint point(double latitude) {
        return new TrackPoint(latitude, 120.0d, "", "", "", "");
    }
}
