package com.aeromaintenance.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aeromaintenance.ai.AeroViewModel
import com.aeromaintenance.ai.DemoScript
import com.aeromaintenance.ai.Screen
import com.aeromaintenance.ai.ui.LocalRequestNotifications
import com.aeromaintenance.ai.ui.components.Bullet
import com.aeromaintenance.ai.ui.components.Panel
import com.aeromaintenance.ai.ui.components.PrimaryButton
import com.aeromaintenance.ai.ui.components.SecondaryButton
import com.aeromaintenance.ai.ui.components.StageChip
import com.aeromaintenance.ai.ui.theme.Aero
import com.aeromaintenance.ai.ui.theme.AeroType

@Composable
fun FacultyDemoScreen(vm: AeroViewModel, padding: PaddingValues) {
    val requestNotifications = LocalRequestNotifications.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = withScreenPadding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("problem") {
            Panel(accent = Aero.Red) {
                Text("Problem", style = MaterialTheme.typography.labelLarge, color = Aero.Red)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Unexpected aircraft component degradation can result in maintenance delays, downtime, and operational risk.",
                    style = MaterialTheme.typography.titleLarge, color = Aero.Text,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Fixed-interval maintenance either replaces parts that still have life left or misses a part that wears faster than expected.",
                    style = MaterialTheme.typography.bodyMedium, color = Aero.TextMuted,
                )
            }
        }
        item("solution") {
            Panel(accent = Aero.Green) {
                Text("Proposed solution", style = MaterialTheme.typography.labelLarge, color = Aero.Green)
                Spacer(Modifier.height(4.dp))
                Text("AI-assisted predictive maintenance using aircraft telemetry.", style = MaterialTheme.typography.titleLarge, color = Aero.Text)
                Spacer(Modifier.height(6.dp))
                Text(
                    "The app watches sensor trends, scores how abnormal they are, names the component at risk, estimates how many operating hours remain, and turns that into an alert and a work order.",
                    style = MaterialTheme.typography.bodyMedium, color = Aero.TextMuted,
                )
            }
        }
        item("pipeline") {
            Panel {
                Text("AI pipeline", style = MaterialTheme.typography.titleMedium, color = Aero.Text)
                Spacer(Modifier.height(10.dp))
                val stages = listOf("Telemetry", "Preprocessing", "Anomaly detection", "Risk prediction", "RUL", "Maintenance recommendation")
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    stages.forEachIndexed { i, s ->
                        StageChip(s)
                        if (i < stages.lastIndex) Text("↓", style = MaterialTheme.typography.titleMedium, color = Aero.Cyan)
                    }
                }
            }
        }
        item("benefits") {
            Panel {
                Text("Real-world benefits", style = MaterialTheme.typography.titleMedium, color = Aero.Text)
                Spacer(Modifier.height(6.dp))
                listOf(
                    "Early fault detection",
                    "Reduced unexpected downtime",
                    "Better maintenance planning",
                    "Component health monitoring",
                    "Data-driven maintenance decisions",
                ).forEach { Bullet(it, color = Aero.Green) }
            }
        }
        item("future") {
            Panel {
                Text("Future scope", style = MaterialTheme.typography.titleMedium, color = Aero.Text)
                Spacer(Modifier.height(6.dp))
                listOf(
                    "Real aircraft IoT/sensor integration",
                    "Edge AI",
                    "Real-time streaming",
                    "Digital twins",
                    "Federated learning",
                    "Explainable AI",
                    "Integration with maintenance management systems",
                ).forEach { Bullet(it, color = Aero.Magenta) }
            }
        }
        item("script") {
            Panel {
                Text("Guided demo, about 70 seconds", style = MaterialTheme.typography.titleMedium, color = Aero.Text)
                Text("Pause at any step to talk; the narration appears above the navigation bar.", style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted)
                Spacer(Modifier.height(10.dp))
                DemoScript.steps.forEachIndexed { i, s ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text("${i + 1}", style = AeroType.Numeric, color = Aero.Cyan, modifier = Modifier.width(24.dp))
                        Text(s.title, style = MaterialTheme.typography.bodyMedium, color = Aero.Text)
                    }
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    "Start demo",
                    onClick = { requestNotifications(); vm.startDemo() },
                    icon = Icons.Filled.PlayArrow,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                SecondaryButton("How the AI works", onClick = { vm.navigate(Screen.Insights) }, icon = Icons.Filled.Insights, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
