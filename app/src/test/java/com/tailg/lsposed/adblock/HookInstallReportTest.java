package com.tailg.lsposed.adblock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HookInstallReportTest {
    @Test
    public void allHooksInstalled_isHealthy() {
        HookInstallReport report = new HookInstallReport();
        report.markRequested(6);
        for (int i = 0; i < 6; i++) {
            report.markInstalled();
        }

        assertFalse(report.shouldWarnSummary());
        assertEquals(6, report.accounted());
        assertEquals("Hook summary requested=6 installed=6 skipped=0 failed=0", report.summaryMessage());
    }

    @Test
    public void allRequestedHooksFailed_warnsAndResets() {
        HookInstallReport report = new HookInstallReport();
        report.markRequested(4);
        report.markFailed(3);
        report.markSkipped();

        assertTrue(report.shouldWarnSummary());
        assertEquals(4, report.accounted());
        assertEquals("Hook summary requested=4 installed=0 skipped=1 failed=3", report.summaryMessage());
    }

    @Test
    public void noRequestedHooks_keepsInstalledFlag() {
        HookInstallReport report = new HookInstallReport();

        assertFalse(report.shouldWarnSummary());
        assertEquals(0, report.accounted());
        assertEquals("Hook summary requested=0 installed=0 skipped=0 failed=0", report.summaryMessage());
    }

    @Test
    public void partialInstallation_warns() {
        HookInstallReport report = new HookInstallReport();
        report.markRequested(3);
        report.markInstalled();
        report.markSkipped();
        report.markFailed();

        assertTrue(report.shouldWarnSummary());
        assertEquals(3, report.accounted());
    }

    @Test
    public void remainingRequests_canBeMarkedFailed() {
        HookInstallReport report = new HookInstallReport();
        report.markRequested(5);
        report.markInstalled();
        report.markRemainingFailed();

        assertTrue(report.shouldWarnSummary());
        assertEquals(5, report.accounted());
        assertEquals("Hook summary requested=5 installed=1 skipped=0 failed=4", report.summaryMessage());
    }
}
