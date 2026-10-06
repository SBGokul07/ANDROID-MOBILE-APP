package com.aeromaintenance.ai.data;

/** Alert severity shown on the Alerts screen. */
public enum Severity {
    CRITICAL("CRITICAL", 3),
    WARNING("WARNING", 2),
    MONITOR("MONITOR", 1);

    public final String label;
    public final int rank;

    Severity(String label, int rank) {
        this.label = label;
        this.rank = rank;
    }

    /** "Critical", "Warning", "Monitor". */
    public String title() {
        return label.charAt(0) + label.substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}
