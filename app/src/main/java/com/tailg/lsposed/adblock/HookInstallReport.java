package com.tailg.lsposed.adblock;

final class HookInstallReport {
    private int requested;
    private int installed;
    private int skipped;
    private int failed;

    void markRequested(int count) {
        requested += count;
    }

    void markInstalled() {
        installed++;
    }

    void markSkipped() {
        skipped++;
    }

    void markFailed() {
        failed++;
    }

    void markFailed(int count) {
        failed += count;
    }

    void markRemainingFailed() {
        failed += Math.max(0, requested - accounted());
    }

    int accounted() {
        return installed + skipped + failed;
    }

    boolean shouldWarnSummary() {
        return failed > 0 || skipped > 0 || accounted() != requested;
    }

    String summaryMessage() {
        return "Hook summary requested=" + requested
                + " installed=" + installed
                + " skipped=" + skipped
                + " failed=" + failed;
    }
}
