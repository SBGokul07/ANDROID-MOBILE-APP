package com.aeromaintenance.ai.data;

/**
 * Fault signature injected into the abnormal data stream. Each aircraft in the
 * fleet has one, so the "abnormal" toggle produces a realistic fault for that tail.
 */
public enum FaultMode {
    BEARING_DEGRADATION("Progressive engine-bearing wear", 0.25, 2.0),
    HYDRAULIC_PUMP_WEAR("Hydraulic pump wear", 0.10, 1.0),
    TURBINE_EGT_DRIFT("Turbine hot-section EGT drift", 0.10, 1.0),
    FUEL_PUMP_DEGRADATION("Fuel pump degradation", 0.05, 1.0);

    public final String label;
    /** Fraction of the window at which degradation starts. */
    public final double onset;
    /** Shape of the degradation ramp: 1 = linear drift, 2 = accelerating wear. */
    public final double exponent;

    FaultMode(String label, double onset, double exponent) {
        this.label = label;
        this.onset = onset;
        this.exponent = exponent;
    }
}
