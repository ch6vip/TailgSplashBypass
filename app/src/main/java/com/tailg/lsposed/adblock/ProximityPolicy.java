package com.tailg.lsposed.adblock;

final class ProximityPolicy {
    static final float MIN_METERS = 0.5f;
    static final float MAX_METERS = 10.0f;
    static final float STEP_METERS = 0.5f;

    private ProximityPolicy() {
    }

    static Distances normalize(float unlockMeters, float lockMeters) {
        float unlock = valid(unlockMeters)
                ? snap(unlockMeters)
                : ConfigKeys.DEFAULT_PROXIMITY_UNLOCK_METERS;
        float lock = valid(lockMeters)
                ? snap(lockMeters)
                : ConfigKeys.DEFAULT_PROXIMITY_LOCK_METERS;

        unlock = clamp(unlock, MIN_METERS, MAX_METERS - STEP_METERS);
        lock = clamp(lock, MIN_METERS + STEP_METERS, MAX_METERS);
        if (lock < unlock + STEP_METERS) {
            lock = Math.min(MAX_METERS, unlock + STEP_METERS);
            unlock = Math.min(unlock, lock - STEP_METERS);
        }
        return new Distances(unlock, lock);
    }

    private static boolean valid(float value) {
        return Float.isFinite(value) && value >= MIN_METERS && value <= MAX_METERS;
    }

    private static float snap(float value) {
        return Math.round(value / STEP_METERS) * STEP_METERS;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static final class Distances {
        final float unlockMeters;
        final float lockMeters;

        Distances(float unlockMeters, float lockMeters) {
            this.unlockMeters = unlockMeters;
            this.lockMeters = lockMeters;
        }
    }
}
