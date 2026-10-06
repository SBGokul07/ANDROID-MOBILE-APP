package com.aeromaintenance.ai.data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * In-memory fleet data for the prototype. All aircraft, alerts and work orders
 * are synthetic. No external aircraft API is used.
 */
public final class FleetRepository {

    private FleetRepository() {
    }

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    public static String formatTimestamp(LocalDateTime t) {
        return t.format(TIMESTAMP);
    }

    public static String componentStatus(int health) {
        if (health >= 85) return "NORMAL";
        if (health >= 70) return "WARNING";
        return "ATTENTION";
    }

    private static List<ComponentHealthRecord> comps(int engine, int hydraulic, int bearing, int fuel) {
        return Arrays.asList(
                new ComponentHealthRecord("Engine", engine, componentStatus(engine), "Core engine performance"),
                new ComponentHealthRecord("Hydraulic System", hydraulic, componentStatus(hydraulic), "Pumps, accumulators, lines"),
                new ComponentHealthRecord("Engine Bearing", bearing, componentStatus(bearing), "Main shaft bearing assembly"),
                new ComponentHealthRecord("Fuel System", fuel, componentStatus(fuel), "Fuel pump, metering, filters"));
    }

    private static Aircraft aircraft(String id, String model, int hours, int cycles, int health,
                                     FleetStatus status, RiskLevel risk, String lastMaintenance, String base,
                                     FaultMode fault, double severity, List<ComponentHealthRecord> components) {
        return new Aircraft(id, model, hours, cycles, health, status, risk, lastMaintenance, base,
                fault, severity, null, 0.0, components);
    }

    /** The twelve simulated aircraft: 8 healthy, 3 warning, 1 critical. */
    public static final List<Aircraft> AIRCRAFT = Collections.unmodifiableList(Arrays.asList(
            new Aircraft("AERO-101", "A320", 8421, 4215, 72, FleetStatus.CRITICAL, RiskLevel.HIGH,
                    "14 Aug 2026", "Hangar 2",
                    FaultMode.BEARING_DEGRADATION, 1.0,
                    FaultMode.HYDRAULIC_PUMP_WEAR, 1.0,
                    comps(91, 78, 62, 94)),
            aircraft("AERO-102", "A320", 6980, 3610, 91, FleetStatus.HEALTHY, RiskLevel.LOW,
                    "02 Sep 2026", "Hangar 1", FaultMode.BEARING_DEGRADATION, 1.0, comps(94, 92, 90, 93)),
            aircraft("AERO-103", "B737-800", 11240, 6118, 79, FleetStatus.WARNING, RiskLevel.MEDIUM,
                    "21 Jul 2026", "Hangar 2", FaultMode.HYDRAULIC_PUMP_WEAR, 0.85, comps(88, 79, 86, 90)),
            aircraft("AERO-104", "A321", 5312, 2544, 91, FleetStatus.HEALTHY, RiskLevel.LOW,
                    "10 Sep 2026", "Hangar 1", FaultMode.BEARING_DEGRADATION, 1.0, comps(93, 91, 92, 90)),
            aircraft("AERO-105", "ATR 72-600", 9876, 8930, 82, FleetStatus.WARNING, RiskLevel.MEDIUM,
                    "03 Aug 2026", "Line station", FaultMode.TURBINE_EGT_DRIFT, 1.0, comps(83, 88, 85, 89)),
            aircraft("AERO-106", "A320neo", 3104, 1722, 93, FleetStatus.HEALTHY, RiskLevel.LOW,
                    "18 Sep 2026", "Hangar 1", FaultMode.BEARING_DEGRADATION, 1.0, comps(95, 93, 94, 92)),
            aircraft("AERO-107", "B737-800", 12655, 7004, 81, FleetStatus.WARNING, RiskLevel.MEDIUM,
                    "29 Jul 2026", "Hangar 3", FaultMode.FUEL_PUMP_DEGRADATION, 1.0, comps(86, 87, 88, 82)),
            aircraft("AERO-108", "A321neo", 2488, 1190, 91, FleetStatus.HEALTHY, RiskLevel.LOW,
                    "22 Sep 2026", "Hangar 1", FaultMode.BEARING_DEGRADATION, 1.0, comps(92, 90, 93, 91)),
            aircraft("AERO-109", "A320", 9950, 5402, 89, FleetStatus.HEALTHY, RiskLevel.LOW,
                    "05 Sep 2026", "Hangar 2", FaultMode.BEARING_DEGRADATION, 1.0, comps(90, 88, 89, 91)),
            aircraft("AERO-110", "B737 MAX 8", 4021, 2210, 92, FleetStatus.HEALTHY, RiskLevel.LOW,
                    "12 Sep 2026", "Hangar 3", FaultMode.BEARING_DEGRADATION, 1.0, comps(94, 91, 92, 93)),
            aircraft("AERO-111", "ATR 72-600", 7330, 6880, 90, FleetStatus.HEALTHY, RiskLevel.LOW,
                    "27 Aug 2026", "Line station", FaultMode.TURBINE_EGT_DRIFT, 1.0, comps(89, 92, 90, 91)),
            aircraft("AERO-112", "A320neo", 1876, 1004, 93, FleetStatus.HEALTHY, RiskLevel.LOW,
                    "30 Sep 2026", "Hangar 1", FaultMode.BEARING_DEGRADATION, 1.0, comps(95, 92, 94, 93))
    ));

    public static Aircraft aircraftById(String id) {
        for (Aircraft a : AIRCRAFT) {
            if (a.id.equals(id)) return a;
        }
        throw new IllegalArgumentException("Unknown aircraft " + id);
    }

    public static boolean exists(String id) {
        for (Aircraft a : AIRCRAFT) {
            if (a.id.equals(id)) return true;
        }
        return false;
    }

    /** Aircraft that are already flagged default to the abnormal stream. */
    public static DataCondition defaultCondition(Aircraft a) {
        return a.status == FleetStatus.HEALTHY ? DataCondition.NORMAL : DataCondition.ABNORMAL;
    }

    /** Average health score of the fleet (87 for the simulated fleet). */
    public static int fleetHealthScore() {
        double sum = 0;
        for (Aircraft a : AIRCRAFT) sum += a.healthScore;
        return (int) Math.round(sum / AIRCRAFT.size());
    }

    public static int countByStatus(FleetStatus status) {
        int n = 0;
        for (Aircraft a : AIRCRAFT) {
            if (a.status == status) n++;
        }
        return n;
    }

    /**
     * Work orders already in the plan when the app starts. Due hours of the AI
     * predicted ones are refreshed from the inference engine at start-up so they
     * always agree with the AI prediction screen.
     */
    public static List<MaintenanceTask> initialTasks() {
        List<MaintenanceTask> tasks = new ArrayList<>();
        tasks.add(new MaintenanceTask("WO-5512", "AERO-101", "Engine Bearing Inspection", "Engine Bearing",
                42, TaskStatus.SCHEDULED, "AI prediction", Arrays.asList(
                "Borescope inspection of the bearing compartment",
                "Check magnetic chip detector and oil filter for metal particles",
                "Engine vibration survey after inspection"), true, null));
        tasks.add(new MaintenanceTask("WO-5507", "AERO-103", "Hydraulic Pump Inspection", "Hydraulic System",
                115, TaskStatus.SCHEDULED, "AI prediction", Arrays.asList(
                "Measure pump case-drain flow",
                "Record pressure ripple at ground idle",
                "Inspect pump mounting and seals"), true, null));
        tasks.add(new MaintenanceTask("WO-5499", "AERO-105", "Hot-section Borescope", "Turbine",
                190, TaskStatus.IN_PROGRESS, "AI prediction", Arrays.asList(
                "Borescope turbine stage-1 blades",
                "Review EGT margin trend"), true, null));
        tasks.add(new MaintenanceTask("WO-5496", "AERO-107", "Fuel System Inspection", "Fuel System",
                210, TaskStatus.SCHEDULED, "AI prediction", Arrays.asList(
                "Inspect fuel pump and replace filter element",
                "Check fuel metering unit calibration"), true, null));
        tasks.add(new MaintenanceTask("WO-5480", "AERO-102", "A-check lubrication", "Airframe",
                0, TaskStatus.COMPLETED, "Scheduled programme", Arrays.asList(
                "Lubricate flight-control hinges",
                "Service landing-gear struts"), false, null));
        tasks.add(new MaintenanceTask("WO-5474", "AERO-109", "Landing-gear servicing", "Landing gear",
                0, TaskStatus.COMPLETED, "Scheduled programme", Arrays.asList(
                "Tyre pressure and wear check",
                "Brake wear pin inspection"), false, null));
        return tasks;
    }
}
