package com.tailg.lsposed.adblock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.mmkv.MMKV;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

@SuppressLint({"SetTextI18n", "UseSwitchCompatOrMaterialCode"})
final class OfficialSettingsPanel {
    private static final String TAG = "TailgSettingsPanel";
    private static final int DISTANCE_PROGRESS_MAX = 18;
    private static final Map<Activity, WeakReference<AlertDialog>> OPEN_DIALOGS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final Activity activity;
    private final MMKV preferences;
    private final List<ToggleSpec> specs = buildSpecs();
    private final Map<String, Switch> switches = new LinkedHashMap<>();
    private final Map<String, View> rows = new LinkedHashMap<>();

    private boolean refreshing;
    private SeekBar unlockSeekBar;
    private SeekBar lockSeekBar;
    private TextView unlockValue;
    private TextView lockValue;
    private View unlockRow;
    private View lockRow;

    private OfficialSettingsPanel(Activity activity) {
        this.activity = activity;
        this.preferences = HostConfigStore.open(activity);
    }

    static void show(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        WeakReference<AlertDialog> existingReference = OPEN_DIALOGS.get(activity);
        AlertDialog existing = existingReference == null ? null : existingReference.get();
        if (existing != null && existing.isShowing()) {
            return;
        }

        OfficialSettingsPanel panel;
        try {
            panel = new OfficialSettingsPanel(activity);
        } catch (Throwable error) {
            Log.e(TAG, "Open host MMKV settings failed", error);
            Toast.makeText(activity, "无法打开 Tailg 工具箱配置", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Tailg 工具箱")
                .setView(panel.createContent())
                .setPositiveButton("关闭", null)
                .create();
        dialog.setOnDismissListener(ignored -> OPEN_DIALOGS.remove(activity));
        OPEN_DIALOGS.put(activity, new WeakReference<>(dialog));
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            int height = Math.round(
                    activity.getResources().getDisplayMetrics().heightPixels * 0.88f
            );
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height);
        }
    }

    private View createContent() {
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int horizontalPadding = dp(20);
        content.setPadding(horizontalPadding, dp(4), horizontalPadding, dp(20));

        for (Group group : Group.values()) {
            List<ToggleSpec> groupSpecs = specsForGroup(group);
            if (groupSpecs.isEmpty()) {
                continue;
            }
            content.addView(createGroupTitle(group.title));
            for (ToggleSpec spec : groupSpecs) {
                content.addView(createToggleRow(spec));
            }
            if (group == Group.PROXIMITY) {
                unlockRow = createDistanceRow(true);
                lockRow = createDistanceRow(false);
                content.addView(unlockRow);
                content.addView(lockRow);
            }
        }

        TextView footer = new TextView(activity);
        footer.setText("设置已保存在官方 App，重启后生效");
        footer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
        footer.setAlpha(0.62f);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        footerParams.topMargin = dp(20);
        content.addView(footer, footerParams);

        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        refreshFromPreferences();
        return scroll;
    }

    private TextView createGroupTitle(String title) {
        TextView view = new TextView(activity);
        view.setText(title);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setTextColor(resolveAccentColor());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(18);
        params.bottomMargin = dp(4);
        view.setLayoutParams(params);
        return view;
    }

    private View createToggleRow(ToggleSpec spec) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(64));
        row.setPadding(dp(4), dp(8), dp(2), dp(8));
        applySelectableBackground(row);

        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(activity);
        title.setText(spec.title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
        labels.addView(title);

        TextView description = new TextView(activity);
        description.setText(spec.description);
        description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        description.setAlpha(0.66f);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        descriptionParams.topMargin = dp(2);
        labels.addView(description, descriptionParams);

        LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        labelsParams.rightMargin = dp(12);
        row.addView(labels, labelsParams);

        Switch toggle = new Switch(activity);
        toggle.setContentDescription(spec.title);
        row.addView(toggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        toggle.setOnCheckedChangeListener((button, checked) -> {
            if (refreshing) {
                return;
            }
            persistBoolean(spec.key, checked);
            applyGating();
        });
        row.setOnClickListener(view -> {
            if (toggle.isEnabled()) {
                toggle.toggle();
            }
        });

        switches.put(spec.key, toggle);
        rows.put(spec.key, row);
        return row;
    }

    private View createDistanceRow(boolean unlock) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(4), dp(8), dp(4), dp(10));

        LinearLayout heading = new LinearLayout(activity);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(activity);
        title.setText(unlock ? "靠近解锁距离" : "远离落锁距离");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.0f);
        heading.addView(title, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        ));

        TextView value = new TextView(activity);
        value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f);
        value.setTextColor(resolveAccentColor());
        value.setGravity(Gravity.END);
        value.setMinWidth(dp(64));
        heading.addView(value);
        row.addView(heading);

        TextView description = new TextView(activity);
        description.setText(unlock
                ? "旧 BleConnectService 使用的解锁阈值"
                : "至少比解锁阈值大 0.5 米");
        description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        description.setAlpha(0.66f);
        row.addView(description);

        SeekBar seekBar = new SeekBar(activity);
        seekBar.setMax(DISTANCE_PROGRESS_MAX);
        seekBar.setContentDescription(unlock ? "靠近解锁距离" : "远离落锁距离");
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                updateDistanceLabels();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                if (!refreshing) {
                    persistDistances(unlock);
                }
            }
        });
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        seekParams.topMargin = dp(4);
        row.addView(seekBar, seekParams);

        if (unlock) {
            unlockSeekBar = seekBar;
            unlockValue = value;
        } else {
            lockSeekBar = seekBar;
            lockValue = value;
        }
        return row;
    }

    private void refreshFromPreferences() {
        refreshing = true;
        try {
            for (ToggleSpec spec : specs) {
                Switch toggle = switches.get(spec.key);
                if (toggle != null) {
                    toggle.setChecked(preferences.getBoolean(spec.key, spec.defaultValue));
                }
            }
            ProximityPolicy.Distances distances = ProximityPolicy.normalize(
                    preferences.getFloat(
                            ConfigKeys.KEY_PROXIMITY_UNLOCK_METERS,
                            ConfigKeys.DEFAULT_PROXIMITY_UNLOCK_METERS
                    ),
                    preferences.getFloat(
                            ConfigKeys.KEY_PROXIMITY_LOCK_METERS,
                            ConfigKeys.DEFAULT_PROXIMITY_LOCK_METERS
                    )
            );
            setDistanceProgress(distances);
        } catch (RuntimeException error) {
            Log.w(TAG, "Read host settings failed", error);
            Toast.makeText(activity, "读取配置失败", Toast.LENGTH_SHORT).show();
        } finally {
            refreshing = false;
        }
        updateDistanceLabels();
        applyGating();
    }

    private void persistBoolean(String key, boolean value) {
        if (!ConfigKeys.isBooleanKey(key)) {
            return;
        }
        try {
            if (!preferences.encode(key, value)) {
                throw new IllegalStateException("MMKV encode returned false");
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "Write host boolean setting failed: " + key, error);
            Toast.makeText(activity, "保存模块配置失败", Toast.LENGTH_SHORT).show();
            refreshFromPreferences();
        }
    }

    private void persistDistances(boolean unlockChanged) {
        float unlock = unlockFromProgress(unlockSeekBar.getProgress());
        float lock = lockFromProgress(lockSeekBar.getProgress());
        if (unlockChanged && lock < unlock + ProximityPolicy.STEP_METERS) {
            lock = Math.min(ProximityPolicy.MAX_METERS, unlock + ProximityPolicy.STEP_METERS);
        } else if (!unlockChanged && lock < unlock + ProximityPolicy.STEP_METERS) {
            unlock = Math.max(ProximityPolicy.MIN_METERS, lock - ProximityPolicy.STEP_METERS);
        }
        ProximityPolicy.Distances distances = ProximityPolicy.normalize(unlock, lock);
        setDistanceProgress(distances);
        updateDistanceLabels();

        try {
            boolean unlockSaved = preferences.encode(
                    ConfigKeys.KEY_PROXIMITY_UNLOCK_METERS,
                    distances.unlockMeters
            );
            boolean lockSaved = preferences.encode(
                    ConfigKeys.KEY_PROXIMITY_LOCK_METERS,
                    distances.lockMeters
            );
            if (!unlockSaved || !lockSaved) {
                throw new IllegalStateException("MMKV encode returned false");
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "Write host distance settings failed", error);
            Toast.makeText(activity, "保存距离配置失败", Toast.LENGTH_SHORT).show();
            refreshFromPreferences();
        }
    }

    private void setDistanceProgress(ProximityPolicy.Distances distances) {
        if (unlockSeekBar == null || lockSeekBar == null) {
            return;
        }
        boolean previousRefreshing = refreshing;
        refreshing = true;
        unlockSeekBar.setProgress(Math.round(
                (distances.unlockMeters - ProximityPolicy.MIN_METERS)
                        / ProximityPolicy.STEP_METERS
        ));
        lockSeekBar.setProgress(Math.round(
                (distances.lockMeters - ProximityPolicy.MIN_METERS
                        - ProximityPolicy.STEP_METERS)
                        / ProximityPolicy.STEP_METERS
        ));
        refreshing = previousRefreshing;
    }

    private void updateDistanceLabels() {
        if (unlockValue == null || lockValue == null
                || unlockSeekBar == null || lockSeekBar == null) {
            return;
        }
        unlockValue.setText(String.format(
                Locale.CHINA,
                "%.1f 米",
                unlockFromProgress(unlockSeekBar.getProgress())
        ));
        lockValue.setText(String.format(
                Locale.CHINA,
                "%.1f 米",
                lockFromProgress(lockSeekBar.getProgress())
        ));
    }

    private float unlockFromProgress(int progress) {
        return ProximityPolicy.MIN_METERS + progress * ProximityPolicy.STEP_METERS;
    }

    private float lockFromProgress(int progress) {
        return ProximityPolicy.MIN_METERS
                + ProximityPolicy.STEP_METERS
                + progress * ProximityPolicy.STEP_METERS;
    }

    private void applyGating() {
        Switch masterSwitch = switches.get(ConfigKeys.KEY_ENABLE_MODULE);
        boolean masterEnabled = masterSwitch != null && masterSwitch.isChecked();
        for (ToggleSpec spec : specs) {
            boolean enabled = ConfigKeys.KEY_ENABLE_MODULE.equals(spec.key)
                    || masterEnabled && (spec.dependsOn == null || isChecked(spec.dependsOn));
            Switch toggle = switches.get(spec.key);
            View row = rows.get(spec.key);
            if (toggle != null) {
                toggle.setEnabled(enabled);
            }
            if (row != null) {
                row.setEnabled(enabled);
                row.setClickable(enabled);
                row.setAlpha(enabled ? 1.0f : 0.42f);
            }
        }

        boolean distanceEnabled = masterEnabled
                && isChecked(ConfigKeys.KEY_OVERRIDE_PROXIMITY_DISTANCE);
        setDistanceEnabled(unlockRow, unlockSeekBar, distanceEnabled);
        setDistanceEnabled(lockRow, lockSeekBar, distanceEnabled);
    }

    private boolean isChecked(String key) {
        Switch toggle = switches.get(key);
        return toggle != null && toggle.isChecked();
    }

    private void setDistanceEnabled(View row, SeekBar seekBar, boolean enabled) {
        if (row != null) {
            row.setEnabled(enabled);
            row.setAlpha(enabled ? 1.0f : 0.42f);
        }
        if (seekBar != null) {
            seekBar.setEnabled(enabled);
        }
    }

    private List<ToggleSpec> specsForGroup(Group group) {
        List<ToggleSpec> result = new ArrayList<>();
        for (ToggleSpec spec : specs) {
            if (spec.group == group) {
                result.add(spec);
            }
        }
        return result;
    }

    private List<ToggleSpec> buildSpecs() {
        List<ToggleSpec> result = new ArrayList<>();
        add(result, ConfigKeys.KEY_ENABLE_MODULE, "启用模块",
                "关闭后下次启动不安装功能 Hook", ConfigKeys.DEFAULT_ENABLE_MODULE,
                Group.GENERAL, null);
        add(result, ConfigKeys.KEY_STRICT_VERSION_GUARD, "仅支持版本启用",
                "只对已验证的官方 App 3.5.9 注入", ConfigKeys.DEFAULT_STRICT_VERSION_GUARD,
                Group.GENERAL, null);
        add(result, ConfigKeys.KEY_SHOW_OFFICIAL_SETTINGS_ENTRY, "显示官方设置入口",
                "在账号功能上方显示 Tailg 工具箱", ConfigKeys.DEFAULT_SHOW_OFFICIAL_SETTINGS_ENTRY,
                Group.GENERAL, null);

        add(result, ConfigKeys.KEY_HOOK_SETUP_VIEW, "跳过开屏初始化",
                "重定向 SplashActivity.setupView", ConfigKeys.DEFAULT_HOOK_SETUP_VIEW,
                Group.SPLASH, null);
        add(result, ConfigKeys.KEY_HOOK_COUNT_DOWN, "跳过开屏倒计时",
                "重定向 SplashActivity.countDown", ConfigKeys.DEFAULT_HOOK_COUNT_DOWN,
                Group.SPLASH, null);
        add(result, ConfigKeys.KEY_HOOK_CONFIG_BEAN, "拦截开屏广告配置",
                "开屏资源与 Banner 的总开关", ConfigKeys.DEFAULT_HOOK_CONFIG_BEAN,
                Group.SPLASH, null);
        add(result, ConfigKeys.KEY_FORCE_EMPTY_RES, "清空开屏资源",
                "清空首页和底部开屏资源地址", ConfigKeys.DEFAULT_FORCE_EMPTY_RES,
                Group.SPLASH, ConfigKeys.KEY_HOOK_CONFIG_BEAN);
        add(result, ConfigKeys.KEY_FORCE_DURATION_ZERO, "开屏倒计时归零",
                "将广告配置倒计时设为 0", ConfigKeys.DEFAULT_FORCE_DURATION_ZERO,
                Group.SPLASH, ConfigKeys.KEY_HOOK_CONFIG_BEAN);

        add(result, ConfigKeys.KEY_FORCE_EMPTY_BANNER, "拦截首页 Banner",
                "清空首页 Banner 配置", ConfigKeys.DEFAULT_FORCE_EMPTY_BANNER,
                Group.HOME, ConfigKeys.KEY_HOOK_CONFIG_BEAN);
        add(result, ConfigKeys.KEY_HOOK_APP_UPDATE, "拦截 App 升级弹窗",
                "不影响车辆固件或 OTA", ConfigKeys.DEFAULT_HOOK_APP_UPDATE,
                Group.HOME, null);
        add(result, ConfigKeys.KEY_SIMPLIFY_HOME_NAV, "精简首页导航",
                "隐藏圈子和商城", ConfigKeys.DEFAULT_SIMPLIFY_HOME_NAV,
                Group.HOME, null);
        add(result, ConfigKeys.KEY_ENABLE_VEHICLE_DIAGNOSTICS, "车辆能力诊断",
                "长按爱车打开脱敏诊断", ConfigKeys.DEFAULT_ENABLE_VEHICLE_DIAGNOSTICS,
                Group.HOME, null);

        add(result, ConfigKeys.KEY_BLOCK_USAGE_REPORT, "屏蔽使用行为上报",
                "阻止 collect/report 请求", ConfigKeys.DEFAULT_BLOCK_USAGE_REPORT,
                Group.PRIVACY, null);
        add(result, ConfigKeys.KEY_BLOCK_BUGLY, "屏蔽 Bugly 崩溃上报",
                "跳过腾讯 Bugly 初始化", ConfigKeys.DEFAULT_BLOCK_BUGLY,
                Group.PRIVACY, null);

        add(result, ConfigKeys.KEY_ENABLE_TRACK_EXPORT, "轨迹导出",
                "在轨迹详情页提供 GPX 和 CSV", ConfigKeys.DEFAULT_ENABLE_TRACK_EXPORT,
                Group.DATA, null);
        add(result, ConfigKeys.KEY_TRIM_TRACK_ENDPOINTS, "隐藏轨迹首尾位置",
                "导出时移除首尾附近各 200 米", ConfigKeys.DEFAULT_TRIM_TRACK_ENDPOINTS,
                Group.DATA, ConfigKeys.KEY_ENABLE_TRACK_EXPORT);

        add(result, ConfigKeys.KEY_OVERRIDE_PROXIMITY_DISTANCE, "覆盖旧版 RSSI 距离",
                "只修改旧 BleConnectService 阈值", ConfigKeys.DEFAULT_OVERRIDE_PROXIMITY_DISTANCE,
                Group.PROXIMITY, null);

        add(result, ConfigKeys.KEY_VERBOSE_LOG, "详细日志",
                "输出 Hook 安装详情", ConfigKeys.DEFAULT_VERBOSE_LOG,
                Group.DEBUG, null);
        return result;
    }

    private void add(
            List<ToggleSpec> target,
            String key,
            String title,
            String description,
            boolean defaultValue,
            Group group,
            String dependsOn
    ) {
        target.add(new ToggleSpec(key, title, description, defaultValue, group, dependsOn));
    }

    private void applySelectableBackground(View view) {
        TypedValue value = new TypedValue();
        if (activity.getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground,
                value,
                true
        ) && value.resourceId != 0) {
            view.setBackgroundResource(value.resourceId);
        }
    }

    private int resolveAccentColor() {
        TypedValue value = new TypedValue();
        if (activity.getTheme().resolveAttribute(android.R.attr.colorAccent, value, true)) {
            if (value.resourceId != 0) {
                try {
                    return activity.getColor(value.resourceId);
                } catch (RuntimeException ignored) {
                    // Use the resolved literal or fallback below.
                }
            }
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return value.data;
            }
        }
        return Color.rgb(0, 121, 107);
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private enum Group {
        GENERAL("总控"),
        SPLASH("开屏广告"),
        HOME("首页与诊断"),
        PRIVACY("隐私"),
        DATA("轨迹数据"),
        PROXIMITY("旧版 RSSI 感应距离"),
        DEBUG("调试");

        final String title;

        Group(String title) {
            this.title = title;
        }
    }

    private static final class ToggleSpec {
        final String key;
        final String title;
        final String description;
        final boolean defaultValue;
        final Group group;
        final String dependsOn;

        ToggleSpec(
                String key,
                String title,
                String description,
                boolean defaultValue,
                Group group,
                String dependsOn
        ) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.defaultValue = defaultValue;
            this.group = group;
            this.dependsOn = dependsOn;
        }
    }
}
