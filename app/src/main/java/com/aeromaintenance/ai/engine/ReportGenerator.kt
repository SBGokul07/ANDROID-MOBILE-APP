package com.aeromaintenance.ai.engine

import com.aeromaintenance.ai.data.Aircraft
import com.aeromaintenance.ai.data.RiskLevel
import com.aeromaintenance.ai.data.SensorType
import kotlin.math.abs

data class EvidenceRow(
    val sensor: SensorType,
    val baseline: String,
    val current: String,
    val trend: String,
    val flagged: Boolean,
)

data class MaintenanceReport(
    val reportNumber: String,
    val generatedAt: String,
    val aircraft: Aircraft,
    val result: AnalysisResult,
    val evidence: List<EvidenceRow>,
    val summary: String,
)

/** Builds the formatted maintenance report from an analysis result. */
object ReportGenerator {

    fun build(aircraft: Aircraft, result: AnalysisResult, generatedAt: String, sequence: Int): MaintenanceReport {
        val evidence = result.features.map { f ->
            val d = f.sensor.decimals
            val slope = f.slopePerHour
            val trend = (if (slope >= 0) "+" else "") + InferenceEngine.fmt(slope, d + 2) + " /h"
            EvidenceRow(
                sensor = f.sensor,
                baseline = InferenceEngine.fmt(f.baseline, d),
                current = InferenceEngine.fmt(f.current, d),
                trend = trend,
                flagged = abs(f.deviation) >= 3.0,
            )
        }
        val p = result.primary
        val summary = if (result.risk == RiskLevel.LOW) {
            "No abnormal behaviour was found in the analysed window. All components are within limits."
        } else {
            "${p.component.label}: ${p.condition.lowercase()}. Failure risk ${result.risk.label}, " +
                "estimated remaining useful life ${InferenceEngine.rulText(p.rulHours)} (operating hours)."
        }
        return MaintenanceReport(
            reportNumber = "AMR-" + aircraft.id.removePrefix("AERO-") + "-" + sequence.toString().padStart(3, '0'),
            generatedAt = generatedAt,
            aircraft = aircraft,
            result = result,
            evidence = evidence,
            summary = summary,
        )
    }

    /** Plain-text version for the Android share sheet. */
    fun toPlainText(r: MaintenanceReport): String {
        val a = r.aircraft
        val res = r.result
        val p = res.primary
        val sb = StringBuilder()
        sb.appendLine("AEROMAINTENANCE AI – PREDICTIVE MAINTENANCE REPORT")
        sb.appendLine("Report ${r.reportNumber}   Generated ${r.generatedAt}")
        sb.appendLine("Prototype / simulated AI inference – not for operational use")
        sb.appendLine()
        sb.appendLine("Aircraft: ${a.id} (${a.model})")
        sb.appendLine("Flight hours: ${InferenceEngine.fmt(a.flightHours.toDouble(), 0)}   Flight cycles: ${InferenceEngine.fmt(a.flightCycles.toDouble(), 0)}")
        sb.appendLine("Data set: ${res.condition.label}")
        sb.appendLine()
        sb.appendLine("Detected component: ${if (res.risk == RiskLevel.LOW) "None" else p.component.label}")
        sb.appendLine("Risk: ${res.risk.label}")
        sb.appendLine("Anomaly score: ${InferenceEngine.fmt(res.anomalyScore, 2)}")
        sb.appendLine("Estimated RUL: ${InferenceEngine.rulText(p.rulHours)}")
        sb.appendLine("Confidence: ${InferenceEngine.fmt(res.confidence * 100, 1)}%")
        sb.appendLine("Predicted condition: ${p.condition}")
        sb.appendLine()
        sb.appendLine("AI recommendation: ${p.recommendation}")
        p.workPackage.forEach { sb.appendLine("  - $it") }
        sb.appendLine()
        sb.appendLine("Explanation: ${res.explanation}")
        sb.appendLine()
        sb.appendLine("Component assessment:")
        res.components.forEach {
            sb.appendLine("  ${it.component.label}: health ${it.health}%, risk ${it.risk.label}, RUL ${InferenceEngine.rulText(it.rulHours)}")
        }
        sb.appendLine()
        sb.appendLine("Academic research prototype using simulated data. Not certified for aviation maintenance or safety decisions.")
        return sb.toString()
    }
}
