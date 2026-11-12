package com.yangfeisg.kauth.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Default = Typography()

// Titles in the system serif (a Song typeface for Chinese), like lettering beside a painting;
// body text stays sans for small sizes.
internal val KauthTypography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = FontFamily.Serif),
    displayMedium = Default.displayMedium.copy(fontFamily = FontFamily.Serif),
    displaySmall = Default.displaySmall.copy(fontFamily = FontFamily.Serif),
    headlineLarge = Default.headlineLarge.copy(fontFamily = FontFamily.Serif),
    headlineMedium = Default.headlineMedium.copy(fontFamily = FontFamily.Serif),
    headlineSmall = Default.headlineSmall.copy(fontFamily = FontFamily.Serif),
    titleLarge = Default.titleLarge.copy(fontFamily = FontFamily.Serif),
    titleMedium = Default.titleMedium.copy(fontFamily = FontFamily.Serif),
    titleSmall = Default.titleSmall.copy(fontFamily = FontFamily.Serif),
)

/** The one-time code itself: large serif numerals, tabular so they do not jitter when they change. */
val CodeTextStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 36.sp,
    lineHeight = 44.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 1.sp,
    fontFeatureSettings = "tnum",
)
