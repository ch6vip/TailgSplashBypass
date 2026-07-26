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
}
