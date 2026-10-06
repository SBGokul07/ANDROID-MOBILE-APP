package com.aeromaintenance.ai.engine

import com.aeromaintenance.ai.data.ComponentType
import com.aeromaintenance.ai.data.DataCondition
import com.aeromaintenance.ai.data.RiskLevel
import com.aeromaintenance.ai.data.SensorType
import com.aeromaintenance.ai.data.TelemetryWindow
import com.aeromaintenance.ai.data.formatNumber
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class SensorFeatures(
    val sensor: SensorType,
    val baseline: Double,
    /** Smoothed value at the start of the window (mean of the first 12 h). */
    val windowStart: Double,
    /** Trend-filtered current value (linear fit evaluated at the latest sample). */
    val current: Double,
    /** Mean z-score of the last 24 h against the fleet baseline. */
    val levelZ: Double,
    /** Linear trend over the last 24 h, in sensor units per operating hour. */
    val slopePerHour: Double,
    /** Goodness of fit of that trend (0..1). */
    val r2: Double,
    /** Short-term fluctuation relative to the baseline fluctuation (1.0 = normal). */
    val volatilityRatio: Double,
    /** Combined deviation index for this channel, in baseline standard deviations. */
    val deviation: Double,
)

data class Contribution(val sensor: SensorType, val share: Double)

data class ComponentAssessment(
    val component: ComponentType,
    val health: Int,
    val risk: RiskLevel,
    val rulHours: Int,
    val indicatorName: String,
    val indicatorUnit: String,
    val indicatorDecimals: Int,
    val indicatorValue: Double,
    val indicatorLimit: Double,
    val trendPerHour: Double,
    val trendR2: Double,
    val condition: String,
    val recommendation: String,
    val shortAction: String,
    val workPackage: List<String>,
    val contributions: List<Contribution>,
)

data class PipelineStage(val title: String, val detail: String)

data class AnalysisResult(
    val aircraftId: String,
    val condition: DataCondition,
    val anomalyScore: Double,
    val deviationIndex: Double,
    val risk: RiskLevel,
    val primary: ComponentAssessment,
    val components: List<ComponentAssessment>,
    val features: List<SensorFeatures>,
    val confidence: Double,
    val onsetHoursAgo: Int?,
    val explanation: String,
    val stages: List<PipelineStage>,
    val spikesRemoved: Int,
    val samplesAnalysed: Int,
)

/** Output of the preprocessing stage, exposed so the UI can draw the cleaned signal. */
data class PreprocessedWindow(
    val clean: Map<SensorType, DoubleArray>,
    val smoothed: Map<SensorType, DoubleArray>,
    val hydraulicRipple: DoubleArray,
    val spikesRemoved: Int,
    val spikeIndices: Map<SensorType, List<Int>>,
)

/**
 * Prototype inference engine.
 *
 * IMPORTANT: this is *simulated* AI inference. There is no trained neural network
 * inside the app. The engine implements the same pipeline a production system
 * would use (preprocess → features → normal-behaviour model → anomaly score →
 * component health → risk → RUL → recommendation), but the "normal-behaviour
 * model" is the fleet baseline statistics in [SensorType], and RUL comes from a
 * linear degradation-trend extrapolation. It is deterministic and fully
 * explainable, which is what a classroom demonstration needs.
 */
object InferenceEngine {

    const val MODEL_NAME = "Predictive Maintenance Neural Network"
    const val MODEL_TAG = "Prototype / Simulated AI Inference"
    const val MODEL_VERSION = "PM-NN-proto 0.9"

    /** Hours of recent data used for features and trend fitting. */
    const val RECENT = 24

    /** Deviation (in σ) at which the anomaly score crosses 0.5. */
    const val ANOMALY_MIDPOINT = 4.65
    const val ANOMALY_STEEPNESS = 0.56

    const val CONF_W_CERTAINTY = 0.48
    const val CONF_W_EVIDENCE = 0.30
    const val CONF_W_QUALITY = 0.22

    const val ALERT_THRESHOLD = 0.40
    const val HIGH_THRESHOLD = 0.75

    /** Deviation (in σ) at which the anomaly score reaches [ALERT_THRESHOLD]. */
    val detectionSigma: Double
        get() = ANOMALY_MIDPOINT + ln(ALERT_THRESHOLD / (1 - ALERT_THRESHOLD)) / ANOMALY_STEEPNESS

    /** Health curve exponent: health = 100 × (1 − marginConsumed^γ). */
    const val HEALTH_GAMMA = 1.6

    /** Relative importance of each channel in the deviation index. */
    val sensorWeights: Map<SensorType, Double> = mapOf(
        SensorType.VIBRATION to 0.30,
        SensorType.ENGINE_TEMP to 0.20,
        SensorType.OIL_PRESSURE to 0.15,
        SensorType.HYDRAULIC_PRESSURE to 0.15,
        SensorType.RPM to 0.10,
        SensorType.FUEL_FLOW to 0.10,
    )

    /** Fault-signature matrix: which channels evidence each component's degradation. */
    val componentSignatures: Map<ComponentType, Map<SensorType, Double>> = mapOf(
        ComponentType.ENGINE_BEARING to mapOf(
            SensorType.VIBRATION to 0.55, SensorType.OIL_PRESSURE to 0.25, SensorType.ENGINE_TEMP to 0.20,
        ),
        ComponentType.HYDRAULIC_PUMP to mapOf(SensorType.HYDRAULIC_PRESSURE to 1.0),
        ComponentType.TURBINE to mapOf(
            SensorType.ENGINE_TEMP to 0.60, SensorType.FUEL_FLOW to 0.25, SensorType.RPM to 0.15,
        ),
        ComponentType.FUEL_PUMP to mapOf(SensorType.FUEL_FLOW to 0.70, SensorType.RPM to 0.30),
    )

    // Hydraulic ripple: rolling standard deviation of hydraulic pressure.
    private const val RIPPLE_WINDOW = 24
    const val RIPPLE_NOMINAL = 6.5
    const val RIPPLE_LIMIT = 120.0

    // Component indicator nominal values and limits (prototype values).
    const val BEARING_NOMINAL = 2.1
    const val BEARING_LIMIT = 7.0
    const val EGT_NOMINAL = 690.0
    const val EGT_LIMIT = 800.0
    const val FUEL_NOMINAL = 2800.0
    const val FUEL_LIMIT = 3150.0

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    fun preprocess(window: TelemetryWindow): PreprocessedWindow {
        val clean = linkedMapOf<SensorType, DoubleArray>()
        val smoothed = linkedMapOf<SensorType, DoubleArray>()
        val spikeIdx = linkedMapOf<SensorType, List<Int>>()
        var spikes = 0
        for ((sensor, raw) in window.channels) {
            val (c, idx) = hampel(raw)
            clean[sensor] = c
            spikeIdx[sensor] = idx
            spikes += idx.size
            smoothed[sensor] = movingAverage(c, 5)
        }
        val ripple = movingAverage(rollingRipple(clean.getValue(SensorType.HYDRAULIC_PRESSURE), RIPPLE_WINDOW), 9)
        return PreprocessedWindow(clean, smoothed, ripple, spikes, spikeIdx)
    }

    fun analyze(window: TelemetryWindow): AnalysisResult {
        val pre = preprocess(window)
        val n = window.size

        // ---- Feature extraction -------------------------------------------------
        val features = window.channels.keys.map { sensor ->
            extractFeatures(sensor, pre.clean.getValue(sensor), pre.smoothed.getValue(sensor))
        }
        val bySensor = features.associateBy { it.sensor }

        // ---- Normal-behaviour model: weighted deviation index -----------------
        val wSum = sensorWeights.values.sum()
        val deviationIndex = sqrt(
            features.sumOf { f -> sensorWeights.getValue(f.sensor) * f.deviation * f.deviation } / wSum
        )

        // ---- Anomaly detection -------------------------------------------------
        val anomalyScore = 1.0 / (1.0 + exp(-ANOMALY_STEEPNESS * (deviationIndex - ANOMALY_MIDPOINT)))
        val scoreRisk = when {
            anomalyScore >= HIGH_THRESHOLD -> RiskLevel.HIGH
            anomalyScore >= ALERT_THRESHOLD -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        // ---- Component health, risk and RUL -----------------------------------
        val components = ComponentType.values().map { assessComponent(it, bySensor, pre) }
        val ranked = components.sortedWith(
            compareByDescending<ComponentAssessment> { it.risk.rank }.thenBy { it.health }.thenBy { it.rulHours }
        )
        val primary = ranked.first()
        val risk = if (scoreRisk.rank > primary.risk.rank) scoreRisk else primary.risk

        // ---- Onset (CUSUM change-point on the primary indicator) --------------
        val onsetIndex = if (risk != RiskLevel.LOW) cusumOnset(primary.component, pre) else null
        val onsetHoursAgo = onsetIndex?.let { ((n - 1 - it) * window.sampleIntervalHours).roundToInt() }

        // ---- Confidence --------------------------------------------------------
        // Three equally weighted factors, each 0..1:
        //  1. decision certainty: how far the anomaly score is from 0.5
        //  2. evidence: R² of the degradation trend (abnormal) or closeness to baseline (normal)
        //  3. data quality: share of samples that did not need spike correction
        val certainty = max(anomalyScore, 1.0 - anomalyScore)
        val evidence = if (risk != RiskLevel.LOW) primary.trendR2
            else (1.0 - deviationIndex / ANOMALY_MIDPOINT).coerceIn(0.0, 1.0)
        val dataQuality = 1.0 - pre.spikesRemoved.toDouble() / (n * window.channels.size)
        val confidence = (CONF_W_CERTAINTY * certainty + CONF_W_EVIDENCE * evidence + CONF_W_QUALITY * dataQuality) /
            (CONF_W_CERTAINTY + CONF_W_EVIDENCE + CONF_W_QUALITY)

        val explanation = explain(primary, risk, bySensor, pre, deviationIndex)

        val stages = listOf(
            PipelineStage(
                "Data acquisition",
                "${n * window.channels.size} samples: ${window.channels.size} channels × $n operating hours, " +
                    window.condition.label.lowercase(),
            ),
            PipelineStage(
                "Preprocessing",
                "Hampel filter removed ${pre.spikesRemoved} sensor spike${if (pre.spikesRemoved == 1) "" else "s"}; " +
                    "5-point moving average; z-score against fleet baseline",
            ),
            PipelineStage(
                "Feature extraction",
                "${features.size * 3} features: level, trend and volatility for ${features.size} channels over the last $RECENT h",
            ),
            PipelineStage(
                "Time-series model",
                "Normal-behaviour model: weighted deviation ${fmt(deviationIndex, 1)}σ from the learned baseline",
            ),
            PipelineStage(
                "Anomaly detection",
                "Anomaly score ${fmt(anomalyScore, 2)} (alert threshold ${fmt(ALERT_THRESHOLD, 2)})" +
                    (onsetHoursAgo?.let { "; onset ≈ $it h ago" } ?: "; no change-point found"),
            ),
            PipelineStage(
                "Component health",
                components.joinToString(", ") { "${it.component.label} ${it.health}%" },
            ),
            PipelineStage(
                "Failure-risk prediction",
                "${primary.component.label} → ${risk.label} risk",
            ),
            PipelineStage(
                "RUL estimation",
                "${primary.indicatorName} trend ${signed(primary.trendPerHour, primary.indicatorDecimals + 2)} " +
                    "${primary.indicatorUnit}/h → limit ${fmt(primary.indicatorLimit, primary.indicatorDecimals)} " +
                    "${primary.indicatorUnit} in ${rulText(primary.rulHours)}",
            ),
            PipelineStage("Maintenance recommendation", primary.recommendation),
        )

        return AnalysisResult(
            aircraftId = window.aircraftId,
            condition = window.condition,
            anomalyScore = anomalyScore,
            deviationIndex = deviationIndex,
            risk = risk,
            primary = primary,
            components = components,
            features = features,
            confidence = confidence,
            onsetHoursAgo = onsetHoursAgo,
            explanation = explanation,
            stages = stages,
            spikesRemoved = pre.spikesRemoved,
            samplesAnalysed = n * window.channels.size,
        )
    }

    fun rulText(hours: Int): String = if (hours >= 999) "999+ h" else "$hours h"

    // ---------------------------------------------------------------------
    // Feature extraction
    // ---------------------------------------------------------------------

    private fun extractFeatures(sensor: SensorType, clean: DoubleArray, smooth: DoubleArray): SensorFeatures {
        val n = smooth.size
        val recent = smooth.copyOfRange(n - RECENT, n)
        val fit = linearFit(recent)
        val current = fit.intercept + fit.slope * (RECENT - 1)
        val levelZ = (recent.average() - sensor.baselineMean) / sensor.baselineStd
        val diffs = DoubleArray(RECENT - 1) { clean[n - RECENT + it + 1] - clean[n - RECENT + it] }
        val volatility = std(diffs) / (sensor.baselineStd * sqrt(2.0))
        val deviation = sqrt(levelZ * levelZ + (2.0 * max(0.0, volatility - 1.0)).pow(2))
        return SensorFeatures(
            sensor = sensor,
            baseline = sensor.baselineMean,
            windowStart = smooth.copyOfRange(0, 12).average(),
            current = current,
            levelZ = levelZ,
            slopePerHour = fit.slope,
            r2 = fit.r2,
            volatilityRatio = volatility,
            deviation = deviation,
        )
    }

    // ---------------------------------------------------------------------
    // Component assessment
    // ---------------------------------------------------------------------

    private class Indicator(
        val name: String, val unit: String, val decimals: Int,
        val current: Double, val slope: Double, val r2: Double,
        val nominal: Double, val limit: Double, val wearFloor: Double,
    )

    /**
     * Trend window per component. Bearing wear accelerates, so only the most recent
     * 24 h describe its current rate; EGT, fuel-flow and ripple drift slowly, so a
     * longer window gives a steadier slope.
     */
    val trendWindowHours: Map<ComponentType, Int> = mapOf(
        ComponentType.ENGINE_BEARING to 24,
        ComponentType.HYDRAULIC_PUMP to 48,
        ComponentType.TURBINE to 72,
        ComponentType.FUEL_PUMP to 72,
    )

    private fun indicatorFor(c: ComponentType, pre: PreprocessedWindow): Indicator {
        val w = trendWindowHours.getValue(c)
        fun fitOf(series: DoubleArray): Triple<Double, Double, Double> {
            val n = series.size
            val fit = linearFit(series.copyOfRange(n - w, n))
            return Triple(fit.intercept + fit.slope * (w - 1), fit.slope, fit.r2)
        }
        return when (c) {
            ComponentType.ENGINE_BEARING -> fitOf(pre.smoothed.getValue(SensorType.VIBRATION)).let { (cur, sl, r2) ->
                Indicator("Vibration", "mm/s", 1, cur, sl, r2, BEARING_NOMINAL, BEARING_LIMIT, 0.008)
            }
            ComponentType.TURBINE -> fitOf(pre.smoothed.getValue(SensorType.ENGINE_TEMP)).let { (cur, sl, r2) ->
                Indicator("Engine temperature", "°C", 0, cur, sl, r2, EGT_NOMINAL, EGT_LIMIT, 0.15)
            }
            ComponentType.FUEL_PUMP -> fitOf(pre.smoothed.getValue(SensorType.FUEL_FLOW)).let { (cur, sl, r2) ->
                Indicator("Fuel flow", "kg/h", 0, cur, sl, r2, FUEL_NOMINAL, FUEL_LIMIT, 0.6)
            }
            ComponentType.HYDRAULIC_PUMP -> fitOf(pre.hydraulicRipple).let { (cur, sl, r2) ->
                Indicator("Pressure ripple", "PSI", 0, cur, sl, r2, RIPPLE_NOMINAL, RIPPLE_LIMIT, 0.15)
            }
        }
    }

    private fun assessComponent(
        c: ComponentType,
        f: Map<SensorType, SensorFeatures>,
        pre: PreprocessedWindow,
    ): ComponentAssessment {
        val ind = indicatorFor(c, pre)
        val margin = ((ind.current - ind.nominal) / (ind.limit - ind.nominal)).coerceIn(0.0, 1.0)
        val health = min(98, (100.0 * (1.0 - margin.pow(HEALTH_GAMMA))).roundToInt()).coerceAtLeast(1)
        val rate = max(ind.slope, ind.wearFloor)
        // Rounded down: a remaining-life estimate should never be optimistic.
        val rul = floor(((ind.limit - ind.current) / rate).coerceIn(0.0, 999.0)).toInt()
        val risk = when {
            rul < 72 || health < 65 -> RiskLevel.HIGH
            rul < 250 || health < 85 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
        val signature = componentSignatures.getValue(c)
        val raw = signature.map { (s, w) -> s to w * max(f.getValue(s).deviation, 0.05) }
        val total = raw.sumOf { it.second }
        val contributions = raw.map { Contribution(it.first, it.second / total) }.sortedByDescending { it.share }
        val text = Recommendations.forComponent(c, risk)
        return ComponentAssessment(
            component = c,
            health = health,
            risk = risk,
            rulHours = rul,
            indicatorName = ind.name,
            indicatorUnit = ind.unit,
            indicatorDecimals = ind.decimals,
            indicatorValue = ind.current,
            indicatorLimit = ind.limit,
            trendPerHour = ind.slope,
            trendR2 = ind.r2,
            condition = text.condition,
            recommendation = text.recommendation,
            shortAction = text.shortAction,
            workPackage = text.workPackage,
            contributions = contributions,
        )
    }

    private fun cusumOnset(c: ComponentType, pre: PreprocessedWindow): Int? {
        val (series, _, sd) = when (c) {
            ComponentType.ENGINE_BEARING -> Triple(pre.smoothed.getValue(SensorType.VIBRATION), SensorType.VIBRATION.baselineMean, SensorType.VIBRATION.baselineStd)
            ComponentType.TURBINE -> Triple(pre.smoothed.getValue(SensorType.ENGINE_TEMP), SensorType.ENGINE_TEMP.baselineMean, SensorType.ENGINE_TEMP.baselineStd)
            ComponentType.FUEL_PUMP -> Triple(pre.smoothed.getValue(SensorType.FUEL_FLOW), SensorType.FUEL_FLOW.baselineMean, SensorType.FUEL_FLOW.baselineStd)
            ComponentType.HYDRAULIC_PUMP -> Triple(pre.hydraulicRipple, RIPPLE_NOMINAL, 2.5)
        }
        // Use the first 24 h as the local reference so per-tail offsets do not trigger it.
        val ref = series.copyOfRange(0, 24).average()
        val k = 0.5
        val h = 8.0
        var s = 0.0
        var lastZero = 0
        for (i in series.indices) {
            val z = (series[i] - ref) / sd
            s = max(0.0, s + z - k)
            if (s == 0.0) lastZero = i
            if (s > h) return lastZero
        }
        return null
    }

    // ---------------------------------------------------------------------
    // Natural-language explanation (template-based, generated from features)
    // ---------------------------------------------------------------------

    private fun explain(
        primary: ComponentAssessment,
        risk: RiskLevel,
        f: Map<SensorType, SensorFeatures>,
        pre: PreprocessedWindow,
        deviationIndex: Double,
    ): String {
        if (risk == RiskLevel.LOW) {
            return "All six channels are inside their normal bands. Overall deviation from the fleet baseline is " +
                "${fmt(deviationIndex, 1)}σ, below the ${fmt(detectionSigma, 1)}σ detection threshold, and no sustained degradation " +
                "trend was found. Continue routine monitoring."
        }
        val vib = f.getValue(SensorType.VIBRATION)
        val egt = f.getValue(SensorType.ENGINE_TEMP)
        val oil = f.getValue(SensorType.OIL_PRESSURE)
        val fuel = f.getValue(SensorType.FUEL_FLOW)
        val hyd = f.getValue(SensorType.HYDRAULIC_PRESSURE)
        return when (primary.component) {
            ComponentType.ENGINE_BEARING -> {
                val pct = ((vib.current - vib.windowStart) / vib.windowStart * 100).roundToInt()
                "Vibration rose from ${fmt(vib.windowStart, 1)} to ${fmt(vib.current, 1)} mm/s (+$pct%) and is still " +
                    "climbing at ${fmt(vib.slopePerHour, 3)} mm/s per hour. Engine temperature is up " +
                    "${fmt(egt.current - egt.windowStart, 0)} °C and oil pressure is down " +
                    "${fmt(oil.windowStart - oil.current, 0)} PSI over the same period. This combined pattern " +
                    "matches the signature of progressive bearing wear."
            }
            ComponentType.HYDRAULIC_PUMP -> {
                val r = pre.hydraulicRipple
                val r0 = r.copyOfRange(RIPPLE_WINDOW, RIPPLE_WINDOW + 12).average()
                val ratio = primary.indicatorValue / RIPPLE_NOMINAL
                "Hydraulic pressure ripple has grown from about ${fmt(r0, 0)} to ${fmt(primary.indicatorValue, 0)} PSI " +
                    "(${fmt(ratio, 1)}× the fleet baseline) while mean pressure moved only " +
                    "${fmt(hyd.current - hyd.windowStart, 0)} PSI. Growing ripple at stable pressure is an early " +
                    "indicator of pump wear."
            }
            ComponentType.TURBINE -> {
                "Engine temperature has risen ${fmt(egt.current - egt.windowStart, 0)} °C to ${fmt(egt.current, 0)} °C, " +
                    "drifting at ${fmt(egt.slopePerHour, 2)} °C per hour, with fuel flow up " +
                    "${fmt(fuel.current - fuel.windowStart, 0)} kg/h. EGT margin to the " +
                    "${fmt(primary.indicatorLimit, 0)} °C limit is eroding."
            }
            ComponentType.FUEL_PUMP -> {
                "Fuel flow is ${fmt(fuel.current - fuel.baseline, 0)} kg/h above baseline and fluctuating " +
                    "${fmt(fuel.volatilityRatio, 1)}× more than normal while core speed is steady. This points to " +
                    "fuel pump or metering wear."
            }
        }
    }

    // ---------------------------------------------------------------------
    // Numerics
    // ---------------------------------------------------------------------

    data class Fit(val slope: Double, val intercept: Double, val r2: Double)

    fun linearFit(y: DoubleArray): Fit {
        val n = y.size
        val xm = (n - 1) / 2.0
        val ym = y.average()
        var sxy = 0.0
        var sxx = 0.0
        for (i in 0 until n) {
            sxy += (i - xm) * (y[i] - ym)
            sxx += (i - xm) * (i - xm)
        }
        val slope = if (sxx == 0.0) 0.0 else sxy / sxx
        val intercept = ym - slope * xm
        var ssRes = 0.0
        var ssTot = 0.0
        for (i in 0 until n) {
            val pred = intercept + slope * i
            ssRes += (y[i] - pred).pow(2)
            ssTot += (y[i] - ym).pow(2)
        }
        val r2 = if (ssTot == 0.0) 0.0 else (1.0 - ssRes / ssTot).coerceIn(0.0, 1.0)
        return Fit(slope, intercept, r2)
    }

    /** Hampel filter: replaces samples more than 4.5 scaled MADs from the local median. */
    fun hampel(x: DoubleArray, half: Int = 5, t: Double = 4.5): Pair<DoubleArray, List<Int>> {
        val y = x.copyOf()
        val idx = mutableListOf<Int>()
        for (i in x.indices) {
            val lo = max(0, i - half)
            val hi = min(x.size - 1, i + half)
            val w = x.copyOfRange(lo, hi + 1)
            val med = median(w)
            val mad = 1.4826 * median(DoubleArray(w.size) { abs(w[it] - med) })
            if (mad > 0 && abs(x[i] - med) > t * mad) {
                y[i] = med
                idx += i
            }
        }
        return y to idx
    }

    fun movingAverage(x: DoubleArray, window: Int): DoubleArray {
        val half = window / 2
        return DoubleArray(x.size) { i ->
            val lo = max(0, i - half)
            val hi = min(x.size - 1, i + half)
            var s = 0.0
            for (j in lo..hi) s += x[j]
            s / (hi - lo + 1)
        }
    }

    /**
     * Pressure ripple: rolling RMS of sample-to-sample differences divided by √2.
     * For white noise this equals the noise standard deviation, and it ignores
     * slow changes in mean pressure.
     */
    fun rollingRipple(x: DoubleArray, window: Int): DoubleArray = DoubleArray(x.size) { i ->
        val lo = max(1, i - window + 1)
        if (i < 1) return@DoubleArray RIPPLE_NOMINAL
        var s = 0.0
        for (j in lo..i) s += (x[j] - x[j - 1]).pow(2)
        sqrt(s / (i - lo + 1) / 2.0)
    }

    private fun median(v: DoubleArray): Double {
        val s = v.sortedArray()
        val m = s.size / 2
        return if (s.size % 2 == 0) (s[m - 1] + s[m]) / 2.0 else s[m]
    }

    private fun std(v: DoubleArray): Double {
        if (v.size < 2) return 0.0
        val m = v.average()
        return sqrt(v.sumOf { (it - m) * (it - m) } / (v.size - 1))
    }

    fun fmt(v: Double, decimals: Int): String = formatNumber(v, decimals)

    private fun signed(v: Double, decimals: Int): String = (if (v >= 0) "+" else "") + fmt(v, decimals)
}
