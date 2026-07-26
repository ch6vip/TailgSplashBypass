package com.tailg.lsposed.adblock;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

final class FastStartupController {
    private static final String TAG = "TailgFastStartup";
    private static final String QB_SDK = "com.tencent.smtt.sdk.QbSdk";
    private static final String TBS_CORE_SETTINGS =
            "com.tencent.smtt.export.external.TbsCoreSettings";
    private static final String UNICORN = "com.qiyukf.unicorn.api.Unicorn";

    private static final AtomicBoolean TBS_CONFIGURED = new AtomicBoolean(false);
    private static final AtomicBoolean UNICORN_INITIALIZED = new AtomicBoolean(false);
    private static final ThreadLocal<Boolean> ALLOW_UNICORN_INITIALIZATION =
            ThreadLocal.withInitial(() -> false);

    private FastStartupController() {
    }

    static void ensureTbsConfigured(ClassLoader classLoader) {
        if (TBS_CONFIGURED.get()) {
            return;
        }
        synchronized (TBS_CONFIGURED) {
            if (TBS_CONFIGURED.get()) {
                return;
            }
            try {
                Class<?> qbSdk = Class.forName(QB_SDK, false, classLoader);
                Class<?> settings = Class.forName(TBS_CORE_SETTINGS, false, classLoader);
                Map<String, Object> values = new HashMap<>();
                values.put(
                        readStringField(settings, "TBS_SETTINGS_USE_SPEEDY_CLASSLOADER"),
                        true
                );
                values.put(
                        readStringField(settings, "TBS_SETTINGS_USE_DEXLOADER_SERVICE"),
                        true
                );
                Method initSettings = qbSdk.getDeclaredMethod("initTbsSettings", Map.class);
                Method setWifiPolicy = qbSdk.getDeclaredMethod(
                        "setDownloadWithoutWifi",
                        boolean.class
                );
                initSettings.invoke(null, values);
                setWifiPolicy.invoke(null, false);
                TBS_CONFIGURED.set(true);
            } catch (Throwable error) {
                Log.w(TAG, "Lazy X5 configuration failed", unwrap(error));
            }
        }
    }

    static boolean isUnicornInitializationAllowed() {
        return UNICORN_INITIALIZED.get()
                || Boolean.TRUE.equals(ALLOW_UNICORN_INITIALIZATION.get());
    }

    static void ensureUnicornInitialized(ClassLoader classLoader) {
        if (UNICORN_INITIALIZED.get()) {
            return;
        }
        synchronized (UNICORN_INITIALIZED) {
            if (UNICORN_INITIALIZED.get()) {
                return;
            }
            try {
                Class<?> unicorn = Class.forName(UNICORN, false, classLoader);
                Method initSdk = unicorn.getDeclaredMethod("initSdk");
                ALLOW_UNICORN_INITIALIZATION.set(true);
                Object result = initSdk.invoke(null);
                if (!(result instanceof Boolean) || (Boolean) result) {
                    UNICORN_INITIALIZED.set(true);
                }
            } catch (Throwable error) {
                Log.w(TAG, "Lazy customer-service initialization failed", unwrap(error));
            } finally {
                ALLOW_UNICORN_INITIALIZATION.remove();
            }
        }
    }

    private static String readStringField(Class<?> type, String name) throws Exception {
        Field field = type.getField(name);
        Object value = field.get(null);
        if (!(value instanceof String) || ((String) value).isEmpty()) {
            throw new IllegalStateException("Invalid X5 setting field: " + name);
        }
        return (String) value;
    }

    private static Throwable unwrap(Throwable error) {
        Throwable cause = error.getCause();
        return cause == null ? error : cause;
    }
}
