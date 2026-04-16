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
    private static final String PREFS_NAME = "tailg_adblock";
    private static final String KEY_ENABLE_MODULE = "enable_module";
    private static final String KEY_HOOK_SETUP_VIEW = "hook_setup_view";
    private static final String KEY_HOOK_COUNT_DOWN = "hook_count_down";
    private static final String KEY_HOOK_CONFIG_BEAN = "hook_config_bean";
    private static final String KEY_FORCE_EMPTY_RES = "force_empty_res";
    private static final String KEY_FORCE_DURATION_ZERO = "force_duration_zero";
    private static final String KEY_VERBOSE_LOG = "verbose_log";

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
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        setTitle("Tailg AdBlock Settings");
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
        tip.setText("修改后需要在 LSPosed 中重启目标应用（必要时重启系统）。");
        container.addView(tip);

        enableModuleSwitch = createSwitch("启用模块", true, container);
        setupViewSwitch = createSwitch("Hook setupView -> setupViewNo", true, container);
        countDownSwitch = createSwitch("Hook countDown -> countDownNo", true, container);
        configBeanSwitch = createSwitch("Hook ConfigGetBean 字段", true, container);
        emptyResSwitch = createSwitch("强制清空开屏资源URL", true, container);
        durationZeroSwitch = createSwitch("强制倒计时为0", true, container);
        verboseLogSwitch = createSwitch("输出详细日志", true, container);

        Button saveButton = new Button(this);
        saveButton.setText("保存配置");
        saveButton.setOnClickListener(v -> saveConfig());
        container.addView(saveButton);
        return root;
    }

    private Switch createSwitch(String title, boolean defaultValue, LinearLayout parent) {
        Switch sw = new Switch(this);
        sw.setText(title);
        sw.setChecked(defaultValue);
        parent.addView(sw);
        return sw;
    }

    private void bindInitialValues() {
        enableModuleSwitch.setChecked(prefs.getBoolean(KEY_ENABLE_MODULE, true));
        setupViewSwitch.setChecked(prefs.getBoolean(KEY_HOOK_SETUP_VIEW, true));
        countDownSwitch.setChecked(prefs.getBoolean(KEY_HOOK_COUNT_DOWN, true));
        configBeanSwitch.setChecked(prefs.getBoolean(KEY_HOOK_CONFIG_BEAN, true));
        emptyResSwitch.setChecked(prefs.getBoolean(KEY_FORCE_EMPTY_RES, true));
        durationZeroSwitch.setChecked(prefs.getBoolean(KEY_FORCE_DURATION_ZERO, true));
        verboseLogSwitch.setChecked(prefs.getBoolean(KEY_VERBOSE_LOG, true));
    }

    private void saveConfig() {
        prefs.edit()
                .putBoolean(KEY_ENABLE_MODULE, enableModuleSwitch.isChecked())
                .putBoolean(KEY_HOOK_SETUP_VIEW, setupViewSwitch.isChecked())
                .putBoolean(KEY_HOOK_COUNT_DOWN, countDownSwitch.isChecked())
                .putBoolean(KEY_HOOK_CONFIG_BEAN, configBeanSwitch.isChecked())
                .putBoolean(KEY_FORCE_EMPTY_RES, emptyResSwitch.isChecked())
                .putBoolean(KEY_FORCE_DURATION_ZERO, durationZeroSwitch.isChecked())
                .putBoolean(KEY_VERBOSE_LOG, verboseLogSwitch.isChecked())
                .apply();
        Toast.makeText(this, "已保存，重启目标应用后生效", Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}
