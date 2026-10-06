package com.aeromaintenance.ai.data

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Generates realistic, fully deterministic synthetic telemetry.
 *
 * - 120 samples per channel, one per operating hour (a 5-day operating window).
 * - NORMAL: every channel is Gaussian noise around the fleet baseline, plus a
 *   small periodic flight-phase component.
 * - ABNORMAL: the aircraft's fault signature is injected. Degradation starts
 *   at 25 % of the window and grows quadratically, so vibration, temperature and
 *   pressure fluctuation visibly accelerate towards the end of the window.
 *
 * The same aircraft + condition always produces exactly the same numbers, which
 * keeps the faculty demonstration repeatable.
 */
object TelemetrySimulator {

    const val SAMPLES = 120
    const val SAMPLE_INTERVAL_HOURS = 1.0

    /** Two deliberate sensor glitches so the preprocessing stage has real work to do. */
    const val VIBRATION_SPIKE_INDEX = 41
    const val TEMPERATURE_SPIKE_INDEX = 77

    fun generate(aircraft: Aircraft, condition: DataCondition): TelemetryWindow {
        val seed = aircraft.id.hashCode() * 31 + condition.ordinal * 7919
        val rnd = Random(seed)
        val n = SAMPLES

        val egt = DoubleArray(n)
        val vib = DoubleArray(n)
        val oil = DoubleArray(n)
        val rpm = DoubleArray(n)
        val fuel = DoubleArray(n)
        val hyd = DoubleArray(n)

        // Small per-tail offsets so every aircraft looks slightly different.
        val tailRnd = Random(aircraft.id.hashCode())
        val egtOffset = tailRnd.nextDouble(-3.0, 3.0)
        val vibOffset = tailRnd.nextDouble(-0.08, 0.08)
        val fuelOffset = tailRnd.nextDouble(-10.0, 10.0)

        val faults = mutableListOf(aircraft.faultMode to aircraft.faultSeverity)
        aircraft.secondaryFault?.let { faults += it to aircraft.secondarySeverity }

        for (i in 0 until n) {
            val p = i / (n - 1.0)
            val phase = 2 * PI * i / 12.0

            var egtShift = 0.0; var egtNoise = 4.0
            var vibShift = 0.0; var vibNoise = 0.07
            var oilShift = 0.0; var oilNoise = 0.45
            var rpmShift = 0.0; var rpmNoise = 18.0; var rpmOsc = 0.0
            var fuelShift = 0.0; var fuelNoise = 10.0
            var hydShift = 0.0; var hydNoise = 6.0; var hydOsc = 0.0

            if (condition == DataCondition.ABNORMAL) {
                for ((mode, sev) in faults) {
                    val k = sev * degradation(p, mode)
                    when (mode) {
                        FaultMode.BEARING_DEGRADATION -> {
                            vibShift += 2.72 * k; vibNoise += 0.05 * k
                            egtShift += 34.0 * k; egtNoise += 2.0 * k
                            oilShift -= 4.0 * k; oilNoise += 0.9 * k
                            rpmShift += 20.0 * k; rpmOsc += 55.0 * k
                            fuelShift += 40.0 * k
                        }
                        FaultMode.HYDRAULIC_PUMP_WEAR -> {
                            hydShift += 33.0 * k; hydNoise += 15.0 * k; hydOsc += 85.0 * k
                        }
                        FaultMode.TURBINE_EGT_DRIFT -> {
                            egtShift += 40.0 * k; egtNoise += 2.5 * k
                            fuelShift += 35.0 * k
                            rpmShift -= 15.0 * k
                        }
                        FaultMode.FUEL_PUMP_DEGRADATION -> {
                            fuelShift += 130.0 * k; fuelNoise += 22.0 * k
                            rpmNoise += 20.0 * k; rpmShift -= 25.0 * k
                        }
                    }
                }
            }

            egt[i] = 690.0 + egtOffset + 2.0 * sin(phase) + egtShift + egtNoise * rnd.gauss()
            vib[i] = 2.1 + vibOffset + 0.04 * sin(phase + 1.0) + vibShift + vibNoise * rnd.gauss()
            oil[i] = 55.0 + 0.3 * cos(phase) + oilShift + oilNoise * rnd.gauss()
            rpm[i] = 10400.0 + 8.0 * sin(phase) + rpmShift + rpmOsc * sin(i * 0.9) + rpmNoise * rnd.gauss()
            fuel[i] = 2800.0 + fuelOffset + 6.0 * sin(phase) + fuelShift + fuelNoise * rnd.gauss()
            hyd[i] = 3000.0 + 3.0 * cos(phase) + hydShift + hydOsc * sin(i * 1.3) + hydNoise * rnd.gauss()
        }

        // Inject the two sensor glitches (single-sample spikes).
        vib[VIBRATION_SPIKE_INDEX] += 1.1
        egt[TEMPERATURE_SPIKE_INDEX] += 22.0

        return TelemetryWindow(
            aircraftId = aircraft.id,
            condition = condition,
            sampleIntervalHours = SAMPLE_INTERVAL_HOURS,
            channels = linkedMapOf(
                SensorType.ENGINE_TEMP to egt,
                SensorType.VIBRATION to vib,
                SensorType.OIL_PRESSURE to oil,
                SensorType.RPM to rpm,
                SensorType.FUEL_FLOW to fuel,
                SensorType.HYDRAULIC_PRESSURE to hyd,
            ),
        )
    }

    /**
     * Degradation profile of a fault: 0 before onset, then a ramp to 1 at the end
     * of the window. Bearing wear accelerates (quadratic); EGT and fuel-flow drift
     * are close to linear.
     */
    fun degradation(p: Double, mode: FaultMode): Double =
        if (p < mode.onset) 0.0
        else ((p - mode.onset) / (1.0 - mode.onset)).pow(mode.exponent)

    /** CSV export of a window (used for the sample dataset in /docs). */
    fun toCsv(window: TelemetryWindow): String {
        val sensors = window.channels.keys.toList()
        val sb = StringBuilder()
        sb.append("hour_offset,")
        sb.append(sensors.joinToString(",") { "${it.name.lowercase()}_${it.unit.replace("/", "_per_").replace("°", "deg_").replace(" ", "")}" })
        sb.append('\n')
        for (i in 0 until window.size) {
            val hoursAgo = (window.size - 1 - i) * window.sampleIntervalHours
            sb.append(if (hoursAgo == 0.0) "0" else "-" + formatPlain(hoursAgo, 0))
            for (s in sensors) {
                sb.append(',').append(formatPlain(window.channels.getValue(s)[i], s.decimals + 1))
            }
            sb.append('\n')
        }
        return sb.toString()
    }

    private fun formatPlain(v: Double, decimals: Int): String {
        val f = 10.0.pow(decimals)
        val r = Math.round(v * f) / f
        return if (decimals == 0) Math.round(r).toString() else r.toString()
    }
}

/** Box–Muller standard normal sample. */
private fun Random.gauss(): Double {
    val u1 = max(nextDouble(), 1e-12)
    val u2 = nextDouble()
    return sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
}
