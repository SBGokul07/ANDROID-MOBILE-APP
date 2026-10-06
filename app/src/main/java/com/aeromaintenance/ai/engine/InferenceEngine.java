package com.aeromaintenance.ai.engine;

import com.aeromaintenance.ai.data.ComponentType;
import com.aeromaintenance.ai.data.Format;
import com.aeromaintenance.ai.data.RiskLevel;
import com.aeromaintenance.ai.data.SensorType;
import com.aeromaintenance.ai.data.TelemetryWindow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Prototype inference engine.
 *
 * <p>IMPORTANT: this is <b>simulated</b> AI inference. There is no trained neural
 * network inside the app. The engine implements the same pipeline a production
 * system would use (preprocess → features → normal-behaviour model → anomaly
 * score → component health → risk → RUL → recommendation), but the
 * "normal-behaviour model" is the fleet baseline statistics in {@link SensorType},
 * and RUL comes from a linear degradation-trend extrapolation. It is deterministic
 * and fully explainable, which is what a classroom demonstration needs.</p>
 *
 * <p>The class has no Android dependencies, so it is unit-tested on the JVM and
 * could later be replaced by a TensorFlow Lite model behind the same method.</p>
 */
public final class InferenceEngine {

    private InferenceEngine() {
    }

    public static final String MODEL_NAME = "Predictive Maintenance Neural Network";
    public static final String MODEL_TAG = "Prototype / Simulated AI Inference";
    public static final String MODEL_VERSION = "PM-NN-proto 0.9";

    /** Hours of recent data used for features and trend fitting. */
    public static final int RECENT = 24;

    /** Deviation (in σ) at which the anomaly score crosses 0.5. */
    public static final double ANOMALY_MIDPOINT = 4.65;
    public static final double ANOMALY_STEEPNESS = 0.56;

    public static final double CONF_W_CERTAINTY = 0.48;
    public static final double CONF_W_EVIDENCE = 0.30;
    public static final double CONF_W_QUALITY = 0.22;

    public static final double ALERT_THRESHOLD = 0.40;
    public static final double HIGH_THRESHOLD = 0.75;

    /** Health curve exponent: health = 100 × (1 − marginConsumed^γ). */
    public static final double HEALTH_GAMMA = 1.6;

    // Hydraulic ripple: rolling standard deviation of hydraulic pressure.
    private static final int RIPPLE_WINDOW = 24;
    public static final double RIPPLE_NOMINAL = 6.5;
    public static final double RIPPLE_LIMIT = 120.0;

    // Component indicator nominal values and limits (prototype values).
    public static final double BEARING_NOMINAL = 2.1;
    public static final double BEARING_LIMIT = 7.0;
    public static final double EGT_NOMINAL = 690.0;
    public static final double EGT_LIMIT = 800.0;
    public static final double FUEL_NOMINAL = 2800.0;
    public static final double FUEL_LIMIT = 3150.0;

    /** Deviation (in σ) at which the anomaly score reaches {@link #ALERT_THRESHOLD}. */
    public static double detectionSigma() {
        return ANOMALY_MIDPOINT + Math.log(ALERT_THRESHOLD / (1 - ALERT_THRESHOLD)) / ANOMALY_STEEPNESS;
    }

    /** Relative importance of each channel in the deviation index. */
    public static final Map<SensorType, Double> SENSOR_WEIGHTS;

    /** Fault-signature matrix: which channels evidence each component's degradation. */
    public static final Map<ComponentType, Map<SensorType, Double>> COMPONENT_SIGNATURES;

    /**
     * Trend window per component. Bearing wear accelerates, so only the most recent
     * 24 h describe its current rate; EGT, fuel flow and ripple drift slowly, so a
     * longer window gives a steadier slope.
     */
    public static final Map<ComponentType, Integer> TREND_WINDOW_HOURS;

    static {
        LinkedHashMap<SensorType, Double> w = new LinkedHashMap<>();
        w.put(SensorType.VIBRATION, 0.30);
        w.put(SensorType.ENGINE_TEMP, 0.20);
        w.put(SensorType.OIL_PRESSURE, 0.15);
        w.put(SensorType.HYDRAULIC_PRESSURE, 0.15);
        w.put(SensorType.RPM, 0.10);
        w.put(SensorType.FUEL_FLOW, 0.10);
        SENSOR_WEIGHTS = Collections.unmodifiableMap(w);

        LinkedHashMap<ComponentType, Map<SensorType, Double>> sig = new LinkedHashMap<>();
        LinkedHashMap<SensorType, Double> bearing = new LinkedHashMap<>();
        bearing.put(SensorType.VIBRATION, 0.55);
        bearing.put(SensorType.OIL_PRESSURE, 0.25);
        bearing.put(SensorType.ENGINE_TEMP, 0.20);
        LinkedHashMap<SensorType, Double> pump = new LinkedHashMap<>();
        pump.put(SensorType.HYDRAULIC_PRESSURE, 1.0);
        LinkedHashMap<SensorType, Double> turbine = new LinkedHashMap<>();
        turbine.put(SensorType.ENGINE_TEMP, 0.60);
        turbine.put(SensorType.FUEL_FLOW, 0.25);
        turbine.put(SensorType.RPM, 0.15);
        LinkedHashMap<SensorType, Double> fuel = new LinkedHashMap<>();
        fuel.put(SensorType.FUEL_FLOW, 0.70);
        fuel.put(SensorType.RPM, 0.30);
        sig.put(ComponentType.ENGINE_BEARING, Collections.unmodifiableMap(bearing));
        sig.put(ComponentType.HYDRAULIC_PUMP, Collections.unmodifiableMap(pump));
        sig.put(ComponentType.TURBINE, Collections.unmodifiableMap(turbine));
        sig.put(ComponentType.FUEL_PUMP, Collections.unmodifiableMap(fuel));
        COMPONENT_SIGNATURES = Collections.unmodifiableMap(sig);

        EnumMap<ComponentType, Integer> tw = new EnumMap<>(ComponentType.class);
        tw.put(ComponentType.ENGINE_BEARING, 24);
        tw.put(ComponentType.HYDRAULIC_PUMP, 48);
        tw.put(ComponentType.TURBINE, 72);
        tw.put(ComponentType.FUEL_PUMP, 72);
        TREND_WINDOW_HOURS = Collections.unmodifiableMap(tw);
    }

    // =====================================================================
    // Public API
    // =====================================================================

    /** Stage 2: spike removal, smoothing and the hydraulic ripple signal. */
    public static PreprocessedWindow preprocess(TelemetryWindow window) {
        EnumMap<SensorType, double[]> clean = new EnumMap<>(SensorType.class);
        EnumMap<SensorType, double[]> smoothed = new EnumMap<>(SensorType.class);
        EnumMap<SensorType, List<Integer>> spikeIdx = new EnumMap<>(SensorType.class);
        int spikes = 0;
        for (Map.Entry<SensorType, double[]> e : window.channels.entrySet()) {
            List<Integer> idx = new ArrayList<>();
            double[] c = hampel(e.getValue(), 5, 4.5, idx);
            clean.put(e.getKey(), c);
            spikeIdx.put(e.getKey(), Collections.unmodifiableList(idx));
            spikes += idx.size();
            smoothed.put(e.getKey(), movingAverage(c, 5));
        }
        double[] ripple = movingAverage(rollingRipple(clean.get(SensorType.HYDRAULIC_PRESSURE), RIPPLE_WINDOW), 9);
        return new PreprocessedWindow(clean, smoothed, ripple, spikes, spikeIdx);
    }

    /** Runs the whole pipeline on one telemetry window. */
    public static AnalysisResult analyze(TelemetryWindow window) {
        PreprocessedWindow pre = preprocess(window);
        int n = window.size();
        int channelCount = window.channels.size();

        // ---- Feature extraction ---------------------------------------------
        List<SensorFeatures> features = new ArrayList<>();
        EnumMap<SensorType, SensorFeatures> bySensor = new EnumMap<>(SensorType.class);
        for (SensorType sensor : window.channels.keySet()) {
            SensorFeatures f = extractFeatures(sensor, pre.clean.get(sensor), pre.smoothed.get(sensor));
            features.add(f);
            bySensor.put(sensor, f);
        }

        // ---- Normal-behaviour model: weighted deviation index ---------------
        double wSum = 0.0;
        for (double w : SENSOR_WEIGHTS.values()) wSum += w;
        double weighted = 0.0;
        for (SensorFeatures f : features) {
            weighted += SENSOR_WEIGHTS.get(f.sensor) * f.deviation * f.deviation;
        }
        double deviationIndex = Math.sqrt(weighted / wSum);

        // ---- Anomaly detection -----------------------------------------------
        double anomalyScore = 1.0 / (1.0 + Math.exp(-ANOMALY_STEEPNESS * (deviationIndex - ANOMALY_MIDPOINT)));
        RiskLevel scoreRisk;
        if (anomalyScore >= HIGH_THRESHOLD) scoreRisk = RiskLevel.HIGH;
        else if (anomalyScore >= ALERT_THRESHOLD) scoreRisk = RiskLevel.MEDIUM;
        else scoreRisk = RiskLevel.LOW;

        // ---- Component health, risk and RUL ---------------------------------
        List<ComponentAssessment> components = new ArrayList<>();
        for (ComponentType c : ComponentType.values()) {
            components.add(assessComponent(c, bySensor, pre));
        }
        List<ComponentAssessment> ranked = new ArrayList<>(components);
        Collections.sort(ranked, RISK_ORDER);
        ComponentAssessment primary = ranked.get(0);
        RiskLevel risk = scoreRisk.rank > primary.risk.rank ? scoreRisk : primary.risk;

        // ---- Onset (CUSUM change-point on the primary indicator) -------------
        Integer onsetIndex = risk != RiskLevel.LOW ? cusumOnset(primary.component, pre) : null;
        Integer onsetHoursAgo = onsetIndex == null ? null
                : (int) Math.round((n - 1 - onsetIndex) * window.sampleIntervalHours);

        // ---- Confidence -------------------------------------------------------
        // Three weighted factors, each 0..1:
        //  1. decision certainty: how far the anomaly score is from 0.5
        //  2. evidence: R² of the degradation trend (abnormal) or closeness to baseline (normal)
        //  3. data quality: share of samples that did not need spike correction
        double certainty = Math.max(anomalyScore, 1.0 - anomalyScore);
        double evidence = risk != RiskLevel.LOW
                ? primary.trendR2
                : clamp(1.0 - deviationIndex / ANOMALY_MIDPOINT, 0.0, 1.0);
        double dataQuality = 1.0 - (double) pre.spikesRemoved / (n * channelCount);
        double confidence = (CONF_W_CERTAINTY * certainty + CONF_W_EVIDENCE * evidence + CONF_W_QUALITY * dataQuality)
                / (CONF_W_CERTAINTY + CONF_W_EVIDENCE + CONF_W_QUALITY);

        String explanation = explain(primary, risk, bySensor, pre, deviationIndex);

        StringBuilder health = new StringBuilder();
        for (ComponentAssessment c : components) {
            if (health.length() > 0) health.append(", ");
            health.append(c.component.label).append(' ').append(c.health).append('%');
        }

        List<PipelineStage> stages = Arrays.asList(
                new PipelineStage("Data acquisition",
                        (n * channelCount) + " samples: " + channelCount + " channels × " + n + " operating hours, "
                                + window.condition.label.toLowerCase(Locale.ROOT)),
                new PipelineStage("Preprocessing",
                        "Hampel filter removed " + pre.spikesRemoved + " sensor spike" + (pre.spikesRemoved == 1 ? "" : "s")
                                + "; 5-point moving average; z-score against fleet baseline"),
                new PipelineStage("Feature extraction",
                        (features.size() * 3) + " features: level, trend and volatility for " + features.size()
                                + " channels over the last " + RECENT + " h"),
                new PipelineStage("Time-series model",
                        "Normal-behaviour model: weighted deviation " + fmt(deviationIndex, 1) + "σ from the learned baseline"),
                new PipelineStage("Anomaly detection",
                        "Anomaly score " + fmt(anomalyScore, 2) + " (alert threshold " + fmt(ALERT_THRESHOLD, 2) + ")"
                                + (onsetHoursAgo != null ? "; onset ≈ " + onsetHoursAgo + " h ago" : "; no change-point found")),
                new PipelineStage("Component health", health.toString()),
                new PipelineStage("Failure-risk prediction", primary.component.label + " → " + risk.label + " risk"),
                new PipelineStage("RUL estimation",
                        primary.indicatorName + " trend " + Format.signed(primary.trendPerHour, primary.indicatorDecimals + 2) + " "
                                + primary.indicatorUnit + "/h → limit " + fmt(primary.indicatorLimit, primary.indicatorDecimals) + " "
                                + primary.indicatorUnit + " in " + rulText(primary.rulHours)),
                new PipelineStage("Maintenance recommendation", primary.recommendation));

        return new AnalysisResult(window.aircraftId, window.condition, anomalyScore, deviationIndex, risk,
                primary, components, features, confidence, onsetHoursAgo, explanation, stages,
                pre.spikesRemoved, n * channelCount);
    }

    /** Most severe first: highest risk, then lowest health, then shortest RUL. */
    public static final Comparator<ComponentAssessment> RISK_ORDER = new Comparator<ComponentAssessment>() {
        @Override
        public int compare(ComponentAssessment a, ComponentAssessment b) {
            if (a.risk.rank != b.risk.rank) return Integer.compare(b.risk.rank, a.risk.rank);
            if (a.health != b.health) return Integer.compare(a.health, b.health);
            return Integer.compare(a.rulHours, b.rulHours);
        }
    };

    /** Highest risk first, then shortest RUL (tables and charts). */
    public static final Comparator<ComponentAssessment> RISK_THEN_RUL = new Comparator<ComponentAssessment>() {
        @Override
        public int compare(ComponentAssessment a, ComponentAssessment b) {
            if (a.risk.rank != b.risk.rank) return Integer.compare(b.risk.rank, a.risk.rank);
            return Integer.compare(a.rulHours, b.rulHours);
        }
    };

    public static String rulText(int hours) {
        return hours >= 999 ? "999+ h" : hours + " h";
    }

    public static String fmt(double v, int decimals) {
        return Format.number(v, decimals);
    }

    /** Health-indicator history of a component, for the RUL projection chart. */
    public static double[] indicatorHistory(ComponentType c, PreprocessedWindow pre) {
        switch (c) {
            case ENGINE_BEARING:
                return pre.smoothed.get(SensorType.VIBRATION);
            case TURBINE:
                return pre.smoothed.get(SensorType.ENGINE_TEMP);
            case FUEL_PUMP:
                return pre.smoothed.get(SensorType.FUEL_FLOW);
            case HYDRAULIC_PUMP:
            default:
                return pre.hydraulicRipple;
        }
    }

    public static double indicatorNominal(ComponentType c) {
        switch (c) {
            case ENGINE_BEARING:
                return BEARING_NOMINAL;
            case TURBINE:
                return EGT_NOMINAL;
            case FUEL_PUMP:
                return FUEL_NOMINAL;
            case HYDRAULIC_PUMP:
            default:
                return RIPPLE_NOMINAL;
        }
    }

    // =====================================================================
    // Feature extraction
    // =====================================================================

    private static SensorFeatures extractFeatures(SensorType sensor, double[] clean, double[] smooth) {
        int n = smooth.length;
        double[] recent = Arrays.copyOfRange(smooth, n - RECENT, n);
        Fit fit = linearFit(recent);
        double current = fit.intercept + fit.slope * (RECENT - 1);
        double levelZ = (average(recent) - sensor.baselineMean) / sensor.baselineStd;
        double[] diffs = new double[RECENT - 1];
        for (int i = 0; i < diffs.length; i++) {
            diffs[i] = clean[n - RECENT + i + 1] - clean[n - RECENT + i];
        }
        double volatility = std(diffs) / (sensor.baselineStd * Math.sqrt(2.0));
        double deviation = Math.sqrt(levelZ * levelZ + Math.pow(2.0 * Math.max(0.0, volatility - 1.0), 2));
        return new SensorFeatures(sensor, sensor.baselineMean, average(Arrays.copyOfRange(smooth, 0, 12)),
                current, levelZ, fit.slope, fit.r2, volatility, deviation);
    }

    // =====================================================================
    // Component assessment
    // =====================================================================

    private static final class Indicator {
        final String name;
        final String unit;
        final int decimals;
        final double current;
        final double slope;
        final double r2;
        final double nominal;
        final double limit;
        /** Minimum wear rate, so a flat trend never gives an infinite RUL. */
        final double wearFloor;

        Indicator(String name, String unit, int decimals, double[] series, int window,
                  double nominal, double limit, double wearFloor) {
            int n = series.length;
            Fit fit = linearFit(Arrays.copyOfRange(series, n - window, n));
            this.name = name;
            this.unit = unit;
            this.decimals = decimals;
            this.current = fit.intercept + fit.slope * (window - 1);
            this.slope = fit.slope;
            this.r2 = fit.r2;
            this.nominal = nominal;
            this.limit = limit;
            this.wearFloor = wearFloor;
        }
    }

    private static Indicator indicatorFor(ComponentType c, PreprocessedWindow pre) {
        int w = TREND_WINDOW_HOURS.get(c);
        switch (c) {
            case ENGINE_BEARING:
                return new Indicator("Vibration", "mm/s", 1, pre.smoothed.get(SensorType.VIBRATION), w,
                        BEARING_NOMINAL, BEARING_LIMIT, 0.008);
            case TURBINE:
                return new Indicator("Engine temperature", "°C", 0, pre.smoothed.get(SensorType.ENGINE_TEMP), w,
                        EGT_NOMINAL, EGT_LIMIT, 0.15);
            case FUEL_PUMP:
                return new Indicator("Fuel flow", "kg/h", 0, pre.smoothed.get(SensorType.FUEL_FLOW), w,
                        FUEL_NOMINAL, FUEL_LIMIT, 0.6);
            case HYDRAULIC_PUMP:
            default:
                return new Indicator("Pressure ripple", "PSI", 0, pre.hydraulicRipple, w,
                        RIPPLE_NOMINAL, RIPPLE_LIMIT, 0.15);
        }
    }

    private static ComponentAssessment assessComponent(ComponentType c, Map<SensorType, SensorFeatures> f,
                                                       PreprocessedWindow pre) {
        Indicator ind = indicatorFor(c, pre);
        double margin = clamp((ind.current - ind.nominal) / (ind.limit - ind.nominal), 0.0, 1.0);
        int health = Math.max(1, Math.min(98, (int) Math.round(100.0 * (1.0 - Math.pow(margin, HEALTH_GAMMA)))));
        double rate = Math.max(ind.slope, ind.wearFloor);
        // Rounded down: a remaining-life estimate should never be optimistic.
        int rul = (int) Math.floor(clamp((ind.limit - ind.current) / rate, 0.0, 999.0));
        RiskLevel risk;
        if (rul < 72 || health < 65) risk = RiskLevel.HIGH;
        else if (rul < 250 || health < 85) risk = RiskLevel.MEDIUM;
        else risk = RiskLevel.LOW;

        Map<SensorType, Double> signature = COMPONENT_SIGNATURES.get(c);
        List<SensorType> sensors = new ArrayList<>();
        List<Double> raw = new ArrayList<>();
        double total = 0.0;
        for (Map.Entry<SensorType, Double> e : signature.entrySet()) {
            double value = e.getValue() * Math.max(f.get(e.getKey()).deviation, 0.05);
            sensors.add(e.getKey());
            raw.add(value);
            total += value;
        }
        List<Contribution> contributions = new ArrayList<>();
        for (int i = 0; i < sensors.size(); i++) {
            contributions.add(new Contribution(sensors.get(i), raw.get(i) / total));
        }
        Collections.sort(contributions, new Comparator<Contribution>() {
            @Override
            public int compare(Contribution a, Contribution b) {
                return Double.compare(b.share, a.share);
            }
        });

        Recommendations.Text text = Recommendations.forComponent(c, risk);
        return new ComponentAssessment(c, health, risk, rul, ind.name, ind.unit, ind.decimals,
                ind.current, ind.limit, ind.slope, ind.r2, text.condition, text.recommendation,
                text.shortAction, text.workPackage, contributions);
    }

    /** One-sided CUSUM change-point test. Returns the sample index where degradation began. */
    private static Integer cusumOnset(ComponentType c, PreprocessedWindow pre) {
        double[] series;
        double sd;
        switch (c) {
            case ENGINE_BEARING:
                series = pre.smoothed.get(SensorType.VIBRATION);
                sd = SensorType.VIBRATION.baselineStd;
                break;
            case TURBINE:
                series = pre.smoothed.get(SensorType.ENGINE_TEMP);
                sd = SensorType.ENGINE_TEMP.baselineStd;
                break;
            case FUEL_PUMP:
                series = pre.smoothed.get(SensorType.FUEL_FLOW);
                sd = SensorType.FUEL_FLOW.baselineStd;
                break;
            case HYDRAULIC_PUMP:
            default:
                series = pre.hydraulicRipple;
                sd = 2.5;
                break;
        }
        // Use the first 24 h as the local reference so per-tail offsets do not trigger it.
        double ref = average(Arrays.copyOfRange(series, 0, 24));
        double k = 0.5;
        double h = 8.0;
        double s = 0.0;
        int lastZero = 0;
        for (int i = 0; i < series.length; i++) {
            double z = (series[i] - ref) / sd;
            s = Math.max(0.0, s + z - k);
            if (s == 0.0) lastZero = i;
            if (s > h) return lastZero;
        }
        return null;
    }

    // =====================================================================
    // Natural-language explanation (template-based, generated from features)
    // =====================================================================

    private static String explain(ComponentAssessment primary, RiskLevel risk, Map<SensorType, SensorFeatures> f,
                                  PreprocessedWindow pre, double deviationIndex) {
        if (risk == RiskLevel.LOW) {
            return "All six channels are inside their normal bands. Overall deviation from the fleet baseline is "
                    + fmt(deviationIndex, 1) + "σ, below the " + fmt(detectionSigma(), 1) + "σ detection threshold, "
                    + "and no sustained degradation trend was found. Continue routine monitoring.";
        }
        SensorFeatures vib = f.get(SensorType.VIBRATION);
        SensorFeatures egt = f.get(SensorType.ENGINE_TEMP);
        SensorFeatures oil = f.get(SensorType.OIL_PRESSURE);
        SensorFeatures fuel = f.get(SensorType.FUEL_FLOW);
        SensorFeatures hyd = f.get(SensorType.HYDRAULIC_PRESSURE);
        switch (primary.component) {
            case ENGINE_BEARING: {
                int pct = (int) Math.round((vib.current - vib.windowStart) / vib.windowStart * 100);
                return "Vibration rose from " + fmt(vib.windowStart, 1) + " to " + fmt(vib.current, 1) + " mm/s (+" + pct
                        + "%) and is still climbing at " + fmt(vib.slopePerHour, 3) + " mm/s per hour. Engine temperature is up "
                        + fmt(egt.current - egt.windowStart, 0) + " °C and oil pressure is down "
                        + fmt(oil.windowStart - oil.current, 0) + " PSI over the same period. This combined pattern "
                        + "matches the signature of progressive bearing wear.";
            }
            case HYDRAULIC_PUMP: {
                double[] r = pre.hydraulicRipple;
                double r0 = average(Arrays.copyOfRange(r, RIPPLE_WINDOW, RIPPLE_WINDOW + 12));
                double ratio = primary.indicatorValue / RIPPLE_NOMINAL;
                return "Hydraulic pressure ripple has grown from about " + fmt(r0, 0) + " to " + fmt(primary.indicatorValue, 0)
                        + " PSI (" + fmt(ratio, 1) + "× the fleet baseline) while mean pressure moved only "
                        + fmt(hyd.current - hyd.windowStart, 0) + " PSI. Growing ripple at stable pressure is an early "
                        + "indicator of pump wear.";
            }
            case TURBINE:
                return "Engine temperature has risen " + fmt(egt.current - egt.windowStart, 0) + " °C to " + fmt(egt.current, 0)
                        + " °C, drifting at " + fmt(egt.slopePerHour, 2) + " °C per hour, with fuel flow up "
                        + fmt(fuel.current - fuel.windowStart, 0) + " kg/h. EGT margin to the "
                        + fmt(primary.indicatorLimit, 0) + " °C limit is eroding.";
            case FUEL_PUMP:
            default:
                return "Fuel flow is " + fmt(fuel.current - fuel.baseline, 0) + " kg/h above baseline and fluctuating "
                        + fmt(fuel.volatilityRatio, 1) + "× more than normal while core speed is steady. This points to "
                        + "fuel pump or metering wear.";
        }
    }

    // =====================================================================
    // Numerics
    // =====================================================================

    /** Result of an ordinary least-squares line fit. */
    public static final class Fit {
        public final double slope;
        public final double intercept;
        public final double r2;

        Fit(double slope, double intercept, double r2) {
            this.slope = slope;
            this.intercept = intercept;
            this.r2 = r2;
        }
    }

    public static Fit linearFit(double[] y) {
        int n = y.length;
        double xm = (n - 1) / 2.0;
        double ym = average(y);
        double sxy = 0.0;
        double sxx = 0.0;
        for (int i = 0; i < n; i++) {
            sxy += (i - xm) * (y[i] - ym);
            sxx += (i - xm) * (i - xm);
        }
        double slope = sxx == 0.0 ? 0.0 : sxy / sxx;
        double intercept = ym - slope * xm;
        double ssRes = 0.0;
        double ssTot = 0.0;
        for (int i = 0; i < n; i++) {
            double pred = intercept + slope * i;
            ssRes += Math.pow(y[i] - pred, 2);
            ssTot += Math.pow(y[i] - ym, 2);
        }
        double r2 = ssTot == 0.0 ? 0.0 : clamp(1.0 - ssRes / ssTot, 0.0, 1.0);
        return new Fit(slope, intercept, r2);
    }

    /**
     * Hampel filter: replaces samples more than {@code t} scaled MADs from the local
     * median (window ±{@code half}). Indices of replaced samples are added to {@code replaced}.
     */
    public static double[] hampel(double[] x, int half, double t, List<Integer> replaced) {
        double[] y = x.clone();
        for (int i = 0; i < x.length; i++) {
            int lo = Math.max(0, i - half);
            int hi = Math.min(x.length - 1, i + half);
            double[] w = Arrays.copyOfRange(x, lo, hi + 1);
            double med = median(w);
            double[] dev = new double[w.length];
            for (int j = 0; j < w.length; j++) dev[j] = Math.abs(w[j] - med);
            double mad = 1.4826 * median(dev);
            if (mad > 0 && Math.abs(x[i] - med) > t * mad) {
                y[i] = med;
                replaced.add(i);
            }
        }
        return y;
    }

    /** Centred moving average; the window shrinks at the edges. */
    public static double[] movingAverage(double[] x, int window) {
        int half = window / 2;
        double[] out = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            int lo = Math.max(0, i - half);
            int hi = Math.min(x.length - 1, i + half);
            double s = 0.0;
            for (int j = lo; j <= hi; j++) s += x[j];
            out[i] = s / (hi - lo + 1);
        }
        return out;
    }

    /**
     * Pressure ripple: rolling RMS of sample-to-sample differences divided by √2.
     * For white noise this equals the noise standard deviation, and it ignores slow
     * changes in mean pressure.
     */
    public static double[] rollingRipple(double[] x, int window) {
        double[] out = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            if (i < 1) {
                out[i] = RIPPLE_NOMINAL;
                continue;
            }
            int lo = Math.max(1, i - window + 1);
            double s = 0.0;
            for (int j = lo; j <= i; j++) s += Math.pow(x[j] - x[j - 1], 2);
            out[i] = Math.sqrt(s / (i - lo + 1) / 2.0);
        }
        return out;
    }

    static double average(double[] v) {
        double sum = 0.0;
        for (double d : v) sum += d;
        return sum / v.length;
    }

    private static double median(double[] v) {
        double[] s = v.clone();
        Arrays.sort(s);
        int m = s.length / 2;
        return s.length % 2 == 0 ? (s[m - 1] + s[m]) / 2.0 : s[m];
    }

    private static double std(double[] v) {
        if (v.length < 2) return 0.0;
        double m = average(v);
        double sum = 0.0;
        for (double d : v) sum += (d - m) * (d - m);
        return Math.sqrt(sum / (v.length - 1));
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
