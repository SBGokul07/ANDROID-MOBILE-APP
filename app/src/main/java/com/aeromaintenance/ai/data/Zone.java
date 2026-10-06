package com.aeromaintenance.ai.data;

/** Where a sensor reading sits relative to its limits. */
public enum Zone {
    NORMAL,
    /** Caution band (amber). */
    WARN,
    /** Past the warning limit (red). */
    ALARM
}
