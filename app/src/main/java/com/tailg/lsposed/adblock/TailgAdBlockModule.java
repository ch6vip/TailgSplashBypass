package com.tailg.lsposed.adblock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
        boolean versionSupported = VersionGuardPolicy.isSupported(versionInfo.versionName);
        boolean shouldInstallHooks = VersionGuardPolicy.shouldInstallHooks(
                versionInfo.versionName,
                config.strictVersionGuard
        );
        if (config.verboseLog) {
            log(Log.INFO, TAG, "Target version=" + versionInfo.versionName + " (" + versionInfo.versionCode + ")");
        }
        if (!versionSupported) {
            log(Log.WARN, TAG, "Unsupported target version: " + versionInfo.versionName);
            if (!shouldInstallHooks) {
                log(Log.WARN, TAG, "strict_version_guard enabled, skip installing hooks.");
                hooksInstalled.set(false);
                return;
            }
        }

        HookRequestPlan requestPlan = HookRequestPlan.fromConfig(
                config.hookSetupView,
                config.hookCountDown,
                config.hookConfigBean,
                config.forceEmptyRes,
                config.forceDurationZero
        );
        HookInstallStats stats = new HookInstallStats();
        stats.markRequested(requestPlan.totalRequestCount());
        try {
            ClassLoader classLoader = param.getClassLoader();
            if (requestPlan.hasSplashHooks()) {
                installSplashHooks(classLoader, config, stats);
            }
            if (requestPlan.hasConfigBeanHooks()) {
                installConfigBeanHooks(classLoader, config, stats);
            }
            logInstallSummary(stats);

            if (stats.requested > 0 && stats.installed == 0) {
                hooksInstalled.set(false);
            }
        } catch (Throwable t) {
            hooksInstalled.set(false);
            log(Log.ERROR, TAG, "Install hooks failed", t);
        }
    }

    private void installSplashHooks(ClassLoader classLoader, ModuleConfig config, HookInstallStats stats) {
        Class<?> splashClazz = tryLoadClass(classLoader, SPLASH_ACTIVITY, "SplashActivity", stats);
        if (splashClazz == null) {
            return;
        }

        if (config.hookSetupView) {
            installVoidRedirectHook(splashClazz, "setupView", "setupViewNo", config.verboseLog, stats);
        }
        if (config.hookCountDown) {
            installVoidRedirectHook(splashClazz, "countDown", "countDownNo", config.verboseLog, stats);
        }
    }

    private void installConfigBeanHooks(ClassLoader classLoader, ModuleConfig config, HookInstallStats stats) {
        Class<?> beanClazz = tryLoadClass(classLoader, CONFIG_GET_BEAN, "ConfigGetBean", stats);
        if (beanClazz == null) {
            return;
        }

        hookStringMethod(beanClazz, "getIsShow", "0", config.verboseLog, stats);

        if (config.forceEmptyRes) {
            hookStringMethod(beanClazz, "getHomeResource", "", config.verboseLog, stats);
            hookStringMethod(beanClazz, "getFootResource", "", config.verboseLog, stats);
        }
        if (config.forceDurationZero) {
            hookStringMethod(beanClazz, "getDurationTime", "0", config.verboseLog, stats);
        }
    }

    private Class<?> tryLoadClass(ClassLoader classLoader, String className, String alias, HookInstallStats stats) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            stats.markFailed();
            log(Log.WARN, TAG, "Class missing: " + alias + " (" + className + ")");
            return null;
        } catch (Throwable t) {
            stats.markFailed();
            log(Log.ERROR, TAG, "Load class failed: " + alias + " (" + className + ")", t);
            return null;
        }
    }

    private void installVoidRedirectHook(
            Class<?> targetClazz,
            String sourceMethodName,
            String targetMethodName,
            boolean verboseLog,
            HookInstallStats stats
    ) {
        Method sourceMethod = findNoArgMethod(targetClazz, sourceMethodName);
        Method targetMethod = findNoArgMethod(targetClazz, targetMethodName);
        if (sourceMethod == null || targetMethod == null) {
            stats.markSkipped();
            return;
        }
        if (sourceMethod.getReturnType() != Void.TYPE || targetMethod.getReturnType() != Void.TYPE) {
            stats.markSkipped();
            log(Log.WARN, TAG, "Incompatible method signature: " + sourceMethodName + " / " + targetMethodName);
            return;
        }

        try {
            targetMethod.setAccessible(true);
            hook(sourceMethod).setPriority(PRIORITY_HIGHEST).intercept(chain -> {
                try {
                    targetMethod.invoke(chain.getThisObject());
                } catch (Throwable invokeError) {
                    log(Log.ERROR, TAG, "Redirect invoke failed: " + sourceMethodName + " -> " + targetMethodName, invokeError);
                }
                return null;
            });
            stats.markInstalled();
            if (verboseLog) {
                log(Log.INFO, TAG, "Hooked " + sourceMethodName + " -> " + targetMethodName);
            }
        } catch (Throwable t) {
            stats.markFailed();
            log(Log.ERROR, TAG, "Install redirect hook failed: " + sourceMethodName + " -> " + targetMethodName, t);
        }
    }

    private void hookStringMethod(
            Class<?> targetClazz,
            String methodName,
            String replacementValue,
            boolean verboseLog,
            HookInstallStats stats
    ) {
        Method method = findNoArgMethod(targetClazz, methodName);
        if (method == null) {
            stats.markSkipped();
            return;
        }
        if (method.getReturnType() != String.class) {
            stats.markSkipped();
            log(Log.WARN, TAG, "Incompatible return type: " + targetClazz.getSimpleName() + "#" + methodName);
            return;
        }

        try {
            hook(method).setPriority(PRIORITY_HIGHEST).intercept(chain -> replacementValue);
            stats.markInstalled();
            if (verboseLog) {
                log(Log.INFO, TAG, "Hooked " + targetClazz.getSimpleName() + "#" + methodName + " => \"" + replacementValue + "\"");
            }
        } catch (Throwable t) {
            stats.markFailed();
            log(Log.ERROR, TAG, "Install string hook failed: " + targetClazz.getSimpleName() + "#" + methodName, t);
        }
    }

    private Method findNoArgMethod(Class<?> targetClazz, String methodName) {
        try {
            return targetClazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            log(Log.WARN, TAG, "Method missing: " + targetClazz.getSimpleName() + "#" + methodName);
            return null;
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Resolve method failed: " + targetClazz.getSimpleName() + "#" + methodName, t);
            return null;
        }
    }

    private void logInstallSummary(HookInstallStats stats) {
        String summary = "Hook summary requested=" + stats.requested
                + " installed=" + stats.installed
                + " skipped=" + stats.skipped
                + " failed=" + stats.failed;
        if (stats.failed > 0 && stats.installed == 0) {
            log(Log.WARN, TAG, summary);
            return;
        }
        log(Log.INFO, TAG, summary);
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
                prefs.getBoolean(ConfigKeys.KEY_STRICT_VERSION_GUARD, defaults.strictVersionGuard),
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
                PackageManager packageManager = context.getPackageManager();
                PackageInfo packageInfo;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageInfo = packageManager.getPackageInfo(
                            TARGET_PACKAGE,
                            PackageManager.PackageInfoFlags.of(0L)
                    );
                } else {
                    packageInfo = packageManager.getPackageInfo(TARGET_PACKAGE, 0);
                }
                long versionCode = resolveVersionCode(packageInfo);
                String versionName = packageInfo.versionName == null ? "unknown" : packageInfo.versionName;
                return new VersionInfo(versionName, versionCode);
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Detect version failed", t);
        }
        return new VersionInfo("unknown", -1L);
    }

    private long resolveVersionCode(PackageInfo packageInfo) {
        try {
            Method getLongVersionCode = PackageInfo.class.getMethod("getLongVersionCode");
            Object value = getLongVersionCode.invoke(packageInfo);
            if (value instanceof Long) {
                return (Long) value;
            }
        } catch (Throwable ignore) {
            // fallback below
        }
        try {
            return PackageInfo.class.getField("versionCode").getInt(packageInfo);
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Resolve versionCode via reflection failed", t);
            return -1L;
        }
    }

    private static final class HookInstallStats {
        int requested;
        int installed;
        int skipped;
        int failed;

        void markRequested(int count) {
            requested += count;
        }

        void markInstalled() {
            installed++;
        }

        void markSkipped() {
            skipped++;
        }

        void markFailed() {
            failed++;
        }
    }

    private static final class ModuleConfig {
        final boolean enableModule;
        final boolean strictVersionGuard;
        final boolean hookSetupView;
        final boolean hookCountDown;
        final boolean hookConfigBean;
        final boolean forceEmptyRes;
        final boolean forceDurationZero;
        final boolean verboseLog;

        ModuleConfig(
                boolean enableModule,
                boolean strictVersionGuard,
                boolean hookSetupView,
                boolean hookCountDown,
                boolean hookConfigBean,
                boolean forceEmptyRes,
                boolean forceDurationZero,
                boolean verboseLog
        ) {
            this.enableModule = enableModule;
            this.strictVersionGuard = strictVersionGuard;
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
                    ConfigKeys.DEFAULT_STRICT_VERSION_GUARD,
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
