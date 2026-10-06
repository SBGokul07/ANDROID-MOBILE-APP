package com.aeromaintenance.ai.data;

/** Which simulated data stream is fed to the AI pipeline (the NORMAL / ABNORMAL toggle). */
public enum DataCondition {
    NORMAL("Normal data"),
    ABNORMAL("Abnormal data");

    public final String label;

    DataCondition(String label) {
        this.label = label;
    }
}
