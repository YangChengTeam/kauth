package com.yangfeisg.kauth.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Ink wash: ink on rice paper, with cinnabar (the red of a seal) as the only colour.
// By day the ink is black on paper; by night the paper tone becomes the ink.
private val Paper = Color(0xFFF6F2E9)
private val Ink = Color(0xFF1C1B19)
private val NightPaper = Color(0xFF141413)
private val NightInk = Color(0xFFE9E4D8)

/** Seal red, for the few places that sit on a fixed dark ground (the camera) rather than on the theme. */
val Cinnabar = Color(0xFFE0685A)

internal val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = Color(0xFFE4DDCC),
    onPrimaryContainer = Ink,
    secondary = Color(0xFF5C5850),
    onSecondary = Paper,
    secondaryContainer = Color(0xFFE6DFD0),
    onSecondaryContainer = Ink,
    tertiary = Color(0xFFB3382C),
    onTertiary = Paper,
    tertiaryContainer = Color(0xFFF3D5CF),
    onTertiaryContainer = Color(0xFF5E1810),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE6DFD0),
    onSurfaceVariant = Color(0xFF5F5A50),
    surfaceContainerLowest = Color(0xFFFBF8F1),
    surfaceContainerLow = Color(0xFFF1ECE1),
    surfaceContainer = Color(0xFFECE6D9),
    surfaceContainerHigh = Color(0xFFE6DFD0),
    surfaceContainerHighest = Color(0xFFDFD7C6),
    outline = Color(0xFF8C8676),
    outlineVariant = Color(0xFFCFC8B8),
    error = Color(0xFFB3382C),
    onError = Paper,
    errorContainer = Color(0xFFF3D5CF),
    onErrorContainer = Color(0xFF5E1810),
    inverseSurface = Color(0xFF2B2A27),
    inverseOnSurface = Paper,
    inversePrimary = NightInk,
    // Also the ink that blooms out of the add button.
    scrim = Ink,
)

internal val DarkColors = darkColorScheme(
    primary = NightInk,
    onPrimary = Ink,
    primaryContainer = Color(0xFF3A3832),
    onPrimaryContainer = NightInk,
    secondary = Color(0xFFB5AFA2),
    onSecondary = Ink,
    secondaryContainer = Color(0xFF33322E),
    onSecondaryContainer = NightInk,
    tertiary = Cinnabar,
    onTertiary = Color(0xFF2A0A06),
    tertiaryContainer = Color(0xFF6B231A),
    onTertiaryContainer = Color(0xFFF3D5CF),
    background = NightPaper,
    onBackground = NightInk,
    surface = NightPaper,
    onSurface = NightInk,
    surfaceVariant = Color(0xFF33322E),
    onSurfaceVariant = Color(0xFFB5AFA2),
    surfaceContainerLowest = Color(0xFF0F0F0E),
    surfaceContainerLow = Color(0xFF1A1A18),
    surfaceContainer = Color(0xFF1F1E1C),
    surfaceContainerHigh = Color(0xFF292825),
    surfaceContainerHighest = Color(0xFF33322E),
    outline = Color(0xFF8A8578),
    outlineVariant = Color(0xFF45433D),
    error = Cinnabar,
    onError = Color(0xFF2A0A06),
    errorContainer = Color(0xFF6B231A),
    onErrorContainer = Color(0xFFF3D5CF),
    inverseSurface = NightInk,
    inverseOnSurface = Ink,
    inversePrimary = Ink,
    scrim = Color(0xFF000000),
)
