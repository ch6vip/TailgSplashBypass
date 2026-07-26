package com.tailg.lsposed.adblock;

final class BatteryInfoRoutePolicy {
    enum Route {
        NORMAL,
        C39,
        TLV,
        BMS,
        DYNAMICS,
        UNAVAILABLE
    }

    private BatteryInfoRoutePolicy() {
    }

    static Route resolve(
            Integer currentModelType,
            Integer carModelType,
            Integer isGps,
            String bmsTlvType,
            String shareCarFlag
    ) {
        int modelType = validModelType(currentModelType)
                ? currentModelType
                : carModelType == null ? -1 : carModelType;
        if (modelType == 1 || modelType == 2) {
            return Route.NORMAL;
        }
        if (modelType == 10 || modelType == 14) {
            return Route.C39;
        }
        if (isGps != null && isGps == 1) {
            if ("176".equals(bmsTlvType)) {
                return Route.BMS;
            }
            if ("160".equals(bmsTlvType) || "208".equals(bmsTlvType)) {
                return Route.TLV;
            }
            return Route.NORMAL;
        }

        // The official non-GPS flow depends on a live ViewModel batteryType. A settings
        // shortcut cannot obtain it safely, and shared vehicles are rejected there too.
        if (shareCarFlag == null || Boolean.parseBoolean(shareCarFlag)) {
            return Route.UNAVAILABLE;
        }
        return Route.UNAVAILABLE;
    }

    static int effectiveModelType(Integer currentModelType, Integer carModelType) {
        if (validModelType(currentModelType)) {
            return currentModelType;
        }
        return carModelType == null ? -1 : carModelType;
    }

    static int intentModelType(Integer carModelType, Integer currentModelType) {
        return carModelType == null
                ? effectiveModelType(currentModelType, null)
                : carModelType;
    }

    static boolean canOpenManually(
            Route route,
            Integer currentModelType,
            Integer carModelType,
            Integer isGps,
            String bmsTlvType
    ) {
        int modelType = effectiveModelType(currentModelType, carModelType);
        switch (route) {
            case NORMAL:
                return modelType == 1
                        || modelType == 2
                        || isGps != null && isGps == 1;
            case C39:
                return modelType == 10 || modelType == 14;
            case TLV:
                return !hasDedicatedBatteryPage(modelType)
                        && isGps != null
                        && isGps == 1
                        && ("160".equals(bmsTlvType) || "208".equals(bmsTlvType));
            case BMS:
                return !hasDedicatedBatteryPage(modelType)
                        && isGps != null
                        && isGps == 1
                        && "176".equals(bmsTlvType);
            case DYNAMICS:
                return true;
            case UNAVAILABLE:
            default:
                return false;
        }
    }

    static boolean canOpenManually(
            Route route,
            Integer currentModelType,
            Integer carModelType,
            Integer isGps,
            String bmsTlvType,
            boolean force
    ) {
        return (force && route != Route.UNAVAILABLE)
                || canOpenManually(
                        route,
                        currentModelType,
                        carModelType,
                        isGps,
                        bmsTlvType
                );
    }

    private static boolean validModelType(Integer modelType) {
        return modelType != null && modelType >= 0;
    }

    private static boolean hasDedicatedBatteryPage(int modelType) {
        return modelType == 1
                || modelType == 2
                || modelType == 10
                || modelType == 14;
    }
}
