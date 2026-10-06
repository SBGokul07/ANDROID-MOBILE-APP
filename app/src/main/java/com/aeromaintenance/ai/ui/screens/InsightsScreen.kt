package com.aeromaintenance.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aeromaintenance.ai.AeroViewModel
import com.aeromaintenance.ai.engine.InferenceEngine
import com.aeromaintenance.ai.ui.components.Panel
import com.aeromaintenance.ai.ui.components.SectionHeader
import com.aeromaintenance.ai.ui.components.Tag
import com.aeromaintenance.ai.ui.theme.Aero
import com.aeromaintenance.ai.ui.theme.AeroType

private data class ArchStage(val name: String, val concept: String, val prototype: String)

private val architecture = listOf(
    ArchStage(
        "Sensor data",
        "Engine and system telemetry recorded in flight.",
        "Six simulated channels, one sample per operating hour, 120 hours per aircraft. Normal and abnormal data sets.",
    ),
    ArchStage(
        "Preprocessing",
        "Remove sensor glitches, smooth noise, put channels on a common scale.",
        "Hampel filter (median ± 4.5 MAD) replaces spikes, 5-point moving average, z-score against the fleet baseline.",
    ),
    ArchStage(
        "Feature extraction",
        "Summarise each window with numbers that describe behaviour.",
        "Per channel over the last 24 h: level (z-score), trend (least-squares slope and R²), volatility (fluctuation vs normal). 18 features.",
    ),
    ArchStage(
        "Time-series model",
        "Learn what normal looks like and measure how far today's data is from it.",
        "Normal-behaviour model: weighted root-mean-square of each channel's deviation from the learned baseline, in σ.",
    ),
    ArchStage(
        "Anomaly detection",
        "Turn the deviation into a score and decide whether it is abnormal.",
        "Logistic function maps deviation to a 0–1 score (0.5 at ${InferenceEngine.fmt(InferenceEngine.ANOMALY_MIDPOINT, 2)}σ). Alert above ${InferenceEngine.fmt(InferenceEngine.ALERT_THRESHOLD, 2)}. CUSUM finds when it started.",
    ),
    ArchStage(
        "Risk prediction",
        "Attribute the anomaly to a component and rate the failure risk.",
        "Fault-signature matrix links channels to components. Health = 100 × (1 − margin used^1.6). HIGH if RUL < 72 h or health < 65 %.",
    ),
    ArchStage(
        "RUL estimation",
        "Estimate operating hours until the component reaches its limit.",
        "Linear extrapolation of the health-indicator trend to its limit (for example vibration to 7.0 mm/s), rounded down.",
    ),
)

private val productionModels = listOf(
    "LSTM" to "Long short-term memory network; learns long temporal patterns in sensor sequences. Common baseline for RUL regression.",
    "GRU" to "Gated recurrent unit; a lighter LSTM with similar accuracy and faster training, suited to on-device inference.",
    "BiLSTM" to "Bidirectional LSTM; reads each window forwards and backwards for richer context when classifying fault types.",
    "1D CNN" to "Convolutions along time; fast at spotting local shapes such as vibration bursts or pressure ripple.",
    "CNN-LSTM" to "CNN layers extract local features, LSTM layers model how they evolve; strong on multi-sensor RUL benchmarks.",
    "Transformer" to "Self-attention over the whole window; captures long-range interactions between channels.",
    "Autoencoder" to "Trained only on healthy data; a high reconstruction error flags an anomaly without needing labelled failures.",
)

@Composable
fun InsightsScreen(vm: AeroViewModel, padding: PaddingValues) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = withScreenPadding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("label") {
            Panel(accent = Aero.Magenta) {
                Tag(InferenceEngine.MODEL_TAG, Aero.Magenta)
                Spacer(Modifier.height(10.dp))
                Text("What this prototype actually does", style = MaterialTheme.typography.titleLarge, color = Aero.Text)
                Spacer(Modifier.height(6.dp))
                Text(
                    "The app shows the full predictive-maintenance workflow on simulated telemetry. The \"neural network\" label names the role the model " +
                        "plays in a production system; inside this prototype each stage is a transparent statistical method, so every number on screen can be traced and explained. " +
                        "No trained aviation model exists in the app, and its outputs are not real aircraft predictions.",
                    style = MaterialTheme.typography.bodyMedium, color = Aero.TextMuted,
                )
            }
        }
        item("arch-h") {
            SectionHeader("Architecture", supporting = "Grey: the concept. Cyan: how this prototype implements it.")
        }
        item("arch") {
            Panel(contentPadding = 14.dp) {
                architecture.forEachIndexed { i, s ->
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(30.dp).fillMaxHeight()) {
                            Box(
                                Modifier.clip(RoundedCornerShape(8.dp)).background(Aero.Cyan.copy(alpha = 0.14f))
                                    .border(1.dp, Aero.Cyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                            ) { Text("${i + 1}", style = AeroType.Tag, color = Aero.Cyan) }
                            if (i < architecture.lastIndex) {
                                Box(Modifier.padding(top = 4.dp).width(1.5.dp).weight(1f).background(Aero.HairlineStrong))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f).padding(bottom = 14.dp)) {
                            Text(s.name, style = MaterialTheme.typography.titleMedium, color = Aero.Text)
                            Text(s.concept, style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted)
                            Spacer(Modifier.height(4.dp))
                            Text(s.prototype, style = MaterialTheme.typography.bodySmall, color = Aero.Cyan)
                        }
                    }
                }
            }
        }
        item("models-h") {
            SectionHeader("Models for a production version", supporting = "Would replace the statistical model once real fleet data is available")
        }
        item("models") {
            Panel(contentPadding = 0.dp) {
                productionModels.forEachIndexed { i, (name, desc) ->
                    if (i > 0) HorizontalDivider(color = Aero.Hairline)
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(name, style = MaterialTheme.typography.titleSmall, color = Aero.Magenta, modifier = Modifier.width(96.dp))
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item("data-h") { SectionHeader("Training a real model would need") }
        item("data") {
            Panel {
                listOf(
                    "Labelled run-to-failure histories, for example the NASA C-MAPSS turbofan degradation data set, for a first benchmark.",
                    "Operator fleet data with maintenance records to label when components were actually replaced.",
                    "Validation against engineering limits from the maintenance manual and sign-off by licensed engineers.",
                ).forEach { com.aeromaintenance.ai.ui.components.Bullet(it) }
            }
        }
        item("consts") {
            Panel {
                Text("Prototype parameters", style = MaterialTheme.typography.titleMedium, color = Aero.Text)
                Spacer(Modifier.height(8.dp))
                ParamRow("Window", "120 operating hours, 6 channels")
                ParamRow("Recent feature window", "${InferenceEngine.RECENT} h")
                ParamRow("Anomaly alert threshold", InferenceEngine.fmt(InferenceEngine.ALERT_THRESHOLD, 2))
                ParamRow("High-risk threshold", InferenceEngine.fmt(InferenceEngine.HIGH_THRESHOLD, 2))
                ParamRow("Detection sensitivity", "${InferenceEngine.fmt(InferenceEngine.detectionSigma, 1)}σ")
                ParamRow("Bearing vibration limit", "${InferenceEngine.fmt(InferenceEngine.BEARING_LIMIT, 1)} mm/s")
                ParamRow("EGT limit", "${InferenceEngine.fmt(InferenceEngine.EGT_LIMIT, 0)} °C")
            }
        }
    }
}

@Composable
private fun ParamRow(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(k, style = MaterialTheme.typography.bodyMedium, color = Aero.TextMuted, modifier = Modifier.weight(1f))
        Text(v, style = AeroType.Numeric, color = Aero.Text)
    }
}
