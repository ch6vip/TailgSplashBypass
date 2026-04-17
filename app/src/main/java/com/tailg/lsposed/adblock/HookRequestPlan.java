package com.tailg.lsposed.adblock;

final class HookRequestPlan {
    private final int splashRequestCount;
    private final int configBeanRequestCount;

    private HookRequestPlan(int splashRequestCount, int configBeanRequestCount) {
        this.splashRequestCount = splashRequestCount;
        this.configBeanRequestCount = configBeanRequestCount;
    }

    static HookRequestPlan fromConfig(
            boolean hookSetupView,
            boolean hookCountDown,
            boolean hookConfigBean,
            boolean forceEmptyRes,
            boolean forceDurationZero
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
        }

        return new HookRequestPlan(splash, configBean);
    }

    boolean hasSplashHooks() {
        return splashRequestCount > 0;
    }

    boolean hasConfigBeanHooks() {
        return configBeanRequestCount > 0;
    }

    int splashRequestCount() {
        return splashRequestCount;
    }

    int configBeanRequestCount() {
        return configBeanRequestCount;
    }

    int totalRequestCount() {
        return splashRequestCount + configBeanRequestCount;
    }
}
