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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.aeromaintenance.ai.data.MaintenanceTask
import com.aeromaintenance.ai.data.RiskLevel
import com.aeromaintenance.ai.data.TaskStatus
import com.aeromaintenance.ai.engine.ComponentAssessment
import com.aeromaintenance.ai.engine.InferenceEngine
import com.aeromaintenance.ai.ui.components.AircraftSelector
import com.aeromaintenance.ai.ui.components.Bullet
import com.aeromaintenance.ai.ui.components.ConditionToggle
import com.aeromaintenance.ai.ui.components.Panel
import com.aeromaintenance.ai.ui.components.PrimaryButton
import com.aeromaintenance.ai.ui.components.RulRiskChart
import com.aeromaintenance.ai.ui.components.SectionHeader
import com.aeromaintenance.ai.ui.components.Tag
import com.aeromaintenance.ai.ui.theme.Aero
import com.aeromaintenance.ai.ui.theme.AeroType
import com.aeromaintenance.ai.ui.theme.healthColor
import com.aeromaintenance.ai.ui.theme.riskColor
import com.aeromaintenance.ai.ui.theme.taskStatusColor

@Composable
fun MaintenanceScreen(vm: AeroViewModel, padding: PaddingValues) {
    Column(Modifier.fillMaxSize()) {
        // Tabs sit under the top bar; the list below handles the rest of the insets.
        val top = padding.calculateTopPadding()
        Row(
            Modifier
                .padding(top = top + 4.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Aero.Panel)
                .border(1.dp, Aero.Hairline, RoundedCornerShape(12.dp))
                .padding(4.dp),
        ) {
            listOf("Predictive maintenance", "Work orders").forEachIndexed { i, label ->
                val sel = vm.maintenanceTab == i
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (sel) Aero.Cyan.copy(alpha = 0.16f) else Aero.Panel)
                        .clickable { vm.maintenanceTab = i }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = if (sel) Aero.Cyan else Aero.TextMuted)
                }
            }
        }
        val inner = PaddingValues(bottom = padding.calculateBottomPadding())
        if (vm.maintenanceTab == 0) PredictiveTab(vm, inner) else PlanningTab(vm, inner)
    }
}

@Composable
private fun PredictiveTab(vm: AeroViewModel, padding: PaddingValues) {
    val id = vm.selectedId
    val cond = vm.conditionOf(id)
    val result = vm.resultFor(id, cond)
    val rows = result.components.sortedWith(compareByDescending<ComponentAssessment> { it.risk.rank }.thenBy { it.rulHours })

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = withScreenPadding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("sel") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AircraftSelector(vm)
                ConditionToggle(cond, onChange = { vm.setCondition(id, it) })
            }
        }
        item("table-h") {
            SectionHeader("Component predictions", supporting = "${id}, ${cond.label.lowercase()}. Anomaly score ${InferenceEngine.fmt(result.anomalyScore, 2)}.")
        }
        item("table") {
            Panel(contentPadding = 0.dp) {
                Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 6.dp)) {
                    HeaderCell("Component", 1.5f)
                    HeaderCell("Health", 0.75f)
                    HeaderCell("Risk", 0.95f)
                    HeaderCell("RUL", 0.75f)
                    HeaderCell("Action", 0.8f)
                }
                rows.forEach { c ->
                    HorizontalDivider(color = Aero.Hairline)
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(c.component.label, style = MaterialTheme.typography.bodyMedium, color = Aero.Text, modifier = Modifier.weight(1.5f))
                        Text("${c.health}%", style = AeroType.Numeric, color = healthColor(c.health), modifier = Modifier.weight(0.75f))
                        Box(Modifier.weight(0.95f)) { Tag(c.risk.label, riskColor(c.risk)) }
                        Text(InferenceEngine.rulText(c.rulHours), style = AeroType.Numeric, color = Aero.Text, modifier = Modifier.weight(0.75f))
                        Text(c.shortAction, style = MaterialTheme.typography.labelLarge, color = riskColor(c.risk), modifier = Modifier.weight(0.8f))
                    }
                }
            }
        }
        item("chart-h") {
            SectionHeader("Risk chart", supporting = "Remaining useful life per component. Dashed lines are the HIGH and MEDIUM risk rules.")
        }
        item("chart") {
            Panel(contentPadding = 14.dp) { RulRiskChart(rows, animationKey = id to cond) }
        }
        item("fleet-h") {
            SectionHeader("Fleet watchlist", supporting = "Components at MEDIUM or HIGH risk across all 12 aircraft, soonest first")
        }
        item("fleet") {
            val watch = vm.fleetPredictions.flatMap { r -> r.components.filter { it.risk != RiskLevel.LOW }.map { r.aircraftId to it } }
                .sortedBy { it.second.rulHours }
            Panel(contentPadding = 0.dp) {
                watch.forEachIndexed { i, (aid, c) ->
                    if (i > 0) HorizontalDivider(color = Aero.Hairline)
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.selectAircraft(aid); vm.navigate(Screen.AircraftDetail(aid)) }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("$aid  ${c.component.label}", style = MaterialTheme.typography.titleSmall, color = Aero.Text)
                            Text("Health ${c.health}%, ${c.shortAction.lowercase()}", style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted)
                        }
                        Text(InferenceEngine.rulText(c.rulHours), style = AeroType.Numeric, color = riskColor(c.risk))
                        Spacer(Modifier.width(10.dp))
                        Tag(c.risk.label, riskColor(c.risk))
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(text: String, weight: Float) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = Aero.TextFaint, modifier = Modifier.weight(weight))
}

@Composable
private fun PlanningTab(vm: AeroViewModel, padding: PaddingValues) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(vm.demoFocus) {
        if (vm.demoFocus == "task") listState.animateScrollToItem(0)
    }
    val shown = vm.tasks.filter { filter == null || it.status.name == filter }
        .sortedWith(compareBy<MaintenanceTask> { it.status.ordinal }.thenBy { if (it.status == TaskStatus.COMPLETED) 0 else it.dueHours })

    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = withScreenPadding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("filters") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("All ${vm.tasks.size}", filter == null, Aero.Cyan) { filter = null }
                TaskStatus.entries.forEach { s ->
                    FilterPill("${s.label} ${vm.tasks.count { it.status == s }}", filter == s.name, taskStatusColor(s)) { filter = s.name }
                }
            }
        }
        item("upcoming-h") {
            SectionHeader("Upcoming maintenance", supporting = "Due times are operating hours, taken from the AI's remaining-useful-life estimate")
        }
        items(shown, key = { it.id }) { t -> TaskCard(t, highlight = vm.highlightedTaskId == t.id && vm.demoFocus == "task", onAdvance = { vm.advanceTask(t.id) }) }
    }
}

@Composable
private fun TaskCard(t: MaintenanceTask, highlight: Boolean, onAdvance: () -> Unit) {
    val statusColor = taskStatusColor(t.status)
    Panel(highlight = highlight, accent = if (t.updatedByAiAt != null) Aero.Magenta else null) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(t.aircraftId, style = MaterialTheme.typography.labelLarge, color = Aero.Cyan)
                Text(t.title, style = MaterialTheme.typography.titleMedium, color = Aero.Text)
                Text("${t.id}, ${t.source.lowercase()}", style = MaterialTheme.typography.bodySmall, color = Aero.TextFaint)
            }
            Tag(t.status.label.uppercase(), statusColor)
        }
        Spacer(Modifier.height(10.dp))
        if (t.status != TaskStatus.COMPLETED) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Due in", style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted, modifier = Modifier.padding(bottom = 3.dp))
                Spacer(Modifier.width(6.dp))
                Text("${t.dueHours}", style = AeroType.ReadoutMedium, color = dueColor(t.dueHours))
                Spacer(Modifier.width(6.dp))
                Text("operating hours", style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted, modifier = Modifier.padding(bottom = 3.dp))
            }
        } else {
            Text("Completed and signed off", style = MaterialTheme.typography.bodyMedium, color = Aero.Green)
        }
        t.updatedByAiAt?.let {
            Text("Updated by AI analysis at $it", style = MaterialTheme.typography.bodySmall, color = Aero.Magenta)
        }
        Spacer(Modifier.height(8.dp))
        t.workPackage.forEach { Bullet(it, color = statusColor) }
        if (t.status != TaskStatus.COMPLETED) {
            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                if (t.status == TaskStatus.SCHEDULED) "Start work" else "Mark completed",
                onClick = onAdvance,
                icon = if (t.status == TaskStatus.SCHEDULED) Icons.Filled.PlayArrow else Icons.Filled.CheckCircle,
                color = if (t.status == TaskStatus.SCHEDULED) Aero.Cyan else Aero.Green,
                modifier = Modifier.fillMaxWidth().height(46.dp),
            )
        }
    }
}

private fun dueColor(h: Int) = when {
    h < 72 -> Aero.Red
    h < 250 -> Aero.Orange
    else -> Aero.Green
}
