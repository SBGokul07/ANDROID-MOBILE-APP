package com.aeromaintenance.ai.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aeromaintenance.ai.AeroViewModel
import com.aeromaintenance.ai.ReportUi
import com.aeromaintenance.ai.data.RiskLevel
import com.aeromaintenance.ai.data.formatNumber
import com.aeromaintenance.ai.engine.InferenceEngine
import com.aeromaintenance.ai.engine.MaintenanceReport
import com.aeromaintenance.ai.engine.ReportGenerator
import com.aeromaintenance.ai.ui.components.AircraftSelector
import com.aeromaintenance.ai.ui.components.ConditionToggle
import com.aeromaintenance.ai.ui.components.Panel
import com.aeromaintenance.ai.ui.components.PrimaryButton
import com.aeromaintenance.ai.ui.components.SecondaryButton
import com.aeromaintenance.ai.ui.theme.Aero
import com.aeromaintenance.ai.ui.theme.AeroType

private val PaperRed = Color(0xFFC62828)
private val PaperOrange = Color(0xFFC2410C)
private val PaperGreen = Color(0xFF2B7A3E)

private fun paperRisk(r: RiskLevel) = when (r) {
    RiskLevel.HIGH -> PaperRed
    RiskLevel.MEDIUM -> PaperOrange
    RiskLevel.LOW -> PaperGreen
}

@Composable
fun ReportsScreen(vm: AeroViewModel, padding: PaddingValues) {
    val context = LocalContext.current
    val id = vm.selectedId
    val cond = vm.conditionOf(id)
    val state = vm.reportUi

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = withScreenPadding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("controls") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AircraftSelector(vm)
                ConditionToggle(cond, onChange = { vm.setCondition(id, it) })
                PrimaryButton(
                    if (state is ReportUi.Ready) "Generate again" else "Generate maintenance report",
                    onClick = { vm.generateReport() },
                    enabled = state !is ReportUi.Generating,
                    icon = if (state is ReportUi.Ready) Icons.Filled.Refresh else Icons.Filled.Description,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        when (state) {
            ReportUi.Idle -> item("idle") {
                Panel {
                    Text("No report yet", style = MaterialTheme.typography.titleMedium, color = Aero.Text)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "The report combines the aircraft record, the AI finding, the sensor evidence and the recommended work package for $id. " +
                            "It can be shared from the phone as text.",
                        style = MaterialTheme.typography.bodyMedium, color = Aero.TextMuted,
                    )
                }
            }
            is ReportUi.Generating -> item("gen") {
                Panel {
                    vm.reportSteps.forEachIndexed { i, s ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                                when {
                                    i < state.step -> Icon(Icons.Filled.Check, null, tint = Aero.Green, modifier = Modifier.size(20.dp))
                                    i == state.step -> CircularProgressIndicator(color = Aero.Cyan, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    else -> Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Aero.Hairline))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(s, style = MaterialTheme.typography.bodyMedium, color = if (i <= state.step) Aero.Text else Aero.TextFaint)
                        }
                    }
                }
            }
            is ReportUi.Ready -> {
                item("paper") { ReportPaper(state.report) }
                item("share") {
                    SecondaryButton(
                        "Share report",
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Maintenance report ${state.report.reportNumber}")
                                putExtra(Intent.EXTRA_TEXT, ReportGenerator.toPlainText(state.report))
                            }
                            context.startActivity(Intent.createChooser(send, "Share report"))
                        },
                        icon = Icons.Filled.Share,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportPaper(r: MaintenanceReport) {
    val res = r.result
    val p = res.primary
    val low = res.risk == RiskLevel.LOW
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Aero.Paper)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("AeroMaintenance AI", style = MaterialTheme.typography.labelMedium, color = Aero.InkMuted)
                Text("Predictive Maintenance Report", style = MaterialTheme.typography.headlineSmall, color = Aero.Ink)
            }
            Box(
                Modifier.border(1.5.dp, paperRisk(res.risk), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
            ) { Text("${res.risk.label} RISK", style = AeroType.Tag, color = paperRisk(res.risk)) }
        }
        Spacer(Modifier.height(4.dp))
        Text("Report ${r.reportNumber}, generated ${r.generatedAt}", style = MaterialTheme.typography.bodySmall, color = Aero.InkMuted)
        Text("AI-assisted prediction from simulated sensor data. Prototype, not for operational use.", style = MaterialTheme.typography.bodySmall, color = PaperOrange)

        PaperSection("Aircraft")
        PaperRow("Aircraft", r.aircraft.id)
        PaperRow("Model", r.aircraft.model)
        PaperRow("Flight hours", formatNumber(r.aircraft.flightHours.toDouble(), 0))
        PaperRow("Flight cycles", formatNumber(r.aircraft.flightCycles.toDouble(), 0))
        PaperRow("Last maintenance", r.aircraft.lastMaintenance)
        PaperRow("Data analysed", "${res.condition.label}, ${res.samplesAnalysed} samples")

        PaperSection("AI finding")
        PaperRow("Detected component", if (low) "None" else p.component.label, bold = true)
        PaperRow("Risk", res.risk.label, valueColor = paperRisk(res.risk), bold = true)
        PaperRow("Anomaly score", InferenceEngine.fmt(res.anomalyScore, 2))
        PaperRow("Estimated RUL", "${InferenceEngine.rulText(p.rulHours)} (operating hours)")
        PaperRow("Confidence", "${InferenceEngine.fmt(res.confidence * 100, 1)}%")
        PaperRow("Predicted condition", p.condition)
        Spacer(Modifier.height(6.dp))
        Text(r.summary, style = MaterialTheme.typography.bodyMedium, color = Aero.Ink)

        PaperSection("AI recommendation")
        Text(p.recommendation, style = MaterialTheme.typography.titleMedium, color = Aero.Ink)
        Spacer(Modifier.height(4.dp))
        p.workPackage.forEachIndexed { i, w ->
            Text("${i + 1}. $w", style = MaterialTheme.typography.bodyMedium, color = Aero.Ink, modifier = Modifier.padding(vertical = 2.dp))
        }

        PaperSection("Sensor evidence")
        Row(Modifier.padding(vertical = 4.dp)) {
            PaperHead("Channel", 1.5f); PaperHead("Baseline", 1f); PaperHead("Current", 1f); PaperHead("Trend", 1.1f)
        }
        r.evidence.forEach { e ->
            HorizontalDivider(color = Color(0xFFDDE2E8))
            Row(Modifier.padding(vertical = 5.dp)) {
                val c = if (e.flagged) PaperRed else Aero.Ink
                val w = if (e.flagged) FontWeight.SemiBold else FontWeight.Normal
                Text("${e.sensor.shortLabel} (${e.sensor.unit})", style = MaterialTheme.typography.bodySmall, color = c, fontWeight = w, modifier = Modifier.weight(1.5f))
                Text(e.baseline, style = MaterialTheme.typography.bodySmall, color = Aero.InkMuted, modifier = Modifier.weight(1f))
                Text(e.current, style = MaterialTheme.typography.bodySmall, color = c, fontWeight = w, modifier = Modifier.weight(1f))
                Text(e.trend, style = MaterialTheme.typography.bodySmall, color = Aero.InkMuted, modifier = Modifier.weight(1.1f))
            }
        }
        Text("Red rows deviate more than 3σ from the fleet baseline.", style = MaterialTheme.typography.labelSmall, color = Aero.InkMuted, modifier = Modifier.padding(top = 4.dp))

        PaperSection("Explanation")
        Text(res.explanation, style = MaterialTheme.typography.bodyMedium, color = Aero.Ink)

        PaperSection("Component assessment")
        res.components.forEach { c ->
            PaperRow(c.component.label, "${c.health}%, ${c.risk.label}, RUL ${InferenceEngine.rulText(c.rulHours)}", valueColor = paperRisk(c.risk))
        }

        PaperSection("Engineering review")
        Text("Reviewed by (licensed engineer): ______________________", style = MaterialTheme.typography.bodyMedium, color = Aero.Ink, modifier = Modifier.padding(top = 4.dp))
        Text("Date: ____________   Decision: accept / reject / defer", style = MaterialTheme.typography.bodyMedium, color = Aero.Ink, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFFDDE2E8))
        Spacer(Modifier.height(8.dp))
        Text(
            "Academic research prototype using simulated/synthetic data. Not certified for real-world aviation safety, maintenance or flight-critical decision-making.",
            style = MaterialTheme.typography.labelSmall, color = Aero.InkMuted,
        )
    }
}

@Composable
private fun PaperSection(title: String) {
    Spacer(Modifier.height(16.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, color = Aero.Ink)
    Spacer(Modifier.height(2.dp))
    Box(Modifier.fillMaxWidth().height(1.5.dp).background(Aero.Ink))
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun PaperRow(label: String, value: String, valueColor: Color = Aero.Ink, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Aero.InkMuted, modifier = Modifier.weight(1f))
        Text(
            value, style = MaterialTheme.typography.bodyMedium, color = valueColor,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1.3f),
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.PaperHead(text: String, weight: Float) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = Aero.InkMuted, modifier = Modifier.weight(weight))
}
