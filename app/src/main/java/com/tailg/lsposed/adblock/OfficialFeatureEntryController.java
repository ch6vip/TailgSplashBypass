package com.tailg.lsposed.adblock;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.lang.reflect.Method;

final class OfficialFeatureEntryController {
    private static final String TAG = "TailgFeatureEntry";
    private static final String BATTERY_ACTIVITY =
            "com.thinkerride.tbox.centercontrol.battery.BatteryChartActivity";
    private static final String GROUP_LIST =
            "com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView";
    private static final String GROUP_LIST_ITEM =
            "com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView";
    private static final String BATTERY_TAG =
            "com.tailg.lsposed.adblock:battery_dynamics_entry";
    private static final String CUSTOM_SOUND_TAG =
            "com.tailg.lsposed.adblock:custom_sound_entry";

    private OfficialFeatureEntryController() {
    }

    static void addTBoxEntries(
            Object target,
            ClassLoader classLoader,
            boolean batteryDynamicsEnabled,
            boolean customSoundEnabled
    ) {
        if (!(target instanceof Activity activity)
                || activity.isFinishing()
                || activity.isDestroyed()) {
            return;
        }

        boolean addBattery = shouldAddBattery(target, batteryDynamicsEnabled);
        boolean addCustomSound = shouldAddCustomSound(target, customSoundEnabled);
        if (!addBattery && !addCustomSound) {
            return;
        }

        try {
            Object groupObject = ReflectionAccess.getField(target, "n");
            if (!(groupObject instanceof ViewGroup group)) {
                return;
            }
            if (addBattery && group.findViewWithTag(BATTERY_TAG) != null) {
                addBattery = false;
            }
            if (addCustomSound && group.findViewWithTag(CUSTOM_SOUND_TAG) != null) {
                addCustomSound = false;
            }
            if (!addBattery && !addCustomSound) {
                return;
            }

            Class<?> groupClass = Class.forName(GROUP_LIST, false, classLoader);
            Class<?> itemClass = Class.forName(GROUP_LIST_ITEM, false, classLoader);
            if (!groupClass.isInstance(groupObject)) {
                return;
            }

            Method newSection = groupClass.getDeclaredMethod("newSection", Context.class);
            Object section = newSection.invoke(null, activity);
            Method setTitle = section.getClass().getMethod("setTitle", CharSequence.class);
            Method addItem = section.getClass().getMethod(
                    "addItemView",
                    itemClass,
                    View.OnClickListener.class
            );
            Method addTo = section.getClass().getMethod("addTo", groupClass);
            Method createItem = target.getClass().getDeclaredMethod("a", String.class);
            createItem.setAccessible(true);

            setTitle.invoke(section, "Tailg 工具箱");
            if (addBattery) {
                View row = createRow(
                        target,
                        createItem,
                        resolveString(activity, "battery_dynamics", "电池动态"),
                        BATTERY_TAG
                );
                addItem.invoke(
                        section,
                        row,
                        (View.OnClickListener) view -> openBatteryDynamics(activity, classLoader)
                );
            }
            if (addCustomSound) {
                View row = createRow(
                        target,
                        createItem,
                        resolveString(activity, "sound_effect_setting_custom", "自定义音效"),
                        CUSTOM_SOUND_TAG
                );
                addItem.invoke(
                        section,
                        row,
                        (View.OnClickListener) view -> openCustomSound(target, activity)
                );
            }
            addTo.invoke(section, groupObject);
        } catch (Throwable error) {
            Log.w(TAG, "Add TBox feature entries failed", error);
        }
    }

    private static boolean shouldAddBattery(Object target, boolean enabled) {
        if (!enabled) {
            return false;
        }
        try {
            boolean isUseCar = Boolean.TRUE.equals(ReflectionAccess.getField(target, "C"));
            Object deviceFunction = ReflectionAccess.getField(target, "z");
            boolean alreadySupported = Boolean.TRUE.equals(
                    ReflectionAccess.invokeNoArg(deviceFunction, "isIntelligentControl")
            );
            return OfficialFeatureEntryPolicy.shouldAddBatteryDynamics(
                    true,
                    isUseCar,
                    alreadySupported
            );
        } catch (Throwable error) {
            Log.w(TAG, "Resolve battery dynamics entry policy failed", error);
            return false;
        }
    }

    private static boolean shouldAddCustomSound(Object target, boolean enabled) {
        if (!enabled) {
            return false;
        }
        try {
            Object deviceFunction = ReflectionAccess.getField(target, "z");
            boolean isBound = Boolean.TRUE.equals(ReflectionAccess.invokeNoArg(target, "m"));
            boolean customSoundSupported = Boolean.TRUE.equals(
                    ReflectionAccess.invokeNoArg(deviceFunction, "isSupportBoxVoiceCust")
            );
            Object tboxVoice = ReflectionAccess.invokeNoArg(deviceFunction, "getTboxVoice");
            boolean entryAlreadyVisible = tboxVoice instanceof Number
                    && ((Number) tboxVoice).intValue() == 1;
            return OfficialFeatureEntryPolicy.shouldAddCustomSound(
                    true,
                    isBound,
                    customSoundSupported,
                    entryAlreadyVisible
            );
        } catch (Throwable error) {
            Log.w(TAG, "Resolve custom sound entry policy failed", error);
            return false;
        }
    }

    private static View createRow(
            Object target,
            Method createItem,
            String title,
            String tag
    ) throws ReflectiveOperationException {
        Object rowObject = createItem.invoke(target, title);
        if (!(rowObject instanceof View row)) {
            throw new IllegalStateException("TBox item factory returned a non-View");
        }
        row.setTag(tag);
        return row;
    }

    private static void openBatteryDynamics(Activity activity, ClassLoader classLoader) {
        try {
            Class<?> activityClass = Class.forName(BATTERY_ACTIVITY, false, classLoader);
            Method start = activityClass.getDeclaredMethod("start", Context.class);
            start.invoke(null, activity);
        } catch (Throwable error) {
            showOpenError(activity, "电池动态", error);
        }
    }

    private static void openCustomSound(Object target, Activity activity) {
        try {
            // Reuse the official permission and custom-audio checks in TBoxSetActivity#r().
            ReflectionAccess.invokeNoArg(target, "r");
        } catch (Throwable error) {
            showOpenError(activity, "自定义音效", error);
        }
    }

    private static String resolveString(Activity activity, String name, String fallback) {
        int identifier = activity.getResources().getIdentifier(
                name,
                "string",
                activity.getPackageName()
        );
        return identifier == 0 ? fallback : activity.getString(identifier);
    }

    private static void showOpenError(Activity activity, String page, Throwable error) {
        Log.w(TAG, "Open " + page + " failed", error);
        Toast.makeText(activity, page + "页面打开失败", Toast.LENGTH_SHORT).show();
    }
}
