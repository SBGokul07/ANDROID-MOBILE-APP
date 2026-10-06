package com.aeromaintenance.ai.engine;

import com.aeromaintenance.ai.data.SensorType;

/** Share of a component's evidence that comes from one sensor (explanation bars). */
public final class Contribution {
    public final SensorType sensor;
    public final double share;

    public Contribution(SensorType sensor, double share) {
        this.sensor = sensor;
        this.share = share;
    }
}
