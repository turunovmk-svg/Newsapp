package com.mikhail.newsapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Композируемая функция темы в стиле Cyberpunk.
 */
@Composable
fun CyberTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        CyberDarkColorScheme()
    } else {
        ColorScheme(
            primary = CyberColors.NeonCyan,
            onPrimary = Color(0xFF000000),
            secondary = CyberColors.NeonPurple,
            onSecondary = Color(0xFFFFFFFF),
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF000000),
            surface = CyberColors.SurfaceGlass,
            onSurface = Color(0xFF000000),
            error = Color(0xFFFF3B30),
            onError = Color(0xFFFFFFFF),
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CyberTypography(),
        content = content
    )
}
