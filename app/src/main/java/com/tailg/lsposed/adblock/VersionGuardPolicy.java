package com.tailg.lsposed.adblock;

final class VersionGuardPolicy {
    private static final String SUPPORTED_VERSION = "3.5.9";

    private VersionGuardPolicy() {
    }

    static boolean shouldInstallHooks(String versionName, boolean strictVersionGuard) {
        if (!strictVersionGuard) {
            return true;
        }
        return isSupported(versionName);
    }

    static boolean isSupported(String versionName) {
        return SUPPORTED_VERSION.equals(versionName);
    }
}
