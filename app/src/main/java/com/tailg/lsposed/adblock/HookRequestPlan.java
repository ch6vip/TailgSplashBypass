package com.tailg.lsposed.adblock;

final class HookRequestPlan {
    private final int splashRequestCount;
    private final int configBeanRequestCount;
    private final int appUpdateRequestCount;
    private final int repositoryRequestCount;
    private final int homeActivityRequestCount;
    private final int trackExportRequestCount;
    private final int proximityRequestCount;

    private HookRequestPlan(
            int splashRequestCount,
            int configBeanRequestCount,
            int appUpdateRequestCount,
            int repositoryRequestCount,
            int homeActivityRequestCount,
            int trackExportRequestCount,
            int proximityRequestCount
    ) {
        this.splashRequestCount = splashRequestCount;
        this.configBeanRequestCount = configBeanRequestCount;
        this.appUpdateRequestCount = appUpdateRequestCount;
        this.repositoryRequestCount = repositoryRequestCount;
        this.homeActivityRequestCount = homeActivityRequestCount;
        this.trackExportRequestCount = trackExportRequestCount;
        this.proximityRequestCount = proximityRequestCount;
    }

    static HookRequestPlan fromConfig(
            boolean hookSetupView,
            boolean hookCountDown,
            boolean hookConfigBean,
            boolean forceEmptyRes,
            boolean forceDurationZero,
            boolean forceEmptyBanner,
            boolean hookAppUpdate,
            boolean blockUsageReport,
            boolean blockBugly,
            boolean simplifyHomeNav,
            boolean enableVehicleDiagnostics,
            boolean enableTrackExport,
            boolean overrideProximityDistance
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

        int repository = blockUsageReport ? 1 : 0;
        int homeActivity = (blockBugly ? 1 : 0)
                + (simplifyHomeNav || enableVehicleDiagnostics ? 1 : 0);
        int trackExport = enableTrackExport ? 1 : 0;
        int proximity = overrideProximityDistance ? 2 : 0;

        return new HookRequestPlan(
                splash,
                configBean,
                appUpdate,
                repository,
                homeActivity,
                trackExport,
                proximity
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

    boolean hasRepositoryHooks() {
        return repositoryRequestCount > 0;
    }

    boolean hasHomeActivityHooks() {
        return homeActivityRequestCount > 0;
    }

    boolean hasTrackExportHooks() {
        return trackExportRequestCount > 0;
    }

    boolean hasProximityHooks() {
        return proximityRequestCount > 0;
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

    int repositoryRequestCount() {
        return repositoryRequestCount;
    }

    int homeActivityRequestCount() {
        return homeActivityRequestCount;
    }

    int trackExportRequestCount() {
        return trackExportRequestCount;
    }

    int proximityRequestCount() {
        return proximityRequestCount;
    }

    int totalRequestCount() {
        return splashRequestCount
                + configBeanRequestCount
                + appUpdateRequestCount
                + repositoryRequestCount
                + homeActivityRequestCount
                + trackExportRequestCount
                + proximityRequestCount;
    }
}
