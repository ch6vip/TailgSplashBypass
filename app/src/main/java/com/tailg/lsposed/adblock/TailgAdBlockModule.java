package com.tailg.lsposed.adblock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

public class TailgAdBlockModule extends XposedModule {
    private static final String TAG = "TailgAdBlockModule";
    private static final String TARGET_PACKAGE = "com.tailg.run.intelligence";
    private static final String SPLASH_ACTIVITY =
            "com.tailg.run.intelligence.model.splash.activity.SplashActivity";
    private static final String CONFIG_GET_BEAN =
            "com.tailg.run.intelligence.model.home.bean.ConfigGetBean";

    private static final String PREFS_NAME = "tailg_adblock";
    private static final String KEY_ENABLE_MODULE = "enable_module";
    private static final String KEY_HOOK_SETUP_VIEW = "hook_setup_view";
    private static final String KEY_HOOK_COUNT_DOWN = "hook_count_down";
    private static final String KEY_HOOK_CONFIG_BEAN = "hook_config_bean";
    private static final String KEY_FORCE_EMPTY_RES = "force_empty_res";
    private static final String KEY_FORCE_DURATION_ZERO = "force_duration_zero";
    private static final String KEY_VERBOSE_LOG = "verbose_log";
    private static final String EXPECTED_VERSION_PREFIX = "3.5";

    private volatile boolean hooksInstalled;

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }
        if (hooksInstalled) {
            return;
        }

        ModuleConfig config = readConfig();
        if (!config.enableModule) {
            log(Log.INFO, TAG, "Module disabled by config.");
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

            hooksInstalled = true;
            log(Log.INFO, TAG, "Hooks installed for " + TARGET_PACKAGE);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Install hooks failed", t);
        }
    }

    private void installSplashHooks(ClassLoader classLoader, ModuleConfig config) throws ClassNotFoundException {
        Class<?> splashClazz = Class.forName(SPLASH_ACTIVITY, false, classLoader);
        if (config.hookSetupView) {
            installVoidRedirectHook(splashClazz, "setupView", "setupViewNo");
        }
        if (config.hookCountDown) {
            installVoidRedirectHook(splashClazz, "countDown", "countDownNo");
        }
    }

    private void installConfigBeanHooks(ClassLoader classLoader, ModuleConfig config) throws ClassNotFoundException {
        Class<?> beanClazz = Class.forName(CONFIG_GET_BEAN, false, classLoader);

        hookStringMethod(beanClazz, "getIsShow", "0");
        if (config.forceEmptyRes) {
            hookStringMethod(beanClazz, "getHomeResource", "");
            hookStringMethod(beanClazz, "getFootResource", "");
        }
        if (config.forceDurationZero) {
            hookStringMethod(beanClazz, "getDurationTime", "0");
        }
    }

    private void installVoidRedirectHook(Class<?> targetClazz, String sourceMethodName, String targetMethodName) {
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
            log(Log.INFO, TAG, "Hooked " + sourceMethodName + " -> " + targetMethodName);
        } catch (NoSuchMethodException e) {
            log(Log.WARN, TAG, "Method missing: " + sourceMethodName + "/" + targetMethodName);
        }
    }

    private void hookStringMethod(Class<?> targetClazz, String methodName, String replacementValue) {
        try {
            Method method = targetClazz.getDeclaredMethod(methodName);
            hook(method).setPriority(PRIORITY_HIGHEST).intercept(chain -> replacementValue);
            log(Log.INFO, TAG, "Hooked " + targetClazz.getSimpleName() + "#" + methodName + " => \"" + replacementValue + "\"");
        } catch (NoSuchMethodException e) {
            log(Log.WARN, TAG, "Method missing: " + targetClazz.getSimpleName() + "#" + methodName);
        }
    }

    private ModuleConfig readConfig() {
        SharedPreferences prefs = null;
        try {
            prefs = getRemotePreferences(PREFS_NAME);
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Read remote preferences failed, fallback to defaults", t);
        }

        ModuleConfig defaults = ModuleConfig.defaults();
        if (prefs == null) {
            return defaults;
        }

        return new ModuleConfig(
                prefs.getBoolean(KEY_ENABLE_MODULE, defaults.enableModule),
                prefs.getBoolean(KEY_HOOK_SETUP_VIEW, defaults.hookSetupView),
                prefs.getBoolean(KEY_HOOK_COUNT_DOWN, defaults.hookCountDown),
                prefs.getBoolean(KEY_HOOK_CONFIG_BEAN, defaults.hookConfigBean),
                prefs.getBoolean(KEY_FORCE_EMPTY_RES, defaults.forceEmptyRes),
                prefs.getBoolean(KEY_FORCE_DURATION_ZERO, defaults.forceDurationZero),
                prefs.getBoolean(KEY_VERBOSE_LOG, defaults.verboseLog)
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
            return new ModuleConfig(true, true, true, true, true, true, true);
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
