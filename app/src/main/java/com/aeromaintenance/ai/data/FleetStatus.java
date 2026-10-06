package com.aeromaintenance.ai.data;

/** Overall status of an aircraft in the fleet overview. */
public enum FleetStatus {
    HEALTHY("Healthy"),
    WARNING("Warning"),
    CRITICAL("Critical");

    public final String label;

    FleetStatus(String label) {
        this.label = label;
    }
}
