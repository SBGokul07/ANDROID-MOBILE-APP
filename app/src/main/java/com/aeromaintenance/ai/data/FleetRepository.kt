package com.aeromaintenance.ai.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * In-memory fleet data for the prototype. All aircraft, alerts and work orders
 * are synthetic. No external aircraft API is used.
 */
object FleetRepository {

    private val timestampFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")

    fun formatTimestamp(t: LocalDateTime): String = t.format(timestampFormat)

    fun componentStatus(health: Int): String = when {
        health >= 85 -> "NORMAL"
        health >= 70 -> "WARNING"
        else -> "ATTENTION"
    }

    private fun comps(engine: Int, hydraulic: Int, bearing: Int, fuel: Int) = listOf(
        ComponentHealthRecord("Engine", engine, componentStatus(engine), "Core engine performance"),
        ComponentHealthRecord("Hydraulic System", hydraulic, componentStatus(hydraulic), "Pumps, accumulators, lines"),
        ComponentHealthRecord("Engine Bearing", bearing, componentStatus(bearing), "Main shaft bearing assembly"),
        ComponentHealthRecord("Fuel System", fuel, componentStatus(fuel), "Fuel pump, metering, filters"),
    )

    val aircraft: List<Aircraft> = listOf(
        Aircraft(
            id = "AERO-101", model = "A320", flightHours = 8421, flightCycles = 4215,
            healthScore = 72, status = FleetStatus.CRITICAL, risk = RiskLevel.HIGH,
            lastMaintenance = "14 Aug 2026", base = "Hangar 2",
            faultMode = FaultMode.BEARING_DEGRADATION, faultSeverity = 1.0,
            secondaryFault = FaultMode.HYDRAULIC_PUMP_WEAR, secondarySeverity = 1.0,
            components = comps(engine = 91, hydraulic = 78, bearing = 62, fuel = 94),
        ),
        Aircraft(
            id = "AERO-102", model = "A320", flightHours = 6980, flightCycles = 3610,
            healthScore = 91, status = FleetStatus.HEALTHY, risk = RiskLevel.LOW,
            lastMaintenance = "02 Sep 2026", base = "Hangar 1",
            faultMode = FaultMode.BEARING_DEGRADATION, faultSeverity = 1.0,
            components = comps(94, 92, 90, 93),
        ),
        Aircraft(
            id = "AERO-103", model = "B737-800", flightHours = 11240, flightCycles = 6118,
            healthScore = 79, status = FleetStatus.WARNING, risk = RiskLevel.MEDIUM,
            lastMaintenance = "21 Jul 2026", base = "Hangar 2",
            faultMode = FaultMode.HYDRAULIC_PUMP_WEAR, faultSeverity = 0.85,
            components = comps(88, 79, 86, 90),
        ),
        Aircraft(
            id = "AERO-104", model = "A321", flightHours = 5312, flightCycles = 2544,
            healthScore = 91, status = FleetStatus.HEALTHY, risk = RiskLevel.LOW,
            lastMaintenance = "10 Sep 2026", base = "Hangar 1",
            faultMode = FaultMode.BEARING_DEGRADATION, faultSeverity = 1.0,
            components = comps(93, 91, 92, 90),
        ),
        Aircraft(
            id = "AERO-105", model = "ATR 72-600", flightHours = 9876, flightCycles = 8930,
            healthScore = 82, status = FleetStatus.WARNING, risk = RiskLevel.MEDIUM,
            lastMaintenance = "03 Aug 2026", base = "Line station",
            faultMode = FaultMode.TURBINE_EGT_DRIFT, faultSeverity = 1.0,
            components = comps(83, 88, 85, 89),
        ),
        Aircraft(
            id = "AERO-106", model = "A320neo", flightHours = 3104, flightCycles = 1722,
            healthScore = 93, status = FleetStatus.HEALTHY, risk = RiskLevel.LOW,
            lastMaintenance = "18 Sep 2026", base = "Hangar 1",
            faultMode = FaultMode.BEARING_DEGRADATION, faultSeverity = 1.0,
            components = comps(95, 93, 94, 92),
        ),
        Aircraft(
            id = "AERO-107", model = "B737-800", flightHours = 12655, flightCycles = 7004,
            healthScore = 81, status = FleetStatus.WARNING, risk = RiskLevel.MEDIUM,
            lastMaintenance = "29 Jul 2026", base = "Hangar 3",
            faultMode = FaultMode.FUEL_PUMP_DEGRADATION, faultSeverity = 1.0,
            components = comps(86, 87, 88, 82),
        ),
        Aircraft(
            id = "AERO-108", model = "A321neo", flightHours = 2488, flightCycles = 1190,
            healthScore = 91, status = FleetStatus.HEALTHY, risk = RiskLevel.LOW,
            lastMaintenance = "22 Sep 2026", base = "Hangar 1",
            faultMode = FaultMode.BEARING_DEGRADATION, faultSeverity = 1.0,
            components = comps(92, 90, 93, 91),
        ),
        Aircraft(
            id = "AERO-109", model = "A320", flightHours = 9950, flightCycles = 5402,
            healthScore = 89, status = FleetStatus.HEALTHY, risk = RiskLevel.LOW,
            lastMaintenance = "05 Sep 2026", base = "Hangar 2",
            faultMode = FaultMode.BEARING_DEGRADATION, faultSeverity = 1.0,
            components = comps(90, 88, 89, 91),
        ),
        Aircraft(
            id = "AERO-110", model = "B737 MAX 8", flightHours = 4021, flightCycles = 2210,
            healthScore = 92, status = FleetStatus.HEALTHY, risk = RiskLevel.LOW,
            lastMaintenance = "12 Sep 2026", base = "Hangar 3",
            faultMode = FaultMode.BEARING_DEGRADATION, faultSeverity = 1.0,
            components = comps(94, 91, 92, 93),
        ),
        Aircraft(
            id = "AERO-111", model = "ATR 72-600", flightHours = 7330, flightCycles = 6880,
            healthScore = 90, status = FleetStatus.HEALTHY, risk = RiskLevel.LOW,
            lastMaintenance = "27 Aug 2026", base = "Line station",
            faultMode = FaultMode.TURBINE_EGT_DRIFT, faultSeverity = 1.0,
            components = comps(89, 92, 90, 91),
        ),
        Aircraft(
            id = "AERO-112", model = "A320neo", flightHours = 1876, flightCycles = 1004,
            healthScore = 93, status = FleetStatus.HEALTHY, risk = RiskLevel.LOW,
            lastMaintenance = "30 Sep 2026", base = "Hangar 1",
            faultMode = FaultMode.BEARING_DEGRADATION, faultSeverity = 1.0,
            components = comps(95, 92, 94, 93),
        ),
    )

    fun aircraftById(id: String): Aircraft = aircraft.first { it.id == id }

    /** Aircraft that are already flagged default to the abnormal stream. */
    fun defaultCondition(a: Aircraft): DataCondition =
        if (a.status == FleetStatus.HEALTHY) DataCondition.NORMAL else DataCondition.ABNORMAL

    val fleetHealthScore: Int
        get() = Math.round(aircraft.map { it.healthScore }.average()).toInt()

    /**
     * Work orders already in the plan when the app starts. Due hours of the AI
     * predicted ones are refreshed from the inference engine at start-up so they
     * always agree with the AI prediction screen.
     */
    fun initialTasks(): List<MaintenanceTask> = listOf(
        MaintenanceTask(
            id = "WO-5512", aircraftId = "AERO-101", title = "Engine Bearing Inspection",
            component = "Engine Bearing", dueHours = 42, status = TaskStatus.SCHEDULED,
            source = "AI prediction",
            workPackage = listOf(
                "Borescope inspection of the bearing compartment",
                "Check magnetic chip detector and oil filter for metal particles",
                "Engine vibration survey after inspection",
            ),
            createdByAi = true,
        ),
        MaintenanceTask(
            id = "WO-5507", aircraftId = "AERO-103", title = "Hydraulic Pump Inspection",
            component = "Hydraulic System", dueHours = 115, status = TaskStatus.SCHEDULED,
            source = "AI prediction",
            workPackage = listOf(
                "Measure pump case-drain flow",
                "Record pressure ripple at ground idle",
                "Inspect pump mounting and seals",
            ),
            createdByAi = true,
        ),
        MaintenanceTask(
            id = "WO-5499", aircraftId = "AERO-105", title = "Hot-section Borescope",
            component = "Turbine", dueHours = 190, status = TaskStatus.IN_PROGRESS,
            source = "AI prediction",
            workPackage = listOf(
                "Borescope turbine stage-1 blades",
                "Review EGT margin trend",
            ),
            createdByAi = true,
        ),
        MaintenanceTask(
            id = "WO-5496", aircraftId = "AERO-107", title = "Fuel System Inspection",
            component = "Fuel System", dueHours = 210, status = TaskStatus.SCHEDULED,
            source = "AI prediction",
            workPackage = listOf(
                "Inspect fuel pump and replace filter element",
                "Check fuel metering unit calibration",
            ),
            createdByAi = true,
        ),
        MaintenanceTask(
            id = "WO-5480", aircraftId = "AERO-102", title = "A-check lubrication",
            component = "Airframe", dueHours = 0, status = TaskStatus.COMPLETED,
            source = "Scheduled programme",
            workPackage = listOf("Lubricate flight-control hinges", "Service landing-gear struts"),
        ),
        MaintenanceTask(
            id = "WO-5474", aircraftId = "AERO-109", title = "Landing-gear servicing",
            component = "Landing gear", dueHours = 0, status = TaskStatus.COMPLETED,
            source = "Scheduled programme",
            workPackage = listOf("Tyre pressure and wear check", "Brake wear pin inspection"),
        ),
    )
}
