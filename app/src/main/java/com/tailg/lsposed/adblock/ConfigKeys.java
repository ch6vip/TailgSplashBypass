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
    static final String KEY_FAST_STARTUP = "fast_startup";
    static final String KEY_BLOCK_USAGE_REPORT = "block_usage_report";
    static final String KEY_BLOCK_BUGLY = "block_bugly";
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
    static final boolean DEFAULT_FAST_STARTUP = false;
    static final boolean DEFAULT_BLOCK_USAGE_REPORT = true;
    static final boolean DEFAULT_BLOCK_BUGLY = false;
    static final boolean DEFAULT_VERBOSE_LOG = false;

    static final String[] BOOLEAN_KEYS = {
            KEY_ENABLE_MODULE,
            KEY_STRICT_VERSION_GUARD,
            KEY_HOOK_SETUP_VIEW,
            KEY_HOOK_COUNT_DOWN,
            KEY_HOOK_CONFIG_BEAN,
            KEY_FORCE_EMPTY_RES,
            KEY_FORCE_DURATION_ZERO,
            KEY_FORCE_EMPTY_BANNER,
            KEY_HOOK_APP_UPDATE,
            KEY_FAST_STARTUP,
            KEY_BLOCK_USAGE_REPORT,
            KEY_BLOCK_BUGLY,
            KEY_VERBOSE_LOG
    };

    static boolean isBooleanKey(String key) {
        for (String candidate : BOOLEAN_KEYS) {
            if (candidate.equals(key)) {
                return true;
            }
        }
        return false;
    }

    static boolean defaultBooleanFor(String key) {
        switch (key) {
            case KEY_ENABLE_MODULE:
                return DEFAULT_ENABLE_MODULE;
            case KEY_STRICT_VERSION_GUARD:
                return DEFAULT_STRICT_VERSION_GUARD;
            case KEY_HOOK_SETUP_VIEW:
                return DEFAULT_HOOK_SETUP_VIEW;
            case KEY_HOOK_COUNT_DOWN:
                return DEFAULT_HOOK_COUNT_DOWN;
            case KEY_HOOK_CONFIG_BEAN:
                return DEFAULT_HOOK_CONFIG_BEAN;
            case KEY_FORCE_EMPTY_RES:
                return DEFAULT_FORCE_EMPTY_RES;
            case KEY_FORCE_DURATION_ZERO:
                return DEFAULT_FORCE_DURATION_ZERO;
            case KEY_FORCE_EMPTY_BANNER:
                return DEFAULT_FORCE_EMPTY_BANNER;
            case KEY_HOOK_APP_UPDATE:
                return DEFAULT_HOOK_APP_UPDATE;
            case KEY_FAST_STARTUP:
                return DEFAULT_FAST_STARTUP;
            case KEY_BLOCK_USAGE_REPORT:
                return DEFAULT_BLOCK_USAGE_REPORT;
            case KEY_BLOCK_BUGLY:
                return DEFAULT_BLOCK_BUGLY;
            case KEY_VERBOSE_LOG:
                return DEFAULT_VERBOSE_LOG;
            default:
                throw new IllegalArgumentException("Unknown boolean config key: " + key);
        }
    }

    private ConfigKeys() {
    }
}
