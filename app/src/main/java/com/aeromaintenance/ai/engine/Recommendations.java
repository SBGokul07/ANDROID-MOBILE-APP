package com.aeromaintenance.ai.engine;

import com.aeromaintenance.ai.data.ComponentType;
import com.aeromaintenance.ai.data.RiskLevel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Rule base that turns (component, risk) into a predicted condition, a
 * recommendation and a work package. In a production system this would be
 * reviewed by licensed engineers and linked to the aircraft maintenance manual.
 */
public final class Recommendations {

    private Recommendations() {
    }

    public static final class Text {
        public final String condition;
        public final String recommendation;
        public final String shortAction;
        public final List<String> workPackage;

        Text(String condition, String recommendation, String shortAction, String... workPackage) {
            this.condition = condition;
            this.recommendation = recommendation;
            this.shortAction = shortAction;
            this.workPackage = Collections.unmodifiableList(Arrays.asList(workPackage));
        }
    }

    private static final Text NORMAL = new Text(
            "Operating within normal limits",
            "No action required. Continue routine condition monitoring.",
            "Normal",
            "Continue routine condition monitoring");

    public static Text forComponent(ComponentType c, RiskLevel risk) {
        if (risk == RiskLevel.LOW) return NORMAL;
        boolean high = risk == RiskLevel.HIGH;
        switch (c) {
            case ENGINE_BEARING:
                return high
                        ? new Text("Progressive degradation detected",
                        "Schedule engine-bearing inspection during the next available maintenance window.",
                        "Inspect",
                        "Borescope inspection of the bearing compartment",
                        "Check magnetic chip detector and oil filter for metal particles",
                        "Engine vibration survey after inspection")
                        : new Text("Early-stage bearing wear pattern",
                        "Increase vibration monitoring frequency and inspect the bearing at the next A-check.",
                        "Monitor",
                        "Daily vibration trend review",
                        "Oil debris analysis at next service");
            case HYDRAULIC_PUMP:
                return high
                        ? new Text("Severe hydraulic pressure instability",
                        "Inspect the hydraulic pump at the next maintenance opportunity.",
                        "Inspect",
                        "Measure pump case-drain flow",
                        "Record pressure ripple at ground idle",
                        "Inspect pump mounting and seals")
                        : new Text("Pressure ripple increasing: early pump wear",
                        "Monitor the hydraulic pump and inspect it at the next A-check.",
                        "Monitor",
                        "Measure pump case-drain flow",
                        "Record pressure ripple at ground idle");
            case TURBINE:
                return high
                        ? new Text("Rapid EGT margin erosion",
                        "Plan a hot-section borescope inspection at the next maintenance window.",
                        "Inspect",
                        "Borescope turbine stage-1 blades",
                        "Check EGT thermocouple harness",
                        "Engine performance run")
                        : new Text("Gradual EGT margin erosion",
                        "Monitor EGT margin and plan a hot-section borescope inspection.",
                        "Monitor",
                        "Weekly EGT margin trend review",
                        "Plan hot-section borescope");
            case FUEL_PUMP:
            default:
                return high
                        ? new Text("Fuel delivery degradation",
                        "Inspect the fuel pump and metering unit at the next maintenance opportunity.",
                        "Inspect",
                        "Inspect fuel pump and replace filter element",
                        "Check fuel metering unit calibration")
                        : new Text("Fuel flow drifting above baseline",
                        "Inspect the fuel pump and filter at the next scheduled check.",
                        "Monitor",
                        "Inspect fuel pump and replace filter element",
                        "Check fuel metering unit calibration");
        }
    }
}
