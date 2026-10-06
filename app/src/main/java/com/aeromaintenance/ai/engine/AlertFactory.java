package com.aeromaintenance.ai.engine;

import com.aeromaintenance.ai.data.Alert;
import com.aeromaintenance.ai.data.ComponentType;
import com.aeromaintenance.ai.data.Severity;

/**
 * Turns an analysis result into an alert. Alerts are only raised for MEDIUM and
 * HIGH risk; a LOW-risk result produces no alert.
 */
public final class AlertFactory {

    private AlertFactory() {
    }

    /** MEDIUM-risk findings with less than this much RUL are raised as WARNING, otherwise MONITOR. */
    public static final int WARNING_RUL_HOURS = 150;

    /** Severity for a result, or null when the risk is LOW. */
    public static Severity severityFor(AnalysisResult result) {
        switch (result.risk) {
            case HIGH:
                return Severity.CRITICAL;
            case MEDIUM:
                return result.primary.rulHours < WARNING_RUL_HOURS ? Severity.WARNING : Severity.MONITOR;
            default:
                return null;
        }
    }

    /** Name used for the affected system in alerts and work orders. */
    public static String systemName(ComponentType c) {
        switch (c) {
            case ENGINE_BEARING:
                return "Engine Bearing";
            case HYDRAULIC_PUMP:
                return "Hydraulic System";
            case TURBINE:
                return "Turbine";
            case FUEL_PUMP:
            default:
                return "Fuel System";
        }
    }

    public static String title(ComponentType c) {
        switch (c) {
            case ENGINE_BEARING:
                return "Engine Bearing anomaly detected";
            case HYDRAULIC_PUMP:
                return "Hydraulic pressure fluctuation";
            case TURBINE:
                return "Turbine temperature trend increasing";
            case FUEL_PUMP:
            default:
                return "Fuel flow above baseline";
        }
    }

    public static String workOrderTitle(ComponentType c) {
        switch (c) {
            case ENGINE_BEARING:
                return "Engine Bearing Inspection";
            case HYDRAULIC_PUMP:
                return "Hydraulic Pump Inspection";
            case TURBINE:
                return "Hot-section Borescope";
            case FUEL_PUMP:
            default:
                return "Fuel System Inspection";
        }
    }

    /** Builds the alert for a result, or returns null when the risk is LOW. */
    public static Alert fromResult(AnalysisResult result, String id, String timestamp, boolean isNew) {
        Severity severity = severityFor(result);
        if (severity == null) return null;
        ComponentType c = result.primary.component;
        return new Alert(id, timestamp, result.aircraftId, systemName(c), severity, title(c),
                result.explanation, result.primary.recommendation, false, isNew);
    }
}
