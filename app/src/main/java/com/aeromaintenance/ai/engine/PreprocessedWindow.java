package com.aeromaintenance.ai.engine;

import com.aeromaintenance.ai.data.SensorType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Output of the preprocessing stage, exposed so the UI can draw the cleaned signal. */
public final class PreprocessedWindow {
    /** Spike-free signal (Hampel filter). */
    public final Map<SensorType, double[]> clean;
    /** Clean signal after a 5-point moving average. */
    public final Map<SensorType, double[]> smoothed;
    /** Rolling pressure ripple of the hydraulic channel. */
    public final double[] hydraulicRipple;
    public final int spikesRemoved;
    public final Map<SensorType, List<Integer>> spikeIndices;

    public PreprocessedWindow(Map<SensorType, double[]> clean, Map<SensorType, double[]> smoothed,
                              double[] hydraulicRipple, int spikesRemoved,
                              Map<SensorType, List<Integer>> spikeIndices) {
        this.clean = Collections.unmodifiableMap(clean);
        this.smoothed = Collections.unmodifiableMap(smoothed);
        this.hydraulicRipple = hydraulicRipple;
        this.spikesRemoved = spikesRemoved;
        this.spikeIndices = Collections.unmodifiableMap(spikeIndices);
    }

    public double[] smoothed(SensorType s) {
        return smoothed.get(s);
    }

    public List<Integer> spikes(SensorType s) {
        List<Integer> list = spikeIndices.get(s);
        return list == null ? Collections.<Integer>emptyList() : list;
    }
}
