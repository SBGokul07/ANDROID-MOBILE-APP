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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeromaintenance.ai.engine.ComponentAssessment
import com.aeromaintenance.ai.engine.InferenceEngine
import com.aeromaintenance.ai.ui.theme.Aero
import com.aeromaintenance.ai.ui.theme.Barlow
import com.aeromaintenance.ai.ui.theme.riskColor
import kotlin.math.min

/**
 * Risk chart: remaining useful life per component, coloured by risk. The two
 * dashed lines are the risk rules (under 72 h = HIGH, under 250 h = MEDIUM),
 * so the chart shows *why* each component got its rating.
 */
@Composable
fun RulRiskChart(components: List<ComponentAssessment>, modifier: Modifier = Modifier, maxHours: Int = 600, animationKey: Any? = Unit) {
    val measurer = rememberTextMeasurer()
    val grow = remember(animationKey) { Animatable(0f) }
    LaunchedEffect(animationKey) { grow.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
    val nameStyle = TextStyle(fontFamily = Barlow, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Aero.Text)
    val valueStyle = TextStyle(fontFamily = Barlow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
    val axisStyle = TextStyle(fontFamily = Barlow, fontSize = 11.sp, color = Aero.TextFaint)
    val rowH = 44.dp
    Canvas(modifier.fillMaxWidth().height(rowH * components.size + 26.dp)) {
        val left = 112.dp.toPx()
        val right = 8.dp.toPx()
        val w = size.width - left - right
        val rh = rowH.toPx()
        val chartH = rh * components.size
        fun xOf(hours: Int) = left + w * (min(hours, maxHours) / maxHours.toFloat())
        val dash = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
        listOf(72 to Aero.Red, 250 to Aero.Orange).forEach { (hrs, c) ->
            val x = xOf(hrs)
            drawLine(c.copy(alpha = 0.7f), Offset(x, 0f), Offset(x, chartH), strokeWidth = 1.dp.toPx(), pathEffect = dash)
            label(measurer, "$hrs h", Offset(x, chartH + 6.dp.toPx()), axisStyle.copy(color = c), alignRight = false)
        }
        label(measurer, "${maxHours}+ h", Offset(left + w, chartH + 6.dp.toPx()), axisStyle, alignRight = true)
        components.forEachIndexed { i, c ->
            val cy = rh * i + rh / 2f
            val color = riskColor(c.risk)
            label(measurer, c.component.label, Offset(0f, cy - 9.dp.toPx()), nameStyle)
            val barH = 14.dp.toPx()
            drawRoundRect(Aero.Hairline, Offset(left, cy - barH / 2), Size(w, barH), CornerRadius(4.dp.toPx()))
            val bw = (xOf(c.rulHours) - left) * grow.value
            drawRoundRect(color, Offset(left, cy - barH / 2), Size(bw.coerceAtLeast(3.dp.toPx()), barH), CornerRadius(4.dp.toPx()))
            val text = "${InferenceEngine.rulText(c.rulHours)}  ${c.risk.label}"
            val layout = measurer.measure(text, valueStyle.copy(color = color))
            val tx = if (left + bw + 6.dp.toPx() + layout.size.width < size.width) left + bw + 6.dp.toPx() else left + bw - layout.size.width - 6.dp.toPx()
            val textColor = if (tx < left + bw) Aero.Night else color
            label(measurer, text, Offset(tx, cy - 8.dp.toPx()), valueStyle.copy(color = textColor))
        }
    }
}
