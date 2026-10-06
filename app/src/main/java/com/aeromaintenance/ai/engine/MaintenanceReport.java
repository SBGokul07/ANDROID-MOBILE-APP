package com.aeromaintenance.ai.engine;

import com.aeromaintenance.ai.data.Aircraft;

import java.util.Collections;
import java.util.List;

/** The AI-generated maintenance report shown on the Reports screen. */
public final class MaintenanceReport {
    public final String reportNumber;
    public final String generatedAt;
    public final Aircraft aircraft;
    public final AnalysisResult result;
    public final List<EvidenceRow> evidence;
    public final String summary;

    public MaintenanceReport(String reportNumber, String generatedAt, Aircraft aircraft,
                             AnalysisResult result, List<EvidenceRow> evidence, String summary) {
        this.reportNumber = reportNumber;
        this.generatedAt = generatedAt;
        this.aircraft = aircraft;
        this.result = result;
        this.evidence = Collections.unmodifiableList(evidence);
        this.summary = summary;
    }
}
