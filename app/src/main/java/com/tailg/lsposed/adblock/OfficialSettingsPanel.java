package com.tailg.lsposed.adblock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
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
    private static final int BLE_INTERVAL_PROGRESS_MAX =
            (BleReconnectPolicy.MAX_INTERVAL_SECONDS
                    - BleReconnectPolicy.MIN_INTERVAL_SECONDS)
                    / BleReconnectPolicy.INTERVAL_STEP_SECONDS;
    private static final int BLE_ATTEMPTS_PROGRESS_MAX =
            BleReconnectPolicy.MAX_ATTEMPTS - BleReconnectPolicy.MIN_ATTEMPTS;
    private static final Map<Activity, WeakReference<Dialog>> OPEN_DIALOGS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final Activity activity;
    private final MMKV preferences;
    private final MaterialPalette palette;
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
    private SeekBar reconnectIntervalSeekBar;
    private SeekBar reconnectAttemptsSeekBar;
    private TextView reconnectIntervalValue;
    private TextView reconnectAttemptsValue;
    private View reconnectIntervalRow;
    private View reconnectAttemptsRow;

    private OfficialSettingsPanel(Activity activity) {
        this.activity = activity;
        this.preferences = HostConfigStore.open(activity);
        this.palette = MaterialPalette.create(activity);
    }

    static void show(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        WeakReference<Dialog> existingReference = OPEN_DIALOGS.get(activity);
        Dialog existing = existingReference == null ? null : existingReference.get();
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

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window initialWindow = dialog.getWindow();
        if (initialWindow != null) {
            initialWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(panel.createContent(dialog));
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(ignored -> OPEN_DIALOGS.remove(activity));
        OPEN_DIALOGS.put(activity, new WeakReference<>(dialog));
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.46f;
            attributes.gravity = Gravity.CENTER;
            window.setAttributes(attributes);

            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
            int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
            int width = Math.min(screenWidth - panel.dp(24), panel.dp(600));
            int height = Math.min(screenHeight - panel.dp(32), panel.dp(760));
            window.setLayout(Math.max(width, panel.dp(280)), Math.max(height, panel.dp(420)));
        }
    }

    private View createContent(Dialog dialog) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(roundedBackground(palette.surface, 8));
        root.setClipToOutline(true);
        root.setElevation(dp(12));

        root.addView(createTopBar(dialog), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(createDivider(0), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
        ));

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), 0, dp(12), dp(16));

        for (Group group : Group.values()) {
            List<ToggleSpec> groupSpecs = specsForGroup(group);
            if (groupSpecs.isEmpty()) {
                continue;
            }
            content.addView(createGroup(group, groupSpecs));
        }

        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        ));
        root.addView(createDivider(0), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
        ));

        TextView footer = new TextView(activity);
        footer.setText("设置自动保存 · 重启官方 App 后生效");
        footer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
        footer.setTextColor(palette.onSurfaceVariant);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(20), dp(10), dp(20), dp(14));
        footer.setLetterSpacing(0.0f);
        root.addView(footer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        refreshFromPreferences();
        return root;
    }

    private View createTopBar(Dialog dialog) {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(20), dp(14), dp(12), dp(12));

        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(activity);
        title.setText("Tailg 工具箱");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24.0f);
        title.setTextColor(palette.onSurface);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLetterSpacing(0.0f);
        labels.addView(title);

        TextView subtitle = new TextView(activity);
        subtitle.setText("台铃官方 App 3.5.9");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.0f);
        subtitle.setTextColor(palette.onSurfaceVariant);
        subtitle.setLetterSpacing(0.0f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.topMargin = dp(2);
        labels.addView(subtitle, subtitleParams);

        bar.addView(labels, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        ));

        ImageButton close = new ImageButton(activity);
        close.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        close.setColorFilter(palette.onSurfaceVariant, PorterDuff.Mode.SRC_IN);
        close.setBackground(iconRippleBackground());
        close.setContentDescription("关闭");
        close.setTooltipText("关闭");
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setOnClickListener(view -> dialog.dismiss());
        bar.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        return bar;
    }

    private View createGroup(Group group, List<ToggleSpec> groupSpecs) {
        LinearLayout section = new LinearLayout(activity);
        section.setOrientation(LinearLayout.VERTICAL);
        int containerColor = group == Group.GENERAL
                ? palette.surfaceContainerHigh
                : palette.surfaceContainer;
        section.setBackground(roundedBackground(containerColor, 8));
        section.setClipToOutline(true);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sectionParams.topMargin = dp(12);
        section.setLayoutParams(sectionParams);

        TextView title = new TextView(activity);
        title.setText(group.title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.0f);
        title.setTextColor(palette.primary);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLetterSpacing(0.0f);
        title.setPadding(dp(16), dp(14), dp(16), dp(10));
        section.addView(title);
        section.addView(createDivider(dp(16)));

        for (int index = 0; index < groupSpecs.size(); index++) {
            if (index > 0) {
                section.addView(createDivider(dp(16)));
            }
            section.addView(createToggleRow(groupSpecs.get(index)));
        }
        if (group == Group.HIDDEN) {
            section.addView(createDivider(dp(16)));
            section.addView(createBatteryInfoActionRow());
        }
        if (group == Group.PROXIMITY) {
            section.addView(createDivider(dp(16)));
            unlockRow = createDistanceRow(true);
            lockRow = createDistanceRow(false);
            section.addView(unlockRow);
            section.addView(createDivider(dp(16)));
            section.addView(lockRow);
        }
        if (group == Group.BLE) {
            section.addView(createDivider(dp(16)));
            reconnectIntervalRow = createReconnectParameterRow(true);
            reconnectAttemptsRow = createReconnectParameterRow(false);
            section.addView(reconnectIntervalRow);
            section.addView(createDivider(dp(16)));
            section.addView(reconnectAttemptsRow);
        }
        return section;
    }

    private View createToggleRow(ToggleSpec spec) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(72));
        row.setPadding(dp(16), dp(10), dp(14), dp(10));
        row.setBackground(rowRippleBackground());

        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(activity);
        title.setText(spec.title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
        title.setTextColor(palette.onSurface);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLetterSpacing(0.0f);
        title.setMaxLines(2);
        labels.addView(title);

        TextView description = new TextView(activity);
        description.setText(spec.description);
        description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.0f);
        description.setTextColor(palette.onSurfaceVariant);
        description.setLetterSpacing(0.0f);
        description.setMaxLines(3);
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
        labelsParams.rightMargin = dp(16);
        row.addView(labels, labelsParams);

        Switch toggle = new Switch(activity);
        toggle.setContentDescription(spec.title);
        toggle.setShowText(false);
        toggle.setSplitTrack(false);
        toggle.setSwitchMinWidth(dp(52));
        toggle.setThumbTextPadding(0);
        toggle.setThumbTintList(palette.switchThumbColors());
        toggle.setTrackTintList(palette.switchTrackColors());
        toggle.setPadding(0, 0, 0, 0);
        row.addView(toggle, new LinearLayout.LayoutParams(
                dp(52),
                dp(48)
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

    private View createBatteryInfoActionRow() {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(72));
        row.setPadding(dp(16), dp(10), dp(14), dp(10));
        row.setBackground(rowRippleBackground());

        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(activity);
        title.setText("电池信息");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
        title.setTextColor(palette.onSurface);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLetterSpacing(0.0f);
        title.setMaxLines(2);
        labels.addView(title);

        TextView description = new TextView(activity);
        description.setText("选择要打开的官方电池信息页");
        description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.0f);
        description.setTextColor(palette.onSurfaceVariant);
        description.setLetterSpacing(0.0f);
        description.setMaxLines(3);
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
        labelsParams.rightMargin = dp(16);
        row.addView(labels, labelsParams);

        TextView arrow = new TextView(activity);
        arrow.setText("›");
        arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30.0f);
        arrow.setTextColor(palette.onSurfaceVariant);
        arrow.setGravity(Gravity.CENTER);
        arrow.setLetterSpacing(0.0f);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(32), dp(48)));

        row.setContentDescription("电池信息");
        row.setOnClickListener(view -> showBatteryInfoChooser());
        return row;
    }

    private void showBatteryInfoChooser() {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        Dialog chooser = new Dialog(activity);
        chooser.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window initialWindow = chooser.getWindow();
        if (initialWindow != null) {
            initialWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(roundedBackground(palette.surface, 8));
        root.setClipToOutline(true);
        root.setElevation(dp(12));
        root.addView(createBatteryChooserHeader(chooser));

        ScrollView choicesScroll = new ScrollView(activity);
        choicesScroll.setFillViewport(true);
        choicesScroll.setClipToPadding(false);
        LinearLayout choices = new LinearLayout(activity);
        choices.setOrientation(LinearLayout.VERTICAL);

        Switch forceOpen = new Switch(activity);
        choices.addView(createDivider(dp(16)));
        choices.addView(createForceOpenRow(forceOpen));

        addBatteryChoice(
                choices,
                chooser,
                forceOpen,
                "自动选择",
                "按官方车型规则匹配",
                null
        );
        addBatteryChoice(
                choices,
                chooser,
                forceOpen,
                "普通电池信息",
                "车型 1/2 或 GPS 车辆",
                BatteryInfoRoutePolicy.Route.NORMAL
        );
        addBatteryChoice(
                choices,
                chooser,
                forceOpen,
                "C39 电池信息",
                "车型 10/14",
                BatteryInfoRoutePolicy.Route.C39
        );
        addBatteryChoice(
                choices,
                chooser,
                forceOpen,
                "TLV 电池信息",
                "电池类型 160/208",
                BatteryInfoRoutePolicy.Route.TLV
        );
        addBatteryChoice(
                choices,
                chooser,
                forceOpen,
                "BMS 电池信息",
                "电池类型 176",
                BatteryInfoRoutePolicy.Route.BMS
        );
        addBatteryChoice(
                choices,
                chooser,
                forceOpen,
                "电池动态",
                "历史电压与温度曲线",
                BatteryInfoRoutePolicy.Route.DYNAMICS
        );
        choicesScroll.addView(choices, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(choicesScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        ));

        chooser.setContentView(root);
        chooser.setCanceledOnTouchOutside(true);
        chooser.show();

        Window window = chooser.getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.32f;
            attributes.gravity = Gravity.CENTER;
            window.setAttributes(attributes);

            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
            int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
            int width = Math.min(screenWidth - dp(48), dp(480));
            int availableHeight = Math.max(screenHeight - dp(48), dp(240));
            int height = Math.min(availableHeight, dp(440));
            window.setLayout(Math.max(width, dp(280)), height);
        }
    }

    private View createBatteryChooserHeader(Dialog chooser) {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(20), dp(12), dp(12), dp(10));

        TextView title = new TextView(activity);
        title.setText("选择电池信息页面");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20.0f);
        title.setTextColor(palette.onSurface);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLetterSpacing(0.0f);
        bar.addView(title, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        ));

        ImageButton close = new ImageButton(activity);
        close.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        close.setColorFilter(palette.onSurfaceVariant, PorterDuff.Mode.SRC_IN);
        close.setBackground(iconRippleBackground());
        close.setContentDescription("关闭");
        close.setTooltipText("关闭");
        close.setPadding(dp(10), dp(10), dp(10), dp(10));
        close.setOnClickListener(view -> chooser.dismiss());
        bar.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));
        return bar;
    }

    private void addBatteryChoice(
            LinearLayout target,
            Dialog chooser,
            Switch forceOpen,
            String title,
            String description,
            BatteryInfoRoutePolicy.Route route
    ) {
        target.addView(createDivider(dp(16)));
        target.addView(createBatteryChoiceRow(
                chooser,
                forceOpen,
                title,
                description,
                route
        ));
    }

    private View createBatteryChoiceRow(
            Dialog chooser,
            Switch forceOpen,
            String titleText,
            String descriptionText,
            BatteryInfoRoutePolicy.Route route
    ) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(64));
        row.setPadding(dp(16), dp(8), dp(14), dp(8));
        row.setBackground(rowRippleBackground());

        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(activity);
        title.setText(titleText);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.0f);
        title.setTextColor(palette.onSurface);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLetterSpacing(0.0f);
        labels.addView(title);

        TextView description = new TextView(activity);
        description.setText(descriptionText);
        description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
        description.setTextColor(palette.onSurfaceVariant);
        description.setLetterSpacing(0.0f);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        descriptionParams.topMargin = dp(1);
        labels.addView(description, descriptionParams);

        LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        labelsParams.rightMargin = dp(12);
        row.addView(labels, labelsParams);

        TextView arrow = new TextView(activity);
        arrow.setText("›");
        arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 27.0f);
        arrow.setTextColor(palette.onSurfaceVariant);
        arrow.setGravity(Gravity.CENTER);
        arrow.setLetterSpacing(0.0f);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(30), dp(44)));

        row.setContentDescription(titleText);
        row.setOnClickListener(view -> {
            boolean opened = route == null
                    ? BatteryInfoShortcutController.open(activity, activity.getClassLoader())
                    : BatteryInfoShortcutController.open(
                            activity,
                            activity.getClassLoader(),
                            route,
                            forceOpen.isChecked()
                    );
            if (opened) {
                chooser.dismiss();
                dismissSettingsPanel();
            }
        });
        return row;
    }

    private View createForceOpenRow(Switch toggle) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(72));
        row.setPadding(dp(16), dp(8), dp(14), dp(8));
        row.setBackground(rowRippleBackground());

        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(activity);
        title.setText("强制打开");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.0f);
        title.setTextColor(palette.onSurface);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLetterSpacing(0.0f);
        labels.addView(title);

        TextView description = new TextView(activity);
        description.setText("跳过兼容校验，页面可能空白或报错");
        description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
        description.setTextColor(palette.onSurfaceVariant);
        description.setLetterSpacing(0.0f);
        description.setMaxLines(2);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        descriptionParams.topMargin = dp(1);
        labels.addView(description, descriptionParams);

        LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        labelsParams.rightMargin = dp(16);
        row.addView(labels, labelsParams);

        toggle.setContentDescription("强制打开");
        toggle.setShowText(false);
        toggle.setSplitTrack(false);
        toggle.setSwitchMinWidth(dp(52));
        toggle.setThumbTextPadding(0);
        toggle.setThumbTintList(palette.switchThumbColors());
        toggle.setTrackTintList(palette.switchTrackColors());
        toggle.setPadding(0, 0, 0, 0);
        row.addView(toggle, new LinearLayout.LayoutParams(dp(52), dp(48)));

        row.setOnClickListener(view -> toggle.toggle());
        return row;
    }

    private void dismissSettingsPanel() {
        WeakReference<Dialog> reference = OPEN_DIALOGS.get(activity);
        Dialog panel = reference == null ? null : reference.get();
        if (panel != null && panel.isShowing()) {
            panel.dismiss();
        }
    }

    private View createDistanceRow(boolean unlock) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(10));

        LinearLayout heading = new LinearLayout(activity);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(activity);
        title.setText(unlock ? "靠近解锁距离" : "远离落锁距离");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.0f);
        title.setTextColor(palette.onSurface);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLetterSpacing(0.0f);
        heading.addView(title, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        ));

        TextView value = new TextView(activity);
        value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f);
        value.setTextColor(palette.primary);
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.setLetterSpacing(0.0f);
        value.setGravity(Gravity.END);
        value.setMinWidth(dp(64));
        heading.addView(value);
        row.addView(heading);

        TextView description = new TextView(activity);
        description.setText(unlock
                ? "旧 BleConnectService 使用的解锁阈值"
                : "至少比解锁阈值大 0.5 米");
        description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        description.setTextColor(palette.onSurfaceVariant);
        description.setLetterSpacing(0.0f);
        row.addView(description);

        SeekBar seekBar = new SeekBar(activity);
        seekBar.setMax(DISTANCE_PROGRESS_MAX);
        seekBar.setContentDescription(unlock ? "靠近解锁距离" : "远离落锁距离");
        seekBar.setSplitTrack(false);
        seekBar.setProgressTintList(ColorStateList.valueOf(palette.primary));
        seekBar.setProgressBackgroundTintList(
                ColorStateList.valueOf(palette.outlineVariant)
        );
        seekBar.setThumbTintList(ColorStateList.valueOf(palette.primary));
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
                dp(44)
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

    private View createReconnectParameterRow(boolean interval) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(10));

        LinearLayout heading = new LinearLayout(activity);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(activity);
        title.setText(interval ? "重试间隔" : "最大尝试次数");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.0f);
        title.setTextColor(palette.onSurface);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLetterSpacing(0.0f);
        heading.addView(title, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        ));

        TextView value = new TextView(activity);
        value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f);
        value.setTextColor(palette.primary);
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.setLetterSpacing(0.0f);
        value.setGravity(Gravity.END);
        value.setMinWidth(dp(64));
        heading.addView(value);
        row.addView(heading);

        TextView description = new TextView(activity);
        description.setText(interval
                ? "每轮恢复检查之间的等待时间"
                : "单次触发最多执行的恢复轮数");
        description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        description.setTextColor(palette.onSurfaceVariant);
        description.setLetterSpacing(0.0f);
        row.addView(description);

        SeekBar seekBar = new SeekBar(activity);
        seekBar.setMax(interval
                ? BLE_INTERVAL_PROGRESS_MAX
                : BLE_ATTEMPTS_PROGRESS_MAX);
        seekBar.setContentDescription(interval ? "蓝牙重试间隔" : "蓝牙最大尝试次数");
        seekBar.setSplitTrack(false);
        seekBar.setProgressTintList(ColorStateList.valueOf(palette.primary));
        seekBar.setProgressBackgroundTintList(
                ColorStateList.valueOf(palette.outlineVariant)
        );
        seekBar.setThumbTintList(ColorStateList.valueOf(palette.primary));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                updateReconnectLabels();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                if (!refreshing) {
                    persistReconnectParameters();
                }
            }
        });
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
        );
        seekParams.topMargin = dp(4);
        row.addView(seekBar, seekParams);

        if (interval) {
            reconnectIntervalSeekBar = seekBar;
            reconnectIntervalValue = value;
        } else {
            reconnectAttemptsSeekBar = seekBar;
            reconnectAttemptsValue = value;
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
            setReconnectProgress(
                    BleReconnectPolicy.normalizeIntervalSeconds(
                            preferences.getInt(
                                    ConfigKeys.KEY_BLE_RECONNECT_INTERVAL_SECONDS,
                                    ConfigKeys.DEFAULT_BLE_RECONNECT_INTERVAL_SECONDS
                            )
                    ),
                    BleReconnectPolicy.normalizeMaxAttempts(
                            preferences.getInt(
                                    ConfigKeys.KEY_BLE_RECONNECT_MAX_ATTEMPTS,
                                    ConfigKeys.DEFAULT_BLE_RECONNECT_MAX_ATTEMPTS
                            )
                    )
            );
        } catch (RuntimeException error) {
            Log.w(TAG, "Read host settings failed", error);
            Toast.makeText(activity, "读取配置失败", Toast.LENGTH_SHORT).show();
        } finally {
            refreshing = false;
        }
        updateDistanceLabels();
        updateReconnectLabels();
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

    private void persistReconnectParameters() {
        if (reconnectIntervalSeekBar == null || reconnectAttemptsSeekBar == null) {
            return;
        }
        int interval = BleReconnectPolicy.MIN_INTERVAL_SECONDS
                + reconnectIntervalSeekBar.getProgress()
                * BleReconnectPolicy.INTERVAL_STEP_SECONDS;
        int attempts = BleReconnectPolicy.MIN_ATTEMPTS
                + reconnectAttemptsSeekBar.getProgress();
        try {
            boolean intervalSaved = preferences.encode(
                    ConfigKeys.KEY_BLE_RECONNECT_INTERVAL_SECONDS,
                    interval
            );
            boolean attemptsSaved = preferences.encode(
                    ConfigKeys.KEY_BLE_RECONNECT_MAX_ATTEMPTS,
                    attempts
            );
            if (!intervalSaved || !attemptsSaved) {
                throw new IllegalStateException("MMKV encode returned false");
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "Write BLE recovery settings failed", error);
            Toast.makeText(activity, "保存蓝牙恢复配置失败", Toast.LENGTH_SHORT).show();
            refreshFromPreferences();
        }
    }

    private void setReconnectProgress(int interval, int attempts) {
        if (reconnectIntervalSeekBar == null || reconnectAttemptsSeekBar == null) {
            return;
        }
        boolean previousRefreshing = refreshing;
        refreshing = true;
        reconnectIntervalSeekBar.setProgress(
                (interval - BleReconnectPolicy.MIN_INTERVAL_SECONDS)
                        / BleReconnectPolicy.INTERVAL_STEP_SECONDS
        );
        reconnectAttemptsSeekBar.setProgress(
                attempts - BleReconnectPolicy.MIN_ATTEMPTS
        );
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

    private void updateReconnectLabels() {
        if (reconnectIntervalValue == null || reconnectAttemptsValue == null
                || reconnectIntervalSeekBar == null || reconnectAttemptsSeekBar == null) {
            return;
        }
        int interval = BleReconnectPolicy.MIN_INTERVAL_SECONDS
                + reconnectIntervalSeekBar.getProgress()
                * BleReconnectPolicy.INTERVAL_STEP_SECONDS;
        int attempts = BleReconnectPolicy.MIN_ATTEMPTS
                + reconnectAttemptsSeekBar.getProgress();
        reconnectIntervalValue.setText(interval + " 秒");
        reconnectAttemptsValue.setText(attempts + " 次");
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
        boolean reconnectEnabled = masterEnabled && isChecked(ConfigKeys.KEY_BLE_RECONNECT);
        setDistanceEnabled(
                reconnectIntervalRow,
                reconnectIntervalSeekBar,
                reconnectEnabled
        );
        setDistanceEnabled(
                reconnectAttemptsRow,
                reconnectAttemptsSeekBar,
                reconnectEnabled
        );
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
        add(result, ConfigKeys.KEY_FAST_STARTUP, "极速启动模式",
                "延迟 X5 与客服 SDK 到首次使用", ConfigKeys.DEFAULT_FAST_STARTUP,
                Group.HOME, null);
        add(result, ConfigKeys.KEY_SIMPLIFY_HOME_NAV, "精简首页导航",
                "隐藏圈子和商城", ConfigKeys.DEFAULT_SIMPLIFY_HOME_NAV,
                Group.HOME, null);
        add(result, ConfigKeys.KEY_SWAP_CONTROL_SERVICE_NAV, "爱车与服务互换位置",
                "将服务移到爱车前，点击功能保持不变",
                ConfigKeys.DEFAULT_SWAP_CONTROL_SERVICE_NAV, Group.HOME, null);
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

        add(result, ConfigKeys.KEY_ENABLE_MONTHLY_RIDE_DATA, "月度骑行数据页",
                "放开首页月度详情入口，保留官方数据分流",
                ConfigKeys.DEFAULT_ENABLE_MONTHLY_RIDE_DATA, Group.HIDDEN, null);
        add(result, ConfigKeys.KEY_SHOW_BRAKE_FORCE_DATA, "骑行记录制动力",
                "在官方轨迹回放显示制动力，无数据时可能为空",
                ConfigKeys.DEFAULT_SHOW_BRAKE_FORCE_DATA, Group.HIDDEN, null);
        add(result, ConfigKeys.KEY_SHOW_BATTERY_DYNAMICS_ENTRY, "电池动态页面入口",
                "在主车设置中补充官方电池历史页",
                ConfigKeys.DEFAULT_SHOW_BATTERY_DYNAMICS_ENTRY, Group.HIDDEN, null);
        add(result, ConfigKeys.KEY_SHOW_CUSTOM_VEHICLE_SOUND, "自定义车辆音效",
                "仅车辆上报支持时补充官方音效入口",
                ConfigKeys.DEFAULT_SHOW_CUSTOM_VEHICLE_SOUND, Group.HIDDEN, null);

        add(result, ConfigKeys.KEY_OVERRIDE_PROXIMITY_DISTANCE, "覆盖旧版 RSSI 距离",
                "只修改旧 BleConnectService 阈值", ConfigKeys.DEFAULT_OVERRIDE_PROXIMITY_DISTANCE,
                Group.PROXIMITY, null);

        add(result, ConfigKeys.KEY_BLE_RECONNECT, "蓝牙连接恢复",
                "返回首页或蓝牙开启后有限重试", ConfigKeys.DEFAULT_BLE_RECONNECT,
                Group.BLE, null);

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

    private View createDivider(int horizontalInset) {
        View divider = new View(activity);
        divider.setBackgroundColor(palette.outlineVariant);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        params.leftMargin = horizontalInset;
        params.rightMargin = horizontalInset;
        divider.setLayoutParams(params);
        return divider;
    }

    private Drawable roundedBackground(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private Drawable rowRippleBackground() {
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        return new RippleDrawable(
                ColorStateList.valueOf(withAlpha(palette.primary, 32)),
                null,
                mask
        );
    }

    private Drawable iconRippleBackground() {
        GradientDrawable content = new GradientDrawable();
        content.setShape(GradientDrawable.OVAL);
        content.setColor(Color.TRANSPARENT);
        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.OVAL);
        mask.setColor(Color.WHITE);
        return new RippleDrawable(
                ColorStateList.valueOf(withAlpha(palette.primary, 40)),
                content,
                mask
        );
    }

    private static int resolveThemeColor(Activity activity, int attribute, int fallback) {
        TypedValue value = new TypedValue();
        if (activity.getTheme().resolveAttribute(attribute, value, true)) {
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
        return fallback;
    }

    private static int blend(int from, int to, float toRatio) {
        float fromRatio = 1.0f - toRatio;
        return Color.rgb(
                Math.round(Color.red(from) * fromRatio + Color.red(to) * toRatio),
                Math.round(Color.green(from) * fromRatio + Color.green(to) * toRatio),
                Math.round(Color.blue(from) * fromRatio + Color.blue(to) * toRatio)
        );
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static float luminance(int color) {
        return (Color.red(color) * 0.2126f
                + Color.green(color) * 0.7152f
                + Color.blue(color) * 0.0722f) / 255.0f;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static final class MaterialPalette {
        final int primary;
        final int onPrimary;
        final int surface;
        final int surfaceContainer;
        final int surfaceContainerHigh;
        final int onSurface;
        final int onSurfaceVariant;
        final int outline;
        final int outlineVariant;

        MaterialPalette(
                int primary,
                int onPrimary,
                int surface,
                int surfaceContainer,
                int surfaceContainerHigh,
                int onSurface,
                int onSurfaceVariant,
                int outline,
                int outlineVariant
        ) {
            this.primary = primary;
            this.onPrimary = onPrimary;
            this.surface = surface;
            this.surfaceContainer = surfaceContainer;
            this.surfaceContainerHigh = surfaceContainerHigh;
            this.onSurface = onSurface;
            this.onSurfaceVariant = onSurfaceVariant;
            this.outline = outline;
            this.outlineVariant = outlineVariant;
        }

        static MaterialPalette create(Activity activity) {
            boolean dark = (activity.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            int fallbackPrimary = dark ? Color.rgb(91, 219, 198) : Color.rgb(0, 105, 92);
            int accent = resolveThemeColor(
                    activity,
                    android.R.attr.colorAccent,
                    fallbackPrimary
            );
            int primary = normalizePrimary(accent, dark);
            int onPrimary = luminance(primary) > 0.56f ? Color.BLACK : Color.WHITE;
            if (dark) {
                return new MaterialPalette(
                        primary,
                        onPrimary,
                        Color.rgb(16, 20, 19),
                        Color.rgb(27, 32, 30),
                        Color.rgb(37, 43, 40),
                        Color.rgb(225, 229, 226),
                        Color.rgb(190, 201, 195),
                        Color.rgb(137, 147, 142),
                        Color.rgb(63, 73, 68)
                );
            }
            return new MaterialPalette(
                    primary,
                    onPrimary,
                    Color.rgb(248, 250, 249),
                    Color.rgb(238, 242, 239),
                    Color.rgb(228, 233, 230),
                    Color.rgb(26, 28, 27),
                    Color.rgb(65, 72, 68),
                    Color.rgb(114, 122, 117),
                    Color.rgb(193, 201, 196)
            );
        }

        private static int normalizePrimary(int accent, boolean dark) {
            float value = luminance(accent);
            if (dark && value < 0.48f) {
                return blend(accent, Color.WHITE, 0.45f);
            }
            if (!dark && value > 0.52f) {
                return blend(accent, Color.BLACK, 0.34f);
            }
            return accent;
        }

        ColorStateList switchThumbColors() {
            int[][] states = {
                    {-android.R.attr.state_enabled, android.R.attr.state_checked},
                    {-android.R.attr.state_enabled, -android.R.attr.state_checked},
                    {android.R.attr.state_checked},
                    {}
            };
            int[] colors = {
                    withAlpha(onSurface, 96),
                    withAlpha(onSurface, 96),
                    onPrimary,
                    outline
            };
            return new ColorStateList(states, colors);
        }

        ColorStateList switchTrackColors() {
            int[][] states = {
                    {-android.R.attr.state_enabled, android.R.attr.state_checked},
                    {-android.R.attr.state_enabled, -android.R.attr.state_checked},
                    {android.R.attr.state_checked},
                    {}
            };
            int[] colors = {
                    withAlpha(onSurface, 48),
                    withAlpha(onSurface, 32),
                    primary,
                    outlineVariant
            };
            return new ColorStateList(states, colors);
        }
    }

    private enum Group {
        GENERAL("总控"),
        SPLASH("开屏广告"),
        HOME("首页与诊断"),
        BLE("车辆连接"),
        PRIVACY("隐私"),
        DATA("轨迹数据"),
        HIDDEN("隐藏功能"),
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
