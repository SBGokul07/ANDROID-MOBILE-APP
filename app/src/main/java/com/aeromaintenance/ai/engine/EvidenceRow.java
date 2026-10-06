package com.aeromaintenance.ai.engine;

import com.aeromaintenance.ai.data.SensorType;

/** One row of the "Sensor evidence" table in the maintenance report. */
public final class EvidenceRow {
    public final SensorType sensor;
    public final String baseline;
    public final String current;
    public final String trend;
    /** More than 3σ from the fleet baseline. */
    public final boolean flagged;

    public EvidenceRow(SensorType sensor, String baseline, String current, String trend, boolean flagged) {
        this.sensor = sensor;
        this.baseline = baseline;
        this.current = current;
        this.trend = trend;
        this.flagged = flagged;
    }
}
