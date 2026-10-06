package com.aeromaintenance.ai.engine;

import com.aeromaintenance.ai.data.SensorType;

/** The three features (level, trend, volatility) extracted for one channel. */
public final class SensorFeatures {
    public final SensorType sensor;
    public final double baseline;
    /** Smoothed value at the start of the window (mean of the first 12 h). */
    public final double windowStart;
    /** Trend-filtered current value (linear fit evaluated at the latest sample). */
    public final double current;
    /** Mean z-score of the last 24 h against the fleet baseline. */
    public final double levelZ;
    /** Linear trend over the last 24 h, in sensor units per operating hour. */
    public final double slopePerHour;
    /** Goodness of fit of that trend (0..1). */
    public final double r2;
    /** Short-term fluctuation relative to the baseline fluctuation (1.0 = normal). */
    public final double volatilityRatio;
    /** Combined deviation index for this channel, in baseline standard deviations. */
    public final double deviation;

    public SensorFeatures(SensorType sensor, double baseline, double windowStart, double current,
                          double levelZ, double slopePerHour, double r2, double volatilityRatio,
                          double deviation) {
        this.sensor = sensor;
        this.baseline = baseline;
        this.windowStart = windowStart;
        this.current = current;
        this.levelZ = levelZ;
        this.slopePerHour = slopePerHour;
        this.r2 = r2;
        this.volatilityRatio = volatilityRatio;
        this.deviation = deviation;
    }
}
