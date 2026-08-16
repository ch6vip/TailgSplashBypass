package com.tailg.lsposed.adblock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HookRequestPlanTest {
    @Test
    public void allEnabled_countsAllRequests() {
        HookRequestPlan plan = Flags.allEnabled().build();

        assertTrue(plan.hasSplashHooks());
        assertTrue(plan.hasConfigBeanHooks());
        assertTrue(plan.hasAppUpdateHooks());
        assertEquals(2, plan.splashRequestCount());
        // getIsShow + resources + duration + banners.
        assertEquals(6, plan.configBeanRequestCount());
        assertEquals(2, plan.appUpdateRequestCount());
        assertEquals(5, plan.fastStartupRequestCount());
        assertEquals(1, plan.repositoryRequestCount());
        assertEquals(1, plan.homeActivityRequestCount());
        assertEquals(17, plan.totalRequestCount());
    }

    @Test
    public void configBeanDisabled_ignoresForceFlags() {
        Flags flags = new Flags();
        flags.hookCountDown = true;
        flags.forceEmptyRes = true;
        flags.forceDurationZero = true;
        flags.forceEmptyBanner = true;
        HookRequestPlan plan = flags.build();

        assertTrue(plan.hasSplashHooks());
        assertFalse(plan.hasConfigBeanHooks());
        assertFalse(plan.hasAppUpdateHooks());
        assertEquals(1, plan.splashRequestCount());
        assertEquals(0, plan.configBeanRequestCount());
        assertEquals(1, plan.totalRequestCount());
    }

    @Test
    public void bannerCountedUnderConfigBean() {
        Flags flags = new Flags();
        flags.hookConfigBean = true;
        flags.forceEmptyBanner = true;
        HookRequestPlan plan = flags.build();

        assertTrue(plan.hasConfigBeanHooks());
        assertFalse(plan.hasAppUpdateHooks());
        assertEquals(3, plan.configBeanRequestCount());
        assertEquals(3, plan.totalRequestCount());
    }

    @Test
    public void appUpdateHooks_countedIndependently() {
        Flags flags = new Flags();
        flags.hookAppUpdate = true;
        HookRequestPlan plan = flags.build();

        assertFalse(plan.hasSplashHooks());
        assertFalse(plan.hasConfigBeanHooks());
        assertTrue(plan.hasAppUpdateHooks());
        assertEquals(2, plan.appUpdateRequestCount());
        assertEquals(2, plan.totalRequestCount());
    }

    @Test
    public void allDisabled_hasZeroRequests() {
        HookRequestPlan plan = new Flags().build();

        assertFalse(plan.hasSplashHooks());
        assertFalse(plan.hasConfigBeanHooks());
        assertFalse(plan.hasAppUpdateHooks());
        assertEquals(0, plan.totalRequestCount());
    }

    @Test
    public void fastStartup_countsDeferredSdkHooks() {
        Flags flags = new Flags();
        flags.fastStartup = true;
        HookRequestPlan plan = flags.build();

        assertTrue(plan.hasFastStartupHooks());
        assertEquals(5, plan.fastStartupRequestCount());
        assertEquals(5, plan.totalRequestCount());
    }

    @Test
    public void buglyBlock_requestsHomeHook() {
        Flags flags = new Flags();
        flags.blockBugly = true;
        HookRequestPlan plan = flags.build();

        assertTrue(plan.hasHomeActivityHooks());
        assertEquals(1, plan.homeActivityRequestCount());
        assertEquals(1, plan.totalRequestCount());
    }

    @Test
    public void usageReportBlock_requestsRepositoryHook() {
        Flags flags = new Flags();
        flags.blockUsageReport = true;
        HookRequestPlan plan = flags.build();

        assertTrue(plan.hasRepositoryHooks());
        assertEquals(1, plan.repositoryRequestCount());
        assertEquals(1, plan.totalRequestCount());
    }

    private static final class Flags {
        boolean hookSetupView;
        boolean hookCountDown;
        boolean hookConfigBean;
        boolean forceEmptyRes;
        boolean forceDurationZero;
        boolean forceEmptyBanner;
        boolean hookAppUpdate;
        boolean fastStartup;
        boolean blockUsageReport;
        boolean blockBugly;

        static Flags allEnabled() {
            Flags flags = new Flags();
            flags.hookSetupView = true;
            flags.hookCountDown = true;
            flags.hookConfigBean = true;
            flags.forceEmptyRes = true;
            flags.forceDurationZero = true;
            flags.forceEmptyBanner = true;
            flags.hookAppUpdate = true;
            flags.fastStartup = true;
            flags.blockUsageReport = true;
            flags.blockBugly = true;
            return flags;
        }

        HookRequestPlan build() {
            return HookRequestPlan.fromConfig(
                    hookSetupView,
                    hookCountDown,
                    hookConfigBean,
                    forceEmptyRes,
                    forceDurationZero,
                    forceEmptyBanner,
                    hookAppUpdate,
                    fastStartup,
                    blockUsageReport,
                    blockBugly
            );
        }
    }
}
