package com.aeromaintenance.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aeromaintenance.ai.AeroViewModel
import com.aeromaintenance.ai.Screen
import com.aeromaintenance.ai.data.Aircraft
import com.aeromaintenance.ai.data.FleetStatus
import com.aeromaintenance.ai.data.SensorType
import com.aeromaintenance.ai.data.formatNumber
import com.aeromaintenance.ai.ui.components.HealthBands
import com.aeromaintenance.ai.ui.components.HealthBar
import com.aeromaintenance.ai.ui.components.InstrumentDial
import com.aeromaintenance.ai.ui.components.KeyValue
import com.aeromaintenance.ai.ui.components.Panel
import com.aeromaintenance.ai.ui.components.PrimaryButton
import com.aeromaintenance.ai.ui.components.SecondaryButton
import com.aeromaintenance.ai.ui.components.SectionHeader
import com.aeromaintenance.ai.ui.components.StatusDot
import com.aeromaintenance.ai.ui.components.Tag
import com.aeromaintenance.ai.ui.components.TrendChart
import com.aeromaintenance.ai.ui.theme.Aero
import com.aeromaintenance.ai.ui.theme.AeroType
import com.aeromaintenance.ai.ui.theme.fleetStatusColor
import com.aeromaintenance.ai.ui.theme.healthColor
import com.aeromaintenance.ai.ui.theme.riskColor

// ---------------------------------------------------------------- fleet list

@Composable
fun FleetScreen(vm: AeroViewModel, padding: PaddingValues) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val shown = vm.fleet.filter { filter == null || it.status.name == filter }
        .sortedWith(compareByDescending<Aircraft> { it.risk.rank }.thenBy { it.healthScore })

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = withScreenPadding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item("filters") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("All ${vm.fleet.size}", filter == null, Aero.Cyan) { filter = null }
                FleetStatus.entries.reversed().forEach { s ->
                    val n = vm.fleet.count { it.status == s }
                    FilterPill("${s.label} $n", filter == s.name, fleetStatusColor(s)) { filter = s.name }
                }
            }
        }
        items(shown, key = { it.id }) { a ->
            AircraftRow(a) {
                vm.selectAircraft(a.id)
                vm.navigate(Screen.AircraftDetail(a.id))
            }
        }
    }
}

@Composable
fun FilterPill(text: String, selected: Boolean, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) color.copy(alpha = 0.18f) else Aero.Panel)
            .border(1.dp, if (selected) color else Aero.Hairline, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) color else Aero.TextMuted)
    }
}

@Composable
private fun AircraftRow(a: Aircraft, onClick: () -> Unit) {
    Panel(contentPadding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InstrumentDial(
                value = a.healthScore.toFloat(), min = 0f, max = 100f, bands = HealthBands,
                modifier = Modifier.size(64.dp), stroke = 5.dp, ticks = 4,
            ) {
                Text("${a.healthScore}", style = AeroType.Numeric, color = healthColor(a.healthScore), modifier = Modifier.align(Alignment.BottomCenter))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.id, style = MaterialTheme.typography.titleMedium, color = Aero.Text)
                    Spacer(Modifier.width(8.dp))
                    Text(a.model, style = MaterialTheme.typography.bodyMedium, color = Aero.TextMuted)
                }
                Text(
                    "${formatNumber(a.flightHours.toDouble(), 0)} flight hours, ${formatNumber(a.flightCycles.toDouble(), 0)} cycles",
                    style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted,
                )
                Text("Last maintenance ${a.lastMaintenance}", style = MaterialTheme.typography.bodySmall, color = Aero.TextFaint)
            }
            Column(horizontalAlignment = Alignment.End) {
                Tag(a.risk.label, riskColor(a.risk))
                Spacer(Modifier.height(10.dp))
                Icon(Icons.Filled.ChevronRight, null, tint = Aero.TextFaint)
            }
        }
    }
}

// ------------------------------------------------------------- detail screen

@Composable
fun AircraftDetailScreen(vm: AeroViewModel, id: String, padding: PaddingValues) {
    val a = vm.aircraft(id)
    val listState = rememberLazyListState()
    DemoScroll(listState, vm.demoFocus, mapOf("components" to 2))
    val cond = vm.conditionOf(id)
    val pre = vm.preprocessed(id, cond)
    val window = vm.telemetry(id, cond)

    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = withScreenPadding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("header") {
            Panel(accent = fleetStatusColor(a.status)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Aircraft information", style = MaterialTheme.typography.labelMedium, color = Aero.TextMuted)
                        Text(a.id, style = MaterialTheme.typography.displaySmall, color = Aero.Text)
                        Text("${a.model}, ${a.base}", style = MaterialTheme.typography.bodyMedium, color = Aero.TextMuted)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Tag(a.status.label.uppercase(), fleetStatusColor(a.status))
                            Tag("${a.risk.label} RISK", riskColor(a.risk))
                        }
                    }
                    InstrumentDial(
                        value = a.healthScore.toFloat(), min = 0f, max = 100f, bands = HealthBands,
                        modifier = Modifier.size(112.dp), stroke = 7.dp, ticks = 4,
                    ) {
                        Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${a.healthScore}", style = AeroType.ReadoutSmall, color = healthColor(a.healthScore))
                            Text("health", style = MaterialTheme.typography.labelSmall, color = Aero.TextMuted)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth()) {
                    KeyValue("Flight hours", formatNumber(a.flightHours.toDouble(), 0), Modifier.weight(1f))
                    KeyValue("Flight cycles", formatNumber(a.flightCycles.toDouble(), 0), Modifier.weight(1f))
                    KeyValue("Last maintenance", a.lastMaintenance, Modifier.weight(1.3f))
                }
            }
        }
        item("components-h") {
            SectionHeader("Component health", supporting = "Last condition assessment")
        }
        item("components") {
            Panel(highlight = vm.demoFocus == "components") {
                a.components.forEachIndexed { i, c ->
                    if (i > 0) Spacer(Modifier.height(14.dp))
                    val color = healthColor(c.health)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(color)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.titleSmall, color = Aero.Text)
                            Text(c.note, style = MaterialTheme.typography.bodySmall, color = Aero.TextFaint)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${c.health}%", style = AeroType.ReadoutSmall, color = color)
                            Text(c.status, style = AeroType.Tag, color = color)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HealthBar(c.health, color)
                }
            }
        }
        item("vib-h") {
            SectionHeader("Vibration, last 120 operating hours", supporting = "${cond.label}. Open sensor monitoring for every channel.")
        }
        item("vib") {
            Panel(contentPadding = 12.dp) {
                TrendChart(
                    sensor = SensorType.VIBRATION,
                    raw = window.channels.getValue(SensorType.VIBRATION),
                    smooth = pre.smoothed.getValue(SensorType.VIBRATION),
                    spikes = pre.spikeIndices[SensorType.VIBRATION].orEmpty(),
                    height = 130.dp,
                    animationKey = cond,
                )
            }
        }
        item("actions") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    "Run AI analysis",
                    onClick = { vm.selectAircraft(id); vm.navigate(Screen.Analysis); vm.runAnalysis() },
                    icon = Icons.Filled.Psychology,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Sensor data", onClick = { vm.selectAircraft(id); vm.navigate(Screen.Monitoring) }, icon = Icons.Filled.MonitorHeart, modifier = Modifier.weight(1f))
                    SecondaryButton("Report", onClick = { vm.selectAircraft(id); vm.navigate(Screen.Reports) }, icon = Icons.Filled.Description, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
