package com.tailg.lsposed.adblock;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import androidx.annotation.Nullable;

import io.github.libxposed.service.XposedService;

public final class ConfigBridgeService extends Service
        implements ModuleApplication.ServiceStateListener {
    static final String MODULE_PACKAGE = "com.tailg.lsposed.adblock";
    static final String SERVICE_CLASS = MODULE_PACKAGE + ".ConfigBridgeService";

    private static final String TAG = "TailgConfigBridge";
    private static final String TARGET_PACKAGE = "com.tailg.run.intelligence";

    private volatile @Nullable SharedPreferences preferences;

    private final IConfigBridge.Stub binder = new IConfigBridge.Stub() {
        @Override
        public Bundle getSnapshot() {
            enforceTrustedCaller();
            long identity = Binder.clearCallingIdentity();
            try {
                SharedPreferences current = preferences;
                if (current == null) {
                    return null;
                }
                Bundle snapshot = new Bundle();
                for (String key : ConfigKeys.BOOLEAN_KEYS) {
                    snapshot.putBoolean(key, current.getBoolean(
                            key,
                            ConfigKeys.defaultBooleanFor(key)
                    ));
                }
                ProximityPolicy.Distances distances = ProximityPolicy.normalize(
                        current.getFloat(
                                ConfigKeys.KEY_PROXIMITY_UNLOCK_METERS,
                                ConfigKeys.DEFAULT_PROXIMITY_UNLOCK_METERS
                        ),
                        current.getFloat(
                                ConfigKeys.KEY_PROXIMITY_LOCK_METERS,
                                ConfigKeys.DEFAULT_PROXIMITY_LOCK_METERS
                        )
                );
                snapshot.putFloat(
                        ConfigKeys.KEY_PROXIMITY_UNLOCK_METERS,
                        distances.unlockMeters
                );
                snapshot.putFloat(
                        ConfigKeys.KEY_PROXIMITY_LOCK_METERS,
                        distances.lockMeters
                );
                return snapshot;
            } catch (RuntimeException error) {
                Log.w(TAG, "Read remote settings failed", error);
                return null;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public boolean putBoolean(String key, boolean value) {
            enforceTrustedCaller();
            if (!ConfigKeys.isBooleanKey(key)) {
                throw new IllegalArgumentException("Unsupported boolean config key: " + key);
            }
            long identity = Binder.clearCallingIdentity();
            try {
                SharedPreferences current = preferences;
                if (current == null) {
                    return false;
                }
                return current.edit().putBoolean(key, value).commit();
            } catch (RuntimeException error) {
                Log.w(TAG, "Write boolean setting failed: " + key, error);
                return false;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public boolean putDistances(float unlockMeters, float lockMeters) {
            enforceTrustedCaller();
            long identity = Binder.clearCallingIdentity();
            try {
                SharedPreferences current = preferences;
                if (current == null) {
                    return false;
                }
                ProximityPolicy.Distances distances = ProximityPolicy.normalize(
                        unlockMeters,
                        lockMeters
                );
                return current.edit()
                        .putFloat(
                                ConfigKeys.KEY_PROXIMITY_UNLOCK_METERS,
                                distances.unlockMeters
                        )
                        .putFloat(
                                ConfigKeys.KEY_PROXIMITY_LOCK_METERS,
                                distances.lockMeters
                        )
                        .commit();
            } catch (RuntimeException error) {
                Log.w(TAG, "Write proximity settings failed", error);
                return false;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        ModuleApplication.addServiceStateListener(this, true);
    }

    @Override
    public void onDestroy() {
        ModuleApplication.removeServiceStateListener(this);
        preferences = null;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onServiceStateChanged(@Nullable XposedService service) {
        if (service == null) {
            preferences = null;
            return;
        }
        try {
            preferences = service.getRemotePreferences(ConfigKeys.PREFS_NAME);
        } catch (RuntimeException error) {
            preferences = null;
            Log.w(TAG, "Connect remote settings failed", error);
        }
    }

    private void enforceTrustedCaller() {
        int callingUid = Binder.getCallingUid();
        if (callingUid == Process.myUid()) {
            return;
        }
        PackageManager packageManager = getPackageManager();
        String[] packages = packageManager.getPackagesForUid(callingUid);
        if (packages != null) {
            for (String packageName : packages) {
                if (TARGET_PACKAGE.equals(packageName)) {
                    return;
                }
            }
        }
        throw new SecurityException("Caller is not the supported Tailg app");
    }
}
