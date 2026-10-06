package com.aeromaintenance.ai;

import com.aeromaintenance.ai.data.Aircraft;
import com.aeromaintenance.ai.data.ComponentType;
import com.aeromaintenance.ai.data.DataCondition;
import com.aeromaintenance.ai.data.FleetRepository;
import com.aeromaintenance.ai.data.FleetStatus;
import com.aeromaintenance.ai.data.Format;
import com.aeromaintenance.ai.data.RiskLevel;
import com.aeromaintenance.ai.data.Severity;
import com.aeromaintenance.ai.data.TelemetrySimulator;
import com.aeromaintenance.ai.engine.AlertFactory;
import com.aeromaintenance.ai.engine.AnalysisResult;
import com.aeromaintenance.ai.engine.InferenceEngine;
import com.aeromaintenance.ai.engine.ReportGenerator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Locks in the numbers the faculty demonstration relies on, and checks that the
 * simulated fleet record agrees with what the engine computes.
 */
public class InferenceEngineTest {

    private static AnalysisResult analyse(String id, DataCondition c) {
        return InferenceEngine.analyze(TelemetrySimulator.generate(FleetRepository.aircraftById(id), c));
    }

    @Test
    public void aero101AbnormalMatchesTheDemoScript() {
        AnalysisResult r = analyse("AERO-101", DataCondition.ABNORMAL);
        assertEquals("0.91", Format.number(r.anomalyScore, 2));
        assertEquals(RiskLevel.HIGH, r.risk);
        assertEquals(ComponentType.ENGINE_BEARING, r.primary.component);
        assertEquals("Progressive degradation detected", r.primary.condition);
        assertEquals(62, r.primary.health);
        assertEquals(42, r.primary.rulHours);
        assertEquals("94.2", Format.number(r.confidence * 100, 1));
        assertEquals("Schedule engine-bearing inspection during the next available maintenance window.",
                r.primary.recommendation);
        assertEquals(Integer.valueOf(73), r.onsetHoursAgo);
        assertEquals(9, r.stages.size());
    }

    @Test
    public void aero101NormalIsLowRisk() {
        AnalysisResult r = analyse("AERO-101", DataCondition.NORMAL);
        assertEquals(RiskLevel.LOW, r.risk);
        assertTrue("normal anomaly score should be well below the alert threshold", r.anomalyScore < 0.2);
    }

    @Test
    public void everyAircraftIsLowRiskOnNormalData() {
        for (Aircraft a : FleetRepository.AIRCRAFT) {
            assertEquals(a.id, RiskLevel.LOW, analyse(a.id, DataCondition.NORMAL).risk);
        }
    }

    @Test
    public void fleetRecordAgreesWithTheEngine() {
        for (Aircraft a : FleetRepository.AIRCRAFT) {
            AnalysisResult r = analyse(a.id, FleetRepository.defaultCondition(a));
            assertEquals(a.id, a.risk, r.risk);
        }
        assertEquals(12, FleetRepository.AIRCRAFT.size());
        assertEquals(8, FleetRepository.countByStatus(FleetStatus.HEALTHY));
        assertEquals(3, FleetRepository.countByStatus(FleetStatus.WARNING));
        assertEquals(1, FleetRepository.countByStatus(FleetStatus.CRITICAL));
        assertEquals(87, FleetRepository.fleetHealthScore());
    }

    @Test
    public void analysisIsDeterministic() {
        AnalysisResult a = analyse("AERO-103", DataCondition.ABNORMAL);
        AnalysisResult b = analyse("AERO-103", DataCondition.ABNORMAL);
        assertEquals(a.anomalyScore, b.anomalyScore, 0.0);
        assertEquals(a.primary.rulHours, b.primary.rulHours);
    }

    @Test
    public void alertsFollowRisk() {
        assertEquals(Severity.CRITICAL, AlertFactory.severityFor(analyse("AERO-101", DataCondition.ABNORMAL)));
        assertNull(AlertFactory.severityFor(analyse("AERO-101", DataCondition.NORMAL)));
    }

    @Test
    public void reportContainsTheFinding() {
        Aircraft a = FleetRepository.aircraftById("AERO-101");
        AnalysisResult r = analyse("AERO-101", DataCondition.ABNORMAL);
        String text = ReportGenerator.toPlainText(ReportGenerator.build(a, r, "now", 1));
        assertTrue(text.contains("AERO-101"));
        assertTrue(text.contains("Engine Bearing"));
        assertTrue(text.contains("42 h"));
        assertTrue(text.contains("AMR-101-001"));
    }

    @Test
    public void numberFormatting() {
        assertEquals("8,421", Format.number(8421, 0));
        assertEquals("0.91", Format.number(0.9134, 2));
        assertEquals("-0.5", Format.number(-0.5, 1));
        assertEquals("+0.052", Format.signed(0.0517, 3));
        assertEquals("10,420", Format.number(10420.4, 0));
    }
}
