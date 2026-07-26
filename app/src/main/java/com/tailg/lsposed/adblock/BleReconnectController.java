package com.tailg.lsposed.adblock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressLint("MissingPermission")
final class BleReconnectController {
    private static final String TAG = "TailgBleReconnect";
    private static final String BLE_HANDLER = "com.kenlib.ble_new.util.BleHandler";
    private static final Object LOCK = new Object();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean RECEIVER_REGISTERED = new AtomicBoolean(false);

    private static volatile WeakReference<Activity> homeReference = new WeakReference<>(null);
    private static ClassLoader hostClassLoader;
    private static int intervalSeconds = ConfigKeys.DEFAULT_BLE_RECONNECT_INTERVAL_SECONDS;
    private static int maxAttempts = ConfigKeys.DEFAULT_BLE_RECONNECT_MAX_ATTEMPTS;
    private static int generation;
    private static int cycle;
    private static boolean verboseLog;

    private static final BroadcastReceiver BLUETOOTH_STATE_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())
                    || intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    != BluetoothAdapter.STATE_ON) {
                return;
            }
            Activity home = homeReference.get();
            if (isUsable(home)) {
                startRecovery(home, "bluetooth-on");
            }
        }
    };

    private BleReconnectController() {
    }

    static void onHomeResumed(
            Activity home,
            ClassLoader classLoader,
            int configuredIntervalSeconds,
            int configuredMaxAttempts,
            boolean configuredVerboseLog
    ) {
        if (!isUsable(home)) {
            return;
        }
        synchronized (LOCK) {
            homeReference = new WeakReference<>(home);
            hostClassLoader = classLoader;
            intervalSeconds = BleReconnectPolicy.normalizeIntervalSeconds(
                    configuredIntervalSeconds
            );
            maxAttempts = BleReconnectPolicy.normalizeMaxAttempts(configuredMaxAttempts);
            verboseLog = configuredVerboseLog;
        }
        registerBluetoothReceiver(home.getApplicationContext());
        startRecovery(home, "home-resume");
    }

    private static void registerBluetoothReceiver(Context context) {
        if (context == null || !RECEIVER_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        try {
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                        BLUETOOTH_STATE_RECEIVER,
                        filter,
                        Context.RECEIVER_EXPORTED
                );
            } else {
                context.registerReceiver(BLUETOOTH_STATE_RECEIVER, filter);
            }
        } catch (Throwable error) {
            RECEIVER_REGISTERED.set(false);
            Log.w(TAG, "Register Bluetooth state receiver failed", error);
        }
    }

    private static void startRecovery(Activity home, String reason) {
        MAIN_HANDLER.post(() -> {
            int token;
            synchronized (LOCK) {
                if (!isUsable(home) || homeReference.get() != home) {
                    return;
                }
                generation++;
                cycle = 0;
                token = generation;
                if (verboseLog) {
                    Log.i(TAG, "Start bounded recovery: " + reason);
                }
            }
            runCycle(token);
        });
    }

    private static void runCycle(int token) {
        Activity home;
        ClassLoader classLoader;
        int currentCycle;
        int configuredMaxAttempts;
        int configuredIntervalSeconds;
        synchronized (LOCK) {
            if (token != generation) {
                return;
            }
            home = homeReference.get();
            classLoader = hostClassLoader;
            configuredMaxAttempts = maxAttempts;
            configuredIntervalSeconds = intervalSeconds;
            currentCycle = ++cycle;
        }
        if (!isUsable(home) || classLoader == null || !hasRequiredPermissions(home)) {
            return;
        }

        BluetoothAdapter adapter;
        try {
            adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || !adapter.isEnabled()) {
                return;
            }
        } catch (SecurityException denied) {
            return;
        }

        try {
            Object fragment = findControlFragment(home);
            CarSnapshot car = readCarSnapshot(fragment);
            if (fragment != null && car != null) {
                ConnectionState state = connectionState(
                        home,
                        fragment,
                        classLoader,
                        car.modelType
                );
                if (state == ConnectionState.CONNECTED) {
                    return;
                }
                if (state == ConnectionState.RETRYABLE) {
                    reconnect(fragment, classLoader, car);
                }
            }
        } catch (Throwable error) {
            Log.w(TAG, "Official BLE recovery attempt failed", unwrap(error));
        }

        if (currentCycle < configuredMaxAttempts) {
            MAIN_HANDLER.postDelayed(
                    () -> runCycle(token),
                    configuredIntervalSeconds * 1000L
            );
        } else if (verboseLog) {
            Log.i(TAG, "Bounded recovery finished after " + currentCycle + " cycles");
        }
    }

    private static Object findControlFragment(Activity home) throws Exception {
        Object value = ReflectionAccess.getField(home, "mFragments");
        if (!(value instanceof List<?> fragments)) {
            return null;
        }
        for (Object fragment : fragments) {
            if (fragment != null && "ControlFragment".equals(
                    fragment.getClass().getSimpleName()
            )) {
                return fragment;
            }
        }
        return null;
    }

    private static CarSnapshot readCarSnapshot(Object fragment) throws Exception {
        if (fragment == null) {
            return null;
        }
        Object homeViewModel = ReflectionAccess.getField(fragment, "mHomeViewModel");
        Object carField = ReflectionAccess.getField(homeViewModel, "carControlInfoBeanField");
        Object car = ReflectionAccess.invokeNoArg(carField, "get");
        if (car == null) {
            return null;
        }
        Object typeValue = ReflectionAccess.invokeNoArg(car, "getModelType");
        int modelType;
        try {
            modelType = typeValue instanceof Number number
                    ? number.intValue()
                    : Integer.parseInt(String.valueOf(typeValue));
        } catch (NumberFormatException invalidType) {
            return null;
        }
        if (modelType <= 0) {
            return null;
        }
        Object nameValue = ReflectionAccess.invokeNoArg(car, "getBtname");
        String bluetoothName = nameValue == null ? null : String.valueOf(nameValue).trim();
        return new CarSnapshot(modelType, bluetoothName);
    }

    private static ConnectionState connectionState(
            Context context,
            Object fragment,
            ClassLoader classLoader,
            int modelType
    ) throws Exception {
        if (modelType == 1 || modelType == 2) {
            return legacyConnectionState(context, classLoader);
        }
        Object viewModel = ReflectionAccess.getField(fragment, "mViewModel");
        String fieldName = modelType == 8 || modelType == 283
                ? "bleConnectStatusQgj"
                : "bleConnectStatus";
        Object observable = ReflectionAccess.getField(viewModel, fieldName);
        Object status = ReflectionAccess.invokeNoArg(observable, "get");
        if (status == null) {
            return ConnectionState.WAITING;
        }
        String name = status instanceof Enum<?> enumValue
                ? enumValue.name()
                : String.valueOf(status);
        if ("LOGIN".equals(name)) {
            return ConnectionState.CONNECTED;
        }
        if ("CONNECTING".equals(name)
                || "CONNECTED".equals(name)
                || "READY".equals(name)
                || "DISCONNECTING".equals(name)) {
            return ConnectionState.WAITING;
        }
        return ConnectionState.RETRYABLE;
    }

    private static ConnectionState legacyConnectionState(
            Context context,
            ClassLoader classLoader
    ) throws Exception {
        Object handler = getLegacyBleHandler(classLoader);
        Object value = ReflectionAccess.getField(handler, "mBluetoothGatt");
        if (!(value instanceof BluetoothGatt gatt)) {
            return ConnectionState.RETRYABLE;
        }
        BluetoothDevice device = gatt.getDevice();
        BluetoothManager manager = (BluetoothManager) context.getSystemService(
                Context.BLUETOOTH_SERVICE
        );
        if (device == null || manager == null) {
            return ConnectionState.WAITING;
        }
        int state = manager.getConnectionState(device, BluetoothProfile.GATT);
        if (state == BluetoothProfile.STATE_CONNECTED) {
            return ConnectionState.CONNECTED;
        }
        if (state == BluetoothProfile.STATE_CONNECTING
                || state == BluetoothProfile.STATE_DISCONNECTING) {
            return ConnectionState.WAITING;
        }
        return ConnectionState.RETRYABLE;
    }

    private static void reconnect(
            Object fragment,
            ClassLoader classLoader,
            CarSnapshot car
    ) throws Exception {
        if (car.modelType == 1 || car.modelType == 2) {
            if (car.bluetoothName == null || car.bluetoothName.isEmpty()) {
                return;
            }
            Object handler = getLegacyBleHandler(classLoader);
            Method reconnect = handler.getClass().getDeclaredMethod(
                    "getMatchBleDeviceAndConn",
                    String.class
            );
            reconnect.invoke(handler, car.bluetoothName);
        } else if (car.modelType == 8 || car.modelType == 283) {
            ReflectionAccess.invokeNoArg(fragment, "initBleTLinkQgj");
        } else {
            ReflectionAccess.invokeNoArg(fragment, "initBleTLink");
        }
    }

    private static Object getLegacyBleHandler(ClassLoader classLoader) throws Exception {
        Class<?> handlerClass = Class.forName(BLE_HANDLER, false, classLoader);
        Method getInstance = handlerClass.getDeclaredMethod("getInstance");
        return getInstance.invoke(null);
    }

    private static boolean hasRequiredPermissions(Context context) {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean isUsable(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    private static Throwable unwrap(Throwable error) {
        Throwable cause = error.getCause();
        return cause == null ? error : cause;
    }

    private enum ConnectionState {
        CONNECTED,
        WAITING,
        RETRYABLE
    }

    private static final class CarSnapshot {
        final int modelType;
        final String bluetoothName;

        CarSnapshot(int modelType, String bluetoothName) {
            this.modelType = modelType;
            this.bluetoothName = bluetoothName;
        }
    }
}
