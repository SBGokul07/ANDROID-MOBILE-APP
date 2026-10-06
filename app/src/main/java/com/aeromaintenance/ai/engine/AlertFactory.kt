package com.aeromaintenance.ai.engine

import com.aeromaintenance.ai.data.Alert
import com.aeromaintenance.ai.data.ComponentType
import com.aeromaintenance.ai.data.RiskLevel
import com.aeromaintenance.ai.data.Severity

/**
 * Turns an analysis result into an alert. Alerts are only raised for MEDIUM and
 * HIGH risk; a LOW-risk result produces no alert.
 */
object AlertFactory {

    /** MEDIUM-risk findings with less than this much RUL are raised as WARNING, otherwise MONITOR. */
    const val WARNING_RUL_HOURS = 150

    fun severityFor(result: AnalysisResult): Severity? = when (result.risk) {
        RiskLevel.HIGH -> Severity.CRITICAL
        RiskLevel.MEDIUM -> if (result.primary.rulHours < WARNING_RUL_HOURS) Severity.WARNING else Severity.MONITOR
        RiskLevel.LOW -> null
    }

    /** Name used for the affected system in alerts and work orders. */
    fun systemName(c: ComponentType): String = when (c) {
        ComponentType.ENGINE_BEARING -> "Engine Bearing"
        ComponentType.HYDRAULIC_PUMP -> "Hydraulic System"
        ComponentType.TURBINE -> "Turbine"
        ComponentType.FUEL_PUMP -> "Fuel System"
    }

    fun title(c: ComponentType): String = when (c) {
        ComponentType.ENGINE_BEARING -> "Engine Bearing anomaly detected"
        ComponentType.HYDRAULIC_PUMP -> "Hydraulic pressure fluctuation"
        ComponentType.TURBINE -> "Turbine temperature trend increasing"
        ComponentType.FUEL_PUMP -> "Fuel flow above baseline"
    }

    fun workOrderTitle(c: ComponentType): String = when (c) {
        ComponentType.ENGINE_BEARING -> "Engine Bearing Inspection"
        ComponentType.HYDRAULIC_PUMP -> "Hydraulic Pump Inspection"
        ComponentType.TURBINE -> "Hot-section Borescope"
        ComponentType.FUEL_PUMP -> "Fuel System Inspection"
    }

    fun fromResult(result: AnalysisResult, id: String, timestamp: String, isNew: Boolean): Alert? {
        val severity = severityFor(result) ?: return null
        val c = result.primary.component
        return Alert(
            id = id,
            timestamp = timestamp,
            aircraftId = result.aircraftId,
            component = systemName(c),
            severity = severity,
            title = title(c),
            explanation = result.explanation,
            recommendedAction = result.primary.recommendation,
            isNew = isNew,
        )
    }
}
