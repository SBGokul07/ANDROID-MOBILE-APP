package com.aeromaintenance.ai

import com.aeromaintenance.ai.engine.AnalysisResult
import com.aeromaintenance.ai.engine.InferenceEngine

/** One step of the guided START DEMO sequence. */
data class DemoStep(
    val title: String,
    val durationMs: Long,
    /** Narration shown to the audience. Uses the live analysis result once it exists. */
    val narration: (AnalysisResult?) -> String,
)

/**
 * The ten-step walkthrough from the project brief. The view model performs the
 * action for each step; this file holds the wording and timing so it is easy to
 * rehearse and adjust.
 */
object DemoScript {
    const val AIRCRAFT = "AERO-101"

    val steps: List<DemoStep> = listOf(
        DemoStep("Select aircraft $AIRCRAFT", 6000) {
            "$AIRCRAFT is an A320 with 8,421 flight hours. We start from its health page: four monitored components, one already under attention."
        },
        DemoStep("Display sensor telemetry", 6500) {
            "Six telemetry channels, one sample per operating hour for the last 120 hours. This is the normal data set: every trace stays inside its green band."
        },
        DemoStep("Show abnormal trend", 7500) {
            "Switching to the abnormal data set. Vibration and engine temperature start climbing and oil pressure drops. The traces leave the normal band and turn amber, then red."
        },
        DemoStep("Run AI analysis", 2500) {
            "The telemetry runs through the pipeline: spike removal, feature extraction, the normal-behaviour model, anomaly detection, health, risk and RUL."
        },
        DemoStep("Display anomaly score", 6500) { r ->
            val s = r?.let { InferenceEngine.fmt(it.anomalyScore, 2) } ?: "0.91"
            "Anomaly score $s on a scale of 0 to 1. Anything above ${InferenceEngine.fmt(InferenceEngine.ALERT_THRESHOLD, 2)} is treated as abnormal behaviour."
        },
        DemoStep("Predict component failure risk", 6500) { r ->
            val c = r?.primary?.component?.label ?: "Engine Bearing"
            val risk = r?.risk?.label ?: "HIGH"
            "The deviation pattern matches the $c fault signature. Failure risk is $risk."
        },
        DemoStep("Estimate remaining useful life", 7000) { r ->
            val p = r?.primary
            if (p == null) "Remaining useful life is estimated from the degradation trend."
            else "${p.indicatorName} is rising ${InferenceEngine.fmt(p.trendPerHour, 3)} ${p.indicatorUnit} per hour. At that rate it reaches the " +
                "${InferenceEngine.fmt(p.indicatorLimit, p.indicatorDecimals)} ${p.indicatorUnit} limit in ${InferenceEngine.rulText(p.rulHours)} of operation."
        },
        DemoStep("Generate maintenance recommendation", 7000) { r ->
            "Recommendation: ${r?.primary?.recommendation ?: "Schedule engine-bearing inspection during the next available maintenance window."}"
        },
        DemoStep("Create alert", 6500) {
            "A critical alert is raised for the maintenance team, in the app and as a phone notification, with the AI's explanation attached."
        },
        DemoStep("Show maintenance action", 8000) { r ->
            val due = r?.primary?.rulHours?.let { InferenceEngine.rulText(it) } ?: "42 h"
            "The inspection lands in the maintenance plan, due within $due. An engineer reviews the finding and starts the work order."
        },
    )
}
