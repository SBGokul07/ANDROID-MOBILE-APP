package com.aeromaintenance.ai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeromaintenance.ai.data.formatNumber
import com.aeromaintenance.ai.ui.theme.Aero
import com.aeromaintenance.ai.ui.theme.Barlow
import kotlin.math.max
import kotlin.math.min

/**
 * Remaining-useful-life chart: the health indicator's history (cyan), its
 * nominal level, the failure limit (red), and the AI's projection from now to
 * the limit (magenta, dashed). The horizontal distance of the projection is the RUL.
 */
@Composable
fun ProjectionChart(
    history: DoubleArray,
    current: Double,
    nominal: Double,
    limit: Double,
    rulHours: Int,
    decimals: Int,
    unit: String,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
    animationKey: Any? = Unit,
) {
    val measurer = rememberTextMeasurer()
    val grow = remember(animationKey) { Animatable(0f) }
    LaunchedEffect(animationKey) { grow.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
    val style = TextStyle(fontFamily = Barlow, fontSize = 11.sp, color = Aero.TextFaint)

    Canvas(modifier.fillMaxWidth().height(height)) {
        val n = history.size
        val ahead = min(rulHours, 240)
        val total = n + max(ahead, 24) + 8
        val left = 40.dp.toPx()
        val right = 6.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = 18.dp.toPx()
        val w = size.width - left - right
        val h = size.height - top - bottom
        val lo = min(history.minOrNull() ?: nominal, nominal)
        val hi = max(limit, history.maxOrNull() ?: limit)
        val pad = (hi - lo) * 0.08
        val y0 = lo - pad
        val y1 = hi + pad
        fun x(i: Double) = left + (w * i / (total - 1)).toFloat()
        fun y(v: Double) = top + (h * (1 - (v - y0) / (y1 - y0))).toFloat()
        val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))

        // Limit and nominal
        drawLine(Aero.Red.copy(alpha = 0.85f), Offset(left, y(limit)), Offset(left + w, y(limit)), strokeWidth = 1.4.dp.toPx())
        label(measurer, "limit ${formatNumber(limit, decimals)} $unit", Offset(left + 4.dp.toPx(), y(limit) - 15.dp.toPx()), style.copy(color = Aero.Red))
        drawLine(Aero.Cyan.copy(alpha = 0.3f), Offset(left, y(nominal)), Offset(left + w, y(nominal)), strokeWidth = 1.dp.toPx(), pathEffect = dash)
        label(measurer, formatNumber(y1, decimals), Offset(0f, top - 4.dp.toPx()), style)
        label(measurer, formatNumber(y0, decimals), Offset(0f, top + h - 12.dp.toPx()), style)
        drawLine(Aero.Hairline, Offset(left, top + h), Offset(left + w, top + h), strokeWidth = 1.dp.toPx())

        // History
        val path = Path()
        for (i in 0 until n) {
            val p = Offset(x(i.toDouble()), y(history[i]))
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, Aero.Cyan, style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round))

        // Now marker
        val nowX = x((n - 1).toDouble())
        drawLine(Aero.HairlineStrong, Offset(nowX, top), Offset(nowX, top + h), strokeWidth = 1.dp.toPx())
        label(measurer, "−${n - 1} h", Offset(left, top + h + 3.dp.toPx()), style)
        label(measurer, "now", Offset(nowX, top + h + 3.dp.toPx()), style, alignRight = true)

        // Projection
        val endIndex = (n - 1) + ahead * grow.value
        val endValue = current + (limit - current) * (ahead / max(rulHours, 1).toDouble()) * grow.value
        val start = Offset(nowX, y(current))
        val end = Offset(x(endIndex.toDouble()), y(endValue))
        drawLine(Aero.Magenta, start, end, strokeWidth = 2.6.dp.toPx(), pathEffect = dash, cap = StrokeCap.Round)
        drawCircle(Aero.Magenta, 4.5.dp.toPx(), start)
        if (grow.value >= 1f) {
            drawCircle(Aero.Magenta, 5.dp.toPx(), end)
            val text = if (rulHours > ahead) "+$ahead h (limit beyond chart)" else "+$rulHours h"
            label(measurer, text, Offset(end.x, top + h + 3.dp.toPx()), style.copy(color = Aero.Magenta), alignRight = true)
        }
    }
}
