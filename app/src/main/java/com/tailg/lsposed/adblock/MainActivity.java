package com.tailg.lsposed.adblock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.libxposed.service.XposedService;

/**
 * 设置页（模块 Launcher 图标）。
 *
 * <p>观感参考 HookVip：Material 3 + DayNight，Android 12+ 动态取色。开关分组为卡片，
 * 每行标题 + 副标题；即时保存；主开关与 ConfigGetBean 总开关关闭时联动置灰其从属项。</p>
 *
 * <p>UI 由数据驱动：{@link #buildSpecs()} 定义所有开关及其分组/依赖，界面按此生成，
 * 后续新增开关只需在该表加一行 + 两条字符串。</p>
 */
public class MainActivity extends AppCompatActivity implements ModuleApplication.ServiceStateListener {
    private static final String TAG = "TailgAdBlockSettings";

    private SharedPreferences prefs;
    private TextView serviceStatus;
    private boolean refreshingSwitches;
    private final Map<String, MaterialSwitch> switches = new LinkedHashMap<>();
    private final Map<String, View> rows = new LinkedHashMap<>();
    private final List<ToggleSpec> specs = buildSpecs();
    private Slider unlockDistanceSlider;
    private Slider lockDistanceSlider;
    private TextView unlockDistanceValue;
    private TextView lockDistanceValue;
    private View unlockDistanceRow;
    private View lockDistanceRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CollapsingToolbarLayout collapsing = findViewById(R.id.collapsing_toolbar);
        collapsing.setTitle(getString(R.string.header_title));
        serviceStatus = findViewById(R.id.service_status);

        buildSections();
        applyGating();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ModuleApplication.addServiceStateListener(this, true);
    }

    @Override
    protected void onStop() {
        ModuleApplication.removeServiceStateListener(this);
        super.onStop();
    }

    @Override
    public void onServiceStateChanged(@Nullable XposedService service) {
        runOnUiThread(() -> bindRemotePreferences(service));
    }

    private void bindRemotePreferences(XposedService service) {
        if (service == null) {
            prefs = null;
            serviceStatus.setText(R.string.service_status_disconnected);
            refreshSwitches();
            return;
        }

        try {
            SharedPreferences remotePrefs = service.getRemotePreferences(ConfigKeys.PREFS_NAME);
            migrateLegacyPreferences(remotePrefs);
            prefs = remotePrefs;
            serviceStatus.setText(getString(R.string.service_status_connected, service.getFrameworkName()));
        } catch (RuntimeException e) {
            handleRemotePreferencesError(e);
            return;
        }
        refreshSwitches();
    }

    private void migrateLegacyPreferences(SharedPreferences remotePrefs) {
        if (!remotePrefs.getAll().isEmpty()) {
            return;
        }

        SharedPreferences legacyPrefs = getSharedPreferences(ConfigKeys.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = remotePrefs.edit();
        boolean hasLegacyValues = false;
        for (ToggleSpec spec : specs) {
            if (legacyPrefs.contains(spec.key)) {
                editor.putBoolean(spec.key, legacyPrefs.getBoolean(spec.key, spec.def));
                hasLegacyValues = true;
            }
        }
        if (legacyPrefs.contains(ConfigKeys.KEY_PROXIMITY_UNLOCK_METERS)) {
            editor.putFloat(
                    ConfigKeys.KEY_PROXIMITY_UNLOCK_METERS,
                    legacyPrefs.getFloat(
                            ConfigKeys.KEY_PROXIMITY_UNLOCK_METERS,
                            ConfigKeys.DEFAULT_PROXIMITY_UNLOCK_METERS
                    )
            );
            hasLegacyValues = true;
        }
        if (legacyPrefs.contains(ConfigKeys.KEY_PROXIMITY_LOCK_METERS)) {
            editor.putFloat(
                    ConfigKeys.KEY_PROXIMITY_LOCK_METERS,
                    legacyPrefs.getFloat(
                            ConfigKeys.KEY_PROXIMITY_LOCK_METERS,
                            ConfigKeys.DEFAULT_PROXIMITY_LOCK_METERS
                    )
            );
            hasLegacyValues = true;
        }
        if (hasLegacyValues) {
            editor.apply();
        }
    }

    private void refreshSwitches() {
        refreshingSwitches = true;
        try {
            setSwitchValues(false);
            setDistanceValues(false);
        } catch (RuntimeException e) {
            Log.w(TAG, "Read remote preferences failed", e);
            prefs = null;
            serviceStatus.setText(R.string.service_status_error);
            setSwitchValues(true);
            setDistanceValues(true);
        } finally {
            refreshingSwitches = false;
        }
        applyGating();
    }

    private void setSwitchValues(boolean useDefaults) {
        for (ToggleSpec spec : specs) {
            MaterialSwitch toggle = switches.get(spec.key);
            if (toggle != null) {
                boolean checked = useDefaults || prefs == null
                        ? spec.def
                        : prefs.getBoolean(spec.key, spec.def);
                toggle.setChecked(checked);
            }
        }
    }

    private void handleRemotePreferencesError(RuntimeException error) {
        Log.w(TAG, "Access remote preferences failed", error);
        prefs = null;
        serviceStatus.setText(R.string.service_status_error);
        refreshSwitches();
    }

    private void buildSections() {
        LinearLayout container = findViewById(R.id.sections_container);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Group group : Group.values()) {
            List<ToggleSpec> groupSpecs = specsForGroup(group);
            if (groupSpecs.isEmpty()) {
                continue;
            }

            View section = inflater.inflate(R.layout.view_section, container, false);
            TextView sectionTitle = section.findViewById(R.id.section_title);
            sectionTitle.setText(group.titleRes);
            LinearLayout rowsContainer = section.findViewById(R.id.section_rows);

            for (ToggleSpec spec : groupSpecs) {
                View row = inflater.inflate(R.layout.row_switch, rowsContainer, false);
                TextView title = row.findViewById(R.id.row_title);
                TextView subtitle = row.findViewById(R.id.row_subtitle);
                MaterialSwitch toggle = row.findViewById(R.id.row_switch);

                title.setText(spec.titleRes);
                subtitle.setText(spec.descRes);
                toggle.setChecked(spec.def);
                toggle.setOnCheckedChangeListener((button, checked) -> {
                    if (refreshingSwitches || prefs == null) {
                        return;
                    }
                    try {
                        prefs.edit().putBoolean(spec.key, checked).apply();
                    } catch (RuntimeException e) {
                        handleRemotePreferencesError(e);
                        return;
                    }
                    applyGating();
                });
                row.setOnClickListener(v -> toggle.toggle());

                switches.put(spec.key, toggle);
                rows.put(spec.key, row);
                rowsContainer.addView(row);
            }
            if (group == Group.PROXIMITY) {
                buildDistanceRows(inflater, rowsContainer);
            }
            container.addView(section);
        }
    }

    private void buildDistanceRows(LayoutInflater inflater, LinearLayout container) {
        unlockDistanceRow = inflater.inflate(R.layout.row_slider, container, false);
        lockDistanceRow = inflater.inflate(R.layout.row_slider, container, false);

        unlockDistanceSlider = unlockDistanceRow.findViewById(R.id.distance_slider);
        lockDistanceSlider = lockDistanceRow.findViewById(R.id.distance_slider);
        unlockDistanceSlider.setId(View.generateViewId());
        lockDistanceSlider.setId(View.generateViewId());
        unlockDistanceValue = unlockDistanceRow.findViewById(R.id.slider_value);
        lockDistanceValue = lockDistanceRow.findViewById(R.id.slider_value);

        TextView unlockTitle = unlockDistanceRow.findViewById(R.id.slider_title);
        TextView lockTitle = lockDistanceRow.findViewById(R.id.slider_title);
        unlockTitle.setText(R.string.slider_unlock_distance);
        unlockTitle.setLabelFor(unlockDistanceSlider.getId());
        ((TextView) unlockDistanceRow.findViewById(R.id.slider_subtitle))
                .setText(R.string.desc_unlock_distance);
        lockTitle.setText(R.string.slider_lock_distance);
        lockTitle.setLabelFor(lockDistanceSlider.getId());
        ((TextView) lockDistanceRow.findViewById(R.id.slider_subtitle))
                .setText(R.string.desc_lock_distance);

        unlockDistanceSlider.setValueFrom(ProximityPolicy.MIN_METERS);
        unlockDistanceSlider.setValueTo(
                ProximityPolicy.MAX_METERS - ProximityPolicy.STEP_METERS
        );
        unlockDistanceSlider.setStepSize(ProximityPolicy.STEP_METERS);
        lockDistanceSlider.setValueFrom(
                ProximityPolicy.MIN_METERS + ProximityPolicy.STEP_METERS
        );
        lockDistanceSlider.setValueTo(ProximityPolicy.MAX_METERS);
        lockDistanceSlider.setStepSize(ProximityPolicy.STEP_METERS);

        unlockDistanceSlider.addOnChangeListener((slider, value, fromUser) -> {
            updateDistanceLabels();
            if (fromUser) {
                persistDistances(true);
            }
        });
        lockDistanceSlider.addOnChangeListener((slider, value, fromUser) -> {
            updateDistanceLabels();
            if (fromUser) {
                persistDistances(false);
            }
        });

        container.addView(unlockDistanceRow);
        container.addView(lockDistanceRow);
        setDistanceValues(true);
    }

    private void setDistanceValues(boolean useDefaults) {
        if (unlockDistanceSlider == null || lockDistanceSlider == null) {
            return;
        }
        float unlock = useDefaults || prefs == null
                ? ConfigKeys.DEFAULT_PROXIMITY_UNLOCK_METERS
                : prefs.getFloat(
                        ConfigKeys.KEY_PROXIMITY_UNLOCK_METERS,
                        ConfigKeys.DEFAULT_PROXIMITY_UNLOCK_METERS
                );
        float lock = useDefaults || prefs == null
                ? ConfigKeys.DEFAULT_PROXIMITY_LOCK_METERS
                : prefs.getFloat(
                        ConfigKeys.KEY_PROXIMITY_LOCK_METERS,
                        ConfigKeys.DEFAULT_PROXIMITY_LOCK_METERS
                );
        ProximityPolicy.Distances distances = ProximityPolicy.normalize(unlock, lock);
        unlockDistanceSlider.setValue(distances.unlockMeters);
        lockDistanceSlider.setValue(distances.lockMeters);
        updateDistanceLabels();
    }

    private void persistDistances(boolean unlockChanged) {
        if (refreshingSwitches || prefs == null) {
            return;
        }
        float unlock = unlockDistanceSlider.getValue();
        float lock = lockDistanceSlider.getValue();
        if (unlockChanged && lock < unlock + ProximityPolicy.STEP_METERS) {
            lock = Math.min(
                    ProximityPolicy.MAX_METERS,
                    unlock + ProximityPolicy.STEP_METERS
            );
        } else if (!unlockChanged && lock < unlock + ProximityPolicy.STEP_METERS) {
            unlock = Math.max(
                    ProximityPolicy.MIN_METERS,
                    lock - ProximityPolicy.STEP_METERS
            );
        }
        ProximityPolicy.Distances distances = ProximityPolicy.normalize(unlock, lock);
        unlockDistanceSlider.setValue(distances.unlockMeters);
        lockDistanceSlider.setValue(distances.lockMeters);
        updateDistanceLabels();
        try {
            prefs.edit()
                    .putFloat(ConfigKeys.KEY_PROXIMITY_UNLOCK_METERS, distances.unlockMeters)
                    .putFloat(ConfigKeys.KEY_PROXIMITY_LOCK_METERS, distances.lockMeters)
                    .apply();
        } catch (RuntimeException error) {
            handleRemotePreferencesError(error);
        }
    }

    private void updateDistanceLabels() {
        if (unlockDistanceValue == null || lockDistanceValue == null) {
            return;
        }
        unlockDistanceValue.setText(getString(
                R.string.distance_value,
                unlockDistanceSlider.getValue()
        ));
        lockDistanceValue.setText(getString(
                R.string.distance_value,
                lockDistanceSlider.getValue()
        ));
    }

    /**
     * 依据主开关与各自的父开关状态置灰从属项。不改动被置灰开关的存储值，
     * 因此父开关重新打开后子项恢复原状态。
     */
    private void applyGating() {
        boolean serviceReady = prefs != null;
        boolean masterOn = serviceReady
                && isChecked(ConfigKeys.KEY_ENABLE_MODULE, ConfigKeys.DEFAULT_ENABLE_MODULE);
        for (ToggleSpec spec : specs) {
            boolean enabled;
            if (ConfigKeys.KEY_ENABLE_MODULE.equals(spec.key)) {
                enabled = serviceReady;
            } else if (spec.dependsOn != null) {
                enabled = masterOn && isChecked(spec.dependsOn, true);
            } else {
                enabled = masterOn;
            }
            setRowEnabled(spec.key, enabled);
        }
        boolean distanceEnabled = masterOn && isChecked(
                ConfigKeys.KEY_OVERRIDE_PROXIMITY_DISTANCE,
                ConfigKeys.DEFAULT_OVERRIDE_PROXIMITY_DISTANCE
        );
        setDistanceRowsEnabled(distanceEnabled);
    }

    private void setDistanceRowsEnabled(boolean enabled) {
        if (unlockDistanceSlider == null || lockDistanceSlider == null) {
            return;
        }
        unlockDistanceSlider.setEnabled(enabled);
        lockDistanceSlider.setEnabled(enabled);
        unlockDistanceRow.setEnabled(enabled);
        lockDistanceRow.setEnabled(enabled);
        unlockDistanceRow.setAlpha(enabled ? 1.0f : 0.4f);
        lockDistanceRow.setAlpha(enabled ? 1.0f : 0.4f);
    }

    private boolean isChecked(String key, boolean def) {
        MaterialSwitch toggle = switches.get(key);
        if (toggle != null) {
            return toggle.isChecked();
        }
        return prefs == null ? def : prefs.getBoolean(key, def);
    }

    private void setRowEnabled(String key, boolean enabled) {
        View row = rows.get(key);
        MaterialSwitch toggle = switches.get(key);
        if (row == null || toggle == null) {
            return;
        }
        toggle.setEnabled(enabled);
        row.setEnabled(enabled);
        row.setClickable(enabled);
        row.setAlpha(enabled ? 1f : 0.4f);
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
        List<ToggleSpec> list = new ArrayList<>();
        list.add(new ToggleSpec(ConfigKeys.KEY_ENABLE_MODULE, R.string.switch_enable_module,
                R.string.desc_enable_module, ConfigKeys.DEFAULT_ENABLE_MODULE, Group.GENERAL, null));
        list.add(new ToggleSpec(ConfigKeys.KEY_STRICT_VERSION_GUARD, R.string.switch_strict_version_guard,
                R.string.desc_strict_version_guard, ConfigKeys.DEFAULT_STRICT_VERSION_GUARD, Group.GENERAL, null));

        list.add(new ToggleSpec(ConfigKeys.KEY_HOOK_SETUP_VIEW, R.string.switch_hook_setup_view,
                R.string.desc_hook_setup_view, ConfigKeys.DEFAULT_HOOK_SETUP_VIEW, Group.SPLASH, null));
        list.add(new ToggleSpec(ConfigKeys.KEY_HOOK_COUNT_DOWN, R.string.switch_hook_count_down,
                R.string.desc_hook_count_down, ConfigKeys.DEFAULT_HOOK_COUNT_DOWN, Group.SPLASH, null));
        list.add(new ToggleSpec(ConfigKeys.KEY_HOOK_CONFIG_BEAN, R.string.switch_hook_config_bean,
                R.string.desc_hook_config_bean, ConfigKeys.DEFAULT_HOOK_CONFIG_BEAN, Group.SPLASH, null));
        list.add(new ToggleSpec(ConfigKeys.KEY_FORCE_EMPTY_RES, R.string.switch_force_empty_res,
                R.string.desc_force_empty_res, ConfigKeys.DEFAULT_FORCE_EMPTY_RES, Group.SPLASH,
                ConfigKeys.KEY_HOOK_CONFIG_BEAN));
        list.add(new ToggleSpec(ConfigKeys.KEY_FORCE_DURATION_ZERO, R.string.switch_force_duration_zero,
                R.string.desc_force_duration_zero, ConfigKeys.DEFAULT_FORCE_DURATION_ZERO, Group.SPLASH,
                ConfigKeys.KEY_HOOK_CONFIG_BEAN));

        list.add(new ToggleSpec(ConfigKeys.KEY_FORCE_EMPTY_BANNER, R.string.switch_force_empty_banner,
                R.string.desc_force_empty_banner, ConfigKeys.DEFAULT_FORCE_EMPTY_BANNER, Group.HOME,
                ConfigKeys.KEY_HOOK_CONFIG_BEAN));
        list.add(new ToggleSpec(ConfigKeys.KEY_HOOK_APP_UPDATE, R.string.switch_hook_app_update,
                R.string.desc_hook_app_update, ConfigKeys.DEFAULT_HOOK_APP_UPDATE, Group.HOME, null));
        list.add(new ToggleSpec(ConfigKeys.KEY_SIMPLIFY_HOME_NAV, R.string.switch_simplify_home_nav,
                R.string.desc_simplify_home_nav, ConfigKeys.DEFAULT_SIMPLIFY_HOME_NAV, Group.HOME, null));
        list.add(new ToggleSpec(ConfigKeys.KEY_ENABLE_VEHICLE_DIAGNOSTICS,
                R.string.switch_vehicle_diagnostics, R.string.desc_vehicle_diagnostics,
                ConfigKeys.DEFAULT_ENABLE_VEHICLE_DIAGNOSTICS, Group.HOME, null));

        list.add(new ToggleSpec(ConfigKeys.KEY_BLOCK_USAGE_REPORT, R.string.switch_block_usage_report,
                R.string.desc_block_usage_report, ConfigKeys.DEFAULT_BLOCK_USAGE_REPORT,
                Group.PRIVACY, null));
        list.add(new ToggleSpec(ConfigKeys.KEY_BLOCK_BUGLY, R.string.switch_block_bugly,
                R.string.desc_block_bugly, ConfigKeys.DEFAULT_BLOCK_BUGLY, Group.PRIVACY, null));

        list.add(new ToggleSpec(ConfigKeys.KEY_ENABLE_TRACK_EXPORT, R.string.switch_track_export,
                R.string.desc_track_export, ConfigKeys.DEFAULT_ENABLE_TRACK_EXPORT, Group.DATA, null));
        list.add(new ToggleSpec(ConfigKeys.KEY_TRIM_TRACK_ENDPOINTS,
                R.string.switch_trim_track_endpoints, R.string.desc_trim_track_endpoints,
                ConfigKeys.DEFAULT_TRIM_TRACK_ENDPOINTS, Group.DATA,
                ConfigKeys.KEY_ENABLE_TRACK_EXPORT));

        list.add(new ToggleSpec(ConfigKeys.KEY_OVERRIDE_PROXIMITY_DISTANCE,
                R.string.switch_override_proximity, R.string.desc_override_proximity,
                ConfigKeys.DEFAULT_OVERRIDE_PROXIMITY_DISTANCE, Group.PROXIMITY, null));

        list.add(new ToggleSpec(ConfigKeys.KEY_VERBOSE_LOG, R.string.switch_verbose_log,
                R.string.desc_verbose_log, ConfigKeys.DEFAULT_VERBOSE_LOG, Group.DEBUG, null));
        return list;
    }

    private enum Group {
        GENERAL(R.string.group_general),
        SPLASH(R.string.group_splash),
        HOME(R.string.group_home),
        PRIVACY(R.string.group_privacy),
        DATA(R.string.group_data),
        PROXIMITY(R.string.group_proximity),
        DEBUG(R.string.group_debug);

        final int titleRes;

        Group(int titleRes) {
            this.titleRes = titleRes;
        }
    }

    private static final class ToggleSpec {
        final String key;
        final int titleRes;
        final int descRes;
        final boolean def;
        final Group group;
        /** 该开关的父开关 key；父开关关闭时本项置灰。null 表示仅受主开关约束。 */
        final String dependsOn;

        ToggleSpec(String key, int titleRes, int descRes, boolean def, Group group, String dependsOn) {
            this.key = key;
            this.titleRes = titleRes;
            this.descRes = descRes;
            this.def = def;
            this.group = group;
            this.dependsOn = dependsOn;
        }
    }
}
