package com.tailg.lsposed.adblock;

final class OfficialFeatureEntryPolicy {
    private OfficialFeatureEntryPolicy() {
    }

    static boolean shouldAddBatteryDynamics(
            boolean enabled,
            boolean isUseCar,
            boolean alreadySupported
    ) {
        return enabled && !isUseCar && !alreadySupported;
    }

    static boolean shouldAddCustomSound(
            boolean enabled,
            boolean isBound,
            boolean customSoundSupported,
            boolean entryAlreadyVisible
    ) {
        return enabled && isBound && customSoundSupported && !entryAlreadyVisible;
    }

    static boolean shouldAddBatteryInfoShortcut(boolean enabled, boolean isUseCar) {
        return enabled && !isUseCar;
    }
}
