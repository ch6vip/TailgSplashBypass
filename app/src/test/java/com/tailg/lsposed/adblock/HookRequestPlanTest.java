package com.tailg.lsposed.adblock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HookRequestPlanTest {
    @Test
    public void allEnabled_countsAllRequests() {
        HookRequestPlan plan = HookRequestPlan.fromConfig(
                true,
                true,
                true,
                true,
                true
        );

        assertTrue(plan.hasSplashHooks());
        assertTrue(plan.hasConfigBeanHooks());
        assertEquals(2, plan.splashRequestCount());
        assertEquals(4, plan.configBeanRequestCount());
        assertEquals(6, plan.totalRequestCount());
    }

    @Test
    public void configBeanDisabled_ignoresForceFlags() {
        HookRequestPlan plan = HookRequestPlan.fromConfig(
                false,
                true,
                false,
                true,
                true
        );

        assertTrue(plan.hasSplashHooks());
        assertFalse(plan.hasConfigBeanHooks());
        assertEquals(1, plan.splashRequestCount());
        assertEquals(0, plan.configBeanRequestCount());
        assertEquals(1, plan.totalRequestCount());
    }

    @Test
    public void allDisabled_hasZeroRequests() {
        HookRequestPlan plan = HookRequestPlan.fromConfig(
                false,
                false,
                false,
                false,
                false
        );

        assertFalse(plan.hasSplashHooks());
        assertFalse(plan.hasConfigBeanHooks());
        assertEquals(0, plan.totalRequestCount());
    }
}
