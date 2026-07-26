package com.tailg.lsposed.adblock;

final class BleReconnectPolicy {
    static final int MIN_INTERVAL_SECONDS = 5;
    static final int MAX_INTERVAL_SECONDS = 60;
    static final int INTERVAL_STEP_SECONDS = 5;
    static final int MIN_ATTEMPTS = 1;
    static final int MAX_ATTEMPTS = 5;

    private BleReconnectPolicy() {
    }

    static int normalizeIntervalSeconds(int value) {
        int clamped = Math.max(MIN_INTERVAL_SECONDS, Math.min(MAX_INTERVAL_SECONDS, value));
        int steps = Math.round(
                (clamped - MIN_INTERVAL_SECONDS) / (float) INTERVAL_STEP_SECONDS
        );
        return MIN_INTERVAL_SECONDS + steps * INTERVAL_STEP_SECONDS;
    }

    static int normalizeMaxAttempts(int value) {
        return Math.max(MIN_ATTEMPTS, Math.min(MAX_ATTEMPTS, value));
    }
}
