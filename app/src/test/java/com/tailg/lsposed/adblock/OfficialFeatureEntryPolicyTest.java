package com.tailg.lsposed.adblock;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OfficialFeatureEntryPolicyTest {
    @Test
    public void batteryEntry_onlyAddedForMainCarWhenOfficialEntryIsMissing() {
        assertTrue(OfficialFeatureEntryPolicy.shouldAddBatteryDynamics(
                true,
                false,
                false
        ));
        assertFalse(OfficialFeatureEntryPolicy.shouldAddBatteryDynamics(
                true,
                true,
                false
        ));
        assertFalse(OfficialFeatureEntryPolicy.shouldAddBatteryDynamics(
                true,
                false,
                true
        ));
        assertFalse(OfficialFeatureEntryPolicy.shouldAddBatteryDynamics(
                false,
                false,
                false
        ));
    }

    @Test
    public void customSoundEntry_requiresBindingAndReportedCapability() {
        assertTrue(OfficialFeatureEntryPolicy.shouldAddCustomSound(
                true,
                true,
                true,
                false
        ));
        assertFalse(OfficialFeatureEntryPolicy.shouldAddCustomSound(
                true,
                false,
                true,
                false
        ));
        assertFalse(OfficialFeatureEntryPolicy.shouldAddCustomSound(
                true,
                true,
                false,
                false
        ));
        assertFalse(OfficialFeatureEntryPolicy.shouldAddCustomSound(
                true,
                true,
                true,
                true
        ));
    }

    @Test
    public void batteryInformationShortcut_onlyAddedForMainCar() {
        assertTrue(OfficialFeatureEntryPolicy.shouldAddBatteryInfoShortcut(true, false));
        assertFalse(OfficialFeatureEntryPolicy.shouldAddBatteryInfoShortcut(true, true));
        assertFalse(OfficialFeatureEntryPolicy.shouldAddBatteryInfoShortcut(false, false));
    }
}
