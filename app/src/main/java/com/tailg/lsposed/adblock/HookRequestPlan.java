package com.tailg.lsposed.adblock;

final class HookRequestPlan {
    private final int splashRequestCount;
    private final int configBeanRequestCount;
    private final int appUpdateRequestCount;
    private final int fastStartupRequestCount;
    private final int repositoryRequestCount;
    private final int homeActivityRequestCount;

    private HookRequestPlan(
            int splashRequestCount,
            int configBeanRequestCount,
            int appUpdateRequestCount,
            int fastStartupRequestCount,
            int repositoryRequestCount,
            int homeActivityRequestCount
    ) {
        this.splashRequestCount = splashRequestCount;
        this.configBeanRequestCount = configBeanRequestCount;
        this.appUpdateRequestCount = appUpdateRequestCount;
        this.fastStartupRequestCount = fastStartupRequestCount;
        this.repositoryRequestCount = repositoryRequestCount;
        this.homeActivityRequestCount = homeActivityRequestCount;
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
            boolean blockBugly
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
        int homeActivity = blockBugly ? 1 : 0;

        return new HookRequestPlan(
                splash,
                configBean,
                appUpdate,
                fastStartupHooks,
                repository,
                homeActivity
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

    int totalRequestCount() {
        return splashRequestCount
                + configBeanRequestCount
                + appUpdateRequestCount
                + fastStartupRequestCount
                + repositoryRequestCount
                + homeActivityRequestCount;
    }
}
