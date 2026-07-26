package com.tailg.lsposed.adblock;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public final class ModuleApplication extends Application implements XposedServiceHelper.OnServiceListener {
    interface ServiceStateListener {
        void onServiceStateChanged(@Nullable XposedService service);
    }

    private static final Set<ServiceStateListener> LISTENERS = new CopyOnWriteArraySet<>();
    private static volatile @Nullable XposedService service;

    static void addServiceStateListener(ServiceStateListener listener, boolean notifyImmediately) {
        LISTENERS.add(listener);
        if (notifyImmediately) {
            listener.onServiceStateChanged(service);
        }
    }

    static void removeServiceStateListener(ServiceStateListener listener) {
        LISTENERS.remove(listener);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        XposedServiceHelper.registerListener(this);
    }

    @Override
    public void onServiceBind(@NonNull XposedService boundService) {
        service = boundService;
        notifyServiceStateChanged(boundService);
    }

    @Override
    public void onServiceDied(@NonNull XposedService deadService) {
        if (service == deadService) {
            service = null;
            notifyServiceStateChanged(null);
        }
    }

    private static void notifyServiceStateChanged(@Nullable XposedService currentService) {
        for (ServiceStateListener listener : LISTENERS) {
            listener.onServiceStateChanged(currentService);
        }
    }
}
