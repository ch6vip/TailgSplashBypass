package com.tailg.lsposed.adblock;

final class HookRequestPlan {
    private final int splashRequestCount;
    private final int configBeanRequestCount;
    private final int appUpdateRequestCount;
    private final int fastStartupRequestCount;
    private final int repositoryRequestCount;
    private final int homeActivityRequestCount;
    private final int trackExportRequestCount;
    private final int hiddenFeatureRequestCount;
    private final int proximityRequestCount;
    private final int officialSettingsRequestCount;

    private HookRequestPlan(
            int splashRequestCount,
            int configBeanRequestCount,
            int appUpdateRequestCount,
            int fastStartupRequestCount,
            int repositoryRequestCount,
            int homeActivityRequestCount,
            int trackExportRequestCount,
            int hiddenFeatureRequestCount,
            int proximityRequestCount,
            int officialSettingsRequestCount
    ) {
        this.splashRequestCount = splashRequestCount;
        this.configBeanRequestCount = configBeanRequestCount;
        this.appUpdateRequestCount = appUpdateRequestCount;
        this.fastStartupRequestCount = fastStartupRequestCount;
        this.repositoryRequestCount = repositoryRequestCount;
        this.homeActivityRequestCount = homeActivityRequestCount;
        this.trackExportRequestCount = trackExportRequestCount;
        this.hiddenFeatureRequestCount = hiddenFeatureRequestCount;
        this.proximityRequestCount = proximityRequestCount;
        this.officialSettingsRequestCount = officialSettingsRequestCount;
    }

    static HookRequestPlan fromConfig(
            boolean hookSetupView,
            boolean hookCountDown,
            boolean hookConfigBean,
            boolean forceEmptyRes,
            boolean forceDurationZero,
            boolean forceEmptyBanner,
            boolean hookAppUpdate,
            boolean fastStartup,
            boolean blockUsageReport,
            boolean blockBugly,
            boolean simplifyHomeNav,
            boolean swapControlServiceNav,
            boolean enableVehicleDiagnostics,
            boolean enableTrackExport,
            boolean enableMonthlyRideData,
            boolean showBrakeForceData,
            boolean showBatteryDynamicsEntry,
            boolean showCustomVehicleSound,
            boolean overrideProximityDistance,
            boolean bleReconnect,
            boolean showOfficialSettingsEntry
    ) {
        int splash = 0;
        if (hookSetupView) {
            splash++;
        }
        if (hookCountDown) {
            splash++;
        }

        int configBean = 0;
        if (hookConfigBean) {
            configBean++; // getIsShow
            if (forceEmptyRes) {
                configBean += 2; // getHomeResource + getFootResource
            }
            if (forceDurationZero) {
                configBean++; // getDurationTime
            }
            if (forceEmptyBanner) {
                configBean += 2; // getBanners + getBannerOssIds
            }
        }

        int appUpdate = 0;
        if (hookAppUpdate) {
            appUpdate += 2; // getIsPop + getIsForce
        }

        int fastStartupHooks = fastStartup ? 5 : 0;
        int repository = blockUsageReport ? 1 : 0;
        int homeActivity = (blockBugly ? 1 : 0)
                + (simplifyHomeNav || swapControlServiceNav || enableVehicleDiagnostics ? 1 : 0)
                + (bleReconnect ? 1 : 0);
        int trackExport = enableTrackExport ? 1 : 0;
        int hiddenFeatures = (enableMonthlyRideData ? 2 : 0)
                + (showBrakeForceData ? 1 : 0)
                + (showBatteryDynamicsEntry || showCustomVehicleSound ? 1 : 0);
        int proximity = overrideProximityDistance ? 2 : 0;
        int officialSettings = showOfficialSettingsEntry ? 2 : 0;

        return new HookRequestPlan(
                splash,
                configBean,
                appUpdate,
                fastStartupHooks,
                repository,
                homeActivity,
                trackExport,
                hiddenFeatures,
                proximity,
                officialSettings
        );
    }

    boolean hasSplashHooks() {
        return splashRequestCount > 0;
    }

    boolean hasConfigBeanHooks() {
        return configBeanRequestCount > 0;
    }

    boolean hasAppUpdateHooks() {
        return appUpdateRequestCount > 0;
    }

    boolean hasFastStartupHooks() {
        return fastStartupRequestCount > 0;
    }

    boolean hasRepositoryHooks() {
        return repositoryRequestCount > 0;
    }

    boolean hasHomeActivityHooks() {
        return homeActivityRequestCount > 0;
    }

    boolean hasTrackExportHooks() {
        return trackExportRequestCount > 0;
    }

    boolean hasHiddenFeatureHooks() {
        return hiddenFeatureRequestCount > 0;
    }

    boolean hasProximityHooks() {
        return proximityRequestCount > 0;
    }

    boolean hasOfficialSettingsHooks() {
        return officialSettingsRequestCount > 0;
    }

    int splashRequestCount() {
        return splashRequestCount;
    }

    int configBeanRequestCount() {
        return configBeanRequestCount;
    }

    int appUpdateRequestCount() {
        return appUpdateRequestCount;
    }

    int fastStartupRequestCount() {
        return fastStartupRequestCount;
    }

    int repositoryRequestCount() {
        return repositoryRequestCount;
    }

    int homeActivityRequestCount() {
        return homeActivityRequestCount;
    }

    int trackExportRequestCount() {
        return trackExportRequestCount;
    }

    int hiddenFeatureRequestCount() {
        return hiddenFeatureRequestCount;
    }

    int proximityRequestCount() {
        return proximityRequestCount;
    }

    int officialSettingsRequestCount() {
        return officialSettingsRequestCount;
    }

    int totalRequestCount() {
        return splashRequestCount
                + configBeanRequestCount
                + appUpdateRequestCount
                + fastStartupRequestCount
                + repositoryRequestCount
                + homeActivityRequestCount
                + trackExportRequestCount
                + hiddenFeatureRequestCount
                + proximityRequestCount
                + officialSettingsRequestCount;
    }
}
