package com.tailg.lsposed.adblock;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private Switch enableModuleSwitch;
    private Switch setupViewSwitch;
    private Switch countDownSwitch;
    private Switch configBeanSwitch;
    private Switch emptyResSwitch;
    private Switch durationZeroSwitch;
    private Switch verboseLogSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(ConfigKeys.PREFS_NAME, MODE_PRIVATE);
        setTitle(R.string.settings_title);
        setContentView(buildContentView());
        bindInitialValues();
    }

    private ScrollView buildContentView() {
        ScrollView root = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16);
        container.setPadding(padding, padding, padding, padding);
        root.addView(container, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView tip = new TextView(this);
        tip.setText(R.string.settings_tip_restart);
        container.addView(tip);

        enableModuleSwitch = createSwitch(R.string.switch_enable_module, ConfigKeys.DEFAULT_ENABLE_MODULE, container);
        setupViewSwitch = createSwitch(R.string.switch_hook_setup_view, ConfigKeys.DEFAULT_HOOK_SETUP_VIEW, container);
        countDownSwitch = createSwitch(R.string.switch_hook_count_down, ConfigKeys.DEFAULT_HOOK_COUNT_DOWN, container);
        configBeanSwitch = createSwitch(R.string.switch_hook_config_bean, ConfigKeys.DEFAULT_HOOK_CONFIG_BEAN, container);
        emptyResSwitch = createSwitch(R.string.switch_force_empty_res, ConfigKeys.DEFAULT_FORCE_EMPTY_RES, container);
        durationZeroSwitch = createSwitch(R.string.switch_force_duration_zero, ConfigKeys.DEFAULT_FORCE_DURATION_ZERO, container);
        verboseLogSwitch = createSwitch(R.string.switch_verbose_log, ConfigKeys.DEFAULT_VERBOSE_LOG, container);

        Button saveButton = new Button(this);
        saveButton.setText(R.string.save_button_text);
        saveButton.setOnClickListener(v -> saveConfig());
        container.addView(saveButton);
        return root;
    }

    private Switch createSwitch(int titleResId, boolean defaultValue, LinearLayout parent) {
        Switch sw = new Switch(this);
        sw.setText(titleResId);
        sw.setChecked(defaultValue);
        parent.addView(sw);
        return sw;
    }

    private void bindInitialValues() {
        enableModuleSwitch.setChecked(prefs.getBoolean(ConfigKeys.KEY_ENABLE_MODULE, ConfigKeys.DEFAULT_ENABLE_MODULE));
        setupViewSwitch.setChecked(prefs.getBoolean(ConfigKeys.KEY_HOOK_SETUP_VIEW, ConfigKeys.DEFAULT_HOOK_SETUP_VIEW));
        countDownSwitch.setChecked(prefs.getBoolean(ConfigKeys.KEY_HOOK_COUNT_DOWN, ConfigKeys.DEFAULT_HOOK_COUNT_DOWN));
        configBeanSwitch.setChecked(prefs.getBoolean(ConfigKeys.KEY_HOOK_CONFIG_BEAN, ConfigKeys.DEFAULT_HOOK_CONFIG_BEAN));
        emptyResSwitch.setChecked(prefs.getBoolean(ConfigKeys.KEY_FORCE_EMPTY_RES, ConfigKeys.DEFAULT_FORCE_EMPTY_RES));
        durationZeroSwitch.setChecked(prefs.getBoolean(ConfigKeys.KEY_FORCE_DURATION_ZERO, ConfigKeys.DEFAULT_FORCE_DURATION_ZERO));
        verboseLogSwitch.setChecked(prefs.getBoolean(ConfigKeys.KEY_VERBOSE_LOG, ConfigKeys.DEFAULT_VERBOSE_LOG));
    }

    private void saveConfig() {
        prefs.edit()
                .putBoolean(ConfigKeys.KEY_ENABLE_MODULE, enableModuleSwitch.isChecked())
                .putBoolean(ConfigKeys.KEY_HOOK_SETUP_VIEW, setupViewSwitch.isChecked())
                .putBoolean(ConfigKeys.KEY_HOOK_COUNT_DOWN, countDownSwitch.isChecked())
                .putBoolean(ConfigKeys.KEY_HOOK_CONFIG_BEAN, configBeanSwitch.isChecked())
                .putBoolean(ConfigKeys.KEY_FORCE_EMPTY_RES, emptyResSwitch.isChecked())
                .putBoolean(ConfigKeys.KEY_FORCE_DURATION_ZERO, durationZeroSwitch.isChecked())
                .putBoolean(ConfigKeys.KEY_VERBOSE_LOG, verboseLogSwitch.isChecked())
                .apply();
        Toast.makeText(this, R.string.settings_saved_toast, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}
