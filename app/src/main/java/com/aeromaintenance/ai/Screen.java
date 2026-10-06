package com.aeromaintenance.ai;

import java.util.Locale;

/** Every screen in the app. The five tab screens appear in the bottom navigation bar. */
public enum Screen {
    DASHBOARD("AeroMaintenance AI", true),
    FLEET("Aircraft", true),
    AIRCRAFT_DETAIL("Aircraft", false),
    MONITORING("Sensor monitoring", true),
    ANALYSIS("AI prediction", true),
    INSIGHTS("AI insights", false),
    MAINTENANCE("Maintenance", true),
    ALERTS("Alerts", false),
    REPORTS("Reports", false),
    FACULTY_DEMO("Faculty demo mode", false),
    ABOUT("About", false);

    public final String title;
    public final boolean isTab;

    Screen(String title, boolean isTab) {
        this.title = title;
        this.isTab = isTab;
    }

    /** Maps the {@code screen} launch extra to a screen (used by deep links and the CI screenshot run). */
    public static Screen fromKey(String key) {
        if (key == null) return null;
        switch (key.toLowerCase(Locale.ROOT)) {
            case "dashboard":
                return DASHBOARD;
            case "aircraft":
            case "fleet":
                return FLEET;
            case "detail":
                return AIRCRAFT_DETAIL;
            case "monitoring":
            case "sensors":
                return MONITORING;
            case "analysis":
            case "ai":
                return ANALYSIS;
            case "insights":
                return INSIGHTS;
            case "maintenance":
            case "predictive":
            case "planning":
                return MAINTENANCE;
            case "alerts":
                return ALERTS;
            case "reports":
                return REPORTS;
            case "faculty":
                return FACULTY_DEMO;
            case "about":
                return ABOUT;
            default:
                return null;
        }
    }
}
