package com.aeromaintenance.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aeromaintenance.ai.AeroViewModel
import com.aeromaintenance.ai.Screen
import com.aeromaintenance.ai.data.FleetRepository
import com.aeromaintenance.ai.data.FleetStatus
import com.aeromaintenance.ai.data.RiskLevel
import com.aeromaintenance.ai.data.Severity
import com.aeromaintenance.ai.engine.InferenceEngine
import com.aeromaintenance.ai.ui.LocalRequestNotifications
import com.aeromaintenance.ai.ui.components.HealthBands
import com.aeromaintenance.ai.ui.components.InstrumentDial
import com.aeromaintenance.ai.ui.components.Panel
import com.aeromaintenance.ai.ui.components.PrimaryButton
import com.aeromaintenance.ai.ui.components.SecondaryButton
import com.aeromaintenance.ai.ui.components.SectionHeader
import com.aeromaintenance.ai.ui.components.Tag
import com.aeromaintenance.ai.ui.components.WorkflowStrip
import com.aeromaintenance.ai.ui.theme.Aero
import com.aeromaintenance.ai.ui.theme.AeroType
import com.aeromaintenance.ai.ui.theme.riskColor
import com.aeromaintenance.ai.ui.theme.severityColor

fun severityTagText(s: Severity): String = when (s) {
    Severity.CRITICAL -> "HIGH RISK"
    Severity.WARNING -> "WARNING"
    Severity.MONITOR -> "MONITOR"
}

@Composable
fun DashboardScreen(vm: AeroViewModel, padding: PaddingValues) {
    val requestNotifications = LocalRequestNotifications.current
    val health = FleetRepository.fleetHealthScore
    val counts = vm.fleet.groupingBy { it.status }.eachCount()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = withScreenPadding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("hero") {
            Panel(contentPadding = 18.dp) {
                Text("Overall fleet health", style = MaterialTheme.typography.titleMedium, color = Aero.TextMuted)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    InstrumentDial(
                        value = health.toFloat(), min = 0f, max = 100f, bands = HealthBands,
                        modifier = Modifier.width(230.dp), stroke = 12.dp,
                    ) {
                        Column(
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("$health", style = AeroType.Readout, color = Aero.Text)
                                Text(" / 100", style = AeroType.ReadoutSmall, color = Aero.TextMuted, modifier = Modifier.padding(bottom = 6.dp))
                            }
                            Tag(healthWord(health), if (health >= 80) Aero.Green else if (health >= 60) Aero.Orange else Aero.Red)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FleetCount("Total aircraft", vm.fleet.size, Aero.Cyan, Modifier.weight(1f)) { vm.navigate(Screen.Fleet) }
                    FleetCount("Healthy", counts[FleetStatus.HEALTHY] ?: 0, Aero.Green, Modifier.weight(1f)) { vm.navigate(Screen.Fleet) }
                    FleetCount("Warning", counts[FleetStatus.WARNING] ?: 0, Aero.Orange, Modifier.weight(1f)) { vm.navigate(Screen.Fleet) }
                    FleetCount("Critical", counts[FleetStatus.CRITICAL] ?: 0, Aero.Red, Modifier.weight(1f)) { vm.navigate(Screen.Fleet) }
                }
            }
        }

        item("demo") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    "Start demo",
                    onClick = { requestNotifications(); vm.startDemo() },
                    icon = Icons.Filled.PlayArrow,
                    modifier = Modifier.weight(1.2f),
                )
                SecondaryButton(
                    "Faculty mode",
                    onClick = { vm.navigate(Screen.FacultyDemo) },
                    icon = Icons.Filled.School,
                    modifier = Modifier.weight(1f).height(52.dp),
                )
            }
        }

        item("alerts-h") {
            SectionHeader("Active alerts", actionLabel = "View all", onAction = { vm.navigate(Screen.Alerts) })
        }
        item("alerts") {
            val active = vm.alerts.filter { !it.acknowledged }.sortedByDescending { it.severity.rank }.take(3)
            Panel(contentPadding = 0.dp) {
                if (active.isEmpty()) {
                    Text("No open alerts. Every alert has been acknowledged.", style = MaterialTheme.typography.bodyMedium, color = Aero.TextMuted, modifier = Modifier.padding(16.dp))
                }
                active.forEachIndexed { i, a ->
                    if (i > 0) HorizontalDivider(color = Aero.Hairline)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.expandedAlertId = a.id; vm.navigate(Screen.Alerts) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(severityColor(a.severity)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.component, style = MaterialTheme.typography.titleSmall, color = Aero.Text)
                            Text("${a.aircraftId}, ${a.title.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted, maxLines = 1)
                        }
                        Spacer(Modifier.width(8.dp))
                        Tag(severityTagText(a.severity), severityColor(a.severity))
                    }
                }
            }
        }

        item("quick-h") { SectionHeader("Quick actions") }
        item("quick") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("Aircraft", Icons.Filled.Flight, Modifier.weight(1f)) { vm.navigate(Screen.Fleet) }
                    QuickAction("Sensor monitoring", Icons.Filled.MonitorHeart, Modifier.weight(1f)) { vm.navigate(Screen.Monitoring) }
                    QuickAction("AI prediction", Icons.Filled.Psychology, Modifier.weight(1f)) { vm.navigate(Screen.Analysis) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("Maintenance", Icons.Filled.Build, Modifier.weight(1f)) { vm.openMaintenance(0) }
                    QuickAction("Alerts", Icons.Filled.Notifications, Modifier.weight(1f), badge = vm.unacknowledgedCount) { vm.navigate(Screen.Alerts) }
                    QuickAction("Reports", Icons.Filled.Description, Modifier.weight(1f)) { vm.navigate(Screen.Reports) }
                }
            }
        }

        item("attention-h") {
            SectionHeader("Aircraft needing attention", supporting = "Latest AI assessment of each tail")
        }
        item("attention") {
            val flagged = vm.fleetPredictions.filter { it.risk != RiskLevel.LOW }
                .sortedWith(compareByDescending<com.aeromaintenance.ai.engine.AnalysisResult> { it.risk.rank }.thenBy { it.primary.rulHours })
            Panel(contentPadding = 0.dp) {
                flagged.forEachIndexed { i, r ->
                    if (i > 0) HorizontalDivider(color = Aero.Hairline)
                    val a = vm.aircraft(r.aircraftId)
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.selectAircraft(a.id); vm.navigate(Screen.AircraftDetail(a.id)) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${a.id}  ${a.model}", style = MaterialTheme.typography.titleSmall, color = Aero.Text)
                            Text(r.primary.component.label, style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("RUL ${InferenceEngine.rulText(r.primary.rulHours)}", style = AeroType.Numeric, color = riskColor(r.risk))
                            Spacer(Modifier.height(3.dp))
                            Tag(r.risk.label, riskColor(r.risk))
                        }
                    }
                }
            }
        }

        item("workflow-h") {
            SectionHeader("How the AI pipeline works", supporting = "Telemetry to maintenance action in nine stages", actionLabel = "Details", onAction = { vm.navigate(Screen.Insights) })
        }
        item("workflow") {
            Panel(contentPadding = 12.dp) { WorkflowStrip(onClick = { vm.navigate(Screen.Insights) }) }
        }
    }
}

fun healthWord(h: Int): String = when {
    h >= 80 -> "GOOD"
    h >= 60 -> "FAIR"
    else -> "POOR"
}

@Composable
private fun FleetCount(label: String, value: Int, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Aero.Night)
            .border(1.dp, Aero.Hairline, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$value", style = AeroType.ReadoutMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Aero.TextMuted, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier, badge: Int = 0, onClick: () -> Unit) {
    Column(
        modifier
            .height(92.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Aero.Panel)
            .border(1.dp, Aero.Hairline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            Icon(icon, contentDescription = null, tint = Aero.Cyan, modifier = Modifier.size(26.dp))
            if (badge > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(start = 18.dp).size(16.dp).clip(RoundedCornerShape(8.dp)).background(Aero.Red),
                    contentAlignment = Alignment.Center,
                ) { Text("$badge", style = AeroType.Tag, color = Aero.Night) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Aero.Text, textAlign = TextAlign.Center, maxLines = 2)
    }
}
