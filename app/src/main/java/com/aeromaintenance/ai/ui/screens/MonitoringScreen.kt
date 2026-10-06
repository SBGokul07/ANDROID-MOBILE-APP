package com.aeromaintenance.ai.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aeromaintenance.ai.AeroViewModel
import com.aeromaintenance.ai.Screen
import com.aeromaintenance.ai.data.DataCondition
import com.aeromaintenance.ai.data.SensorType
import com.aeromaintenance.ai.data.Zone
import com.aeromaintenance.ai.engine.InferenceEngine
import com.aeromaintenance.ai.ui.components.AircraftSelector
import com.aeromaintenance.ai.ui.components.ConditionToggle
import com.aeromaintenance.ai.ui.components.DialBand
import com.aeromaintenance.ai.ui.components.InstrumentDial
import com.aeromaintenance.ai.ui.components.Panel
import com.aeromaintenance.ai.ui.components.PrimaryButton
import com.aeromaintenance.ai.ui.components.PrototypeNote
import com.aeromaintenance.ai.ui.components.SectionHeader
import com.aeromaintenance.ai.ui.components.Tag
import com.aeromaintenance.ai.ui.components.TrendChart
import com.aeromaintenance.ai.ui.theme.Aero
import com.aeromaintenance.ai.ui.theme.AeroType
import com.aeromaintenance.ai.ui.theme.zoneColor
import kotlinx.coroutines.delay
import kotlin.math.sin

/** Colour ranges for a channel's dial, built from its caution / warning limits. */
fun sensorBands(s: SensorType): List<DialBand> {
    val lo = s.chartMin.toFloat()
    val hi = s.chartMax.toFloat()
    val bands = mutableListOf<DialBand>()
    val greenLo = (s.warnLow ?: s.chartMin).toFloat()
    val greenHi = (s.warnHigh ?: s.chartMax).toFloat()
    s.alarmLow?.let { bands += DialBand(lo, it.toFloat(), Aero.Red) }
    s.warnLow?.let { bands += DialBand((s.alarmLow ?: s.chartMin).toFloat(), it.toFloat(), Aero.Amber) }
    bands += DialBand(greenLo, greenHi, Aero.Green)
    s.warnHigh?.let { bands += DialBand(it.toFloat(), (s.alarmHigh ?: s.chartMax).toFloat(), Aero.Amber) }
    s.alarmHigh?.let { bands += DialBand(it.toFloat(), hi, Aero.Red) }
    return bands
}

private val chartOrder = listOf(
    SensorType.ENGINE_TEMP to "Temperature trend",
    SensorType.VIBRATION to "Vibration trend",
    SensorType.HYDRAULIC_PRESSURE to "Hydraulic pressure trend",
    SensorType.OIL_PRESSURE to "Oil pressure trend",
    SensorType.RPM to "RPM trend",
    SensorType.FUEL_FLOW to "Fuel flow trend",
)

@Composable
fun MonitoringScreen(vm: AeroViewModel, padding: PaddingValues) {
    val id = vm.selectedId
    val cond = vm.conditionOf(id)
    val window = vm.telemetry(id, cond)
    val pre = vm.preprocessed(id, cond)
    val result = vm.resultFor(id, cond)
    val onsetIndex = result.onsetHoursAgo?.let { window.size - 1 - it }
    val onsetSensors = InferenceEngine.componentSignatures.getValue(result.primary.component).keys
    val features = result.features.associateBy { it.sensor }

    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
        }
    }

    val listState = rememberLazyListState()
    DemoScroll(listState, vm.demoFocus, mapOf("tiles" to 1, "charts" to 4))

    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = withScreenPadding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("controls") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AircraftSelector(vm)
                ConditionToggle(cond, onChange = { vm.setCondition(id, it) })
                LiveLine(cond)
            }
        }
        item("tiles") {
            Panel(highlight = vm.demoFocus == "tiles", contentPadding = 10.dp) {
                val sensors = SensorType.entries
                sensors.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { s ->
                            val base = features.getValue(s).current
                            val jitter = sin(tick * 1.7 + s.ordinal * 2.1) * s.baselineStd * 0.35
                            InstrumentTile(s, base + jitter, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item("run") {
            PrimaryButton(
                "Run AI analysis",
                onClick = { vm.navigate(Screen.Analysis); vm.runAnalysis() },
                icon = Icons.Filled.Psychology,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item("charts-h") {
            SectionHeader(
                "Telemetry history",
                supporting = if (cond == DataCondition.ABNORMAL) "Abnormal data set: degradation injected from about ${result.onsetHoursAgo ?: 90} h ago"
                else "Normal data set: noise around the fleet baseline",
            )
        }
        chartOrder.forEachIndexed { i, (sensor, title) ->
            item("chart-$sensor") {
                val f = features.getValue(sensor)
                val zone = sensor.zoneOf(f.current)
                Panel(highlight = vm.demoFocus == "charts" && i < 2, contentPadding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleSmall, color = Aero.Text)
                            Text(sensor.label, style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted)
                        }
                        Text("${sensor.format(f.current)} ${sensor.unit}", style = AeroType.ReadoutSmall, color = zoneColor(zone))
                        Spacer(Modifier.width(8.dp))
                        Tag(zoneWord(zone), zoneColor(zone))
                    }
                    Spacer(Modifier.height(8.dp))
                    TrendChart(
                        sensor = sensor,
                        raw = window.channels.getValue(sensor),
                        smooth = pre.smoothed.getValue(sensor),
                        spikes = pre.spikeIndices[sensor].orEmpty(),
                        onsetIndex = if (sensor in onsetSensors) onsetIndex else null,
                        animationKey = id to cond,
                    )
                    Spacer(Modifier.height(4.dp))
                    val slope = f.slopePerHour
                    Text(
                        "Trend over the last 24 h: ${if (slope >= 0) "+" else ""}${InferenceEngine.fmt(slope, sensor.decimals + 2)} ${sensor.unit} per hour" +
                            if (f.volatilityRatio > 1.6) ", fluctuation ${InferenceEngine.fmt(f.volatilityRatio, 1)}× normal" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Aero.TextMuted,
                    )
                }
            }
        }
        item("legend") {
            PrototypeNote(
                "Grey: raw samples. Coloured: cleaned and smoothed signal, green inside limits, amber in the caution band, red past the warning limit. " +
                    "Rings: sensor spikes removed during preprocessing. All telemetry is simulated on the phone; no aircraft data source is connected.",
            )
        }
        item("run2") {
            PrimaryButton(
                "Run AI analysis",
                onClick = { vm.navigate(Screen.Analysis); vm.runAnalysis() },
                icon = Icons.Filled.Psychology,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

fun zoneWord(z: Zone): String = when (z) {
    Zone.NORMAL -> "NORMAL"
    Zone.WARN -> "CAUTION"
    Zone.ALARM -> "WARNING"
}

@Composable
private fun LiveLine(cond: DataCondition) {
    val blink = rememberInfiniteTransition(label = "live").animateFloat(
        0.25f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "liveBlink",
    ).value
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Box(Modifier.size(8.dp).alpha(blink).clip(CircleShape).background(if (cond == DataCondition.NORMAL) Aero.Green else Aero.Red))
        Spacer(Modifier.width(8.dp))
        Text(
            "Live simulated feed, 1 sample per operating hour, read-outs refresh every second",
            style = MaterialTheme.typography.bodySmall,
            color = Aero.TextMuted,
        )
    }
}

@Composable
private fun InstrumentTile(s: SensorType, value: Double, modifier: Modifier) {
    val zone = s.zoneOf(value)
    Column(modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        InstrumentDial(
            value = value.toFloat(),
            min = s.chartMin.toFloat(),
            max = s.chartMax.toFloat(),
            bands = sensorBands(s),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            stroke = 5.dp,
            ticks = 4,
            animationKey = s,
        )
        Box(
            Modifier
                .padding(top = 2.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, zoneColor(zone).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(s.format(value), style = AeroType.ReadoutSmall, color = zoneColor(zone), textAlign = TextAlign.Center)
        }
        Text("${s.shortLabel} (${s.unit})", style = MaterialTheme.typography.labelSmall, color = Aero.TextMuted, textAlign = TextAlign.Center, maxLines = 1)
        Spacer(Modifier.height(6.dp))
    }
}
