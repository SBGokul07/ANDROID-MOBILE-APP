package com.aeromaintenance.ai.ui.theme

import androidx.compose.ui.graphics.Color
import com.aeromaintenance.ai.data.FleetStatus
import com.aeromaintenance.ai.data.RiskLevel
import com.aeromaintenance.ai.data.Severity
import com.aeromaintenance.ai.data.TaskStatus
import com.aeromaintenance.ai.data.Zone

/**
 * Colour language borrowed from glass-cockpit engine displays:
 * green = normal, amber/orange = caution, red = warning, cyan = information and
 * anything you can tap, magenta = predicted / AI-projected values.
 */
object Aero {
    val Night = Color(0xFF07101D)
    val Panel = Color(0xFF0E1B2E)
    val PanelRaised = Color(0xFF13243C)
    val Hairline = Color(0xFF1F3350)
    val HairlineStrong = Color(0xFF2C4669)

    val Text = Color(0xFFE6EEF8)
    val TextMuted = Color(0xFF8FA3BD)
    val TextFaint = Color(0xFF5B6F8A)

    val Cyan = Color(0xFF4CC3F0)
    val CyanDeep = Color(0xFF1C6E95)
    val Green = Color(0xFF3DD68C)
    val Amber = Color(0xFFF2C744)
    val Orange = Color(0xFFFF9A3C)
    val Red = Color(0xFFFF5A4F)
    val Magenta = Color(0xFFD86BFF)

    val Paper = Color(0xFFF4F6F9)
    val Ink = Color(0xFF15202E)
    val InkMuted = Color(0xFF5A6778)
}

fun riskColor(risk: RiskLevel): Color = when (risk) {
    RiskLevel.LOW -> Aero.Green
    RiskLevel.MEDIUM -> Aero.Orange
    RiskLevel.HIGH -> Aero.Red
}

fun severityColor(s: Severity): Color = when (s) {
    Severity.CRITICAL -> Aero.Red
    Severity.WARNING -> Aero.Orange
    Severity.MONITOR -> Aero.Amber
}

fun fleetStatusColor(s: FleetStatus): Color = when (s) {
    FleetStatus.HEALTHY -> Aero.Green
    FleetStatus.WARNING -> Aero.Orange
    FleetStatus.CRITICAL -> Aero.Red
}

fun zoneColor(z: Zone): Color = when (z) {
    Zone.NORMAL -> Aero.Green
    Zone.WARN -> Aero.Amber
    Zone.ALARM -> Aero.Red
}

fun healthColor(health: Int): Color = when {
    health >= 85 -> Aero.Green
    health >= 70 -> Aero.Orange
    else -> Aero.Red
}

fun taskStatusColor(s: TaskStatus): Color = when (s) {
    TaskStatus.SCHEDULED -> Aero.Cyan
    TaskStatus.IN_PROGRESS -> Aero.Amber
    TaskStatus.COMPLETED -> Aero.Green
}
