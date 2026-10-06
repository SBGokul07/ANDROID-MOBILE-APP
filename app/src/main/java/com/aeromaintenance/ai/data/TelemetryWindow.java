package com.aeromaintenance.ai.data;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** One fixed-rate telemetry window: 120 samples per channel, one per operating hour. */
public final class TelemetryWindow {
    public final String aircraftId;
    public final DataCondition condition;
    public final double sampleIntervalHours;
    /** Channel data in a fixed order (EnumMap iterates in declaration order). */
    public final Map<SensorType, double[]> channels;

    public TelemetryWindow(String aircraftId, DataCondition condition, double sampleIntervalHours,
                           EnumMap<SensorType, double[]> channels) {
        this.aircraftId = aircraftId;
        this.condition = condition;
        this.sampleIntervalHours = sampleIntervalHours;
        this.channels = Collections.unmodifiableMap(channels);
    }

    /** Number of samples per channel. */
    public int size() {
        return channels.values().iterator().next().length;
    }

    public double[] channel(SensorType sensor) {
        return channels.get(sensor);
    }

    public double latest(SensorType sensor) {
        double[] c = channels.get(sensor);
        return c[c.length - 1];
    }
}
