package com.tailg.lsposed.adblock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BleReconnectPolicyTest {
    @Test
    public void interval_isClampedAndRoundedToFiveSeconds() {
        assertEquals(5, BleReconnectPolicy.normalizeIntervalSeconds(-1));
        assertEquals(5, BleReconnectPolicy.normalizeIntervalSeconds(7));
        assertEquals(10, BleReconnectPolicy.normalizeIntervalSeconds(8));
        assertEquals(15, BleReconnectPolicy.normalizeIntervalSeconds(15));
        assertEquals(60, BleReconnectPolicy.normalizeIntervalSeconds(100));
    }

    @Test
    public void attempts_areClampedToSupportedRange() {
        assertEquals(1, BleReconnectPolicy.normalizeMaxAttempts(-1));
        assertEquals(3, BleReconnectPolicy.normalizeMaxAttempts(3));
        assertEquals(5, BleReconnectPolicy.normalizeMaxAttempts(100));
    }
}
