package com.tailg.lsposed.adblock;

final class VersionGuardPolicy {
    private static final String[] SUPPORTED_PREFIXES = {"3.5"};

    private VersionGuardPolicy() {
    }

    static boolean shouldInstallHooks(String versionName, boolean strictVersionGuard) {
        if (!strictVersionGuard) {
            return true;
        }
        return isSupported(versionName);
    }

    static boolean isSupported(String versionName) {
        if (versionName == null) {
            return false;
        }
        for (String prefix : SUPPORTED_PREFIXES) {
            if (versionName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
