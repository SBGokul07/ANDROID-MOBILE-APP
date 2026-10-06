package com.aeromaintenance.ai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeromaintenance.ai.data.SensorType
import com.aeromaintenance.ai.data.Zone
import com.aeromaintenance.ai.data.formatNumber
import com.aeromaintenance.ai.ui.theme.Aero
import com.aeromaintenance.ai.ui.theme.Barlow
import com.aeromaintenance.ai.ui.theme.zoneColor
import kotlin.math.max
import kotlin.math.min

/** Straight-line RUL projection drawn in magenta after the last sample. */
data class Projection(val fromValue: Double, val slopePerSample: Double, val limit: Double, val samplesToLimit: Int)

/**
 * Telemetry trend chart. Draws the caution/warning bands of the channel, the raw
 * samples (faint), the cleaned and smoothed trace coloured by zone, removed spikes
 * (rings), the detected onset of degradation, and optionally an RUL projection.
 */
@Composable
fun TrendChart(
    sensor: SensorType,
    raw: DoubleArray,
    smooth: DoubleArray,
    modifier: Modifier = Modifier,
    spikes: List<Int> = emptyList(),
    onsetIndex: Int? = null,
    projection: Projection? = null,
    height: Dp = 150.dp,
    animationKey: Any? = Unit,
    live: Boolean = true,
    yMin: Double = sensor.chartMin,
    yMax: Double = sensor.chartMax,
    zoneOf: (Double) -> Zone = { sensor.zoneOf(it) },
    unitLabel: String = sensor.unit,
    decimals: Int = sensor.decimals,
) {
    val measurer = rememberTextMeasurer()
    val reveal = remember(animationKey) { Animatable(0f) }
    LaunchedEffect(animationKey) { reveal.animateTo(1f, tween(1400, easing = LinearEasing)) }
    val pulse = if (live) {
        rememberInfiniteTransition(label = "live").animateFloat(
            0f, 1f, infiniteRepeatable(tween(1300), RepeatMode.Restart), label = "liveDot",
        ).value
    } else 0f

    val labelStyle = TextStyle(fontFamily = Barlow, fontSize = 11.sp, color = Aero.TextFaint)

    Canvas(modifier.fillMaxWidth().height(height)) {
        val n = smooth.size
        val total = n + (projection?.let { max(it.samplesToLimit + 6, 12) } ?: 0)
        val left = 40.dp.toPx()
        val right = 6.dp.toPx()
        val top = 6.dp.toPx()
        val bottom = 18.dp.toPx()
        val w = size.width - left - right
        val h = size.height - top - bottom
        val lo = yMin
        val hi = yMax
        fun x(i: Double) = left + (w * i / (total - 1)).toFloat()
        fun y(v: Double) = top + (h * (1 - ((v - lo) / (hi - lo)).coerceIn(-0.02, 1.02))).toFloat()

        // Zone bands
        fun band(a: Double, b: Double, color: Color) {
            val y0 = y(max(a, b)); val y1 = y(min(a, b))
            drawRect(color, Offset(left, y0), Size(w, y1 - y0))
        }
        sensor.warnHigh?.let { band(it, sensor.alarmHigh ?: hi, Aero.Amber.copy(alpha = 0.07f)) }
        sensor.alarmHigh?.let { band(it, hi, Aero.Red.copy(alpha = 0.10f)) }
        sensor.warnLow?.let { band(it, sensor.alarmLow ?: lo, Aero.Amber.copy(alpha = 0.07f)) }
        sensor.alarmLow?.let { band(lo, it, Aero.Red.copy(alpha = 0.10f)) }

        val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
        fun hLine(v: Double, color: Color) =
            drawLine(color, Offset(left, y(v)), Offset(left + w, y(v)), strokeWidth = 1.dp.toPx(), pathEffect = dash)
        sensor.warnHigh?.let { hLine(it, Aero.Amber.copy(alpha = 0.6f)) }
        sensor.alarmHigh?.let { hLine(it, Aero.Red.copy(alpha = 0.7f)) }
        sensor.warnLow?.let { hLine(it, Aero.Amber.copy(alpha = 0.6f)) }
        sensor.alarmLow?.let { hLine(it, Aero.Red.copy(alpha = 0.7f)) }
        hLine(sensor.baselineMean, Aero.Cyan.copy(alpha = 0.25f))

        // Frame + axis labels
        drawLine(Aero.Hairline, Offset(left, top + h), Offset(left + w, top + h), strokeWidth = 1.dp.toPx())
        label(measurer, formatNumber(hi, decimals), Offset(0f, top - 2.dp.toPx()), labelStyle)
        label(measurer, formatNumber(lo, decimals), Offset(0f, top + h - 12.dp.toPx()), labelStyle)
        label(measurer, "−${n - 1} h", Offset(left, top + h + 3.dp.toPx()), labelStyle)
        label(measurer, "now", Offset(x((n - 1).toDouble()), top + h + 3.dp.toPx()), labelStyle, alignRight = true)

        // Onset marker
        if (onsetIndex != null && reveal.value > onsetIndex / n.toFloat()) {
            val ox = x(onsetIndex.toDouble())
            drawLine(Aero.Cyan.copy(alpha = 0.8f), Offset(ox, top), Offset(ox, top + h), strokeWidth = 1.2.dp.toPx(), pathEffect = dash)
            label(measurer, "onset −${n - 1 - onsetIndex} h", Offset(ox + 4.dp.toPx(), top + 2.dp.toPx()), labelStyle.copy(color = Aero.Cyan))
        }

        val visible = max(2, (n * reveal.value).toInt())

        // Raw samples
        val rawPath = Path()
        for (i in 0 until min(visible, raw.size)) {
            val p = Offset(x(i.toDouble()), y(raw[i]))
            if (i == 0) rawPath.moveTo(p.x, p.y) else rawPath.lineTo(p.x, p.y)
        }
        drawPath(rawPath, Aero.TextFaint.copy(alpha = 0.55f), style = Stroke(1.dp.toPx()))

        // Smoothed trace, coloured by zone
        val sw = 2.6.dp.toPx()
        for (i in 1 until visible) {
            drawLine(
                zoneColor(zoneOf(smooth[i])),
                Offset(x(i - 1.0), y(smooth[i - 1])),
                Offset(x(i.toDouble()), y(smooth[i])),
                strokeWidth = sw,
                cap = StrokeCap.Round,
            )
        }

        // Removed spikes
        spikes.filter { it < visible && it < raw.size }.forEach { i ->
            drawCircle(Aero.Amber, radius = 4.5.dp.toPx(), center = Offset(x(i.toDouble()), y(raw[i])), style = Stroke(1.5.dp.toPx()))
        }

        // RUL projection
        if (projection != null && reveal.value >= 1f) {
            val sx = x((n - 1).toDouble())
            val sy = y(projection.fromValue)
            val ex = x((n - 1 + projection.samplesToLimit).toDouble())
            val ey = y(projection.limit)
            drawLine(Aero.Red.copy(alpha = 0.8f), Offset(left, ey), Offset(left + w, ey), strokeWidth = 1.2.dp.toPx())
            drawLine(Aero.Magenta, Offset(sx, sy), Offset(ex, ey), strokeWidth = 2.4.dp.toPx(), pathEffect = dash, cap = StrokeCap.Round)
            drawCircle(Aero.Magenta, radius = 4.dp.toPx(), center = Offset(ex, ey))
            label(measurer, "+${projection.samplesToLimit} h", Offset(ex, ey + 6.dp.toPx()), labelStyle.copy(color = Aero.Magenta), alignRight = true)
            drawLine(Aero.Hairline, Offset(sx, top), Offset(sx, top + h), strokeWidth = 1.dp.toPx())
        }

        // Live dot
        if (reveal.value >= 1f) {
            val last = Offset(x((n - 1).toDouble()), y(smooth[n - 1]))
            val c = zoneColor(zoneOf(smooth[n - 1]))
            if (live) drawCircle(c.copy(alpha = (1f - pulse) * 0.5f), radius = 4.dp.toPx() + 10.dp.toPx() * pulse, center = last)
            drawCircle(c, radius = 4.dp.toPx(), center = last)
        }
    }
}

/** Measures unconstrained, then clamps inside the canvas so labels never wrap or overflow. */
internal fun DrawScope.label(measurer: TextMeasurer, text: String, at: Offset, style: TextStyle, alignRight: Boolean = false) {
    val layout = measurer.measure(text, style)
    val w = layout.size.width.toFloat()
    val x = (if (alignRight) at.x - w else at.x).coerceIn(0f, max(0f, size.width - w))
    val y = at.y.coerceIn(0f, max(0f, size.height - layout.size.height))
    drawText(layout, topLeft = Offset(x, y))
}
