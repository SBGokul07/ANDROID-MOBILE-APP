package com.aeromaintenance.ai.data;

/** Failure-risk level predicted by the AI engine. */
public enum RiskLevel {
    LOW("LOW", 0),
    MEDIUM("MEDIUM", 1),
    HIGH("HIGH", 2);

    public final String label;
    /** Higher rank = more severe. Used for sorting. */
    public final int rank;

    RiskLevel(String label, int rank) {
        this.label = label;
        this.rank = rank;
    }
}
