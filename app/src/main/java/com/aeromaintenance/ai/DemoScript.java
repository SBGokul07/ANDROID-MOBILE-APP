package com.aeromaintenance.ai;

import com.aeromaintenance.ai.data.Format;
import com.aeromaintenance.ai.engine.AnalysisResult;
import com.aeromaintenance.ai.engine.ComponentAssessment;
import com.aeromaintenance.ai.engine.InferenceEngine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The ten-step START DEMO walkthrough from the project brief. {@link AppState}
 * performs the action for each step; this class holds the wording and timing so
 * it is easy to rehearse and adjust.
 */
public final class DemoScript {

    private DemoScript() {
    }

    public static final String AIRCRAFT = "AERO-101";

    /** Narration shown to the audience; uses the live analysis result once it exists. */
    public interface Narration {
        String text(AnalysisResult result);
    }

    public static final class Step {
        public final String title;
        public final long durationMs;
        public final Narration narration;

        Step(String title, long durationMs, Narration narration) {
            this.title = title;
            this.durationMs = durationMs;
            this.narration = narration;
        }
    }

    public static final List<Step> STEPS = Collections.unmodifiableList(Arrays.asList(
            new Step("Select aircraft " + AIRCRAFT, 6000, r ->
                    AIRCRAFT + " is an A320 with 8,421 flight hours. We start from its health page: four monitored "
                            + "components, one already under attention."),
            new Step("Display sensor telemetry", 6500, r ->
                    "Six telemetry channels, one sample per operating hour for the last 120 hours. This is the normal "
                            + "data set: every trace stays inside its green band."),
            new Step("Show abnormal trend", 7500, r ->
                    "Switching to the abnormal data set. Vibration and engine temperature start climbing and oil "
                            + "pressure drops. The traces leave the normal band and turn amber, then red."),
            new Step("Run AI analysis", 2500, r ->
                    "The telemetry runs through the pipeline: spike removal, feature extraction, the normal-behaviour "
                            + "model, anomaly detection, health, risk and RUL."),
            new Step("Display anomaly score", 6500, r -> {
                String s = r != null ? Format.number(r.anomalyScore, 2) : "0.91";
                return "Anomaly score " + s + " on a scale of 0 to 1. Anything above "
                        + Format.number(InferenceEngine.ALERT_THRESHOLD, 2) + " is treated as abnormal behaviour.";
            }),
            new Step("Predict component failure risk", 6500, r -> {
                String c = r != null ? r.primary.component.label : "Engine Bearing";
                String risk = r != null ? r.risk.label : "HIGH";
                return "The deviation pattern matches the " + c + " fault signature. Failure risk is " + risk + ".";
            }),
            new Step("Estimate remaining useful life", 7000, r -> {
                if (r == null) return "Remaining useful life is estimated from the degradation trend.";
                ComponentAssessment p = r.primary;
                return p.indicatorName + " is rising " + Format.number(p.trendPerHour, 3) + " " + p.indicatorUnit
                        + " per hour. At that rate it reaches the " + Format.number(p.indicatorLimit, p.indicatorDecimals)
                        + " " + p.indicatorUnit + " limit in " + InferenceEngine.rulText(p.rulHours) + " of operation.";
            }),
            new Step("Generate maintenance recommendation", 7000, r ->
                    "Recommendation: " + (r != null ? r.primary.recommendation
                            : "Schedule engine-bearing inspection during the next available maintenance window.")),
            new Step("Create alert", 6500, r ->
                    "A critical alert is raised for the maintenance team, in the app and as a phone notification, "
                            + "with the AI's explanation attached."),
            new Step("Show maintenance action", 8000, r -> {
                String due = r != null ? InferenceEngine.rulText(r.primary.rulHours) : "42 h";
                return "The inspection lands in the maintenance plan, due within " + due
                        + ". An engineer reviews the finding and starts the work order.";
            })
    ));

    public static int lastIndex() {
        return STEPS.size() - 1;
    }
}
