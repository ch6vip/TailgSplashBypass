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
        assertFalse(report.shouldResetInstalledFlag());
        assertEquals("Hook summary requested=6 installed=6 skipped=0 failed=0", report.summaryMessage());
    }

    @Test
    public void allRequestedHooksFailed_warnsAndResets() {
        HookInstallReport report = new HookInstallReport();
        report.markRequested(4);
        report.markFailed();
        report.markSkipped();

        assertTrue(report.shouldWarnSummary());
        assertTrue(report.shouldResetInstalledFlag());
        assertEquals("Hook summary requested=4 installed=0 skipped=1 failed=1", report.summaryMessage());
    }

    @Test
    public void noRequestedHooks_keepsInstalledFlag() {
        HookInstallReport report = new HookInstallReport();

        assertFalse(report.shouldWarnSummary());
        assertFalse(report.shouldResetInstalledFlag());
        assertEquals("Hook summary requested=0 installed=0 skipped=0 failed=0", report.summaryMessage());
    }
}
