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
                true,
                true,
                true
        );

        assertTrue(plan.hasSplashHooks());
        assertTrue(plan.hasConfigBeanHooks());
        assertTrue(plan.hasAppUpdateHooks());
        assertEquals(2, plan.splashRequestCount());
        // getIsShow + (getHomeResource + getFootResource) + getDurationTime + (getBanners + getBannerOssIds)
        assertEquals(6, plan.configBeanRequestCount());
        // getIsPop + getIsForce
        assertEquals(2, plan.appUpdateRequestCount());
        assertEquals(10, plan.totalRequestCount());
    }

    @Test
    public void configBeanDisabled_ignoresForceFlags() {
        HookRequestPlan plan = HookRequestPlan.fromConfig(
                false,
                true,
                false,
                true,
                true,
                true,
                false
        );

        assertTrue(plan.hasSplashHooks());
        assertFalse(plan.hasConfigBeanHooks());
        assertFalse(plan.hasAppUpdateHooks());
        assertEquals(1, plan.splashRequestCount());
        // forceEmptyBanner is ignored while the ConfigGetBean master flag is off.
        assertEquals(0, plan.configBeanRequestCount());
        assertEquals(1, plan.totalRequestCount());
    }

    @Test
    public void bannerCountedUnderConfigBean() {
        HookRequestPlan plan = HookRequestPlan.fromConfig(
                false,
                false,
                true,
                false,
                false,
                true,
                false
        );

        assertTrue(plan.hasConfigBeanHooks());
        assertFalse(plan.hasAppUpdateHooks());
        // getIsShow + (getBanners + getBannerOssIds)
        assertEquals(3, plan.configBeanRequestCount());
        assertEquals(3, plan.totalRequestCount());
    }

    @Test
    public void appUpdateHooks_countedIndependently() {
        HookRequestPlan plan = HookRequestPlan.fromConfig(
                false,
                false,
                false,
                false,
                false,
                false,
                true
        );

        assertFalse(plan.hasSplashHooks());
        assertFalse(plan.hasConfigBeanHooks());
        assertTrue(plan.hasAppUpdateHooks());
        assertEquals(2, plan.appUpdateRequestCount());
        assertEquals(2, plan.totalRequestCount());
    }

    @Test
    public void allDisabled_hasZeroRequests() {
        HookRequestPlan plan = HookRequestPlan.fromConfig(
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );

        assertFalse(plan.hasSplashHooks());
        assertFalse(plan.hasConfigBeanHooks());
        assertFalse(plan.hasAppUpdateHooks());
        assertEquals(0, plan.totalRequestCount());
    }
}
