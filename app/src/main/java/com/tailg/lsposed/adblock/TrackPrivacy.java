package com.tailg.lsposed.adblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TrackPrivacy {
    static final double DEFAULT_ENDPOINT_RADIUS_METERS = 200.0d;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0d;

    private TrackPrivacy() {
    }

    static List<TrackPoint> trimEndpoints(List<TrackPoint> points, double radiusMeters) {
        if (points == null || points.size() < 2 || radiusMeters <= 0.0d) {
            return points == null ? Collections.emptyList() : new ArrayList<>(points);
        }

        TrackPoint first = points.get(0);
        TrackPoint last = points.get(points.size() - 1);
        int start = 0;
        while (start < points.size()
                && distanceMeters(first, points.get(start)) < radiusMeters) {
            start++;
        }

        int end = points.size() - 1;
        while (end >= start
                && distanceMeters(last, points.get(end)) < radiusMeters) {
            end--;
        }

        if (end - start + 1 < 2) {
            return Collections.emptyList();
        }
        return new ArrayList<>(points.subList(start, end + 1));
    }

    static double distanceMeters(TrackPoint a, TrackPoint b) {
        double lat1 = Math.toRadians(a.latitude);
        double lat2 = Math.toRadians(b.latitude);
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(b.longitude - a.longitude);
        double sinLat = Math.sin(deltaLat / 2.0d);
        double sinLon = Math.sin(deltaLon / 2.0d);
        double h = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
        return 2.0d * EARTH_RADIUS_METERS * Math.asin(Math.min(1.0d, Math.sqrt(h)));
    }
}
