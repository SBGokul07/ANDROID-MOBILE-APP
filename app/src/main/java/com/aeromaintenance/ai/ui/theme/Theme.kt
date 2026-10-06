package com.aeromaintenance.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AeroColorScheme = darkColorScheme(
    primary = Aero.Cyan,
    onPrimary = Aero.Night,
    primaryContainer = Aero.CyanDeep,
    onPrimaryContainer = Aero.Text,
    secondary = Aero.Green,
    onSecondary = Aero.Night,
    tertiary = Aero.Magenta,
    onTertiary = Aero.Night,
    background = Aero.Night,
    onBackground = Aero.Text,
    surface = Aero.Night,
    onSurface = Aero.Text,
    surfaceVariant = Aero.Panel,
    onSurfaceVariant = Aero.TextMuted,
    surfaceContainerLowest = Aero.Night,
    surfaceContainerLow = Aero.Panel,
    surfaceContainer = Aero.Panel,
    surfaceContainerHigh = Aero.PanelRaised,
    surfaceContainerHighest = Aero.PanelRaised,
    outline = Aero.HairlineStrong,
    outlineVariant = Aero.Hairline,
    error = Aero.Red,
    onError = Aero.Night,
    scrim = Aero.Night,
)

@Composable
fun AeroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AeroColorScheme,
        typography = AeroTypography,
        content = content,
    )
}
