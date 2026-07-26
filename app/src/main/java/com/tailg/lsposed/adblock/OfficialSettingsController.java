package com.tailg.lsposed.adblock;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

final class OfficialSettingsController {
    private static final String TAG = "TailgSettingsEntry";
    private static final String MODULE_PACKAGE = "com.tailg.lsposed.adblock";
    private static final String ENTRY_TAG = MODULE_PACKAGE + ":official_settings_entry";
    private static final String DIVIDER_TAG = MODULE_PACKAGE + ":official_settings_divider";
    private static final int CONSTRAINT_PARENT_ID = 0;
    private static final int CONSTRAINT_UNSET = -1;

    private OfficialSettingsController() {
    }

    static void installRevisionEntry(Object fragment) {
        try {
            Object activityObject = ReflectionAccess.invokeNoArg(fragment, "getActivity");
            if (!(activityObject instanceof Activity activity)) {
                return;
            }
            Object binding = ReflectionAccess.getField(fragment, "mBinding");
            Object accountGroupObject = ReflectionAccess.getField(binding, "clAccount");
            Object accountRowObject = ReflectionAccess.getField(binding, "tvAccount");
            Object templateObject = ReflectionAccess.getField(binding, "tvLanguage");
            Object topBackgroundObject = ReflectionAccess.getField(binding, "tvClear");
            Object bottomBackgroundObject = ReflectionAccess.getField(binding, "tvVersion");
            if (!(accountGroupObject instanceof ViewGroup accountGroup)
                    || !(accountRowObject instanceof TextView accountRow)
                    || !(templateObject instanceof TextView template)
                    || !(topBackgroundObject instanceof TextView topBackground)
                    || !(bottomBackgroundObject instanceof TextView bottomBackground)) {
                return;
            }
            TextView row = findTaggedTextView(accountGroup, ENTRY_TAG);
            if (row == null) {
                row = createEntry(activity, accountRow, template);
                row.setTag(ENTRY_TAG);
                row.setBackground(copyDrawable(activity, topBackground.getBackground()));
                accountRow.setBackground(copyDrawable(activity, bottomBackground.getBackground()));
                accountGroup.addView(
                        row,
                        0,
                        new ViewGroup.LayoutParams(0, accountRow.getLayoutParams().height)
                );
                constrainToTop(row);
                constrainBelow(accountRow, row.getId());
            }
            configureEntry(activity, row);
            TextView postedRow = row;
            row.post(() -> configureEntry(activity, postedRow));
        } catch (Throwable error) {
            Log.w(TAG, "Install revised settings entry failed", error);
        }
    }

    static void installLegacyEntry(Activity activity) {
        try {
            Object binding = ReflectionAccess.getField(activity, "mBinding");
            Object titleObject = ReflectionAccess.getField(binding, "clTitle");
            Object accountObject = ReflectionAccess.getField(binding, "tvAccountManagement");
            Object templateObject = ReflectionAccess.getField(binding, "tvSystemInformation");
            Object dividerTemplateObject = ReflectionAccess.getField(binding, "vL1");
            if (!(titleObject instanceof View title)
                    || !(accountObject instanceof TextView accountRow)
                    || !(templateObject instanceof TextView template)
                    || !(dividerTemplateObject instanceof View dividerTemplate)
                    || !(accountRow.getParent() instanceof ViewGroup parent)) {
                return;
            }
            TextView row = findTaggedTextView(parent, ENTRY_TAG);
            View divider = findTaggedView(parent, DIVIDER_TAG);
            if (row == null) {
                row = createEntry(activity, accountRow, template);
                row.setTag(ENTRY_TAG);
                int accountIndex = parent.indexOfChild(accountRow);
                parent.addView(
                        row,
                        Math.max(0, accountIndex),
                        new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                accountRow.getLayoutParams().height
                        )
                );
                constrainBelow(row, title.getId());
            }
            if (divider == null) {
                divider = new View(activity);
                divider.setId(View.generateViewId());
                divider.setTag(DIVIDER_TAG);
                divider.setBackground(copyDrawable(activity, dividerTemplate.getBackground()));
                ViewGroup.MarginLayoutParams dividerParams = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dividerTemplate.getLayoutParams().height
                );
                if (dividerTemplate.getLayoutParams() instanceof ViewGroup.MarginLayoutParams source) {
                    dividerParams.leftMargin = source.leftMargin;
                    dividerParams.rightMargin = source.rightMargin;
                }
                parent.addView(divider, dividerParams);
                constrainBelow(divider, row.getId());
            }
            constrainBelow(accountRow, divider.getId());
            configureEntry(activity, row);
        } catch (Throwable error) {
            Log.w(TAG, "Install legacy settings entry failed", error);
        }
    }

    private static TextView createEntry(
            Activity activity,
            TextView reference,
            TextView iconTemplate
    ) {
        TextView row = new TextView(activity);
        row.setId(View.generateViewId());
        row.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, reference.getTextSize());
        row.setTextColor(reference.getTextColors());
        row.setGravity(reference.getGravity());
        row.setPadding(
                reference.getPaddingLeft(),
                reference.getPaddingTop(),
                reference.getPaddingRight(),
                reference.getPaddingBottom()
        );
        row.setCompoundDrawablePadding(reference.getCompoundDrawablePadding());
        row.setIncludeFontPadding(reference.getIncludeFontPadding());
        row.setBackground(copyDrawable(activity, reference.getBackground()));

        Drawable[] templateDrawables = iconTemplate.getCompoundDrawables();
        Drawable[] referenceDrawables = reference.getCompoundDrawables();
        row.setCompoundDrawablesWithIntrinsicBounds(
                copyDrawable(activity, templateDrawables[0]),
                copyDrawable(activity, referenceDrawables[1]),
                copyDrawable(activity, referenceDrawables[2]),
                copyDrawable(activity, referenceDrawables[3])
        );
        row.setClickable(true);
        row.setFocusable(true);
        return row;
    }

    private static TextView findTaggedTextView(ViewGroup parent, String tag) {
        View view = findTaggedView(parent, tag);
        return view instanceof TextView textView ? textView : null;
    }

    private static View findTaggedView(ViewGroup parent, String tag) {
        for (int index = 0; index < parent.getChildCount(); index++) {
            View child = parent.getChildAt(index);
            if (tag.equals(child.getTag())) {
                return child;
            }
        }
        return null;
    }

    private static void constrainToTop(View view) throws ReflectiveOperationException {
        Object params = view.getLayoutParams();
        ReflectionAccess.setField(params, "startToStart", CONSTRAINT_PARENT_ID);
        ReflectionAccess.setField(params, "endToEnd", CONSTRAINT_PARENT_ID);
        ReflectionAccess.setField(params, "topToTop", CONSTRAINT_PARENT_ID);
        ReflectionAccess.setField(params, "topToBottom", CONSTRAINT_UNSET);
        view.setLayoutParams((ViewGroup.LayoutParams) params);
    }

    private static void constrainBelow(View view, int anchorId)
            throws ReflectiveOperationException {
        Object params = view.getLayoutParams();
        ReflectionAccess.setField(params, "topToTop", CONSTRAINT_UNSET);
        ReflectionAccess.setField(params, "topToBottom", anchorId);
        if (view.getParent() instanceof ViewGroup) {
            ReflectionAccess.setField(params, "startToStart", CONSTRAINT_PARENT_ID);
            ReflectionAccess.setField(params, "endToEnd", CONSTRAINT_PARENT_ID);
        }
        view.setLayoutParams((ViewGroup.LayoutParams) params);
    }

    private static Drawable copyDrawable(Activity activity, Drawable source) {
        if (source == null || source.getConstantState() == null) {
            return source;
        }
        return source.getConstantState().newDrawable(activity.getResources()).mutate();
    }

    private static void configureEntry(Activity activity, TextView row) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        row.setText("Tailg 工具箱");
        row.setContentDescription("打开 Tailg LSPosed 工具箱");
        row.setVisibility(View.VISIBLE);
        row.setOnClickListener(view -> OfficialSettingsPanel.show(activity));
    }
}
