package com.aeromaintenance.ai.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.aeromaintenance.ai.AeroViewModel
import com.aeromaintenance.ai.AnalysisUi
import com.aeromaintenance.ai.Screen
import com.aeromaintenance.ai.data.ComponentType
import com.aeromaintenance.ai.data.DataCondition
import com.aeromaintenance.ai.data.RiskLevel
import com.aeromaintenance.ai.data.SensorType
import com.aeromaintenance.ai.engine.AnalysisResult
import com.aeromaintenance.ai.engine.InferenceEngine
import com.aeromaintenance.ai.engine.PreprocessedWindow
import com.aeromaintenance.ai.ui.LocalRequestNotifications
import com.aeromaintenance.ai.ui.components.AircraftSelector
import com.aeromaintenance.ai.ui.components.Bullet
import com.aeromaintenance.ai.ui.components.ConditionToggle
import com.aeromaintenance.ai.ui.components.DialBand
import com.aeromaintenance.ai.ui.components.HealthBar
import com.aeromaintenance.ai.ui.components.InstrumentDial
import com.aeromaintenance.ai.ui.components.Panel
import com.aeromaintenance.ai.ui.components.PipelineStepper
import com.aeromaintenance.ai.ui.components.PrimaryButton
import com.aeromaintenance.ai.ui.components.ProjectionChart
import com.aeromaintenance.ai.ui.components.PrototypeNote
import com.aeromaintenance.ai.ui.components.SecondaryButton
import com.aeromaintenance.ai.ui.components.Tag
import com.aeromaintenance.ai.ui.theme.Aero
import com.aeromaintenance.ai.ui.theme.AeroType
import com.aeromaintenance.ai.ui.theme.healthColor
import com.aeromaintenance.ai.ui.theme.riskColor

val PipelineTitles = listOf(
    "Data acquisition",
    "Preprocessing",
    "Feature extraction",
    "Time-series model",
    "Anomaly detection",
    "Component health",
    "Failure-risk prediction",
    "RUL estimation",
    "Maintenance recommendation",
)

val AnomalyBands = listOf(
    DialBand(0f, InferenceEngine.ALERT_THRESHOLD.toFloat(), Aero.Green),
    DialBand(InferenceEngine.ALERT_THRESHOLD.toFloat(), InferenceEngine.HIGH_THRESHOLD.toFloat(), Aero.Orange),
    DialBand(InferenceEngine.HIGH_THRESHOLD.toFloat(), 1f, Aero.Red),
)

@Composable
fun AnalysisScreen(vm: AeroViewModel, padding: PaddingValues) {
    val requestNotifications = LocalRequestNotifications.current
    val id = vm.selectedId
    val cond = vm.conditionOf(id)
    val state = vm.analysisUi
    val listState = rememberLazyListState()
    DemoScroll(
        listState, vm.demoFocus,
        mapOf("pipeline" to 1, "score" to 2, "risk" to 3, "rul" to 4, "recommendation" to 5),
    )

    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = withScreenPadding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("model") {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Aero.Magenta.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Psychology, null, tint = Aero.Magenta) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("AI model", style = MaterialTheme.typography.labelMedium, color = Aero.TextMuted)
                        Text(InferenceEngine.MODEL_NAME, style = MaterialTheme.typography.titleMedium, color = Aero.Text)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Tag(InferenceEngine.MODEL_TAG, Aero.Magenta)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${InferenceEngine.MODEL_VERSION}. Deterministic research model running on the phone; see AI insights for exactly what it computes.",
                    style = MaterialTheme.typography.bodySmall, color = Aero.TextFaint,
                )
                Spacer(Modifier.height(14.dp))
                AircraftSelector(vm)
                Spacer(Modifier.height(10.dp))
                ConditionToggle(cond, onChange = { vm.setCondition(id, it) })
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    when (state) {
                        is AnalysisUi.Done -> "Run AI analysis again"
                        is AnalysisUi.Running -> "Analysing…"
                        AnalysisUi.Idle -> "Run AI analysis"
                    },
                    onClick = { vm.runAnalysis() },
                    enabled = state !is AnalysisUi.Running,
                    icon = if (state is AnalysisUi.Done) Icons.Filled.Refresh else Icons.Filled.Psychology,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        when (state) {
            AnalysisUi.Idle -> item("idle") {
                Panel {
                    Text("Analysis status", style = MaterialTheme.typography.labelMedium, color = Aero.TextMuted)
                    Text("Ready", style = MaterialTheme.typography.titleLarge, color = Aero.Text)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Input: ${id}, ${cond.label.lowercase()}, 6 channels × 120 operating hours (720 samples). " +
                            "Tap Run AI analysis to send it through the pipeline.",
                        style = MaterialTheme.typography.bodyMedium, color = Aero.TextMuted,
                    )
                    Spacer(Modifier.height(12.dp))
                    PipelineStepper(PipelineTitles, null, completed = -1)
                }
            }

            is AnalysisUi.Running -> item("running") {
                Panel(highlight = vm.demoFocus == "pipeline") {
                    Text("Analysis status", style = MaterialTheme.typography.labelMedium, color = Aero.TextMuted)
                    Text("Analyzing telemetry…", style = MaterialTheme.typography.titleLarge, color = Aero.Cyan)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.stage / vm.pipelineStageCount.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = Aero.Cyan, trackColor = Aero.Hairline,
                    )
                    Spacer(Modifier.height(14.dp))
                    PipelineStepper(PipelineTitles, null, completed = state.stage)
                }
            }

            is AnalysisUi.Done -> {
                val r = state.result
                val pre = vm.preprocessed(r.aircraftId, r.condition)
                item("score") { ScorePanel(r, vm.demoFocus == "score") }
                item("risk") { RiskPanel(r, vm.demoFocus == "risk") }
                item("rul") { RulPanel(r, pre, vm.demoFocus == "rul") }
                item("recommendation") {
                    RecommendationPanel(
                        r, vm.demoFocus == "recommendation",
                        onAlert = { requestNotifications(); vm.createAlert(r) },
                        onSchedule = { vm.scheduleMaintenance(r) },
                        onReport = { vm.navigate(Screen.Reports); vm.generateReport() },
                    )
                }
                item("explain") { ExplanationPanel(r) }
                item("components") { ComponentTable(r) }
                item("trace") { TracePanel(r, state.completedAt) }
            }
        }
        item("note") {
            PrototypeNote(
                "Prototype / simulated AI inference. No trained aviation model is running: the pipeline applies transparent statistical " +
                    "rules to simulated telemetry. A production system would use a time-series neural network (for example an LSTM or an autoencoder) trained on real fleet data.",
            )
        }
    }
}

@Composable
private fun ScorePanel(r: AnalysisResult, highlight: Boolean) {
    Panel(highlight = highlight, accent = riskColor(r.risk)) {
        Text("Anomaly score", style = MaterialTheme.typography.titleMedium, color = Aero.TextMuted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            InstrumentDial(
                value = r.anomalyScore.toFloat(), min = 0f, max = 1f, bands = AnomalyBands,
                modifier = Modifier.size(168.dp), stroke = 10.dp, animationKey = r,
            ) {
                Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(InferenceEngine.fmt(r.anomalyScore, 2), style = AeroType.ReadoutMedium, color = riskColor(r.risk))
                    Text("of 1.00", style = MaterialTheme.typography.labelSmall, color = Aero.TextMuted)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        r.anomalyScore >= InferenceEngine.HIGH_THRESHOLD -> "Strong anomaly"
                        r.anomalyScore >= InferenceEngine.ALERT_THRESHOLD -> "Anomaly detected"
                        else -> "Within normal behaviour"
                    },
                    style = MaterialTheme.typography.titleSmall, color = Aero.Text,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Deviation from the learned fleet baseline: ${InferenceEngine.fmt(r.deviationIndex, 1)}σ. " +
                        "Alert threshold ${InferenceEngine.fmt(InferenceEngine.ALERT_THRESHOLD, 2)}, high ${InferenceEngine.fmt(InferenceEngine.HIGH_THRESHOLD, 2)}.",
                    style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${r.samplesAnalysed} samples analysed, ${r.spikesRemoved} sensor spikes removed",
                    style = MaterialTheme.typography.bodySmall, color = Aero.TextFaint,
                )
            }
        }
    }
}

@Composable
private fun RiskPanel(r: AnalysisResult, highlight: Boolean) {
    val low = r.risk == RiskLevel.LOW
    val color = riskColor(r.risk)
    Panel(highlight = highlight) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Failure risk", style = MaterialTheme.typography.labelMedium, color = Aero.TextMuted)
                Text(r.risk.label, style = AeroType.Readout, color = color)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Confidence", style = MaterialTheme.typography.labelMedium, color = Aero.TextMuted)
                Text("${InferenceEngine.fmt(r.confidence * 100, 1)}%", style = AeroType.ReadoutMedium, color = Aero.Text)
            }
        }
        HorizontalDivider(color = Aero.Hairline, modifier = Modifier.padding(vertical = 10.dp))
        Text("Component", style = MaterialTheme.typography.labelMedium, color = Aero.TextMuted)
        Text(
            if (low) "No component at risk" else r.primary.component.label,
            style = MaterialTheme.typography.headlineSmall, color = Aero.Text,
        )
        Spacer(Modifier.height(8.dp))
        Text("Predicted condition", style = MaterialTheme.typography.labelMedium, color = Aero.TextMuted)
        Text(r.primary.condition, style = MaterialTheme.typography.titleMedium, color = if (low) Aero.Green else color)
        if (!low) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Component health", style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted, modifier = Modifier.weight(1f))
                Text("${r.primary.health}%", style = AeroType.Numeric, color = healthColor(r.primary.health))
            }
            Spacer(Modifier.height(4.dp))
            HealthBar(r.primary.health, healthColor(r.primary.health))
            r.onsetHoursAgo?.let {
                Spacer(Modifier.height(8.dp))
                Text("Degradation onset detected about $it operating hours ago (CUSUM change-point).", style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted)
            }
        }
    }
}

fun indicatorHistory(c: ComponentType, pre: PreprocessedWindow): DoubleArray = when (c) {
    ComponentType.ENGINE_BEARING -> pre.smoothed.getValue(SensorType.VIBRATION)
    ComponentType.TURBINE -> pre.smoothed.getValue(SensorType.ENGINE_TEMP)
    ComponentType.FUEL_PUMP -> pre.smoothed.getValue(SensorType.FUEL_FLOW)
    ComponentType.HYDRAULIC_PUMP -> pre.hydraulicRipple
}

fun indicatorNominal(c: ComponentType): Double = when (c) {
    ComponentType.ENGINE_BEARING -> InferenceEngine.BEARING_NOMINAL
    ComponentType.TURBINE -> InferenceEngine.EGT_NOMINAL
    ComponentType.FUEL_PUMP -> InferenceEngine.FUEL_NOMINAL
    ComponentType.HYDRAULIC_PUMP -> InferenceEngine.RIPPLE_NOMINAL
}

@Composable
private fun RulPanel(r: AnalysisResult, pre: PreprocessedWindow, highlight: Boolean) {
    val p = r.primary
    val low = r.risk == RiskLevel.LOW
    Panel(highlight = highlight) {
        Text("Estimated remaining useful life", style = MaterialTheme.typography.labelMedium, color = Aero.TextMuted)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(if (p.rulHours >= 999) "999+" else "${p.rulHours}", style = AeroType.Readout, color = Aero.Magenta)
            Spacer(Modifier.width(8.dp))
            Text("operating hours", style = MaterialTheme.typography.titleMedium, color = Aero.Text, modifier = Modifier.padding(bottom = 8.dp))
        }
        Text(
            if (low) "No degradation trend. Shortest projected margin: ${p.component.label}, at the minimum wear rate."
            else "${p.indicatorName} ${InferenceEngine.fmt(p.indicatorValue, p.indicatorDecimals)} ${p.indicatorUnit}, rising " +
                "${InferenceEngine.fmt(p.trendPerHour, p.indicatorDecimals + 2)} ${p.indicatorUnit} per hour (R² ${InferenceEngine.fmt(p.trendR2, 2)}). " +
                "Projected to reach the ${InferenceEngine.fmt(p.indicatorLimit, p.indicatorDecimals)} ${p.indicatorUnit} limit in ${InferenceEngine.rulText(p.rulHours)}.",
            style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted,
        )
        if (!low) {
            Spacer(Modifier.height(10.dp))
            ProjectionChart(
                history = indicatorHistory(p.component, pre),
                current = p.indicatorValue,
                nominal = indicatorNominal(p.component),
                limit = p.indicatorLimit,
                rulHours = p.rulHours,
                decimals = p.indicatorDecimals,
                unit = p.indicatorUnit,
                animationKey = r,
            )
        }
    }
}

@Composable
private fun RecommendationPanel(
    r: AnalysisResult,
    highlight: Boolean,
    onAlert: () -> Unit,
    onSchedule: () -> Unit,
    onReport: () -> Unit,
) {
    val low = r.risk == RiskLevel.LOW
    Panel(highlight = highlight, accent = if (low) Aero.Green else Aero.Cyan) {
        Text("AI recommendation", style = MaterialTheme.typography.labelMedium, color = Aero.Cyan)
        Spacer(Modifier.height(4.dp))
        Text("“${r.primary.recommendation}”", style = MaterialTheme.typography.titleLarge, color = Aero.Text, fontStyle = FontStyle.Normal)
        Spacer(Modifier.height(10.dp))
        Text("Work package", style = MaterialTheme.typography.labelMedium, color = Aero.TextMuted)
        r.primary.workPackage.forEach { Bullet(it) }
        Spacer(Modifier.height(12.dp))
        if (!low) {
            PrimaryButton("Create alert", onClick = onAlert, icon = Icons.Filled.NotificationsActive, modifier = Modifier.fillMaxWidth(), color = Aero.Orange)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Schedule work", onClick = onSchedule, icon = Icons.Filled.Build, modifier = Modifier.weight(1f))
                SecondaryButton("Report", onClick = onReport, icon = Icons.Filled.Description, modifier = Modifier.weight(1f))
            }
        } else {
            SecondaryButton("Generate report", onClick = onReport, icon = Icons.Filled.Description, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ExplanationPanel(r: AnalysisResult) {
    Panel {
        Text("Why the model reached this result", style = MaterialTheme.typography.titleMedium, color = Aero.Text)
        Spacer(Modifier.height(6.dp))
        Text(r.explanation, style = MaterialTheme.typography.bodyMedium, color = Aero.TextMuted)
        if (r.risk != RiskLevel.LOW) {
            Spacer(Modifier.height(12.dp))
            Text("Evidence by sensor for ${r.primary.component.label}", style = MaterialTheme.typography.labelMedium, color = Aero.TextMuted)
            Spacer(Modifier.height(6.dp))
            r.primary.contributions.forEach { c ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Text(c.sensor.shortLabel, style = MaterialTheme.typography.bodySmall, color = Aero.Text, modifier = Modifier.width(96.dp))
                    HealthBar((c.share * 100).toInt(), Aero.Cyan, Modifier.weight(1f), height = 6.dp)
                    Text("${(c.share * 100).toInt()}%", style = AeroType.Numeric, color = Aero.Cyan, modifier = Modifier.width(44.dp).padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ComponentTable(r: AnalysisResult) {
    Panel(contentPadding = 0.dp) {
        Text("All components", style = MaterialTheme.typography.titleMedium, color = Aero.Text, modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp))
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text("Component", style = MaterialTheme.typography.labelSmall, color = Aero.TextFaint, modifier = Modifier.weight(1.6f))
            Text("Health", style = MaterialTheme.typography.labelSmall, color = Aero.TextFaint, modifier = Modifier.weight(0.8f))
            Text("Risk", style = MaterialTheme.typography.labelSmall, color = Aero.TextFaint, modifier = Modifier.weight(1f))
            Text("RUL", style = MaterialTheme.typography.labelSmall, color = Aero.TextFaint, modifier = Modifier.weight(0.8f))
        }
        r.components.sortedWith(compareByDescending<com.aeromaintenance.ai.engine.ComponentAssessment> { it.risk.rank }.thenBy { it.rulHours }).forEach { c ->
            HorizontalDivider(color = Aero.Hairline)
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(c.component.label, style = MaterialTheme.typography.bodyMedium, color = Aero.Text, modifier = Modifier.weight(1.6f))
                Text("${c.health}%", style = AeroType.Numeric, color = healthColor(c.health), modifier = Modifier.weight(0.8f))
                Box(Modifier.weight(1f)) { Tag(c.risk.label, riskColor(c.risk)) }
                Text(InferenceEngine.rulText(c.rulHours), style = AeroType.Numeric, color = Aero.Text, modifier = Modifier.weight(0.8f))
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun TracePanel(r: AnalysisResult, completedAt: String) {
    var open by rememberSaveable { mutableStateOf(false) }
    Panel {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { open = !open },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Pipeline trace", style = MaterialTheme.typography.titleMedium, color = Aero.Text)
                Text("Completed $completedAt, ${if (r.condition == DataCondition.NORMAL) "normal" else "abnormal"} data", style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted)
            }
            Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = Aero.Cyan)
        }
        if (open) {
            Spacer(Modifier.height(12.dp))
            PipelineStepper(r.stages.map { it.title }, r.stages.map { it.detail }, completed = r.stages.size)
        }
    }
}
