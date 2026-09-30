package com.cutmedia.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class CutThemeMode {
    CINEMATIC_GOLD,
    DIWALI_FESTIVE,
    HOLI_VIBRANT
}

// Cinematic Minimal Dark (Default CutMedia)
private val DarkCharcoal = Color(0xFF0C0D12)
private val SurfaceDark = Color(0xFF151720)
private val SurfaceVariantDark = Color(0xFF1E212D)
private val GoldAccent = Color(0xFFD4AF37)
private val GoldAccentLight = Color(0xFFF3E5AB)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val BorderStroke = Color(0xFF262A3B)

// Diwali Festival Palette (Deep Navy + Warm Marigold/Amber)
private val DiwaliDark = Color(0xFF0A0F1D)
private val DiwaliSurface = Color(0xFF141C33)
private val DiwaliAmber = Color(0xFFF59E0B)
private val DiwaliMarigold = Color(0xFFFBBF24)

// Holi Festival Palette (Deep Slate + Vibrant Ruby/Indigo)
private val HoliDark = Color(0xFF0D0A14)
private val HoliSurface = Color(0xFF1A1428)
private val HoliPink = Color(0xFFEC4899)
private val HoliIndigo = Color(0xFF818CF8)

@Composable
fun CutMediaTheme(
    themeMode: CutThemeMode = CutThemeMode.CINEMATIC_GOLD,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        CutThemeMode.CINEMATIC_GOLD -> darkColorScheme(
            primary = GoldAccent,
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF2A2410),
            onPrimaryContainer = GoldAccentLight,
            secondary = Color(0xFF38BDF8),
            background = DarkCharcoal,
            surface = SurfaceDark,
            surfaceVariant = SurfaceVariantDark,
            onBackground = TextWhite,
            onSurface = TextWhite,
            onSurfaceVariant = TextMuted,
            outline = BorderStroke
        )
        CutThemeMode.DIWALI_FESTIVE -> darkColorScheme(
            primary = DiwaliAmber,
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF332005),
            onPrimaryContainer = DiwaliMarigold,
            secondary = Color(0xFFF43F5E),
            background = DiwaliDark,
            surface = DiwaliSurface,
            surfaceVariant = Color(0xFF1E2945),
            onBackground = TextWhite,
            onSurface = TextWhite,
            onSurfaceVariant = Color(0xFFCBD5E1),
            outline = Color(0xFF2B3A60)
        )
        CutThemeMode.HOLI_VIBRANT -> darkColorScheme(
            primary = HoliPink,
            onPrimary = Color.White,
            primaryContainer = Color(0xFF33081E),
            onPrimaryContainer = Color(0xFFFBCFE8),
            secondary = HoliIndigo,
            background = HoliDark,
            surface = HoliSurface,
            surfaceVariant = Color(0xFF281C3D),
            onBackground = TextWhite,
            onSurface = TextWhite,
            onSurfaceVariant = Color(0xFFD8B4FE),
            outline = Color(0xFF3E2C5E)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
