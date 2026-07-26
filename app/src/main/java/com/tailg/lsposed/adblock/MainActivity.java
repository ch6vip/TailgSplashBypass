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
        if (hasLegacyValues) {
            editor.apply();
        }
    }

    private void refreshSwitches() {
        refreshingSwitches = true;
        try {
            setSwitchValues(false);
        } catch (RuntimeException e) {
            Log.w(TAG, "Read remote preferences failed", e);
            prefs = null;
            serviceStatus.setText(R.string.service_status_error);
            setSwitchValues(true);
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
            container.addView(section);
        }
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

        list.add(new ToggleSpec(ConfigKeys.KEY_VERBOSE_LOG, R.string.switch_verbose_log,
                R.string.desc_verbose_log, ConfigKeys.DEFAULT_VERBOSE_LOG, Group.DEBUG, null));
        return list;
    }

    private enum Group {
        GENERAL(R.string.group_general),
        SPLASH(R.string.group_splash),
        HOME(R.string.group_home),
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
