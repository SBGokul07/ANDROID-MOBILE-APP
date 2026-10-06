package com.aeromaintenance.ai.engine;

import com.aeromaintenance.ai.data.Aircraft;
import com.aeromaintenance.ai.data.Format;
import com.aeromaintenance.ai.data.RiskLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds the formatted maintenance report from an analysis result. */
public final class ReportGenerator {

    private ReportGenerator() {
    }

    public static MaintenanceReport build(Aircraft aircraft, AnalysisResult result, String generatedAt, int sequence) {
        List<EvidenceRow> evidence = new ArrayList<>();
        for (SensorFeatures f : result.features) {
            int d = f.sensor.decimals;
            String trend = Format.signed(f.slopePerHour, d + 2) + " /h";
            evidence.add(new EvidenceRow(f.sensor, Format.number(f.baseline, d), Format.number(f.current, d),
                    trend, Math.abs(f.deviation) >= 3.0));
        }
        ComponentAssessment p = result.primary;
        String summary;
        if (result.risk == RiskLevel.LOW) {
            summary = "No abnormal behaviour was found in the analysed window. All components are within limits.";
        } else {
            summary = p.component.label + ": " + p.condition.toLowerCase(Locale.ROOT) + ". Failure risk " + result.risk.label
                    + ", estimated remaining useful life " + InferenceEngine.rulText(p.rulHours) + " (operating hours).";
        }
        String number = "AMR-" + aircraft.id.replace("AERO-", "") + "-" + String.format(Locale.ROOT, "%03d", sequence);
        return new MaintenanceReport(number, generatedAt, aircraft, result, evidence, summary);
    }

    /** Plain-text version for the Android share sheet. */
    public static String toPlainText(MaintenanceReport r) {
        Aircraft a = r.aircraft;
        AnalysisResult res = r.result;
        ComponentAssessment p = res.primary;
        StringBuilder sb = new StringBuilder();
        line(sb, "AEROMAINTENANCE AI – PREDICTIVE MAINTENANCE REPORT");
        line(sb, "Report " + r.reportNumber + "   Generated " + r.generatedAt);
        line(sb, "Prototype / simulated AI inference – not for operational use");
        line(sb, "");
        line(sb, "Aircraft: " + a.id + " (" + a.model + ")");
        line(sb, "Flight hours: " + Format.number(a.flightHours, 0) + "   Flight cycles: " + Format.number(a.flightCycles, 0));
        line(sb, "Data set: " + res.condition.label);
        line(sb, "");
        line(sb, "Detected component: " + (res.risk == RiskLevel.LOW ? "None" : p.component.label));
        line(sb, "Risk: " + res.risk.label);
        line(sb, "Anomaly score: " + Format.number(res.anomalyScore, 2));
        line(sb, "Estimated RUL: " + InferenceEngine.rulText(p.rulHours));
        line(sb, "Confidence: " + Format.number(res.confidence * 100, 1) + "%");
        line(sb, "Predicted condition: " + p.condition);
        line(sb, "");
        line(sb, "AI recommendation: " + p.recommendation);
        for (String w : p.workPackage) line(sb, "  - " + w);
        line(sb, "");
        line(sb, "Explanation: " + res.explanation);
        line(sb, "");
        line(sb, "Component assessment:");
        for (ComponentAssessment c : res.components) {
            line(sb, "  " + c.component.label + ": health " + c.health + "%, risk " + c.risk.label
                    + ", RUL " + InferenceEngine.rulText(c.rulHours));
        }
        line(sb, "");
        line(sb, "Academic research prototype using simulated data. Not certified for aviation maintenance or safety decisions.");
        return sb.toString();
    }

    private static void line(StringBuilder sb, String text) {
        sb.append(text).append('\n');
    }
}
