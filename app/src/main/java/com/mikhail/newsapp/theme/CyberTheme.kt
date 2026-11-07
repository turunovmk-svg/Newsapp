package com.mikhail.newsapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
        ColorScheme.light(
            primary = CyberColors.NeonCyan,
            surface = CyberColors.SurfaceGlass,
            background = Color(0xFFFFFFFF),
        )
    }

    val shapes = Shapes(
        small = RoundedShape(4.dp),
        medium = RoundedShape(8.dp),
        large = RoundedShape(16.dp),
        extraLarge = RoundedShape(24.dp),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CyberTypography(),
        shapes = shapes,
        content = content
    )
}
