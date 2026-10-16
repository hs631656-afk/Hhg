package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisCyanPrimary,
    onPrimary = Color(0xFF00363D),
    primaryContainer = JarvisCyanContainer,
    onPrimaryContainer = Color(0xFF80F5FF),
    secondary = JarvisBlueSecondary,
    onSecondary = Color(0xFF002266),
    secondaryContainer = JarvisBlueContainer,
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = JarvisNeonGreen,
    onTertiary = Color(0xFF003915),
    background = JarvisBackground,
    onBackground = TextPrimary,
    surface = JarvisSurface,
    onSurface = TextPrimary,
    surfaceVariant = JarvisSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = JarvisRed,
    onError = Color(0xFF690005)
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    JarvisTheme(content = content)
}
