package com.aeromaintenance.ai.data;

/**
 * The six telemetry channels. Limits are prototype values chosen to be plausible
 * for a narrow-body turbofan; they are not taken from any manufacturer's
 * maintenance manual. A {@code null} limit means the channel has no such limit.
 */
public enum SensorType {
    ENGINE_TEMP("Engine temperature (EGT)", "Engine temp", "°C", 0,
            690.0, 6.0, 715.0, 740.0, null, null, 660.0, 760.0),
    VIBRATION("Engine vibration", "Vibration", "mm/s", 1,
            2.1, 0.15, 3.5, 7.0, null, null, 0.0, 7.5),
    OIL_PRESSURE("Oil pressure", "Oil pressure", "PSI", 0,
            55.0, 0.9, null, null, 50.0, 45.0, 42.0, 62.0),
    RPM("Core speed (N2)", "RPM", "rpm", 0,
            10400.0, 30.0, 10650.0, 10800.0, 10150.0, null, 10050.0, 10850.0),
    FUEL_FLOW("Fuel flow", "Fuel flow", "kg/h", 0,
            2800.0, 18.0, 2950.0, 3100.0, null, null, 2700.0, 3150.0),
    HYDRAULIC_PRESSURE("Hydraulic pressure", "Hyd pressure", "PSI", 0,
            3000.0, 10.0, 3100.0, 3200.0, 2900.0, 2800.0, 2780.0, 3220.0);

    public final String label;
    public final String shortLabel;
    public final String unit;
    public final int decimals;
    /** Fleet baseline (the learned "normal") mean and standard deviation. */
    public final double baselineMean;
    public final double baselineStd;
    public final Double warnHigh;
    public final Double alarmHigh;
    public final Double warnLow;
    public final Double alarmLow;
    /** Fixed y-axis range for charts so normal and abnormal data are comparable. */
    public final double chartMin;
    public final double chartMax;

    SensorType(String label, String shortLabel, String unit, int decimals,
               double baselineMean, double baselineStd,
               Double warnHigh, Double alarmHigh, Double warnLow, Double alarmLow,
               double chartMin, double chartMax) {
        this.label = label;
        this.shortLabel = shortLabel;
        this.unit = unit;
        this.decimals = decimals;
        this.baselineMean = baselineMean;
        this.baselineStd = baselineStd;
        this.warnHigh = warnHigh;
        this.alarmHigh = alarmHigh;
        this.warnLow = warnLow;
        this.alarmLow = alarmLow;
        this.chartMin = chartMin;
        this.chartMax = chartMax;
    }

    public Zone zoneOf(double value) {
        if (alarmHigh != null && value >= alarmHigh) return Zone.ALARM;
        if (alarmLow != null && value <= alarmLow) return Zone.ALARM;
        if (warnHigh != null && value >= warnHigh) return Zone.WARN;
        if (warnLow != null && value <= warnLow) return Zone.WARN;
        return Zone.NORMAL;
    }

    public String format(double value) {
        return Format.number(value, decimals);
    }
}
