package com.tailg.lsposed.adblock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedModule;

public class TailgAdBlockModule extends XposedModule {
    private static final String TAG = "TailgAdBlockModule";
    private static final String TARGET_PACKAGE = "com.tailg.run.intelligence";
    private static final String SPLASH_ACTIVITY =
            "com.tailg.run.intelligence.model.splash.activity.SplashActivity";
    private static final String CONFIG_GET_BEAN =
            "com.tailg.run.intelligence.model.home.bean.ConfigGetBean";

    private static final String EXPECTED_VERSION_PREFIX = "3.5";

    private final AtomicBoolean hooksInstalled = new AtomicBoolean(false);

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }
        if (!hooksInstalled.compareAndSet(false, true)) {
            return;
        }

        ModuleConfig config = readConfig();
        if (!config.enableModule) {
            log(Log.INFO, TAG, "Module disabled by config.");
            hooksInstalled.set(false);
            return;
        }

        VersionInfo versionInfo = detectVersionInfo();
        if (config.verboseLog) {
            log(Log.INFO, TAG, "Target version=" + versionInfo.versionName + " (" + versionInfo.versionCode + ")");
        }
        if (!versionInfo.versionName.startsWith(EXPECTED_VERSION_PREFIX)) {
            log(Log.WARN, TAG, "Unexpected target version: " + versionInfo.versionName);
        }

        try {
            ClassLoader classLoader = param.getClassLoader();
            if (config.hookSetupView || config.hookCountDown) {
                installSplashHooks(classLoader, config);
            }
            if (config.hookConfigBean) {
                installConfigBeanHooks(classLoader, config);
            }

            if (config.verboseLog) {
                log(Log.INFO, TAG, "Hooks installed for " + TARGET_PACKAGE);
            }
        } catch (Throwable t) {
            hooksInstalled.set(false);
            log(Log.ERROR, TAG, "Install hooks failed", t);
        }
    }

    private void installSplashHooks(ClassLoader classLoader, ModuleConfig config) throws ClassNotFoundException {
        Class<?> splashClazz = Class.forName(SPLASH_ACTIVITY, false, classLoader);
        if (config.hookSetupView) {
            installVoidRedirectHook(splashClazz, "setupView", "setupViewNo", config.verboseLog);
        }
        if (config.hookCountDown) {
            installVoidRedirectHook(splashClazz, "countDown", "countDownNo", config.verboseLog);
        }
    }

    private void installConfigBeanHooks(ClassLoader classLoader, ModuleConfig config) throws ClassNotFoundException {
        Class<?> beanClazz = Class.forName(CONFIG_GET_BEAN, false, classLoader);

        hookStringMethod(beanClazz, "getIsShow", "0", config.verboseLog);
        if (config.forceEmptyRes) {
            hookStringMethod(beanClazz, "getHomeResource", "", config.verboseLog);
            hookStringMethod(beanClazz, "getFootResource", "", config.verboseLog);
        }
        if (config.forceDurationZero) {
            hookStringMethod(beanClazz, "getDurationTime", "0", config.verboseLog);
        }
    }

    private void installVoidRedirectHook(
            Class<?> targetClazz,
            String sourceMethodName,
            String targetMethodName,
            boolean verboseLog
    ) {
        try {
            Method sourceMethod = targetClazz.getDeclaredMethod(sourceMethodName);
            Method targetMethod = targetClazz.getDeclaredMethod(targetMethodName);
            targetMethod.setAccessible(true);

            hook(sourceMethod).setPriority(PRIORITY_HIGHEST).intercept(chain -> {
                try {
                    targetMethod.invoke(chain.getThisObject());
                } catch (Throwable invokeError) {
                    log(Log.ERROR, TAG, "Redirect invoke failed: " + sourceMethodName + " -> " + targetMethodName, invokeError);
                }
                return null;
            });
            if (verboseLog) {
                log(Log.INFO, TAG, "Hooked " + sourceMethodName + " -> " + targetMethodName);
            }
        } catch (NoSuchMethodException e) {
            log(Log.WARN, TAG, "Method missing: " + sourceMethodName + "/" + targetMethodName);
        }
    }

    private void hookStringMethod(
            Class<?> targetClazz,
            String methodName,
            String replacementValue,
            boolean verboseLog
    ) {
        try {
            Method method = targetClazz.getDeclaredMethod(methodName);
            hook(method).setPriority(PRIORITY_HIGHEST).intercept(chain -> replacementValue);
            if (verboseLog) {
                log(Log.INFO, TAG, "Hooked " + targetClazz.getSimpleName() + "#" + methodName + " => \"" + replacementValue + "\"");
            }
        } catch (NoSuchMethodException e) {
            log(Log.WARN, TAG, "Method missing: " + targetClazz.getSimpleName() + "#" + methodName);
        }
    }

    private ModuleConfig readConfig() {
        SharedPreferences prefs = null;
        try {
            prefs = getRemotePreferences(ConfigKeys.PREFS_NAME);
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Read remote preferences failed, fallback to defaults", t);
        }

        ModuleConfig defaults = ModuleConfig.defaults();
        if (prefs == null) {
            return defaults;
        }

        return new ModuleConfig(
                prefs.getBoolean(ConfigKeys.KEY_ENABLE_MODULE, defaults.enableModule),
                prefs.getBoolean(ConfigKeys.KEY_HOOK_SETUP_VIEW, defaults.hookSetupView),
                prefs.getBoolean(ConfigKeys.KEY_HOOK_COUNT_DOWN, defaults.hookCountDown),
                prefs.getBoolean(ConfigKeys.KEY_HOOK_CONFIG_BEAN, defaults.hookConfigBean),
                prefs.getBoolean(ConfigKeys.KEY_FORCE_EMPTY_RES, defaults.forceEmptyRes),
                prefs.getBoolean(ConfigKeys.KEY_FORCE_DURATION_ZERO, defaults.forceDurationZero),
                prefs.getBoolean(ConfigKeys.KEY_VERBOSE_LOG, defaults.verboseLog)
        );
    }

    private VersionInfo detectVersionInfo() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object app = activityThread.getMethod("currentApplication").invoke(null);
            if (app instanceof Context context) {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(TARGET_PACKAGE, 0);
                long versionCode;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    versionCode = packageInfo.getLongVersionCode();
                } else {
                    versionCode = packageInfo.versionCode;
                }
                String versionName = packageInfo.versionName == null ? "unknown" : packageInfo.versionName;
                return new VersionInfo(versionName, versionCode);
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Detect version failed", t);
        }
        return new VersionInfo("unknown", -1L);
    }

    private static final class ModuleConfig {
        final boolean enableModule;
        final boolean hookSetupView;
        final boolean hookCountDown;
        final boolean hookConfigBean;
        final boolean forceEmptyRes;
        final boolean forceDurationZero;
        final boolean verboseLog;

        ModuleConfig(
                boolean enableModule,
                boolean hookSetupView,
                boolean hookCountDown,
                boolean hookConfigBean,
                boolean forceEmptyRes,
                boolean forceDurationZero,
                boolean verboseLog
        ) {
            this.enableModule = enableModule;
            this.hookSetupView = hookSetupView;
            this.hookCountDown = hookCountDown;
            this.hookConfigBean = hookConfigBean;
            this.forceEmptyRes = forceEmptyRes;
            this.forceDurationZero = forceDurationZero;
            this.verboseLog = verboseLog;
        }

        static ModuleConfig defaults() {
            return new ModuleConfig(
                    ConfigKeys.DEFAULT_ENABLE_MODULE,
                    ConfigKeys.DEFAULT_HOOK_SETUP_VIEW,
                    ConfigKeys.DEFAULT_HOOK_COUNT_DOWN,
                    ConfigKeys.DEFAULT_HOOK_CONFIG_BEAN,
                    ConfigKeys.DEFAULT_FORCE_EMPTY_RES,
                    ConfigKeys.DEFAULT_FORCE_DURATION_ZERO,
                    ConfigKeys.DEFAULT_VERBOSE_LOG
            );
        }
    }

    private static final class VersionInfo {
        final String versionName;
        final long versionCode;

        VersionInfo(String versionName, long versionCode) {
            this.versionName = versionName;
            this.versionCode = versionCode;
        }
    }
}
