package com.tailg.lsposed.adblock;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

final class OfficialSettingsController {
    private static final String TAG = "TailgSettingsEntry";
    private static final String MODULE_PACKAGE = "com.tailg.lsposed.adblock";
    private static final String MODULE_SETTINGS_ACTIVITY = MODULE_PACKAGE + ".MainActivity";

    private OfficialSettingsController() {
    }

    static void installRevisionEntry(Object fragment) {
        try {
            Object activityObject = ReflectionAccess.invokeNoArg(fragment, "getActivity");
            if (!(activityObject instanceof Activity activity)) {
                return;
            }
            Object binding = ReflectionAccess.getField(fragment, "mBinding");
            Object rowObject = ReflectionAccess.getField(binding, "tvLanguage");
            if (!(rowObject instanceof TextView row)) {
                return;
            }
            configureEntry(activity, row);
            row.post(() -> configureEntry(activity, row));
        } catch (Throwable error) {
            Log.w(TAG, "Install revised settings entry failed", error);
        }
    }

    static void installLegacyEntry(Activity activity) {
        try {
            Object binding = ReflectionAccess.getField(activity, "mBinding");
            Object rowObject = ReflectionAccess.getField(binding, "tvSystemInformation");
            if (!(rowObject instanceof TextView row)) {
                return;
            }
            Object dividerObject = ReflectionAccess.getField(binding, "vL11");
            if (dividerObject instanceof View divider) {
                divider.setVisibility(View.VISIBLE);
            }
            configureEntry(activity, row);
        } catch (Throwable error) {
            Log.w(TAG, "Install legacy settings entry failed", error);
        }
    }

    private static void configureEntry(Activity activity, TextView row) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        row.setText("Tailg 工具箱");
        row.setContentDescription("打开 Tailg LSPosed 工具箱");
        row.setVisibility(View.VISIBLE);
        row.setOnClickListener(view -> openModuleSettings(activity));
    }

    private static void openModuleSettings(Activity activity) {
        try {
            Intent intent = new Intent()
                    .setClassName(MODULE_PACKAGE, MODULE_SETTINGS_ACTIVITY);
            activity.startActivity(intent);
        } catch (RuntimeException error) {
            Log.w(TAG, "Open module settings failed", error);
            Toast.makeText(activity, "无法打开 Tailg 工具箱，请确认模块已安装", Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
