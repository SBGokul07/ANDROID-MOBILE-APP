package com.aeromaintenance.ai.ui;

import com.aeromaintenance.ai.data.FleetStatus;
import com.aeromaintenance.ai.data.RiskLevel;
import com.aeromaintenance.ai.data.Severity;
import com.aeromaintenance.ai.data.TaskStatus;
import com.aeromaintenance.ai.data.Zone;

/**
 * Colour language borrowed from glass-cockpit engine displays:
 * green = normal, amber/orange = caution, red = warning, cyan = information and
 * anything you can tap, magenta = predicted / AI-projected values.
 */
public final class Palette {

    private Palette() {
    }

    public static final int NIGHT = 0xFF07101D;
    public static final int PANEL = 0xFF0E1B2E;
    public static final int PANEL_RAISED = 0xFF13243C;
    public static final int HAIRLINE = 0xFF1F3350;
    public static final int HAIRLINE_STRONG = 0xFF2C4669;

    public static final int TEXT = 0xFFE6EEF8;
    public static final int TEXT_MUTED = 0xFF8FA3BD;
    public static final int TEXT_FAINT = 0xFF5B6F8A;

    public static final int CYAN = 0xFF4CC3F0;
    public static final int CYAN_DEEP = 0xFF1C6E95;
    public static final int GREEN = 0xFF3DD68C;
    public static final int AMBER = 0xFFF2C744;
    public static final int ORANGE = 0xFFFF9A3C;
    public static final int RED = 0xFFFF5A4F;
    public static final int MAGENTA = 0xFFD86BFF;

    /** Printed-report colours. */
    public static final int PAPER = 0xFFF4F6F9;
    public static final int INK = 0xFF15202E;
    public static final int INK_MUTED = 0xFF5A6778;
    public static final int PAPER_RULE = 0xFFDDE2E8;
    public static final int PAPER_RED = 0xFFC62828;
    public static final int PAPER_ORANGE = 0xFFC2410C;
    public static final int PAPER_GREEN = 0xFF2B7A3E;

    /** The same colour with a new opacity (0..1). */
    public static int alpha(int color, float alpha) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (color & 0x00FFFFFF) | (a << 24);
    }

    public static int risk(RiskLevel r) {
        switch (r) {
            case LOW:
                return GREEN;
            case MEDIUM:
                return ORANGE;
            default:
                return RED;
        }
    }

    public static int paperRisk(RiskLevel r) {
        switch (r) {
            case LOW:
                return PAPER_GREEN;
            case MEDIUM:
                return PAPER_ORANGE;
            default:
                return PAPER_RED;
        }
    }

    public static int severity(Severity s) {
        switch (s) {
            case CRITICAL:
                return RED;
            case WARNING:
                return ORANGE;
            default:
                return AMBER;
        }
    }

    public static int fleetStatus(FleetStatus s) {
        switch (s) {
            case HEALTHY:
                return GREEN;
            case WARNING:
                return ORANGE;
            default:
                return RED;
        }
    }

    public static int zone(Zone z) {
        switch (z) {
            case NORMAL:
                return GREEN;
            case WARN:
                return AMBER;
            default:
                return RED;
        }
    }

    public static int health(int health) {
        if (health >= 85) return GREEN;
        if (health >= 70) return ORANGE;
        return RED;
    }

    public static int taskStatus(TaskStatus s) {
        switch (s) {
            case SCHEDULED:
                return CYAN;
            case IN_PROGRESS:
                return AMBER;
            default:
                return GREEN;
        }
    }

    /** Colour for an operating-hours countdown (same thresholds as the risk rules). */
    public static int dueHours(int hours) {
        if (hours < 72) return RED;
        if (hours < 250) return ORANGE;
        return GREEN;
    }
}
