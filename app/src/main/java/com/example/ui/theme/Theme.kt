package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ForestGreenLight,
    onPrimary = Color.Black,
    primaryContainer = ForestGreenContainer,
    onPrimaryContainer = OnForestGreenContainer,
    secondary = AutoModeBlue,
    onSecondary = Color.White,
    secondaryContainer = AutoModeBlueContainer,
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = StatusWarningAmber,
    onTertiary = Color.Black,
    background = CharcoalBackground,
    onBackground = TextWhitePrimary,
    surface = CharcoalSurface,
    onSurface = TextWhitePrimary,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = TextMutedSecondary,
    outline = CharcoalBorder,
    error = StatusErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreenDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E9),
    onPrimaryContainer = ForestGreenDark,
    secondary = AutoModeBlueDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = AutoModeBlueDark,
    tertiary = StatusWarningAmber,
    onTertiary = Color.Black,
    background = Color(0xFFF1F5F1),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = StatusErrorRed,
    onError = Color.White
)

@Composable
fun OnionRobotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We prefer a dark theme by default for high-contrast outdoor robotics visibility
    val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
