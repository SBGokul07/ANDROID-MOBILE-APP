package com.aeromaintenance.ai

import com.aeromaintenance.ai.data.ComponentType
import com.aeromaintenance.ai.data.DataCondition
import com.aeromaintenance.ai.data.FleetRepository
import com.aeromaintenance.ai.data.FleetStatus
import com.aeromaintenance.ai.data.RiskLevel
import com.aeromaintenance.ai.data.Severity
import com.aeromaintenance.ai.data.TelemetrySimulator
import com.aeromaintenance.ai.engine.AlertFactory
import com.aeromaintenance.ai.engine.InferenceEngine
import com.aeromaintenance.ai.engine.ReportGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks in the numbers the faculty demonstration relies on, and checks that the
 * simulated fleet record agrees with what the engine computes.
 */
class InferenceEngineTest {

    private fun analyse(id: String, c: DataCondition) =
        InferenceEngine.analyze(TelemetrySimulator.generate(FleetRepository.aircraftById(id), c))

    @Test
    fun aero101AbnormalMatchesTheDemoScript() {
        val r = analyse("AERO-101", DataCondition.ABNORMAL)
        assertEquals("0.91", InferenceEngine.fmt(r.anomalyScore, 2))
        assertEquals(RiskLevel.HIGH, r.risk)
        assertEquals(ComponentType.ENGINE_BEARING, r.primary.component)
        assertEquals("Progressive degradation detected", r.primary.condition)
        assertEquals(62, r.primary.health)
        assertEquals(42, r.primary.rulHours)
        assertEquals("94.2", InferenceEngine.fmt(r.confidence * 100, 1))
        assertEquals(
            "Schedule engine-bearing inspection during the next available maintenance window.",
            r.primary.recommendation,
        )
    }

    @Test
    fun aero101NormalIsLowRisk() {
        val r = analyse("AERO-101", DataCondition.NORMAL)
        assertEquals(RiskLevel.LOW, r.risk)
        assertTrue("normal anomaly score should be well below the alert threshold", r.anomalyScore < 0.2)
    }

    @Test
    fun everyAircraftIsLowRiskOnNormalData() {
        FleetRepository.aircraft.forEach { a ->
            assertEquals(a.id, RiskLevel.LOW, analyse(a.id, DataCondition.NORMAL).risk)
        }
    }

    @Test
    fun fleetRecordAgreesWithTheEngine() {
        FleetRepository.aircraft.forEach { a ->
            val r = analyse(a.id, FleetRepository.defaultCondition(a))
            assertEquals(a.id, a.risk, r.risk)
        }
        val counts = FleetRepository.aircraft.groupingBy { it.status }.eachCount()
        assertEquals(12, FleetRepository.aircraft.size)
        assertEquals(8, counts[FleetStatus.HEALTHY])
        assertEquals(3, counts[FleetStatus.WARNING])
        assertEquals(1, counts[FleetStatus.CRITICAL])
        assertEquals(87, FleetRepository.fleetHealthScore)
    }

    @Test
    fun analysisIsDeterministic() {
        val a = analyse("AERO-103", DataCondition.ABNORMAL)
        val b = analyse("AERO-103", DataCondition.ABNORMAL)
        assertEquals(a.anomalyScore, b.anomalyScore, 0.0)
        assertEquals(a.primary.rulHours, b.primary.rulHours)
    }

    @Test
    fun alertsFollowRisk() {
        assertEquals(Severity.CRITICAL, AlertFactory.severityFor(analyse("AERO-101", DataCondition.ABNORMAL)))
        assertEquals(null, AlertFactory.severityFor(analyse("AERO-101", DataCondition.NORMAL)))
    }

    @Test
    fun reportContainsTheFinding() {
        val a = FleetRepository.aircraftById("AERO-101")
        val r = analyse("AERO-101", DataCondition.ABNORMAL)
        val text = ReportGenerator.toPlainText(ReportGenerator.build(a, r, "now", 1))
        assertTrue(text.contains("AERO-101"))
        assertTrue(text.contains("Engine Bearing"))
        assertTrue(text.contains("42 h"))
    }
}
