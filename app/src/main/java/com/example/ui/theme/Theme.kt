package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val P6692ColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = BrightPurple,
    onPrimaryContainer = Color.White,
    secondary = BrightPurple,
    onSecondary = Color.White,
    secondaryContainer = SecondaryBg,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentCyan,
    onTertiary = DarkBg,
    tertiaryContainer = AccentCyan.copy(alpha = 0.2f),
    onTertiaryContainer = AccentCyan,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = SecondaryBg,
    onSurface = TextPrimary,
    surfaceVariant = CardBg,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = CoralRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = P6692ColorScheme,
        typography = Typography,
        content = content
    )
}
