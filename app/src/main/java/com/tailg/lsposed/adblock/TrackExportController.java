package com.tailg.lsposed.adblock;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class TrackExportController {
    private static final String TAG = "TailgTrackExport";
    private static final int MENU_GPX = 1;
    private static final int MENU_CSV = 2;
    private static final long EXPORT_RETENTION_MILLIS = 24L * 60L * 60L * 1000L;
    private static final Map<Activity, Boolean> INSTALLED =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final ExecutorService EXPORT_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "tailg-track-export");
        thread.setDaemon(true);
        return thread;
    });

    private TrackExportController() {
    }

    static void install(Activity activity, ClassLoader targetClassLoader, boolean trimEndpoints) {
        if (INSTALLED.put(activity, Boolean.TRUE) != null) {
            return;
        }
        try {
            Object binding = ReflectionAccess.getField(activity, "mBinding");
            Object deleteObject = ReflectionAccess.getField(binding, "tvDelete");
            if (!(deleteObject instanceof TextView deleteView)
                    || !(deleteView.getParent() instanceof ViewGroup parent)) {
                throw new IllegalStateException("Track delete view unavailable");
            }
            TextView exportView = createExportView(activity, deleteView);
            placeNextToDelete(activity, parent, deleteView, exportView);
            exportView.setOnClickListener(view -> showExportMenu(
                    activity,
                    view,
                    targetClassLoader,
                    trimEndpoints
            ));
        } catch (Throwable error) {
            INSTALLED.remove(activity);
            Log.w(TAG, "Install export action failed", error);
        }
    }

    private static TextView createExportView(Activity activity, TextView reference) {
        TextView export = new TextView(activity);
        export.setText("导出");
        export.setContentDescription("导出轨迹");
        export.setGravity(Gravity.CENTER);
        export.setTextColor(reference.getCurrentTextColor());
        export.setTextSize(TypedValue.COMPLEX_UNIT_PX, reference.getTextSize());
        export.setMinWidth(dp(activity, 48));
        export.setMinHeight(dp(activity, 48));

        TypedValue selectable = new TypedValue();
        if (activity.getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground,
                selectable,
                true
        ) && selectable.resourceId != 0) {
            export.setBackgroundResource(selectable.resourceId);
        }
        return export;
    }

    private static void placeNextToDelete(
            Activity activity,
            ViewGroup parent,
            TextView deleteView,
            TextView exportView
    ) {
        int index = parent.indexOfChild(deleteView);
        ViewGroup.LayoutParams originalParams = deleteView.getLayoutParams();
        parent.removeView(deleteView);

        LinearLayout actions = new LinearLayout(activity);
        actions.setId(View.generateViewId());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        originalParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;

        int actionWidth = dp(activity, 48);
        LinearLayout.LayoutParams childParams = new LinearLayout.LayoutParams(
                actionWidth,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        actions.addView(exportView, childParams);
        actions.addView(deleteView, new LinearLayout.LayoutParams(
                actionWidth,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        parent.addView(actions, index, originalParams);
    }

    private static void showExportMenu(
            Activity activity,
            View anchor,
            ClassLoader targetClassLoader,
            boolean trimEndpoints
    ) {
        PopupMenu popup = new PopupMenu(activity, anchor);
        popup.getMenu().add(Menu.NONE, MENU_GPX, Menu.NONE, "导出 GPX");
        popup.getMenu().add(Menu.NONE, MENU_CSV, Menu.NONE, "导出 CSV");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_GPX) {
                export(activity, targetClassLoader, ExportFormat.GPX, trimEndpoints);
                return true;
            }
            if (item.getItemId() == MENU_CSV) {
                export(activity, targetClassLoader, ExportFormat.CSV, trimEndpoints);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private static void export(
            Activity activity,
            ClassLoader targetClassLoader,
            ExportFormat format,
            boolean trimEndpoints
    ) {
        final List<TrackPoint> sourcePoints;
        try {
            sourcePoints = readTrackPoints(activity);
        } catch (Throwable error) {
            Log.w(TAG, "Read track data failed", error);
            toast(activity, "无法读取当前轨迹");
            return;
        }
        if (sourcePoints.size() < 2) {
            toast(activity, "当前轨迹没有足够的有效坐标");
            return;
        }

        List<TrackPoint> exportPoints = trimEndpoints
                ? TrackPrivacy.trimEndpoints(
                        sourcePoints,
                        TrackPrivacy.DEFAULT_ENDPOINT_RADIUS_METERS
                )
                : new ArrayList<>(sourcePoints);
        if (exportPoints.size() < 2) {
            toast(activity, "轨迹过短，无法在隐藏首尾位置后导出");
            return;
        }

        toast(activity, "正在准备轨迹文件");
        EXPORT_EXECUTOR.execute(() -> {
            try {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                        .format(new Date());
                String basename = "tailg_track_" + timestamp;
                String content = format == ExportFormat.GPX
                        ? TrackExportCodec.toGpx(exportPoints, basename)
                        : TrackExportCodec.toCsv(exportPoints);
                File file = writeExportFile(activity, basename + format.extension, content);
                Uri uri = createShareUri(activity, targetClassLoader, file);
                activity.runOnUiThread(() -> share(activity, uri, file.getName(), format.mimeType));
            } catch (Throwable error) {
                Log.w(TAG, "Export track failed", error);
                activity.runOnUiThread(() -> toast(activity, "轨迹导出失败"));
            }
        });
    }

    private static List<TrackPoint> readTrackPoints(Activity activity) throws ReflectiveOperationException {
        Object viewModel = ReflectionAccess.getField(activity, "mViewModel");
        Object observable = ReflectionAccess.getField(viewModel, "deviceTravelDetailListBean");
        Object rawList = ReflectionAccess.invokeNoArg(observable, "get");
        if (!(rawList instanceof List<?> list)) {
            return Collections.emptyList();
        }

        List<TrackPoint> points = new ArrayList<>(list.size());
        for (Object bean : list) {
            try {
                double latitude = Double.parseDouble(stringValue(bean, "getLat"));
                double longitude = Double.parseDouble(stringValue(bean, "getLng"));
                if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                        || latitude < -90.0d || latitude > 90.0d
                        || longitude < -180.0d || longitude > 180.0d
                        || (latitude == 0.0d && longitude == 0.0d)) {
                    continue;
                }
                points.add(new TrackPoint(
                        latitude,
                        longitude,
                        stringValue(bean, "getReportTime"),
                        stringValue(bean, "getSpeed"),
                        stringValue(bean, "getHeading"),
                        stringValue(bean, "getStarsNum")
                ));
            } catch (NumberFormatException ignored) {
                // Ignore malformed server points while preserving the rest of the route.
            }
        }
        return points;
    }

    private static String stringValue(Object target, String getter) throws ReflectiveOperationException {
        Object value = ReflectionAccess.invokeNoArg(target, getter);
        return value == null ? "" : String.valueOf(value);
    }

    private static File writeExportFile(Activity activity, String filename, String content)
            throws Exception {
        File externalFiles = activity.getExternalFilesDir(null);
        if (externalFiles == null) {
            throw new IllegalStateException("External files directory unavailable");
        }
        File exportDir = new File(externalFiles, "shareData/tailg_export");
        if (!exportDir.isDirectory() && !exportDir.mkdirs()) {
            throw new IllegalStateException("Cannot create export directory");
        }
        cleanupOldExports(exportDir);

        File exportFile = new File(exportDir, filename);
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(exportFile),
                StandardCharsets.UTF_8
        )) {
            writer.write(content);
        }
        return exportFile;
    }

    private static void cleanupOldExports(File exportDir) {
        File[] files = exportDir.listFiles();
        if (files == null) {
            return;
        }
        long cutoff = System.currentTimeMillis() - EXPORT_RETENTION_MILLIS;
        for (File file : files) {
            if (file.isFile() && file.getName().startsWith("tailg_track_")
                    && file.lastModified() < cutoff && !file.delete()) {
                Log.d(TAG, "Could not remove old export: " + file.getName());
            }
        }
    }

    private static Uri createShareUri(
            Activity activity,
            ClassLoader targetClassLoader,
            File file
    ) throws Exception {
        Class<?> fileProvider = Class.forName(
                "androidx.core.content.FileProvider",
                true,
                targetClassLoader
        );
        Method getUriForFile = fileProvider.getMethod(
                "getUriForFile",
                Context.class,
                String.class,
                File.class
        );
        Object result = getUriForFile.invoke(
                null,
                activity,
                activity.getPackageName() + ".fileprovider",
                file
        );
        if (!(result instanceof Uri uri)) {
            throw new IllegalStateException("FileProvider returned no URI");
        }
        return uri;
    }

    private static void share(Activity activity, Uri uri, String filename, String mimeType) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType(mimeType)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_TITLE, filename)
                .setClipData(ClipData.newUri(activity.getContentResolver(), filename, uri))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(Intent.createChooser(send, "导出轨迹"));
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static void toast(Activity activity, String message) {
        if (!activity.isFinishing()) {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        }
    }

    private enum ExportFormat {
        GPX(".gpx", "application/gpx+xml"),
        CSV(".csv", "text/csv");

        final String extension;
        final String mimeType;

        ExportFormat(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }
    }
}
