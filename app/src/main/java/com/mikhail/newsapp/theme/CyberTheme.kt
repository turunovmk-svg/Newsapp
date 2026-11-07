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
        ColorScheme(
            primary = CyberColors.NeonCyan,
            onPrimary = Color(0xFF000000),
            primaryContainer = CyberColors.NeonCyan.copy(alpha = 0.2f),
            onPrimaryContainer = Color(0xFF000000),
            secondary = CyberColors.NeonPurple,
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = CyberColors.NeonPurple.copy(alpha = 0.2f),
            onSecondaryContainer = Color(0xFF000000),
            tertiary = CyberColors.NeonPink,
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = CyberColors.NeonPink.copy(alpha = 0.2f),
            onTertiaryContainer = Color(0xFF000000),
            error = Color(0xFFFF3B30),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFF3B30).copy(alpha = 0.2f),
            onErrorContainer = Color(0xFF000000),
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF000000),
            surface = CyberColors.SurfaceGlass,
            onSurface = Color(0xFF000000),
            surfaceVariant = CyberColors.SurfaceGlassHigh,
            onSurfaceVariant = CyberColors.TextMuted,
            surfaceTint = CyberColors.NeonCyan,
            inverseSurface = CyberColors.BackgroundDark,
            inverseOnSurface = Color(0xFFFFFFFF),
            inversePrimary = CyberColors.NeonCyan,
            outline = CyberColors.BorderGlass,
            outlineVariant = CyberColors.BorderGlass.copy(alpha = 0.5f),
            scrim = Color(0xFF000000).copy(alpha = 0.5f),
            surfaceBright = CyberColors.SurfaceGlassHigh,
            surfaceDim = CyberColors.BackgroundDark,
            surfaceContainer = CyberColors.SurfaceGlass,
            surfaceContainerHigh = CyberColors.SurfaceGlassHigh,
            surfaceContainerHighest = Color(0xFF2A2A40),
            surfaceContainerLow = CyberColors.BackgroundDark2,
            surfaceContainerLowest = CyberColors.BackgroundDark,
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
