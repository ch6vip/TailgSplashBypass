package com.tailg.lsposed.adblock;

final class HookRequestPlan {
    private final int splashRequestCount;
    private final int configBeanRequestCount;
    private final int appUpdateRequestCount;

    private HookRequestPlan(int splashRequestCount, int configBeanRequestCount, int appUpdateRequestCount) {
        this.splashRequestCount = splashRequestCount;
        this.configBeanRequestCount = configBeanRequestCount;
        this.appUpdateRequestCount = appUpdateRequestCount;
    }

    static HookRequestPlan fromConfig(
            boolean hookSetupView,
            boolean hookCountDown,
            boolean hookConfigBean,
            boolean forceEmptyRes,
            boolean forceDurationZero,
            boolean forceEmptyBanner,
            boolean hookAppUpdate
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

        return new HookRequestPlan(splash, configBean, appUpdate);
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

    int splashRequestCount() {
        return splashRequestCount;
    }

    int configBeanRequestCount() {
        return configBeanRequestCount;
    }

    int appUpdateRequestCount() {
        return appUpdateRequestCount;
    }

    int totalRequestCount() {
        return splashRequestCount + configBeanRequestCount + appUpdateRequestCount;
    }
}
