package com.tailg.lsposed.adblock;

import android.util.Log;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

public class TailgAdBlockModule extends XposedModule {
    private static final String TAG = "TailgAdBlockModule";
    private static final String TARGET_PACKAGE = "com.tailg.run.intelligence";
    private static final String SPLASH_ACTIVITY =
            "com.tailg.run.intelligence.model.splash.activity.SplashActivity";

    private volatile boolean hooked;

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }
        if (hooked) {
            return;
        }

        try {
            ClassLoader classLoader = param.getClassLoader();
            Class<?> splashClazz = Class.forName(SPLASH_ACTIVITY, false, classLoader);

            Method setupView = splashClazz.getDeclaredMethod("setupView");
            Method setupViewNo = splashClazz.getDeclaredMethod("setupViewNo");
            setupViewNo.setAccessible(true);

            hook(setupView)
                    .setPriority(PRIORITY_HIGHEST)
                    .intercept(chain -> {
                        try {
                            setupViewNo.invoke(chain.getThisObject());
                        } catch (Throwable invokeError) {
                            log(Log.ERROR, TAG, "invoke setupViewNo failed", invokeError);
                        }
                        // setupView is void, return null to short-circuit original call.
                        return null;
                    });

            hooked = true;
            log(Log.INFO, TAG, "Hook installed for " + TARGET_PACKAGE);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Install hook failed", t);
        }
    }
}
