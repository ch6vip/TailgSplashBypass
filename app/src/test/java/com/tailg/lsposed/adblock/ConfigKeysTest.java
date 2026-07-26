package com.tailg.lsposed.adblock;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConfigKeysTest {
    @Test
    public void booleanKeyWhitelist_hasUniqueDefaultsForEveryEntry() {
        HashSet<String> uniqueKeys = new HashSet<>(Arrays.asList(ConfigKeys.BOOLEAN_KEYS));

        assertEquals(ConfigKeys.BOOLEAN_KEYS.length, uniqueKeys.size());
        for (String key : ConfigKeys.BOOLEAN_KEYS) {
            assertTrue(ConfigKeys.isBooleanKey(key));
            ConfigKeys.defaultBooleanFor(key);
        }
    }

    @Test
    public void unknownBooleanKey_isRejected() {
        assertFalse(ConfigKeys.isBooleanKey("unknown"));
        try {
            ConfigKeys.defaultBooleanFor("unknown");
            fail("Expected an IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("unknown"));
        }
    }

    @Test
    public void optionalFeatures_defaultToOff() {
        assertFalse(ConfigKeys.defaultBooleanFor(ConfigKeys.KEY_FAST_STARTUP));
        assertFalse(ConfigKeys.defaultBooleanFor(ConfigKeys.KEY_BLE_RECONNECT));
        assertFalse(ConfigKeys.defaultBooleanFor(ConfigKeys.KEY_ENABLE_MONTHLY_RIDE_DATA));
        assertFalse(ConfigKeys.defaultBooleanFor(ConfigKeys.KEY_SHOW_BRAKE_FORCE_DATA));
        assertFalse(ConfigKeys.defaultBooleanFor(ConfigKeys.KEY_SHOW_BATTERY_DYNAMICS_ENTRY));
        assertFalse(ConfigKeys.defaultBooleanFor(ConfigKeys.KEY_SHOW_CUSTOM_VEHICLE_SOUND));
        assertEquals(15, ConfigKeys.DEFAULT_BLE_RECONNECT_INTERVAL_SECONDS);
        assertEquals(3, ConfigKeys.DEFAULT_BLE_RECONNECT_MAX_ATTEMPTS);
    }
}
