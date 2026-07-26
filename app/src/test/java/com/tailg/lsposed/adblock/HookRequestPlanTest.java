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
                true,
                true,
                true,
                true,
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
        assertEquals(5, plan.fastStartupRequestCount());
        assertEquals(1, plan.repositoryRequestCount());
        assertEquals(3, plan.homeActivityRequestCount());
        assertEquals(1, plan.trackExportRequestCount());
        assertEquals(2, plan.proximityRequestCount());
        assertEquals(2, plan.officialSettingsRequestCount());
        assertEquals(24, plan.totalRequestCount());
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
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
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
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
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
                true,
                false,
                false,
                false,
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
                false,
                false,
                false,
                false,
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

    @Test
    public void officialSettingsEntry_countsRevisionAndLegacyHooks() {
        HookRequestPlan plan = HookRequestPlan.fromConfig(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true
        );

        assertTrue(plan.hasOfficialSettingsHooks());
        assertEquals(2, plan.officialSettingsRequestCount());
        assertEquals(2, plan.totalRequestCount());
    }

    @Test
    public void swapControlAndService_requestsSharedHomeHook() {
        HookRequestPlan plan = HookRequestPlan.fromConfig(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false
        );

        assertTrue(plan.hasHomeActivityHooks());
        assertEquals(1, plan.homeActivityRequestCount());
        assertEquals(1, plan.totalRequestCount());
    }

    @Test
    public void fastStartup_countsDeferredSdkHooks() {
        HookRequestPlan plan = HookRequestPlan.fromConfig(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );

        assertTrue(plan.hasFastStartupHooks());
        assertEquals(5, plan.fastStartupRequestCount());
        assertEquals(5, plan.totalRequestCount());
    }

    @Test
    public void bleReconnect_requestsHomeResumeHook() {
        HookRequestPlan plan = HookRequestPlan.fromConfig(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false
        );

        assertTrue(plan.hasHomeActivityHooks());
        assertEquals(1, plan.homeActivityRequestCount());
        assertEquals(1, plan.totalRequestCount());
    }
}
