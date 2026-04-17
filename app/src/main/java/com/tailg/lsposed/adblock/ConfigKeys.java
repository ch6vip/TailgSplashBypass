package com.tailg.lsposed.adblock;

final class ConfigKeys {
    static final String PREFS_NAME = "tailg_adblock";
    static final String KEY_ENABLE_MODULE = "enable_module";
    static final String KEY_HOOK_SETUP_VIEW = "hook_setup_view";
    static final String KEY_HOOK_COUNT_DOWN = "hook_count_down";
    static final String KEY_HOOK_CONFIG_BEAN = "hook_config_bean";
    static final String KEY_FORCE_EMPTY_RES = "force_empty_res";
    static final String KEY_FORCE_DURATION_ZERO = "force_duration_zero";
    static final String KEY_VERBOSE_LOG = "verbose_log";

    static final boolean DEFAULT_ENABLE_MODULE = true;
    static final boolean DEFAULT_HOOK_SETUP_VIEW = true;
    static final boolean DEFAULT_HOOK_COUNT_DOWN = true;
    static final boolean DEFAULT_HOOK_CONFIG_BEAN = true;
    static final boolean DEFAULT_FORCE_EMPTY_RES = true;
    static final boolean DEFAULT_FORCE_DURATION_ZERO = true;
    static final boolean DEFAULT_VERBOSE_LOG = true;

    private ConfigKeys() {
    }
}
