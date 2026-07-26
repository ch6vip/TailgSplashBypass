package com.tailg.lsposed.adblock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BatteryInfoRoutePolicyTest {
    @Test
    public void legacyModels_openNormalBatteryPage() {
        assertRoute(BatteryInfoRoutePolicy.Route.NORMAL, 1, 1, null, null, null);
        assertRoute(BatteryInfoRoutePolicy.Route.NORMAL, 2, 2, null, null, null);
    }

    @Test
    public void c39Models_openC39Page() {
        assertRoute(BatteryInfoRoutePolicy.Route.C39, 10, 10, null, null, null);
        assertRoute(BatteryInfoRoutePolicy.Route.C39, 14, 14, null, null, null);
    }

    @Test
    public void gpsModels_routeKnownTlvBatteryTypes() {
        assertRoute(BatteryInfoRoutePolicy.Route.TLV, 8, 8, 1, "160", "false");
        assertRoute(BatteryInfoRoutePolicy.Route.BMS, 8, 8, 1, "176", "false");
        assertRoute(BatteryInfoRoutePolicy.Route.TLV, 8, 8, 1, "208", "false");
    }

    @Test
    public void gpsModels_withoutKnownTlvType_openNormalPage() {
        assertRoute(BatteryInfoRoutePolicy.Route.NORMAL, 8, 8, 1, null, "false");
        assertRoute(BatteryInfoRoutePolicy.Route.NORMAL, 8, 8, 1, "unknown", "false");
    }

    @Test
    public void nonGpsModels_areUnavailableWithoutLiveBatteryType() {
        assertRoute(BatteryInfoRoutePolicy.Route.UNAVAILABLE, 8, 8, 0, "160", "false");
        assertRoute(BatteryInfoRoutePolicy.Route.UNAVAILABLE, 8, 8, 0, "160", "true");
        assertRoute(BatteryInfoRoutePolicy.Route.UNAVAILABLE, 8, 8, 0, "160", null);
    }

    @Test
    public void invalidCurrentModel_fallsBackToCarModel() {
        assertRoute(BatteryInfoRoutePolicy.Route.C39, -1, 14, 0, null, "false");
        assertEquals(14, BatteryInfoRoutePolicy.effectiveModelType(-1, 14));
    }

    @Test
    public void intentPayload_prefersOfficialCarModelValue() {
        assertEquals(14, BatteryInfoRoutePolicy.intentModelType(14, 10));
        assertEquals(10, BatteryInfoRoutePolicy.intentModelType(null, 10));
    }

    @Test
    public void manualNormalPage_acceptsLegacyAndGpsVehicles() {
        assertManual(true, BatteryInfoRoutePolicy.Route.NORMAL, 1, 1, 0, null);
        assertManual(true, BatteryInfoRoutePolicy.Route.NORMAL, 8, 8, 1, "176");
        assertManual(false, BatteryInfoRoutePolicy.Route.NORMAL, 8, 8, 0, null);
    }

    @Test
    public void manualSpecialPages_requireMatchingVehicleData() {
        assertManual(true, BatteryInfoRoutePolicy.Route.C39, 10, 10, 0, null);
        assertManual(false, BatteryInfoRoutePolicy.Route.C39, 8, 8, 1, "160");
        assertManual(true, BatteryInfoRoutePolicy.Route.TLV, 8, 8, 1, "160");
        assertManual(true, BatteryInfoRoutePolicy.Route.TLV, 8, 8, 1, "208");
        assertManual(false, BatteryInfoRoutePolicy.Route.TLV, 8, 8, 1, "176");
        assertManual(false, BatteryInfoRoutePolicy.Route.TLV, 10, 10, 1, "160");
        assertManual(true, BatteryInfoRoutePolicy.Route.BMS, 8, 8, 1, "176");
        assertManual(false, BatteryInfoRoutePolicy.Route.BMS, 8, 8, 1, "208");
        assertManual(false, BatteryInfoRoutePolicy.Route.BMS, 14, 14, 1, "176");
    }

    @Test
    public void batteryDynamics_usesRuntimeUuidCheck() {
        assertManual(true, BatteryInfoRoutePolicy.Route.DYNAMICS, null, null, null, null);
    }

    private static void assertRoute(
            BatteryInfoRoutePolicy.Route expected,
            Integer currentModelType,
            Integer carModelType,
            Integer isGps,
            String bmsTlvType,
            String shareCarFlag
    ) {
        assertEquals(
                expected,
                BatteryInfoRoutePolicy.resolve(
                        currentModelType,
                        carModelType,
                        isGps,
                        bmsTlvType,
                        shareCarFlag
                )
        );
    }

    private static void assertManual(
            boolean expected,
            BatteryInfoRoutePolicy.Route route,
            Integer currentModelType,
            Integer carModelType,
            Integer isGps,
            String bmsTlvType
    ) {
        assertEquals(
                expected,
                BatteryInfoRoutePolicy.canOpenManually(
                        route,
                        currentModelType,
                        carModelType,
                        isGps,
                        bmsTlvType
                )
        );
    }
}
