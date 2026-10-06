package com.aeromaintenance.ai.data;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/**
 * Generates realistic, fully deterministic synthetic telemetry.
 *
 * <ul>
 *   <li>120 samples per channel, one per operating hour (a 5-day operating window).</li>
 *   <li>NORMAL: every channel is Gaussian noise around the fleet baseline, plus a
 *       small periodic flight-phase component.</li>
 *   <li>ABNORMAL: the aircraft's fault signature is injected. For bearing wear the
 *       degradation starts at 25 % of the window and grows quadratically, so
 *       vibration, temperature and pressure fluctuation visibly accelerate towards
 *       the end of the window.</li>
 * </ul>
 *
 * The same aircraft and condition always produce exactly the same numbers, which
 * keeps the faculty demonstration repeatable.
 */
public final class TelemetrySimulator {

    private TelemetrySimulator() {
    }

    public static final int SAMPLES = 120;
    public static final double SAMPLE_INTERVAL_HOURS = 1.0;

    /** Two deliberate sensor glitches so the preprocessing stage has real work to do. */
    public static final int VIBRATION_SPIKE_INDEX = 41;
    public static final int TEMPERATURE_SPIKE_INDEX = 77;

    public static TelemetryWindow generate(Aircraft aircraft, DataCondition condition) {
        int seed = aircraft.id.hashCode() * 31 + condition.ordinal() * 7919;
        XorWowRandom rnd = new XorWowRandom(seed);
        int n = SAMPLES;

        double[] egt = new double[n];
        double[] vib = new double[n];
        double[] oil = new double[n];
        double[] rpm = new double[n];
        double[] fuel = new double[n];
        double[] hyd = new double[n];

        // Small per-tail offsets so every aircraft looks slightly different.
        XorWowRandom tailRnd = new XorWowRandom(aircraft.id.hashCode());
        double egtOffset = tailRnd.nextDouble(-3.0, 3.0);
        double vibOffset = tailRnd.nextDouble(-0.08, 0.08);
        double fuelOffset = tailRnd.nextDouble(-10.0, 10.0);

        List<FaultMode> faults = new ArrayList<>();
        List<Double> severities = new ArrayList<>();
        faults.add(aircraft.faultMode);
        severities.add(aircraft.faultSeverity);
        if (aircraft.secondaryFault != null) {
            faults.add(aircraft.secondaryFault);
            severities.add(aircraft.secondarySeverity);
        }

        for (int i = 0; i < n; i++) {
            double p = i / (n - 1.0);
            double phase = 2 * Math.PI * i / 12.0;

            double egtShift = 0.0, egtNoise = 4.0;
            double vibShift = 0.0, vibNoise = 0.07;
            double oilShift = 0.0, oilNoise = 0.45;
            double rpmShift = 0.0, rpmNoise = 18.0, rpmOsc = 0.0;
            double fuelShift = 0.0, fuelNoise = 10.0;
            double hydShift = 0.0, hydNoise = 6.0, hydOsc = 0.0;

            if (condition == DataCondition.ABNORMAL) {
                for (int f = 0; f < faults.size(); f++) {
                    FaultMode mode = faults.get(f);
                    double k = severities.get(f) * degradation(p, mode);
                    switch (mode) {
                        case BEARING_DEGRADATION:
                            vibShift += 2.72 * k;
                            vibNoise += 0.05 * k;
                            egtShift += 34.0 * k;
                            egtNoise += 2.0 * k;
                            oilShift -= 4.0 * k;
                            oilNoise += 0.9 * k;
                            rpmShift += 20.0 * k;
                            rpmOsc += 55.0 * k;
                            fuelShift += 40.0 * k;
                            break;
                        case HYDRAULIC_PUMP_WEAR:
                            hydShift += 33.0 * k;
                            hydNoise += 15.0 * k;
                            hydOsc += 85.0 * k;
                            break;
                        case TURBINE_EGT_DRIFT:
                            egtShift += 40.0 * k;
                            egtNoise += 2.5 * k;
                            fuelShift += 35.0 * k;
                            rpmShift -= 15.0 * k;
                            break;
                        case FUEL_PUMP_DEGRADATION:
                            fuelShift += 130.0 * k;
                            fuelNoise += 22.0 * k;
                            rpmNoise += 20.0 * k;
                            rpmShift -= 25.0 * k;
                            break;
                    }
                }
            }

            egt[i] = 690.0 + egtOffset + 2.0 * Math.sin(phase) + egtShift + egtNoise * rnd.nextGaussian();
            vib[i] = 2.1 + vibOffset + 0.04 * Math.sin(phase + 1.0) + vibShift + vibNoise * rnd.nextGaussian();
            oil[i] = 55.0 + 0.3 * Math.cos(phase) + oilShift + oilNoise * rnd.nextGaussian();
            rpm[i] = 10400.0 + 8.0 * Math.sin(phase) + rpmShift + rpmOsc * Math.sin(i * 0.9) + rpmNoise * rnd.nextGaussian();
            fuel[i] = 2800.0 + fuelOffset + 6.0 * Math.sin(phase) + fuelShift + fuelNoise * rnd.nextGaussian();
            hyd[i] = 3000.0 + 3.0 * Math.cos(phase) + hydShift + hydOsc * Math.sin(i * 1.3) + hydNoise * rnd.nextGaussian();
        }

        // Inject the two sensor glitches (single-sample spikes).
        vib[VIBRATION_SPIKE_INDEX] += 1.1;
        egt[TEMPERATURE_SPIKE_INDEX] += 22.0;

        EnumMap<SensorType, double[]> channels = new EnumMap<>(SensorType.class);
        channels.put(SensorType.ENGINE_TEMP, egt);
        channels.put(SensorType.VIBRATION, vib);
        channels.put(SensorType.OIL_PRESSURE, oil);
        channels.put(SensorType.RPM, rpm);
        channels.put(SensorType.FUEL_FLOW, fuel);
        channels.put(SensorType.HYDRAULIC_PRESSURE, hyd);
        return new TelemetryWindow(aircraft.id, condition, SAMPLE_INTERVAL_HOURS, channels);
    }

    /**
     * Degradation profile of a fault: 0 before onset, then a ramp to 1 at the end
     * of the window. Bearing wear accelerates (quadratic); EGT and fuel-flow drift
     * are close to linear.
     */
    public static double degradation(double p, FaultMode mode) {
        if (p < mode.onset) return 0.0;
        return Math.pow((p - mode.onset) / (1.0 - mode.onset), mode.exponent);
    }

    /** CSV export of a window (used for the sample dataset in /docs/sample-data). */
    public static String toCsv(TelemetryWindow window) {
        StringBuilder sb = new StringBuilder("hour_offset");
        for (SensorType s : window.channels.keySet()) {
            String unit = s.unit.replace("/", "_per_").replace("°", "deg_").replace(" ", "");
            sb.append(',').append(s.name().toLowerCase(Locale.ROOT)).append('_').append(unit);
        }
        sb.append('\n');
        for (int i = 0; i < window.size(); i++) {
            double hoursAgo = (window.size() - 1 - i) * window.sampleIntervalHours;
            sb.append(hoursAgo == 0.0 ? "0" : "-" + formatPlain(hoursAgo, 0));
            for (SensorType s : window.channels.keySet()) {
                sb.append(',').append(formatPlain(window.channel(s)[i], s.decimals + 1));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String formatPlain(double v, int decimals) {
        double f = Math.pow(10.0, decimals);
        double r = Math.round(v * f) / f;
        return decimals == 0 ? Long.toString(Math.round(r)) : Double.toString(r);
    }
}
