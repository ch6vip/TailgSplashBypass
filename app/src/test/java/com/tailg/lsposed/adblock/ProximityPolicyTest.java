package com.tailg.lsposed.adblock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProximityPolicyTest {
    @Test
    public void validDistances_arePreserved() {
        ProximityPolicy.Distances distances = ProximityPolicy.normalize(2.0f, 3.5f);

        assertEquals(2.0f, distances.unlockMeters, 0.0f);
        assertEquals(3.5f, distances.lockMeters, 0.0f);
    }

    @Test
    public void lockDistance_isKeptBeyondUnlockDistance() {
        ProximityPolicy.Distances distances = ProximityPolicy.normalize(4.0f, 3.0f);

        assertEquals(4.0f, distances.unlockMeters, 0.0f);
        assertEquals(4.5f, distances.lockMeters, 0.0f);
    }

    @Test
    public void invalidValues_fallBackToDefaults() {
        ProximityPolicy.Distances distances = ProximityPolicy.normalize(Float.NaN, 50.0f);

        assertEquals(ConfigKeys.DEFAULT_PROXIMITY_UNLOCK_METERS, distances.unlockMeters, 0.0f);
        assertEquals(ConfigKeys.DEFAULT_PROXIMITY_LOCK_METERS, distances.lockMeters, 0.0f);
    }
}
