package com.aeromaintenance.ai.engine

import com.aeromaintenance.ai.data.ComponentType
import com.aeromaintenance.ai.data.RiskLevel

/**
 * Rule base that turns (component, risk) into a predicted condition, a
 * recommendation and a work package. In a production system this would be
 * reviewed by licensed engineers and linked to the aircraft maintenance manual.
 */
object Recommendations {

    data class Text(
        val condition: String,
        val recommendation: String,
        val shortAction: String,
        val workPackage: List<String>,
    )

    private val normal = Text(
        condition = "Operating within normal limits",
        recommendation = "No action required. Continue routine condition monitoring.",
        shortAction = "Normal",
        workPackage = listOf("Continue routine condition monitoring"),
    )

    fun forComponent(c: ComponentType, risk: RiskLevel): Text = when (c) {
        ComponentType.ENGINE_BEARING -> when (risk) {
            RiskLevel.HIGH -> Text(
                "Progressive degradation detected",
                "Schedule engine-bearing inspection during the next available maintenance window.",
                "Inspect",
                listOf(
                    "Borescope inspection of the bearing compartment",
                    "Check magnetic chip detector and oil filter for metal particles",
                    "Engine vibration survey after inspection",
                ),
            )
            RiskLevel.MEDIUM -> Text(
                "Early-stage bearing wear pattern",
                "Increase vibration monitoring frequency and inspect the bearing at the next A-check.",
                "Monitor",
                listOf("Daily vibration trend review", "Oil debris analysis at next service"),
            )
            RiskLevel.LOW -> normal
        }
        ComponentType.HYDRAULIC_PUMP -> when (risk) {
            RiskLevel.HIGH -> Text(
                "Severe hydraulic pressure instability",
                "Inspect the hydraulic pump at the next maintenance opportunity.",
                "Inspect",
                listOf("Measure pump case-drain flow", "Record pressure ripple at ground idle", "Inspect pump mounting and seals"),
            )
            RiskLevel.MEDIUM -> Text(
                "Pressure ripple increasing: early pump wear",
                "Monitor the hydraulic pump and inspect it at the next A-check.",
                "Monitor",
                listOf("Measure pump case-drain flow", "Record pressure ripple at ground idle"),
            )
            RiskLevel.LOW -> normal
        }
        ComponentType.TURBINE -> when (risk) {
            RiskLevel.HIGH -> Text(
                "Rapid EGT margin erosion",
                "Plan a hot-section borescope inspection at the next maintenance window.",
                "Inspect",
                listOf("Borescope turbine stage-1 blades", "Check EGT thermocouple harness", "Engine performance run"),
            )
            RiskLevel.MEDIUM -> Text(
                "Gradual EGT margin erosion",
                "Monitor EGT margin and plan a hot-section borescope inspection.",
                "Monitor",
                listOf("Weekly EGT margin trend review", "Plan hot-section borescope"),
            )
            RiskLevel.LOW -> normal
        }
        ComponentType.FUEL_PUMP -> when (risk) {
            RiskLevel.HIGH -> Text(
                "Fuel delivery degradation",
                "Inspect the fuel pump and metering unit at the next maintenance opportunity.",
                "Inspect",
                listOf("Inspect fuel pump and replace filter element", "Check fuel metering unit calibration"),
            )
            RiskLevel.MEDIUM -> Text(
                "Fuel flow drifting above baseline",
                "Inspect the fuel pump and filter at the next scheduled check.",
                "Monitor",
                listOf("Inspect fuel pump and replace filter element", "Check fuel metering unit calibration"),
            )
            RiskLevel.LOW -> normal
        }
    }
}
