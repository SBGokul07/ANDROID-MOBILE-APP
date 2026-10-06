package com.aeromaintenance.ai.data

/**
 * Core domain model for AeroMaintenance AI.
 *
 * Everything in the `data` and `engine` packages is plain Kotlin with no Android
 * dependencies, so the simulator and the inference engine can be unit-tested on
 * the JVM and reused outside the app.
 */

enum class FleetStatus(val label: String) {
    HEALTHY("Healthy"),
    WARNING("Warning"),
    CRITICAL("Critical"),
}

enum class RiskLevel(val label: String, val rank: Int) {
    LOW("LOW", 0),
    MEDIUM("MEDIUM", 1),
    HIGH("HIGH", 2),
}

enum class Severity(val label: String, val rank: Int) {
    CRITICAL("CRITICAL", 3),
    WARNING("WARNING", 2),
    MONITOR("MONITOR", 1),
}

/** Which simulated data stream is fed to the AI pipeline. */
enum class DataCondition(val label: String) {
    NORMAL("Normal data"),
    ABNORMAL("Abnormal data"),
}

/**
 * Fault signature injected into the abnormal data stream. Each aircraft in the
 * fleet has one, so the "abnormal" toggle produces a realistic fault for that tail.
 */
enum class FaultMode(
    val label: String,
    /** Fraction of the window at which degradation starts. */
    val onset: Double,
    /** Shape of the degradation ramp: 1 = linear drift, 2 = accelerating wear. */
    val exponent: Double,
) {
    BEARING_DEGRADATION("Progressive engine-bearing wear", onset = 0.25, exponent = 2.0),
    HYDRAULIC_PUMP_WEAR("Hydraulic pump wear", onset = 0.10, exponent = 1.0),
    TURBINE_EGT_DRIFT("Turbine hot-section EGT drift", onset = 0.10, exponent = 1.0),
    FUEL_PUMP_DEGRADATION("Fuel pump degradation", onset = 0.05, exponent = 1.0),
}

/**
 * The six telemetry channels. Limits are prototype values chosen to be
 * plausible for a narrow-body turbofan; they are not taken from any
 * manufacturer's maintenance manual.
 */
enum class SensorType(
    val label: String,
    val shortLabel: String,
    val unit: String,
    val decimals: Int,
    /** Fleet baseline (learned "normal") mean and standard deviation. */
    val baselineMean: Double,
    val baselineStd: Double,
    val warnHigh: Double? = null,
    val alarmHigh: Double? = null,
    val warnLow: Double? = null,
    val alarmLow: Double? = null,
    /** Fixed y-axis range for charts so normal vs abnormal is comparable. */
    val chartMin: Double,
    val chartMax: Double,
) {
    ENGINE_TEMP(
        label = "Engine temperature (EGT)", shortLabel = "Engine temp", unit = "°C", decimals = 0,
        baselineMean = 690.0, baselineStd = 6.0,
        warnHigh = 715.0, alarmHigh = 740.0,
        chartMin = 660.0, chartMax = 760.0,
    ),
    VIBRATION(
        label = "Engine vibration", shortLabel = "Vibration", unit = "mm/s", decimals = 1,
        baselineMean = 2.1, baselineStd = 0.15,
        warnHigh = 3.5, alarmHigh = 7.0,
        chartMin = 0.0, chartMax = 7.5,
    ),
    OIL_PRESSURE(
        label = "Oil pressure", shortLabel = "Oil pressure", unit = "PSI", decimals = 0,
        baselineMean = 55.0, baselineStd = 0.9,
        warnLow = 50.0, alarmLow = 45.0,
        chartMin = 42.0, chartMax = 62.0,
    ),
    RPM(
        label = "Core speed (N2)", shortLabel = "RPM", unit = "rpm", decimals = 0,
        baselineMean = 10400.0, baselineStd = 30.0,
        warnHigh = 10650.0, alarmHigh = 10800.0, warnLow = 10150.0,
        chartMin = 10050.0, chartMax = 10850.0,
    ),
    FUEL_FLOW(
        label = "Fuel flow", shortLabel = "Fuel flow", unit = "kg/h", decimals = 0,
        baselineMean = 2800.0, baselineStd = 18.0,
        warnHigh = 2950.0, alarmHigh = 3100.0,
        chartMin = 2700.0, chartMax = 3150.0,
    ),
    HYDRAULIC_PRESSURE(
        label = "Hydraulic pressure", shortLabel = "Hyd pressure", unit = "PSI", decimals = 0,
        baselineMean = 3000.0, baselineStd = 10.0,
        warnHigh = 3100.0, alarmHigh = 3200.0, warnLow = 2900.0, alarmLow = 2800.0,
        chartMin = 2780.0, chartMax = 3220.0,
    );

    fun zoneOf(value: Double): Zone = when {
        alarmHigh != null && value >= alarmHigh -> Zone.ALARM
        alarmLow != null && value <= alarmLow -> Zone.ALARM
        warnHigh != null && value >= warnHigh -> Zone.WARN
        warnLow != null && value <= warnLow -> Zone.WARN
        else -> Zone.NORMAL
    }

    fun format(value: Double): String = formatNumber(value, decimals)
}

enum class Zone { NORMAL, WARN, ALARM }

/** Components the AI assesses. Each is linked to an indicator channel and a limit. */
enum class ComponentType(val label: String, val system: String) {
    ENGINE_BEARING("Engine Bearing", "Engine"),
    HYDRAULIC_PUMP("Hydraulic Pump", "Hydraulics"),
    TURBINE("Turbine", "Engine hot section"),
    FUEL_PUMP("Fuel Pump", "Fuel system"),
}

data class ComponentHealthRecord(
    val name: String,
    val health: Int,
    val status: String,
    val note: String,
)

data class Aircraft(
    val id: String,
    val model: String,
    val flightHours: Int,
    val flightCycles: Int,
    val healthScore: Int,
    val status: FleetStatus,
    val risk: RiskLevel,
    val lastMaintenance: String,
    val base: String,
    val faultMode: FaultMode,
    /** Severity of the injected fault when "abnormal data" is selected (0..1). */
    val faultSeverity: Double,
    val components: List<ComponentHealthRecord>,
    val secondaryFault: FaultMode? = null,
    val secondarySeverity: Double = 0.0,
)

/** One fixed-rate telemetry window: 120 samples, one per operating hour. */
data class TelemetryWindow(
    val aircraftId: String,
    val condition: DataCondition,
    val sampleIntervalHours: Double,
    val channels: Map<SensorType, DoubleArray>,
) {
    val size: Int get() = channels.values.first().size
    fun latest(sensor: SensorType): Double = channels.getValue(sensor).last()
}

enum class TaskStatus(val label: String) {
    SCHEDULED("Scheduled"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed"),
}

data class MaintenanceTask(
    val id: String,
    val aircraftId: String,
    val title: String,
    val component: String,
    val dueHours: Int,
    val status: TaskStatus,
    val source: String,
    val workPackage: List<String>,
    val createdByAi: Boolean = false,
    /** Set when the AI analysis created or refreshed this work order during the session. */
    val updatedByAiAt: String? = null,
)

data class Alert(
    val id: String,
    val timestamp: String,
    val aircraftId: String,
    val component: String,
    val severity: Severity,
    val title: String,
    val explanation: String,
    val recommendedAction: String,
    val acknowledged: Boolean = false,
    val isNew: Boolean = false,
)

fun formatNumber(value: Double, decimals: Int): String {
    val factor = Math.pow(10.0, decimals.toDouble())
    val rounded = Math.round(value * factor) / factor
    if (decimals == 0) {
        val n = Math.round(rounded)
        return groupThousands(n)
    }
    val whole = rounded.toLong()
    val frac = Math.abs(Math.round((rounded - whole) * factor))
    val sign = if (rounded < 0 && whole == 0L) "-" else ""
    return sign + groupThousands(whole) + "." + frac.toString().padStart(decimals, '0')
}

private fun groupThousands(n: Long): String {
    val s = Math.abs(n).toString()
    val sb = StringBuilder()
    for ((i, c) in s.withIndex()) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
        sb.append(c)
    }
    return (if (n < 0) "-" else "") + sb.toString()
}
