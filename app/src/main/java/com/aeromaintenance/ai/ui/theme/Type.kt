package com.aeromaintenance.ai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aeromaintenance.ai.R

/** Barlow: a DIN-like grotesque with roots in highway and transport signage. */
val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_bold, FontWeight.Bold),
)

/** Narrower cut used for titles and instrument read-outs. */
val BarlowSemiCondensed = FontFamily(
    Font(R.font.barlow_semicondensed_medium, FontWeight.Medium),
    Font(R.font.barlow_semicondensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_semicondensed_bold, FontWeight.Bold),
)

private const val TABULAR = "tnum"

object AeroType {
    /** Large numeric read-out (fleet health, anomaly score, RUL). */
    val Readout = TextStyle(
        fontFamily = BarlowSemiCondensed, fontWeight = FontWeight.SemiBold,
        fontSize = 48.sp, lineHeight = 50.sp, letterSpacing = (-0.01).em, fontFeatureSettings = TABULAR,
    )
    val ReadoutMedium = TextStyle(
        fontFamily = BarlowSemiCondensed, fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp, lineHeight = 34.sp, fontFeatureSettings = TABULAR,
    )
    val ReadoutSmall = TextStyle(
        fontFamily = BarlowSemiCondensed, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 24.sp, fontFeatureSettings = TABULAR,
    )
    /** Severity / risk tags: the brief's own vocabulary (HIGH, CRITICAL…). */
    val Tag = TextStyle(
        fontFamily = BarlowSemiCondensed, fontWeight = FontWeight.Bold,
        fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.06.em,
    )
    val Numeric = TextStyle(
        fontFamily = Barlow, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 18.sp, fontFeatureSettings = TABULAR,
    )
}

val AeroTypography = Typography(
    displaySmall = TextStyle(fontFamily = BarlowSemiCondensed, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = BarlowSemiCondensed, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = BarlowSemiCondensed, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = BarlowSemiCondensed, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = BarlowSemiCondensed, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 15.sp),
)
