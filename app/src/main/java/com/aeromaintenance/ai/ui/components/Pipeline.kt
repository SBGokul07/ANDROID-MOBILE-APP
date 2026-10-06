package com.aeromaintenance.ai.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aeromaintenance.ai.ui.theme.Aero
import com.aeromaintenance.ai.ui.theme.AeroType

/** The end-to-end workflow from the brief (§2), in order. */
val WorkflowStages = listOf(
    "Sensor data",
    "Preprocessing",
    "AI/ML analysis",
    "Anomaly detection",
    "Health assessment",
    "Failure risk",
    "RUL estimate",
    "Recommendation",
    "Mobile alert",
)

/** Horizontal, scrollable workflow strip for the dashboard. */
@Composable
fun WorkflowStrip(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WorkflowStages.forEachIndexed { i, s ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(78.dp)) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (i == WorkflowStages.lastIndex) Aero.Cyan.copy(alpha = 0.2f) else Aero.PanelRaised)
                        .border(1.dp, Aero.Cyan.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${i + 1}", style = AeroType.Tag, color = Aero.Cyan)
                }
                Spacer(Modifier.height(6.dp))
                Text(s, style = MaterialTheme.typography.labelSmall, color = Aero.TextMuted, textAlign = TextAlign.Center, minLines = 2)
            }
            if (i < WorkflowStages.lastIndex) {
                Box(Modifier.padding(bottom = 26.dp).width(14.dp).height(1.dp).background(Aero.HairlineStrong))
            }
        }
    }
}

/**
 * Vertical pipeline used on the AI screen. [completed] stages show a tick and
 * their detail line; the active one pulses.
 */
@Composable
fun PipelineStepper(
    titles: List<String>,
    details: List<String>?,
    completed: Int,
    modifier: Modifier = Modifier,
) {
    val pulse = rememberInfiniteTransition(label = "stage").animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "stageAlpha",
    ).value
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        titles.forEachIndexed { i, title ->
            val done = i < completed
            val active = i == completed
            Row(Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (done) Aero.Green.copy(alpha = 0.18f) else Aero.Night)
                            .border(
                                1.5.dp,
                                when {
                                    done -> Aero.Green
                                    active -> Aero.Cyan.copy(alpha = pulse)
                                    else -> Aero.Hairline
                                },
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Icons.Filled.Check, null, tint = Aero.Green, modifier = Modifier.size(14.dp))
                        else Text("${i + 1}", style = AeroType.Tag, color = if (active) Aero.Cyan else Aero.TextFaint)
                    }
                    if (i < titles.lastIndex) {
                        Box(Modifier.width(1.5.dp).height(if (done && details != null) 34.dp else 14.dp).background(if (done) Aero.Green.copy(alpha = 0.5f) else Aero.Hairline))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).padding(top = 1.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        color = when {
                            done -> Aero.Text
                            active -> Aero.Cyan
                            else -> Aero.TextFaint
                        },
                        modifier = if (active) Modifier.alpha(0.6f + 0.4f * pulse) else Modifier,
                    )
                    if (done && details != null && i < details.size) {
                        Text(details[i], style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
fun StageChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Aero.PanelRaised)
            .border(1.dp, Aero.Hairline, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = Aero.Text)
    }
}
