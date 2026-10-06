package com.aeromaintenance.ai.data;

import java.util.Collections;
import java.util.List;

/** A simulated aircraft in the fleet. Immutable. */
public final class Aircraft {
    public final String id;
    public final String model;
    public final int flightHours;
    public final int flightCycles;
    public final int healthScore;
    public final FleetStatus status;
    public final RiskLevel risk;
    public final String lastMaintenance;
    public final String base;
    /** Fault injected when "abnormal data" is selected for this aircraft. */
    public final FaultMode faultMode;
    /** Severity of that fault (0..1). */
    public final double faultSeverity;
    /** Optional second fault (AERO-101 also has early hydraulic-pump wear). */
    public final FaultMode secondaryFault;
    public final double secondarySeverity;
    public final List<ComponentHealthRecord> components;

    public Aircraft(String id, String model, int flightHours, int flightCycles, int healthScore,
                    FleetStatus status, RiskLevel risk, String lastMaintenance, String base,
                    FaultMode faultMode, double faultSeverity,
                    FaultMode secondaryFault, double secondarySeverity,
                    List<ComponentHealthRecord> components) {
        this.id = id;
        this.model = model;
        this.flightHours = flightHours;
        this.flightCycles = flightCycles;
        this.healthScore = healthScore;
        this.status = status;
        this.risk = risk;
        this.lastMaintenance = lastMaintenance;
        this.base = base;
        this.faultMode = faultMode;
        this.faultSeverity = faultSeverity;
        this.secondaryFault = secondaryFault;
        this.secondarySeverity = secondarySeverity;
        this.components = Collections.unmodifiableList(components);
    }
}
