package com.tailg.lsposed.adblock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

final class BatteryInfoShortcutController {
    private static final String TAG = "TailgBatteryInfo";
    private static final String PREFS_UTIL =
            "com.tailg.run.intelligence.model.util.PrefsUtil";
    private static final String CONTROL_TYPE_UTIL =
            "com.tailg.run.intelligence.model.home.util.ControlTypeUtil";
    private static final String ACTIVITY_UTILS =
            "com.tailg.run.intelligence.model.util.ActivityUtils";
    private static final String INTENT_MSG =
            "com.tailg.run.intelligence.model.bean.IntentMsg";
    private static final String NORMAL_ACTIVITY =
            "com.tailg.run.intelligence.model.mine_battery_information.activity.BatteryInfoActivity";
    private static final String C39_ACTIVITY =
            "com.tailg.run.intelligence.model.mine_battery_information.activity.BatteryInfoC39Activity";
    private static final String TLV_ACTIVITY =
            "com.tailg.run.intelligence.model.battery.BatteryInfoTlvActivity";
    private static final String BMS_ACTIVITY =
            "com.tailg.run.intelligence.model.battery.BmsBatteryTlvActivity";

    private BatteryInfoShortcutController() {
    }

    static void open(Activity activity, ClassLoader classLoader) {
        try {
            Object car = invokeStaticNoArg(classLoader, PREFS_UTIL, "getCarControlInfo");
            if (car == null) {
                showUnavailable(activity);
                return;
            }

            Integer currentModelType = asInteger(
                    invokeStaticNoArg(classLoader, CONTROL_TYPE_UTIL, "getCurrModelType")
            );
            Integer carModelType = asInteger(ReflectionAccess.invokeNoArg(car, "getModelType"));
            Integer isGps = asInteger(ReflectionAccess.invokeNoArg(car, "getIsGps"));
            String bmsTlvType = asString(ReflectionAccess.invokeNoArg(car, "getBmsTlvType"));
            String shareCarFlag = asString(ReflectionAccess.invokeNoArg(car, "getShareCarFlag"));
            BatteryInfoRoutePolicy.Route route = BatteryInfoRoutePolicy.resolve(
                    currentModelType,
                    carModelType,
                    isGps,
                    bmsTlvType,
                    shareCarFlag
            );

            switch (route) {
                case NORMAL:
                    launchWithOfficialUtility(activity, classLoader, NORMAL_ACTIVITY, null);
                    return;
                case C39:
                    Object intentMsg = newIntentMsg(classLoader);
                    ReflectionAccess.setField(
                            intentMsg,
                            "modelType",
                            BatteryInfoRoutePolicy.intentModelType(
                                    carModelType,
                                    currentModelType
                            )
                    );
                    launchWithOfficialUtility(activity, classLoader, C39_ACTIVITY, intentMsg);
                    return;
                case TLV:
                case BMS:
                    launchTlvActivity(
                            activity,
                            classLoader,
                            route == BatteryInfoRoutePolicy.Route.BMS
                                    ? BMS_ACTIVITY
                                    : TLV_ACTIVITY,
                            asString(ReflectionAccess.invokeNoArg(car, "getOnline")),
                            bmsTlvType,
                            asString(ReflectionAccess.invokeNoArg(car, "getCarId")),
                            BatteryInfoRoutePolicy.intentModelType(
                                    carModelType,
                                    currentModelType
                            )
                    );
                    return;
                case UNAVAILABLE:
                default:
                    showUnavailable(activity);
            }
        } catch (Throwable error) {
            Log.w(TAG, "Open battery information failed", error);
            Toast.makeText(activity, "电池信息页面打开失败", Toast.LENGTH_SHORT).show();
        }
    }

    private static Object invokeStaticNoArg(
            ClassLoader classLoader,
            String className,
            String methodName
    ) throws ReflectiveOperationException {
        Class<?> type = Class.forName(className, false, classLoader);
        Method method = type.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(null);
    }

    private static Object newIntentMsg(ClassLoader classLoader)
            throws ReflectiveOperationException {
        return Class.forName(INTENT_MSG, false, classLoader)
                .getDeclaredConstructor()
                .newInstance();
    }

    private static void launchWithOfficialUtility(
            Activity activity,
            ClassLoader classLoader,
            String targetClassName,
            Object intentMsg
    ) throws ReflectiveOperationException {
        Class<?> intentMsgClass = Class.forName(INTENT_MSG, false, classLoader);
        Class<?> activityUtilsClass = Class.forName(ACTIVITY_UTILS, false, classLoader);
        Class<?> targetClass = Class.forName(targetClassName, false, classLoader);
        Method launchActivity = activityUtilsClass.getDeclaredMethod(
                "launchActivity",
                Context.class,
                Class.class,
                intentMsgClass
        );
        launchActivity.invoke(null, activity, targetClass, intentMsg);
    }

    private static void launchTlvActivity(
            Activity activity,
            ClassLoader classLoader,
            String targetClassName,
            String online,
            String bmsTlvType,
            String carId,
            int modelType
    ) throws ClassNotFoundException {
        Intent intent = new Intent(activity, Class.forName(targetClassName, false, classLoader));
        intent.putExtra("online", online);
        intent.putExtra("bmsTlvType", bmsTlvType);
        intent.putExtra("carId", carId);
        intent.putExtra("modelType", modelType);
        activity.startActivity(intent);
    }

    private static Integer asInteger(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    private static String asString(Object value) {
        return value instanceof String ? (String) value : null;
    }

    private static void showUnavailable(Activity activity) {
        Toast.makeText(activity, "当前车辆暂无可用的电池信息页", Toast.LENGTH_SHORT).show();
    }
}
