package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CineCutColorScheme = darkColorScheme(
    primary = CinePrimary,
    onPrimary = Color.White,
    primaryContainer = CineSurfaceHighlight,
    onPrimaryContainer = CineTextPrimary,
    secondary = CineSecondary,
    onSecondary = Color(0xFF1E1300),
    secondaryContainer = CineSurfaceVariant,
    onSecondaryContainer = CineSecondary,
    tertiary = CineTertiary,
    onTertiary = Color(0xFF001F28),
    tertiaryContainer = CineSurfaceVariant,
    onTertiaryContainer = CineTertiary,
    background = CineBackground,
    onBackground = CineTextPrimary,
    surface = CineSurface,
    onSurface = CineTextPrimary,
    surfaceVariant = CineSurfaceVariant,
    onSurfaceVariant = CineTextSecondary,
    outline = CineTimelineRuler,
    error = CineError,
    onError = Color.White
)

@Composable
fun CineCutTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CineCutColorScheme,
        typography = Typography,
        content = content
    )
}
