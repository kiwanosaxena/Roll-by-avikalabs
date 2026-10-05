package com.avikalabs.roll.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.avikalabs.roll.data.local.ThemeMode

data class GlassColorScheme(
    val isDark: Boolean,
    val backgroundBrush: Brush,
    val glassFill: Color,
    val glassBorder: Color,
    val glassFillElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentPrimary: Color,
    val accentSecondary: Color
)

val LocalGlassColors = staticCompositionLocalOf {
    GlassColorScheme(
        isDark = false,
        backgroundBrush = Brush.verticalGradient(listOf(DayBg1, DayBg2, DayBg3, DayBg4)),
        glassFill = DayGlassFill,
        glassBorder = DayGlassBorder,
        glassFillElevated = DayGlassFillElevated,
        textPrimary = DayTextPrimary,
        textSecondary = DayTextSecondary,
        accentPrimary = AccentSunset,
        accentSecondary = AccentSea
    )
}

private val LightColorScheme = lightColorScheme(
    primary = AccentSunset,
    secondary = AccentSea,
    background = DayBg1,
    surface = DayGlassFill,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DayTextPrimary,
    onSurface = DayTextPrimary,
    onSurfaceVariant = DayTextSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentSunset,
    secondary = AccentSea,
    background = NightBg3,
    surface = NightGlassFill,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = NightTextPrimary,
    onSurface = NightTextPrimary,
    onSurfaceVariant = NightTextSecondary
)

@Composable
fun RollTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DAY -> false
        ThemeMode.NIGHT -> true
    }

    val glassColors = if (isDark) {
        GlassColorScheme(
            isDark = true,
            backgroundBrush = Brush.verticalGradient(listOf(NightBg1, NightBg2, NightBg3)),
            glassFill = NightGlassFill,
            glassBorder = NightGlassBorder,
            glassFillElevated = NightGlassFillElevated,
            textPrimary = NightTextPrimary,
            textSecondary = NightTextSecondary,
            accentPrimary = AccentSunset,
            accentSecondary = AccentSea
        )
    } else {
        GlassColorScheme(
            isDark = false,
            backgroundBrush = Brush.verticalGradient(listOf(DayBg1, DayBg2, DayBg3, DayBg4)),
            glassFill = DayGlassFill,
            glassBorder = DayGlassBorder,
            glassFillElevated = DayGlassFillElevated,
            textPrimary = DayTextPrimary,
            textSecondary = DayTextSecondary,
            accentPrimary = AccentSunset,
            accentSecondary = AccentSea
        )
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalGlassColors provides glassColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
