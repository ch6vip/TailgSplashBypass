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

    boolean shouldWarnSummary() {
        return failed > 0 && installed == 0;
    }

    boolean shouldResetInstalledFlag() {
        return requested > 0 && installed == 0;
    }

    String summaryMessage() {
        return "Hook summary requested=" + requested
                + " installed=" + installed
                + " skipped=" + skipped
                + " failed=" + failed;
    }
}
