package com.tailg.lsposed.adblock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

final class HomeEnhancementController {
    private static final String TAG = "TailgHomeEnhancement";
    private static final String PREFS_UTIL =
            "com.tailg.run.intelligence.model.util.PrefsUtil";

    private HomeEnhancementController() {
    }

    static void apply(
            Activity activity,
            ClassLoader targetClassLoader,
            boolean simplifyNavigation,
            boolean enableDiagnostics,
            boolean proximityOverrideEnabled,
            float unlockMeters,
            float lockMeters
    ) {
        try {
            Object binding = ReflectionAccess.getField(activity, "mBinding");
            if (simplifyNavigation) {
                hideBindingView(binding, "rbCircle");
                hideBindingView(binding, "rbShop");
            }
            if (enableDiagnostics) {
                Object controlObject = ReflectionAccess.getField(binding, "rbControl");
                if (controlObject instanceof View controlView) {
                    controlView.setOnLongClickListener(view -> {
                        showDiagnostics(
                                activity,
                                targetClassLoader,
                                proximityOverrideEnabled,
                                unlockMeters,
                                lockMeters
                        );
                        return true;
                    });
                }
            }
        } catch (Throwable error) {
            Log.w(TAG, "Apply home enhancements failed", error);
        }
    }

    private static void hideBindingView(Object binding, String fieldName)
            throws ReflectiveOperationException {
        Object value = ReflectionAccess.getField(binding, fieldName);
        if (value instanceof View view) {
            view.setVisibility(View.GONE);
        }
    }

    private static void showDiagnostics(
            Activity activity,
            ClassLoader targetClassLoader,
            boolean proximityOverrideEnabled,
            float unlockMeters,
            float lockMeters
    ) {
        try {
            Object car = readCarControlInfo(targetClassLoader);
            if (car == null) {
                Toast.makeText(activity, "当前没有已绑定车辆数据", Toast.LENGTH_SHORT).show();
                return;
            }
            String report = buildReport(
                    activity,
                    car,
                    proximityOverrideEnabled,
                    unlockMeters,
                    lockMeters
            );
            TextView text = new TextView(activity);
            int padding = dp(activity, 24);
            text.setPadding(padding, dp(activity, 12), padding, padding);
            text.setText(report);
            text.setTextSize(14.0f);
            text.setTextIsSelectable(true);
            text.setTypeface(Typeface.MONOSPACE);

            ScrollView scroll = new ScrollView(activity);
            scroll.addView(text);
            new AlertDialog.Builder(activity)
                    .setTitle("车辆能力诊断")
                    .setView(scroll)
                    .setNegativeButton("关闭", null)
                    .setPositiveButton("复制", (dialog, which) -> copyReport(activity, report))
                    .show();
        } catch (Throwable error) {
            Log.w(TAG, "Show diagnostics failed", error);
            Toast.makeText(activity, "车辆诊断数据读取失败", Toast.LENGTH_SHORT).show();
        }
    }

    private static Object readCarControlInfo(ClassLoader targetClassLoader) throws Exception {
        Class<?> prefsUtil = Class.forName(PREFS_UTIL, false, targetClassLoader);
        Method getter = prefsUtil.getDeclaredMethod("getCarControlInfo");
        getter.setAccessible(true);
        return getter.invoke(null);
    }

    private static String buildReport(
            Activity activity,
            Object car,
            boolean proximityOverrideEnabled,
            float unlockMeters,
            float lockMeters
    ) {
        StringBuilder out = new StringBuilder(1024);
        out.append("车辆\n");
        append(out, "名称", read(car, "getCarName"));
        append(out, "类型", read(car, "getCarType"));
        append(out, "车型编号", read(car, "getModelType"));
        append(out, "设备类型", read(car, "getDeviceType"));

        out.append("\n状态\n");
        append(out, "在线", read(car, "getOnline"));
        append(out, "设防", read(car, "getDefenceStatus"));
        append(out, "电量", withUnit(read(car, "getElectricQuantity"), "%"));
        append(out, "电压", withUnit(read(car, "getVoltage"), " V"));
        append(out, "里程", withUnit(read(car, "getMileage"), " km"));
        append(out, "GPS 最后上报", read(car, "getGpsReportTime"));
        append(out, "BLE 名称", mask(read(car, "getBtname")));

        out.append("\n能力\n");
        appendCapability(out, "GPS", read(car, "getIsGps"));
        appendCapability(out, "NFC", read(car, "getIsNfc"));
        appendCapability(out, "摄像头", read(car, "getIsCamera"));
        appendCapability(out, "车载屏幕", read(car, "getIsScreen"));
        appendCapability(out, "坐垫感应", read(car, "getIsCushionInduction"));
        appendCapability(out, "坐垫锁", read(car, "getIsCushionLock"));
        appendCapability(out, "边撑感应", read(car, "getIsEnhancedKickstand"));
        appendCapability(out, "坡道驻车", read(car, "getIsHillParking"));
        appendCapability(out, "防溜坡", read(car, "getIsSlipSlope"));
        appendCapability(out, "ThinkRide", read(car, "getIsThinkRide"));
        appendCapability(out, "ThinkRide BLE", read(car, "getIsThinkRideBLE"));
        appendCapability(out, "皮肤切换", read(car, "getIsSkinChange"));
        Object tirePressure = readTirePressureCapability(activity);
        append(out, "胎压快捷项（当前会话）", Boolean.TRUE.equals(tirePressure)
                ? "支持"
                : "未检测到或不支持");

        out.append("\n感应距离\n");
        append(out, "官方解锁阈值", withUnit(readField(car, "minRssiDistance"), " m"));
        append(out, "官方落锁阈值", withUnit(readField(car, "maxRssiDistance"), " m"));
        append(out, "模块覆盖", proximityOverrideEnabled ? "已启用" : "未启用");
        if (proximityOverrideEnabled) {
            append(out, "生效解锁阈值", String.format(Locale.US, "%.1f m", unlockMeters));
            append(out, "生效落锁阈值", String.format(Locale.US, "%.1f m", lockMeters));
        }
        append(out, "RSSI A", read(car, "getRssiA"));
        append(out, "RSSI Factor", read(car, "getRssiFactor"));

        out.append("\n敏感标识符和连接凭据已隐藏。");
        return out.toString();
    }

    private static Object readTirePressureCapability(Activity activity) {
        try {
            Object fragmentsObject = ReflectionAccess.getField(activity, "mFragments");
            if (fragmentsObject instanceof List<?> fragments) {
                for (Object fragment : fragments) {
                    if (fragment != null && "ControlFragment".equals(
                            fragment.getClass().getSimpleName()
                    )) {
                        return ReflectionAccess.getField(fragment, "isSupportTpsFun");
                    }
                }
            }
        } catch (Throwable ignored) {
            // Capability can be unavailable before the BLE ability response arrives.
        }
        return null;
    }

    private static Object read(Object target, String getter) {
        try {
            return ReflectionAccess.invokeNoArg(target, getter);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object readField(Object target, String field) {
        try {
            return ReflectionAccess.getField(target, field);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void append(StringBuilder out, String label, Object value) {
        String text = value == null || String.valueOf(value).trim().isEmpty()
                ? "未知"
                : String.valueOf(value);
        out.append(label).append(": ").append(text).append('\n');
    }

    private static void appendCapability(StringBuilder out, String label, Object value) {
        String text;
        if (value instanceof Boolean bool) {
            text = bool ? "支持" : "不支持";
        } else if (value instanceof Number number) {
            text = number.intValue() == 1 ? "支持" : "不支持";
        } else if ("1".equals(String.valueOf(value))) {
            text = "支持";
        } else if ("0".equals(String.valueOf(value))) {
            text = "不支持";
        } else {
            text = "未知";
        }
        append(out, label, text);
    }

    private static Object withUnit(Object value, String unit) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        return String.valueOf(value) + unit;
    }

    private static String mask(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        if (text.length() <= 4) {
            return "***";
        }
        return text.substring(0, 2) + "***" + text.substring(text.length() - 2);
    }

    private static void copyReport(Context context, String report) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(
                Context.CLIPBOARD_SERVICE
        );
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("车辆能力诊断", report));
            Toast.makeText(context, "诊断信息已复制", Toast.LENGTH_SHORT).show();
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
