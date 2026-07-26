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
    private static final String TBOX_COMPONENT =
            "com.thinkerride.tbox.component.TBoxComponent";
    private static final String DYNAMICS_ACTIVITY =
            "com.thinkerride.tbox.centercontrol.battery.BatteryChartActivity";

    private BatteryInfoShortcutController() {
    }

    static boolean open(Activity activity, ClassLoader classLoader) {
        return open(activity, classLoader, null, false);
    }

    static boolean open(
            Activity activity,
            ClassLoader classLoader,
            BatteryInfoRoutePolicy.Route requestedRoute
    ) {
        return open(activity, classLoader, requestedRoute, false);
    }

    static boolean open(
            Activity activity,
            ClassLoader classLoader,
            BatteryInfoRoutePolicy.Route requestedRoute,
            boolean force
    ) {
        try {
            Object car = invokeStaticNoArg(classLoader, PREFS_UTIL, "getCarControlInfo");
            if (car == null) {
                showUnavailable(activity);
                return false;
            }

            Integer currentModelType = asInteger(
                    invokeStaticNoArg(classLoader, CONTROL_TYPE_UTIL, "getCurrModelType")
            );
            Integer carModelType = asInteger(ReflectionAccess.invokeNoArg(car, "getModelType"));
            Integer isGps = asInteger(ReflectionAccess.invokeNoArg(car, "getIsGps"));
            String bmsTlvType = asString(ReflectionAccess.invokeNoArg(car, "getBmsTlvType"));
            String shareCarFlag = asString(ReflectionAccess.invokeNoArg(car, "getShareCarFlag"));
            BatteryInfoRoutePolicy.Route route = requestedRoute;
            if (route == null) {
                route = BatteryInfoRoutePolicy.resolve(
                        currentModelType,
                        carModelType,
                        isGps,
                        bmsTlvType,
                        shareCarFlag
                );
            } else if (!BatteryInfoRoutePolicy.canOpenManually(
                    route,
                    currentModelType,
                    carModelType,
                    isGps,
                    bmsTlvType,
                    force
            )) {
                showIncompatible(activity);
                return false;
            }

            switch (route) {
                case NORMAL:
                    launchWithOfficialUtility(activity, classLoader, NORMAL_ACTIVITY, null);
                    return true;
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
                    return true;
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
                    return true;
                case DYNAMICS:
                    return launchBatteryDynamics(activity, classLoader, force);
                case UNAVAILABLE:
                default:
                    showUnavailable(activity);
                    return false;
            }
        } catch (Throwable error) {
            Log.w(TAG, "Open battery information failed", error);
            Toast.makeText(activity, "电池信息页面打开失败", Toast.LENGTH_SHORT).show();
            return false;
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

    private static boolean launchBatteryDynamics(
            Activity activity,
            ClassLoader classLoader,
            boolean force
    ) throws ReflectiveOperationException {
        if (!force) {
            Object centerControlAction = invokeStaticNoArg(
                    classLoader,
                    TBOX_COMPONENT,
                    "getCenterControlAction"
            );
            String uuid = asString(
                    ReflectionAccess.invokeNoArg(centerControlAction, "getCurrentUUID")
            );
            if (uuid == null || uuid.trim().isEmpty()) {
                Toast.makeText(
                        activity,
                        "当前车辆缺少电池动态所需的中控标识",
                        Toast.LENGTH_SHORT
                ).show();
                return false;
            }
        }

        Class<?> activityClass = Class.forName(DYNAMICS_ACTIVITY, false, classLoader);
        Method start = activityClass.getDeclaredMethod("start", Context.class);
        start.invoke(null, activity);
        return true;
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

    private static void showIncompatible(Activity activity) {
        Toast.makeText(activity, "当前车辆不支持所选电池信息页", Toast.LENGTH_SHORT).show();
    }
}
