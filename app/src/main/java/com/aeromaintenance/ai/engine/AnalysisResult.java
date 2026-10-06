package com.aeromaintenance.ai.engine;

import com.aeromaintenance.ai.data.DataCondition;
import com.aeromaintenance.ai.data.RiskLevel;
import com.aeromaintenance.ai.data.SensorType;

import java.util.Collections;
import java.util.List;

/** Everything one run of the AI pipeline produces. */
public final class AnalysisResult {
    public final String aircraftId;
    public final DataCondition condition;
    /** 0 = normal behaviour, 1 = extremely abnormal. */
    public final double anomalyScore;
    /** Weighted deviation from the fleet baseline, in standard deviations (σ). */
    public final double deviationIndex;
    public final RiskLevel risk;
    /** The component most at risk. */
    public final ComponentAssessment primary;
    public final List<ComponentAssessment> components;
    public final List<SensorFeatures> features;
    /** 0..1 */
    public final double confidence;
    /** Detected start of degradation in operating hours before now, or null if none. */
    public final Integer onsetHoursAgo;
    public final String explanation;
    public final List<PipelineStage> stages;
    public final int spikesRemoved;
    public final int samplesAnalysed;

    public AnalysisResult(String aircraftId, DataCondition condition, double anomalyScore,
                          double deviationIndex, RiskLevel risk, ComponentAssessment primary,
                          List<ComponentAssessment> components, List<SensorFeatures> features,
                          double confidence, Integer onsetHoursAgo, String explanation,
                          List<PipelineStage> stages, int spikesRemoved, int samplesAnalysed) {
        this.aircraftId = aircraftId;
        this.condition = condition;
        this.anomalyScore = anomalyScore;
        this.deviationIndex = deviationIndex;
        this.risk = risk;
        this.primary = primary;
        this.components = Collections.unmodifiableList(components);
        this.features = Collections.unmodifiableList(features);
        this.confidence = confidence;
        this.onsetHoursAgo = onsetHoursAgo;
        this.explanation = explanation;
        this.stages = Collections.unmodifiableList(stages);
        this.spikesRemoved = spikesRemoved;
        this.samplesAnalysed = samplesAnalysed;
    }

    public SensorFeatures feature(SensorType s) {
        for (SensorFeatures f : features) {
            if (f.sensor == s) return f;
        }
        throw new IllegalArgumentException("No features for " + s);
    }

    public boolean isLowRisk() {
        return risk == RiskLevel.LOW;
    }
}
