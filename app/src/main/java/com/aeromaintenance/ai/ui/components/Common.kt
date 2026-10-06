package com.aeromaintenance.ai.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aeromaintenance.ai.AeroViewModel
import com.aeromaintenance.ai.data.DataCondition
import com.aeromaintenance.ai.ui.theme.Aero
import com.aeromaintenance.ai.ui.theme.AeroType
import com.aeromaintenance.ai.ui.theme.fleetStatusColor

val ScreenPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)

/** A display unit: dark panel with a hairline frame. Highlighted panels pulse in cyan (used by the guided demo). */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    accent: Color? = null,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val pulse = if (highlight) {
        val t = rememberInfiniteTransition(label = "pulse")
        t.animateFloat(0.45f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulseAlpha").value
    } else 0f
    val borderColor = when {
        highlight -> Aero.Cyan.copy(alpha = pulse)
        accent != null -> accent.copy(alpha = 0.55f)
        else -> Aero.Hairline
    }
    val borderWidth = if (highlight) 2.dp else 1.dp
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = Aero.Panel,
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Aero.Text)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted)
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = Aero.Cyan,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

/** Status tag in the brief's vocabulary (HIGH, WARNING, NORMAL…). */
@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier, filled: Boolean = false) {
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (filled) color else color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = if (filled) 1f else 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = AeroType.Tag, color = if (filled) Aero.Night else color, maxLines = 1)
    }
}

@Composable
fun StatusDot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun HealthBar(value: Int, color: Color, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val animated by animateFloatAsState(value / 100f, tween(900), label = "health")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(Aero.Hairline),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(color),
        )
    }
}

@Composable
fun KeyValue(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Aero.Text) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Aero.TextMuted)
        Spacer(Modifier.height(2.dp))
        Text(value, style = AeroType.ReadoutSmall, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    color: Color = Aero.Cyan,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Aero.Night,
            disabledContainerColor = Aero.Hairline,
            disabledContentColor = Aero.TextFaint,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (enabled) Aero.Cyan.copy(alpha = 0.6f) else Aero.Hairline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Aero.Cyan, disabledContentColor = Aero.TextFaint),
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Normal / abnormal data-set switch (brief §13). */
@Composable
fun ConditionToggle(condition: DataCondition, onChange: (DataCondition) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Aero.Night)
            .border(1.dp, Aero.Hairline, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DataCondition.entries.forEach { c ->
            val selected = c == condition
            val tint = if (c == DataCondition.NORMAL) Aero.Green else Aero.Red
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) tint.copy(alpha = 0.18f) else Color.Transparent)
                    .border(1.dp, if (selected) tint.copy(alpha = 0.7f) else Color.Transparent, RoundedCornerShape(9.dp))
                    .clickable { onChange(c) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected) {
                    Icon(Icons.Filled.Check, null, tint = tint, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    if (c == DataCondition.NORMAL) "Normal data" else "Abnormal data",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) tint else Aero.TextMuted,
                )
            }
        }
    }
}

/** Selected-aircraft picker shared by Monitoring, AI prediction, Maintenance and Reports. */
@Composable
fun AircraftSelector(vm: AeroViewModel, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val a = vm.aircraft(vm.selectedId)
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Aero.Panel)
                .border(1.dp, Aero.Hairline, RoundedCornerShape(12.dp))
                .clickable { open = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(fleetStatusColor(a.status), 10.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(a.id, style = MaterialTheme.typography.titleMedium, color = Aero.Text)
                Text("${a.model}, ${a.base}", style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted)
            }
            Text("Change", style = MaterialTheme.typography.labelMedium, color = Aero.Cyan)
            Icon(Icons.Filled.ArrowDropDown, null, tint = Aero.Cyan)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            vm.fleet.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(fleetStatusColor(item.status))
                            Spacer(Modifier.width(10.dp))
                            Text(item.id, style = MaterialTheme.typography.titleSmall, color = Aero.Text)
                            Spacer(Modifier.width(8.dp))
                            Text(item.model, style = MaterialTheme.typography.bodySmall, color = Aero.TextMuted)
                        }
                    },
                    onClick = {
                        vm.selectAircraft(item.id)
                        open = false
                    },
                )
            }
        }
    }
}

/** Honest labelling, shown wherever the AI produces an output. */
@Composable
fun PrototypeNote(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Info, null, tint = Aero.TextFaint, modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Aero.TextFaint)
    }
}

@Composable
fun Bullet(text: String, color: Color = Aero.Cyan, textColor: Color = Aero.Text) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 8.dp).size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = textColor)
    }
}
