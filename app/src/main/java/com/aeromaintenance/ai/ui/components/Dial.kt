package com.aeromaintenance.ai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aeromaintenance.ai.ui.theme.Aero
import kotlin.math.cos
import kotlin.math.sin

/** A coloured range on the dial scale (green arc, amber arc, red arc…). */
data class DialBand(val from: Float, val to: Float, val color: Color)

/**
 * Engine-instrument style dial, modelled on glass-cockpit EGT / N1 gauges:
 * a 240° scale with coloured range arcs on the outside, the filled value arc,
 * and a needle. The read-out goes in [content], centred in the lower half.
 */
@Composable
fun InstrumentDial(
    value: Float,
    min: Float,
    max: Float,
    bands: List<DialBand>,
    modifier: Modifier = Modifier,
    stroke: Dp = 10.dp,
    sweep: Float = 240f,
    ticks: Int = 10,
    animationKey: Any? = Unit,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val anim = remember(animationKey) { Animatable(min) }
    LaunchedEffect(value, animationKey) {
        anim.animateTo(value.coerceIn(min, max), tween(1100, easing = FastOutSlowInEasing))
    }
    val current = anim.value
    val valueColor = bands.firstOrNull { current >= it.from && current <= it.to }?.color ?: Aero.Cyan

    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val bandW = sw * 0.42f
            val gap = sw * 0.55f
            val outer = size.minDimension / 2f
            val bandR = outer - bandW / 2f
            val r = bandR - bandW / 2f - gap - sw / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            val start = 90f + (360f - sweep) / 2f
            fun angleOf(v: Float) = start + sweep * ((v - min) / (max - min)).coerceIn(0f, 1f)
            fun arcRect(radius: Float) = Pair(Offset(c.x - radius, c.y - radius), Size(radius * 2, radius * 2))

            // Range arcs (outer ring)
            val (bTop, bSize) = arcRect(bandR)
            bands.forEach { b ->
                val a0 = angleOf(b.from)
                val a1 = angleOf(b.to)
                drawArc(b.color.copy(alpha = 0.85f), a0, a1 - a0, false, bTop, bSize, style = Stroke(bandW, cap = StrokeCap.Butt))
            }

            // Scale track and value arc
            val (tTop, tSize) = arcRect(r)
            drawArc(Aero.Hairline, start, sweep, false, tTop, tSize, style = Stroke(sw, cap = StrokeCap.Round))
            val va = angleOf(current)
            if (va > start + 0.5f) {
                drawArc(valueColor, start, va - start, false, tTop, tSize, style = Stroke(sw, cap = StrokeCap.Round))
            }

            // Ticks
            for (i in 0..ticks) {
                val a = Math.toRadians((start + sweep * i / ticks).toDouble())
                val r0 = r - sw / 2f - sw * 0.5f
                val r1 = r0 - (if (i % 5 == 0) sw * 0.9f else sw * 0.45f)
                drawLine(
                    Aero.TextFaint,
                    Offset(c.x + (r0 * cos(a)).toFloat(), c.y + (r0 * sin(a)).toFloat()),
                    Offset(c.x + (r1 * cos(a)).toFloat(), c.y + (r1 * sin(a)).toFloat()),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }

            // Needle
            val na = Math.toRadians(va.toDouble())
            val nLen = r - sw * 1.6f
            val tip = Offset(c.x + (nLen * cos(na)).toFloat(), c.y + (nLen * sin(na)).toFloat())
            val tail = Offset(c.x - (sw * 1.2f * cos(na)).toFloat(), c.y - (sw * 1.2f * sin(na)).toFloat())
            drawLine(Aero.Text, tail, tip, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(Aero.Text, radius = sw * 0.55f, center = c)
            drawCircle(Aero.Night, radius = sw * 0.25f, center = c)
        }
        content()
    }
}

/** Default range arcs for a 0–100 health scale. */
val HealthBands = listOf(
    DialBand(0f, 60f, Aero.Red),
    DialBand(60f, 80f, Aero.Orange),
    DialBand(80f, 100f, Aero.Green),
)
