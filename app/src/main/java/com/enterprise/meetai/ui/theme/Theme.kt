package com.enterprise.meetai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class MeetAiCustomColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardBorder: Color,
    val primary: Color,
    val primaryVariant: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDim: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color
)

val DarkCustomColors = MeetAiCustomColors(
    isDark = true,
    background = SlateDarkBg,
    surface = SlateDarkSurface,
    card = SlateDarkCard,
    cardBorder = SlateDarkBorder,
    primary = ExecutiveTeal,
    primaryVariant = ExecutiveTealDark,
    accent = SageAccent,
    textPrimary = TextDarkPrimary,
    textSecondary = TextDarkSecondary,
    textDim = TextDarkDim,
    success = EmeraldSuccess,
    warning = AmberPending,
    error = RoseError,
    info = BlueInfo
)

val LightCustomColors = MeetAiCustomColors(
    isDark = false,
    background = SlateLightBg,
    surface = SlateLightSurface,
    card = SlateLightCard,
    cardBorder = SlateLightBorder,
    primary = ExecutiveTealDark,
    primaryVariant = ExecutiveTeal,
    accent = SageAccent,
    textPrimary = TextLightPrimary,
    textSecondary = TextLightSecondary,
    textDim = TextLightDim,
    success = EmeraldSuccess,
    warning = AmberPending,
    error = RoseError,
    info = BlueInfo
)

val LocalMeetAiColors = staticCompositionLocalOf { DarkCustomColors }

private val DarkColorScheme = darkColorScheme(
    primary = ExecutiveTeal,
    secondary = ExecutiveTealDark,
    tertiary = SageAccent,
    background = SlateDarkBg,
    surface = SlateDarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextDarkPrimary,
    onSurface = TextDarkPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = ExecutiveTealDark,
    secondary = ExecutiveTeal,
    tertiary = SageAccent,
    background = SlateLightBg,
    surface = SlateLightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextLightPrimary,
    onSurface = TextLightPrimary
)

@Composable
fun MeetAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val customColors = if (darkTheme) DarkCustomColors else LightCustomColors

    CompositionLocalProvider(LocalMeetAiColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
