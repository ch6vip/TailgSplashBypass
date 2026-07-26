package com.tailg.lsposed.adblock;

final class ConfigKeys {
    static final String PREFS_NAME = "tailg_adblock";
    static final String KEY_ENABLE_MODULE = "enable_module";
    static final String KEY_STRICT_VERSION_GUARD = "strict_version_guard";
    static final String KEY_HOOK_SETUP_VIEW = "hook_setup_view";
    static final String KEY_HOOK_COUNT_DOWN = "hook_count_down";
    static final String KEY_HOOK_CONFIG_BEAN = "hook_config_bean";
    static final String KEY_FORCE_EMPTY_RES = "force_empty_res";
    static final String KEY_FORCE_DURATION_ZERO = "force_duration_zero";
    static final String KEY_FORCE_EMPTY_BANNER = "force_empty_banner";
    static final String KEY_HOOK_APP_UPDATE = "hook_app_update";
    static final String KEY_BLOCK_USAGE_REPORT = "block_usage_report";
    static final String KEY_BLOCK_BUGLY = "block_bugly";
    static final String KEY_SIMPLIFY_HOME_NAV = "simplify_home_nav";
    static final String KEY_ENABLE_TRACK_EXPORT = "enable_track_export";
    static final String KEY_TRIM_TRACK_ENDPOINTS = "trim_track_endpoints";
    static final String KEY_ENABLE_VEHICLE_DIAGNOSTICS = "enable_vehicle_diagnostics";
    static final String KEY_OVERRIDE_PROXIMITY_DISTANCE = "override_proximity_distance";
    static final String KEY_PROXIMITY_UNLOCK_METERS = "proximity_unlock_meters";
    static final String KEY_PROXIMITY_LOCK_METERS = "proximity_lock_meters";
    static final String KEY_VERBOSE_LOG = "verbose_log";

    static final boolean DEFAULT_ENABLE_MODULE = true;
    static final boolean DEFAULT_STRICT_VERSION_GUARD = true;
    static final boolean DEFAULT_HOOK_SETUP_VIEW = true;
    static final boolean DEFAULT_HOOK_COUNT_DOWN = true;
    static final boolean DEFAULT_HOOK_CONFIG_BEAN = true;
    static final boolean DEFAULT_FORCE_EMPTY_RES = true;
    static final boolean DEFAULT_FORCE_DURATION_ZERO = true;
    static final boolean DEFAULT_FORCE_EMPTY_BANNER = true;
    static final boolean DEFAULT_HOOK_APP_UPDATE = true;
    static final boolean DEFAULT_BLOCK_USAGE_REPORT = true;
    static final boolean DEFAULT_BLOCK_BUGLY = false;
    static final boolean DEFAULT_SIMPLIFY_HOME_NAV = false;
    static final boolean DEFAULT_ENABLE_TRACK_EXPORT = true;
    static final boolean DEFAULT_TRIM_TRACK_ENDPOINTS = true;
    static final boolean DEFAULT_ENABLE_VEHICLE_DIAGNOSTICS = true;
    static final boolean DEFAULT_OVERRIDE_PROXIMITY_DISTANCE = false;
    static final float DEFAULT_PROXIMITY_UNLOCK_METERS = 2.0f;
    static final float DEFAULT_PROXIMITY_LOCK_METERS = 3.0f;
    static final boolean DEFAULT_VERBOSE_LOG = false;

    private ConfigKeys() {
    }
}
