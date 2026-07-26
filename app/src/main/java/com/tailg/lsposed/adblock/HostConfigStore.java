package com.tailg.lsposed.adblock;

import android.content.Context;
import android.content.SharedPreferences;

import com.tencent.mmkv.MMKV;

import java.io.File;

/** Stores module settings in the host app so injected UI and hooks share one source. */
final class HostConfigStore {
    static final String MIGRATION_MARKER = "_host_mmkv_migrated_v1";

    private static final Object LOCK = new Object();
    private static volatile MMKV preferences;

    private HostConfigStore() {
    }

    static MMKV open(Context context) {
        MMKV current = preferences;
        if (current != null) {
            return current;
        }
        synchronized (LOCK) {
            current = preferences;
            if (current != null) {
                return current;
            }

            File rootDirectory = new File(context.getFilesDir(), "mmkv");
            if (!rootDirectory.isDirectory()
                    && !rootDirectory.mkdirs()
                    && !rootDirectory.isDirectory()) {
                throw new IllegalStateException(
                        "Cannot create host MMKV directory: " + rootDirectory
                );
            }
            Context applicationContext = context.getApplicationContext();
            MMKV.initialize(
                    applicationContext == null ? context : applicationContext,
                    rootDirectory.getAbsolutePath()
            );
            MMKV mmkv = MMKV.mmkvWithID(ConfigKeys.PREFS_NAME, MMKV.MULTI_PROCESS_MODE);
            preferences = mmkv;
            return mmkv;
        }
    }

    static void migrateFrom(MMKV target, SharedPreferences legacy) {
        synchronized (LOCK) {
            if (target.getBoolean(MIGRATION_MARKER, false)) {
                return;
            }

            for (String key : ConfigKeys.BOOLEAN_KEYS) {
                if (legacy.contains(key)) {
                    ensureEncoded(target.encode(
                            key,
                            legacy.getBoolean(key, ConfigKeys.defaultBooleanFor(key))
                    ), key);
                }
            }
            copyFloatIfPresent(
                    legacy,
                    target,
                    ConfigKeys.KEY_PROXIMITY_UNLOCK_METERS,
                    ConfigKeys.DEFAULT_PROXIMITY_UNLOCK_METERS
            );
            copyFloatIfPresent(
                    legacy,
                    target,
                    ConfigKeys.KEY_PROXIMITY_LOCK_METERS,
                    ConfigKeys.DEFAULT_PROXIMITY_LOCK_METERS
            );
            ensureEncoded(target.encode(MIGRATION_MARKER, true), MIGRATION_MARKER);
        }
    }

    private static void copyFloatIfPresent(
            SharedPreferences source,
            MMKV target,
            String key,
            float defaultValue
    ) {
        if (source.contains(key)) {
            ensureEncoded(target.encode(key, source.getFloat(key, defaultValue)), key);
        }
    }

    private static void ensureEncoded(boolean encoded, String key) {
        if (!encoded) {
            throw new IllegalStateException("Cannot persist host setting: " + key);
        }
    }
}
