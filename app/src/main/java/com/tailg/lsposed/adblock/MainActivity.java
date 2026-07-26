package com.tailg.lsposed.adblock;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/** Launcher that asks the injected host process to display its local settings panel. */
public final class MainActivity extends Activity {
    static final String TARGET_PACKAGE = "com.tailg.run.intelligence";
    static final String HOME_ACTIVITY =
            "com.tailg.run.intelligence.model.home.activity.HomeActivity";
    static final String EXTRA_OPEN_TOOLBOX =
            "com.tailg.lsposed.adblock.OPEN_TOOLBOX";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(Intent.ACTION_MAIN)
                .setComponent(new ComponentName(TARGET_PACKAGE, HOME_ACTIVITY))
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_OPEN_TOOLBOX, true);
        try {
            startActivity(intent);
        } catch (RuntimeException error) {
            Toast.makeText(this, "无法打开台铃官方 App", Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
