package com.tailg.lsposed.adblock;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VersionGuardPolicyTest {
    @Test
    public void strictGuard_blocksUnsupportedVersion() {
        boolean allowed = VersionGuardPolicy.shouldInstallHooks("3.6.0", true);
        assertFalse(allowed);
    }

    @Test
    public void nonStrictGuard_allowsUnsupportedVersion() {
        boolean allowed = VersionGuardPolicy.shouldInstallHooks("3.6.0", false);
        assertTrue(allowed);
    }

    @Test
    public void supportedVersion_isAlwaysAllowed() {
        assertTrue(VersionGuardPolicy.shouldInstallHooks("3.5.9", true));
        assertTrue(VersionGuardPolicy.shouldInstallHooks("3.5.9", false));
    }

    @Test
    public void nullVersion_treatedAsUnsupported() {
        assertFalse(VersionGuardPolicy.shouldInstallHooks(null, true));
    }
}
