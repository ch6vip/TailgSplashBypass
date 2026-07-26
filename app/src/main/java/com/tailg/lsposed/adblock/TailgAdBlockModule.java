package com.tailg.lsposed.adblock;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

public class TailgAdBlockModule extends XposedModule {
    private static final String TAG = "TailgAdBlockModule";
    private static final String TARGET_PACKAGE = "com.tailg.run.intelligence";
    private static final String SPLASH_ACTIVITY =
            "com.tailg.run.intelligence.model.splash.activity.SplashActivity";
    private static final String CONFIG_GET_BEAN =
            "com.tailg.run.intelligence.model.home.bean.ConfigGetBean";
    private static final String CHECK_APP_VERSION_BEAN =
            "com.tailg.run.intelligence.model.mine_setting.bean.CheckAppVersionBean";
    private static final String TAILG_REPOSITORY =
            "com.tailg.run.intelligence.net.TailgRepository";
    private static final String HOME_ACTIVITY =
            "com.tailg.run.intelligence.model.home.activity.HomeActivity";
    private static final String TRACK_DETAIL_ACTIVITY =
            "com.tailg.run.intelligence.model.mine_historical_track.activity.TrackDetailActivity";
    private static final String CAR_CONTROL_INFO_BEAN =
            "com.tailg.run.intelligence.model.home.bean.CarControlInfoBean";
    private static final String SETTING_REVISION_FRAGMENT =
            "com.tailg.run.intelligence.model.mine_setting.fragment.SettingRevisionFragment";
    private static final String LEGACY_SETTING_ACTIVITY =
            "com.tailg.run.intelligence.model.mine_setting.activity.SettingActivity";

    private final AtomicBoolean initializationScheduled = new AtomicBoolean(false);

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }
        if (!initializationScheduled.compareAndSet(false, true)) {
            return;
        }

        ModuleConfig config = readConfig();
        if (!config.enableModule) {
            log(Log.INFO, TAG, "Module disabled by config.");
            return;
        }

        scheduleHookInitialization(param, config);
    }

    private void scheduleHookInitialization(PackageReadyParam param, ModuleConfig config) {
        try {
            Method attachMethod = Application.class.getDeclaredMethod("attach", Context.class);
            AtomicReference<HookHandle> attachHook = new AtomicReference<>();
            HookHandle handle = hook(attachMethod)
                    .setPriority(PRIORITY_HIGHEST)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object contextArg = chain.getArg(0);
                        if (contextArg instanceof Context context
                                && TARGET_PACKAGE.equals(context.getPackageName())) {
                            HookHandle currentHandle = attachHook.get();
                            if (currentHandle != null) {
                                currentHandle.unhook();
                            }
                            initializeHooks(param.getClassLoader(), context, config);
                        }
                        return chain.proceed();
                    });
            attachHook.set(handle);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Schedule hook initialization failed", t);
        }
    }

    private void initializeHooks(ClassLoader classLoader, Context context, ModuleConfig config) {
        VersionInfo versionInfo = detectVersionInfo(context);
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
                return;
            }
        }

        HookRequestPlan requestPlan = HookRequestPlan.fromConfig(
                config.hookSetupView,
                config.hookCountDown,
                config.hookConfigBean,
                config.forceEmptyRes,
                config.forceDurationZero,
                config.forceEmptyBanner,
                config.hookAppUpdate,
                config.blockUsageReport,
                config.blockBugly,
                config.simplifyHomeNav,
                config.enableVehicleDiagnostics,
                config.enableTrackExport,
                config.overrideProximityDistance,
                config.showOfficialSettingsEntry
        );
        HookInstallReport report = new HookInstallReport();
        report.markRequested(requestPlan.totalRequestCount());
        try {
            if (requestPlan.hasSplashHooks()) {
                installSplashHooks(
                        classLoader,
                        config,
                        requestPlan.splashRequestCount(),
                        report
                );
            }
            if (requestPlan.hasConfigBeanHooks()) {
                installConfigBeanHooks(
                        classLoader,
                        config,
                        requestPlan.configBeanRequestCount(),
                        report
                );
            }
            if (requestPlan.hasAppUpdateHooks()) {
                installAppUpdateHooks(
                        classLoader,
                        config,
                        requestPlan.appUpdateRequestCount(),
                        report
                );
            }
            if (requestPlan.hasRepositoryHooks()) {
                installRepositoryHooks(
                        classLoader,
                        config,
                        requestPlan.repositoryRequestCount(),
                        report
                );
            }
            if (requestPlan.hasHomeActivityHooks()) {
                installHomeActivityHooks(
                        classLoader,
                        config,
                        requestPlan.homeActivityRequestCount(),
                        report
                );
            }
            if (requestPlan.hasTrackExportHooks()) {
                installTrackExportHooks(
                        classLoader,
                        config,
                        requestPlan.trackExportRequestCount(),
                        report
                );
            }
            if (requestPlan.hasProximityHooks()) {
                installProximityHooks(
                        classLoader,
                        config,
                        requestPlan.proximityRequestCount(),
                        report
                );
            }
            if (requestPlan.hasOfficialSettingsHooks()) {
                installOfficialSettingsHooks(
                        classLoader,
                        config,
                        requestPlan.officialSettingsRequestCount(),
                        report
                );
            }
        } catch (Throwable t) {
            report.markRemainingFailed();
            log(Log.ERROR, TAG, "Install hooks failed", t);
        } finally {
            logInstallSummary(report);
        }
    }

    private void installSplashHooks(
            ClassLoader classLoader,
            ModuleConfig config,
            int requestCount,
            HookInstallReport report
    ) {
        Class<?> splashClazz = tryLoadClass(
                classLoader,
                SPLASH_ACTIVITY,
                "SplashActivity",
                requestCount,
                report
        );
        if (splashClazz == null) {
            return;
        }

        if (config.hookSetupView) {
            installVoidRedirectHook(splashClazz, "setupView", "setupViewNo", config.verboseLog, report);
        }
        if (config.hookCountDown) {
            installVoidRedirectHook(splashClazz, "countDown", "countDownNo", config.verboseLog, report);
        }
    }

    private void installConfigBeanHooks(
            ClassLoader classLoader,
            ModuleConfig config,
            int requestCount,
            HookInstallReport report
    ) {
        Class<?> beanClazz = tryLoadClass(
                classLoader,
                CONFIG_GET_BEAN,
                "ConfigGetBean",
                requestCount,
                report
        );
        if (beanClazz == null) {
            return;
        }

        hookStringMethod(beanClazz, "getIsShow", "0", config.verboseLog, report);

        if (config.forceEmptyRes) {
            hookStringMethod(beanClazz, "getHomeResource", "", config.verboseLog, report);
            hookStringMethod(beanClazz, "getFootResource", "", config.verboseLog, report);
        }
        if (config.forceDurationZero) {
            hookStringMethod(beanClazz, "getDurationTime", "0", config.verboseLog, report);
        }
        if (config.forceEmptyBanner) {
            hookStringMethod(beanClazz, "getBanners", "", config.verboseLog, report);
            hookStringMethod(beanClazz, "getBannerOssIds", "", config.verboseLog, report);
        }
    }

    private void installAppUpdateHooks(
            ClassLoader classLoader,
            ModuleConfig config,
            int requestCount,
            HookInstallReport report
    ) {
        Class<?> beanClazz = tryLoadClass(
                classLoader,
                CHECK_APP_VERSION_BEAN,
                "CheckAppVersionBean",
                requestCount,
                report
        );
        if (beanClazz == null) {
            return;
        }

        // HomeActivity 仅在 "1".equals(getIsPop()) 时弹升级框；置 "0" 直接短路掉自动弹窗。
        hookStringMethod(beanClazz, "getIsPop", "0", config.verboseLog, report);
        // 兜底：即使别处仍触发升级框，也保证不是强制/阻塞式。
        hookStringMethod(beanClazz, "getIsForce", "0", config.verboseLog, report);
    }

    private void installRepositoryHooks(
            ClassLoader classLoader,
            ModuleConfig config,
            int requestCount,
            HookInstallReport report
    ) {
        Class<?> repositoryClass = tryLoadClass(
                classLoader,
                TAILG_REPOSITORY,
                "TailgRepository",
                requestCount,
                report
        );
        if (repositoryClass == null) {
            return;
        }
        Method collectReport = findMethod(
                repositoryClass,
                "collectReport",
                String.class,
                List.class
        );
        if (collectReport == null) {
            report.markSkipped();
            return;
        }

        try {
            Class<?> observableClass = Class.forName("io.reactivex.Observable", false, classLoader);
            Object emptyObservable = observableClass.getDeclaredMethod("empty").invoke(null);
            if (!collectReport.getReturnType().isInstance(emptyObservable)) {
                report.markSkipped();
                log(Log.WARN, TAG, "Unexpected TailgRepository#collectReport return type");
                return;
            }
            hook(collectReport)
                    .setPriority(PRIORITY_HIGHEST)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> emptyObservable);
            report.markInstalled();
            if (config.verboseLog) {
                log(Log.INFO, TAG, "Hooked TailgRepository#collectReport => Observable.empty()");
            }
        } catch (Throwable error) {
            report.markFailed();
            log(Log.ERROR, TAG, "Install usage report hook failed", error);
        }
    }

    private void installHomeActivityHooks(
            ClassLoader classLoader,
            ModuleConfig config,
            int requestCount,
            HookInstallReport report
    ) {
        Class<?> homeClass = tryLoadClass(
                classLoader,
                HOME_ACTIVITY,
                "HomeActivity",
                requestCount,
                report
        );
        if (homeClass == null) {
            return;
        }
        if (config.blockBugly) {
            installVoidBlockHook(homeClass, "initTencentBugly", config.verboseLog, report);
        }
        if (config.simplifyHomeNav || config.enableVehicleDiagnostics) {
            Method setupFragment = findNoArgMethod(homeClass, "setupFragment");
            if (setupFragment == null || setupFragment.getReturnType() != Void.TYPE) {
                report.markSkipped();
                return;
            }
            try {
                hook(setupFragment)
                        .setPriority(PRIORITY_HIGHEST)
                        .setExceptionMode(ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            Object target = chain.getThisObject();
                            if (target instanceof Activity activity) {
                                HomeEnhancementController.apply(
                                        activity,
                                        classLoader,
                                        config.simplifyHomeNav,
                                        config.enableVehicleDiagnostics,
                                        config.overrideProximityDistance,
                                        config.proximityUnlockMeters,
                                        config.proximityLockMeters
                                );
                            }
                            return result;
                        });
                report.markInstalled();
            } catch (Throwable error) {
                report.markFailed();
                log(Log.ERROR, TAG, "Install HomeActivity enhancement hook failed", error);
            }
        }
    }

    private void installTrackExportHooks(
            ClassLoader classLoader,
            ModuleConfig config,
            int requestCount,
            HookInstallReport report
    ) {
        Class<?> trackClass = tryLoadClass(
                classLoader,
                TRACK_DETAIL_ACTIVITY,
                "TrackDetailActivity",
                requestCount,
                report
        );
        if (trackClass == null) {
            return;
        }
        Method setEventListener = findNoArgMethod(trackClass, "setEventListener");
        if (setEventListener == null || setEventListener.getReturnType() != Void.TYPE) {
            report.markSkipped();
            return;
        }
        try {
            hook(setEventListener)
                    .setPriority(PRIORITY_HIGHEST)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object target = chain.getThisObject();
                        if (target instanceof Activity activity) {
                            TrackExportController.install(
                                    activity,
                                    classLoader,
                                    config.trimTrackEndpoints
                            );
                        }
                        return result;
                    });
            report.markInstalled();
        } catch (Throwable error) {
            report.markFailed();
            log(Log.ERROR, TAG, "Install track export hook failed", error);
        }
    }

    private void installProximityHooks(
            ClassLoader classLoader,
            ModuleConfig config,
            int requestCount,
            HookInstallReport report
    ) {
        Class<?> beanClass = tryLoadClass(
                classLoader,
                CAR_CONTROL_INFO_BEAN,
                "CarControlInfoBean",
                requestCount,
                report
        );
        if (beanClass == null) {
            return;
        }
        hookStringMethod(
                beanClass,
                "getMinRssiDistance",
                Float.toString(config.proximityUnlockMeters),
                config.verboseLog,
                report
        );
        hookStringMethod(
                beanClass,
                "getMaxRssiDistance",
                Float.toString(config.proximityLockMeters),
                config.verboseLog,
                report
        );
    }

    private void installOfficialSettingsHooks(
            ClassLoader classLoader,
            ModuleConfig config,
            int requestCount,
            HookInstallReport report
    ) {
        int hookCount = requestCount / 2;
        Class<?> revisionFragment = tryLoadClass(
                classLoader,
                SETTING_REVISION_FRAGMENT,
                "SettingRevisionFragment",
                hookCount,
                report
        );
        if (revisionFragment != null) {
            Method onViewCreated = findMethod(
                    revisionFragment,
                    "onViewCreated",
                    View.class,
                    Bundle.class
            );
            installSettingsEntryHook(
                    onViewCreated,
                    "SettingRevisionFragment#onViewCreated",
                    chainTarget -> OfficialSettingsController.installRevisionEntry(chainTarget),
                    config.verboseLog,
                    report
            );
        }

        Class<?> legacyActivity = tryLoadClass(
                classLoader,
                LEGACY_SETTING_ACTIVITY,
                "SettingActivity",
                requestCount - hookCount,
                report
        );
        if (legacyActivity != null) {
            Method setEventListener = findNoArgMethod(legacyActivity, "setEventListener");
            installSettingsEntryHook(
                    setEventListener,
                    "SettingActivity#setEventListener",
                    chainTarget -> {
                        if (chainTarget instanceof Activity activity) {
                            OfficialSettingsController.installLegacyEntry(activity);
                        }
                    },
                    config.verboseLog,
                    report
            );
        }
    }

    private void installSettingsEntryHook(
            Method method,
            String methodLabel,
            SettingsEntryInstaller installer,
            boolean verboseLog,
            HookInstallReport report
    ) {
        if (method == null || method.getReturnType() != Void.TYPE
                || Modifier.isStatic(method.getModifiers())) {
            report.markSkipped();
            return;
        }
        try {
            hook(method)
                    .setPriority(PRIORITY_HIGHEST)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        installer.install(chain.getThisObject());
                        return result;
                    });
            report.markInstalled();
            if (verboseLog) {
                log(Log.INFO, TAG, "Hooked official settings entry: " + methodLabel);
            }
        } catch (Throwable error) {
            report.markFailed();
            log(Log.ERROR, TAG, "Install official settings entry hook failed: "
                    + methodLabel, error);
        }
    }

    private Class<?> tryLoadClass(
            ClassLoader classLoader,
            String className,
            String alias,
            int requestCount,
            HookInstallReport report
    ) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            report.markFailed(requestCount);
            log(Log.WARN, TAG, "Class missing: " + alias + " (" + className + ")");
            return null;
        } catch (Throwable t) {
            report.markFailed(requestCount);
            log(Log.ERROR, TAG, "Load class failed: " + alias + " (" + className + ")", t);
            return null;
        }
    }

    private void installVoidRedirectHook(
            Class<?> targetClazz,
            String sourceMethodName,
            String targetMethodName,
            boolean verboseLog,
            HookInstallReport report
    ) {
        Method sourceMethod = findNoArgMethod(targetClazz, sourceMethodName);
        Method targetMethod = findNoArgMethod(targetClazz, targetMethodName);
        if (sourceMethod == null || targetMethod == null) {
            report.markSkipped();
            return;
        }
        if (sourceMethod.getReturnType() != Void.TYPE
                || targetMethod.getReturnType() != Void.TYPE
                || Modifier.isStatic(sourceMethod.getModifiers())
                != Modifier.isStatic(targetMethod.getModifiers())) {
            report.markSkipped();
            log(Log.WARN, TAG, "Incompatible method signature: " + sourceMethodName + " / " + targetMethodName);
            return;
        }

        try {
            targetMethod.setAccessible(true);
            hook(sourceMethod)
                    .setPriority(PRIORITY_HIGHEST)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        try {
                            targetMethod.invoke(chain.getThisObject());
                        } catch (InvocationTargetException invocationError) {
                            Throwable invokeError = invocationError.getCause() == null
                                    ? invocationError
                                    : invocationError.getCause();
                            log(Log.ERROR, TAG, "Redirect invoke failed: " + sourceMethodName
                                    + " -> " + targetMethodName, invokeError);
                            throw invokeError;
                        } catch (Throwable invokeError) {
                            log(Log.ERROR, TAG, "Redirect invoke failed: " + sourceMethodName
                                    + " -> " + targetMethodName, invokeError);
                            throw invokeError;
                        }
                        return null;
                    });
            report.markInstalled();
            if (verboseLog) {
                log(Log.INFO, TAG, "Hooked " + sourceMethodName + " -> " + targetMethodName);
            }
        } catch (Throwable t) {
            report.markFailed();
            log(Log.ERROR, TAG, "Install redirect hook failed: " + sourceMethodName + " -> " + targetMethodName, t);
        }
    }

    private void installVoidBlockHook(
            Class<?> targetClazz,
            String methodName,
            boolean verboseLog,
            HookInstallReport report
    ) {
        Method method = findNoArgMethod(targetClazz, methodName);
        if (method == null) {
            report.markSkipped();
            return;
        }
        if (method.getReturnType() != Void.TYPE || Modifier.isStatic(method.getModifiers())) {
            report.markSkipped();
            log(Log.WARN, TAG, "Incompatible void method: "
                    + targetClazz.getSimpleName() + "#" + methodName);
            return;
        }
        try {
            hook(method)
                    .setPriority(PRIORITY_HIGHEST)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> null);
            report.markInstalled();
            if (verboseLog) {
                log(Log.INFO, TAG, "Blocked " + targetClazz.getSimpleName() + "#" + methodName);
            }
        } catch (Throwable error) {
            report.markFailed();
            log(Log.ERROR, TAG, "Install void block hook failed: "
                    + targetClazz.getSimpleName() + "#" + methodName, error);
        }
    }

    private void hookStringMethod(
            Class<?> targetClazz,
            String methodName,
            String replacementValue,
            boolean verboseLog,
            HookInstallReport report
    ) {
        Method method = findNoArgMethod(targetClazz, methodName);
        if (method == null) {
            report.markSkipped();
            return;
        }
        if (method.getReturnType() != String.class) {
            report.markSkipped();
            log(Log.WARN, TAG, "Incompatible return type: " + targetClazz.getSimpleName() + "#" + methodName);
            return;
        }

        try {
            hook(method)
                    .setPriority(PRIORITY_HIGHEST)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> replacementValue);
            report.markInstalled();
            if (verboseLog) {
                log(Log.INFO, TAG, "Hooked " + targetClazz.getSimpleName() + "#" + methodName + " => \"" + replacementValue + "\"");
            }
        } catch (Throwable t) {
            report.markFailed();
            log(Log.ERROR, TAG, "Install string hook failed: " + targetClazz.getSimpleName() + "#" + methodName, t);
        }
    }

    private Method findNoArgMethod(Class<?> targetClazz, String methodName) {
        return findMethod(targetClazz, methodName);
    }

    private Method findMethod(Class<?> targetClazz, String methodName, Class<?>... parameterTypes) {
        try {
            return targetClazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            log(Log.WARN, TAG, "Method missing: " + targetClazz.getSimpleName() + "#" + methodName);
            return null;
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Resolve method failed: " + targetClazz.getSimpleName() + "#" + methodName, t);
            return null;
        }
    }

    private void logInstallSummary(HookInstallReport report) {
        String summary = report.summaryMessage();
        if (report.shouldWarnSummary()) {
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

        try {
            ProximityPolicy.Distances distances = ProximityPolicy.normalize(
                    prefs.getFloat(
                            ConfigKeys.KEY_PROXIMITY_UNLOCK_METERS,
                            defaults.proximityUnlockMeters
                    ),
                    prefs.getFloat(
                            ConfigKeys.KEY_PROXIMITY_LOCK_METERS,
                            defaults.proximityLockMeters
                    )
            );
            return new ModuleConfig(
                    prefs.getBoolean(ConfigKeys.KEY_ENABLE_MODULE, defaults.enableModule),
                    prefs.getBoolean(ConfigKeys.KEY_STRICT_VERSION_GUARD, defaults.strictVersionGuard),
                    prefs.getBoolean(ConfigKeys.KEY_HOOK_SETUP_VIEW, defaults.hookSetupView),
                    prefs.getBoolean(ConfigKeys.KEY_HOOK_COUNT_DOWN, defaults.hookCountDown),
                    prefs.getBoolean(ConfigKeys.KEY_HOOK_CONFIG_BEAN, defaults.hookConfigBean),
                    prefs.getBoolean(ConfigKeys.KEY_FORCE_EMPTY_RES, defaults.forceEmptyRes),
                    prefs.getBoolean(ConfigKeys.KEY_FORCE_DURATION_ZERO, defaults.forceDurationZero),
                    prefs.getBoolean(ConfigKeys.KEY_FORCE_EMPTY_BANNER, defaults.forceEmptyBanner),
                    prefs.getBoolean(ConfigKeys.KEY_HOOK_APP_UPDATE, defaults.hookAppUpdate),
                    prefs.getBoolean(ConfigKeys.KEY_BLOCK_USAGE_REPORT, defaults.blockUsageReport),
                    prefs.getBoolean(ConfigKeys.KEY_BLOCK_BUGLY, defaults.blockBugly),
                    prefs.getBoolean(ConfigKeys.KEY_SIMPLIFY_HOME_NAV, defaults.simplifyHomeNav),
                    prefs.getBoolean(ConfigKeys.KEY_ENABLE_TRACK_EXPORT, defaults.enableTrackExport),
                    prefs.getBoolean(ConfigKeys.KEY_TRIM_TRACK_ENDPOINTS, defaults.trimTrackEndpoints),
                    prefs.getBoolean(
                            ConfigKeys.KEY_ENABLE_VEHICLE_DIAGNOSTICS,
                            defaults.enableVehicleDiagnostics
                    ),
                    prefs.getBoolean(
                            ConfigKeys.KEY_OVERRIDE_PROXIMITY_DISTANCE,
                            defaults.overrideProximityDistance
                    ),
                    prefs.getBoolean(
                            ConfigKeys.KEY_SHOW_OFFICIAL_SETTINGS_ENTRY,
                            defaults.showOfficialSettingsEntry
                    ),
                    distances.unlockMeters,
                    distances.lockMeters,
                    prefs.getBoolean(ConfigKeys.KEY_VERBOSE_LOG, defaults.verboseLog)
            );
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Read remote preference values failed, fallback to defaults", t);
            return defaults;
        }
    }

    private VersionInfo detectVersionInfo(Context context) {
        try {
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

    private static final class ModuleConfig {
        final boolean enableModule;
        final boolean strictVersionGuard;
        final boolean hookSetupView;
        final boolean hookCountDown;
        final boolean hookConfigBean;
        final boolean forceEmptyRes;
        final boolean forceDurationZero;
        final boolean forceEmptyBanner;
        final boolean hookAppUpdate;
        final boolean blockUsageReport;
        final boolean blockBugly;
        final boolean simplifyHomeNav;
        final boolean enableTrackExport;
        final boolean trimTrackEndpoints;
        final boolean enableVehicleDiagnostics;
        final boolean overrideProximityDistance;
        final boolean showOfficialSettingsEntry;
        final float proximityUnlockMeters;
        final float proximityLockMeters;
        final boolean verboseLog;

        ModuleConfig(
                boolean enableModule,
                boolean strictVersionGuard,
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
                boolean enableTrackExport,
                boolean trimTrackEndpoints,
                boolean enableVehicleDiagnostics,
                boolean overrideProximityDistance,
                boolean showOfficialSettingsEntry,
                float proximityUnlockMeters,
                float proximityLockMeters,
                boolean verboseLog
        ) {
            this.enableModule = enableModule;
            this.strictVersionGuard = strictVersionGuard;
            this.hookSetupView = hookSetupView;
            this.hookCountDown = hookCountDown;
            this.hookConfigBean = hookConfigBean;
            this.forceEmptyRes = forceEmptyRes;
            this.forceDurationZero = forceDurationZero;
            this.forceEmptyBanner = forceEmptyBanner;
            this.hookAppUpdate = hookAppUpdate;
            this.blockUsageReport = blockUsageReport;
            this.blockBugly = blockBugly;
            this.simplifyHomeNav = simplifyHomeNav;
            this.enableTrackExport = enableTrackExport;
            this.trimTrackEndpoints = trimTrackEndpoints;
            this.enableVehicleDiagnostics = enableVehicleDiagnostics;
            this.overrideProximityDistance = overrideProximityDistance;
            this.showOfficialSettingsEntry = showOfficialSettingsEntry;
            this.proximityUnlockMeters = proximityUnlockMeters;
            this.proximityLockMeters = proximityLockMeters;
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
                    ConfigKeys.DEFAULT_FORCE_EMPTY_BANNER,
                    ConfigKeys.DEFAULT_HOOK_APP_UPDATE,
                    ConfigKeys.DEFAULT_BLOCK_USAGE_REPORT,
                    ConfigKeys.DEFAULT_BLOCK_BUGLY,
                    ConfigKeys.DEFAULT_SIMPLIFY_HOME_NAV,
                    ConfigKeys.DEFAULT_ENABLE_TRACK_EXPORT,
                    ConfigKeys.DEFAULT_TRIM_TRACK_ENDPOINTS,
                    ConfigKeys.DEFAULT_ENABLE_VEHICLE_DIAGNOSTICS,
                    ConfigKeys.DEFAULT_OVERRIDE_PROXIMITY_DISTANCE,
                    ConfigKeys.DEFAULT_SHOW_OFFICIAL_SETTINGS_ENTRY,
                    ConfigKeys.DEFAULT_PROXIMITY_UNLOCK_METERS,
                    ConfigKeys.DEFAULT_PROXIMITY_LOCK_METERS,
                    ConfigKeys.DEFAULT_VERBOSE_LOG
            );
        }
    }

    @FunctionalInterface
    private interface SettingsEntryInstaller {
        void install(Object target);
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
