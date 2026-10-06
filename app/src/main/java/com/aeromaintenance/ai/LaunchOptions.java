package com.aeromaintenance.ai;

import android.content.Intent;

/**
 * Optional intent extras so a presenter (or the CI screenshot script) can open any
 * screen directly, for example:
 * <pre>
 * adb shell am start -n com.aeromaintenance.ai/.MainActivity --es screen analysis \
 *     --es aircraft AERO-101 --es condition abnormal --ez run_analysis true
 * </pre>
 */
public final class LaunchOptions {
    public final String screen;
    public final String aircraft;
    public final String condition;
    public final boolean runAnalysis;
    public final boolean startDemo;
    /** Maintenance tab: 0 = predictive, 1 = work orders. Null when not given. */
    public final Integer tab;
    public final boolean generateReport;

    private LaunchOptions(Intent i) {
        screen = i.getStringExtra("screen");
        aircraft = i.getStringExtra("aircraft");
        condition = i.getStringExtra("condition");
        runAnalysis = i.getBooleanExtra("run_analysis", false);
        startDemo = i.getBooleanExtra("demo", false);
        tab = i.hasExtra("tab") ? i.getIntExtra("tab", 0) : null;
        generateReport = i.getBooleanExtra("report", false);
    }

    /** Null when the intent carries no extras (a normal launch from the home screen). */
    public static LaunchOptions from(Intent intent) {
        if (intent == null || intent.getExtras() == null) return null;
        return new LaunchOptions(intent);
    }
}
