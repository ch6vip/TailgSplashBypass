package com.tailg.lsposed.adblock;

final class TrackPoint {
    final double latitude;
    final double longitude;
    final String reportTime;
    final String speed;
    final String heading;
    final String satellites;

    TrackPoint(
            double latitude,
            double longitude,
            String reportTime,
            String speed,
            String heading,
            String satellites
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.reportTime = valueOrEmpty(reportTime);
        this.speed = valueOrEmpty(speed);
        this.heading = valueOrEmpty(heading);
        this.satellites = valueOrEmpty(satellites);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
