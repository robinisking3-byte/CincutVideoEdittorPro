package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.FestivalConfiguration
import com.example.core.model.FestivalThemeConfig

/**
 * CompositionLocal to expose the active festival theme state across the composable tree.
 */
val LocalFestivalConfig = staticCompositionLocalOf { FestivalConfiguration() }

/**
 * Helper data class wrapping active festival styling parameters.
 */
data class FestivalThemeState(
    val isActive: Boolean,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val surfaceGradient: Brush,
    val cardBorder: BorderStroke,
    val buttonColor: Color,
    val themeConfig: FestivalThemeConfig
)

@Composable
fun rememberFestivalThemeState(config: FestivalConfiguration): FestivalThemeState {
    return remember(config) {
        val theme = config.themeConfig
        val isActive = config.isEventRunning()

        val primary = if (isActive && theme.enableButtonStyling) Color(theme.primaryAccentHex) else CinePrimary
        val secondary = if (isActive) Color(theme.secondaryAccentHex) else CineTertiary
        val gradStart = if (isActive) Color(theme.surfaceGradientStart) else CineSurfaceHighlight
        val gradEnd = if (isActive) Color(theme.surfaceGradientEnd) else CineSurfaceVariant
        val border = if (isActive && theme.enableCardAccents) {
            BorderStroke(1.dp, Color(theme.cardBorderColorHex))
        } else {
            BorderStroke(1.dp, CineTimelineRuler)
        }

        FestivalThemeState(
            isActive = isActive,
            primaryAccent = primary,
            secondaryAccent = secondary,
            surfaceGradient = Brush.linearGradient(listOf(gradStart, gradEnd)),
            cardBorder = border,
            buttonColor = if (isActive && theme.enableButtonStyling) Color(theme.buttonColorHex) else CinePrimary,
            themeConfig = theme
        )
    }
}

/**
 * Festival Card Surface that automatically blends subtle festive glow while preserving CineCut UI identity.
 */
@Composable
fun FestivalThemedCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val festivalConfig = LocalFestivalConfig.current
    val themeState = rememberFestivalThemeState(festivalConfig)

    Surface(
        shape = shape,
        color = CineSurface,
        border = themeState.cardBorder,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .background(if (themeState.isActive && themeState.themeConfig.enableCardAccents) themeState.surfaceGradient else Brush.linearGradient(listOf(CineSurface, CineSurfaceVariant)))
                .padding(14.dp),
            content = content
        )
    }
}

/**
 * Festival Tag Badge (e.g. "DIWALI SPECIAL", "50% OFF")
 */
@Composable
fun FestivalTagBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    textColor: Color = Color.White
) {
    val festivalConfig = LocalFestivalConfig.current
    val themeState = rememberFestivalThemeState(festivalConfig)
    val bg = backgroundColor ?: if (themeState.isActive) themeState.primaryAccent else CinePrimary

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}
