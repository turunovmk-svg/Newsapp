package com.mikhail.newsapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CyberTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        CyberDarkColorScheme()
    } else {
        ColorScheme(
            primary = CyberColors.AccentBlue,
            onPrimary = Color(0xFF090B10),
            primaryContainer = CyberColors.AccentBlue.copy(alpha = 0.2f),
            onPrimaryContainer = Color(0xFF000000),
            secondary = CyberColors.AccentBlueBright,
            onSecondary = Color(0xFF000000),
            secondaryContainer = CyberColors.AccentBlueBright.copy(alpha = 0.2f),
            onSecondaryContainer = Color(0xFF000000),
            tertiary = CyberColors.AccentBlueDim,
            onTertiary = Color(0xFF000000),
            tertiaryContainer = CyberColors.AccentBlueDim.copy(alpha = 0.2f),
            onTertiaryContainer = Color(0xFF000000),
            error = CyberColors.Error,
            onError = Color(0xFFFFFFFF),
            errorContainer = CyberColors.Error.copy(alpha = 0.2f),
            onErrorContainer = Color(0xFF000000),
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF090B10),
            surface = CyberColors.SurfaceGlass.copy(alpha = 0.95f),
            onSurface = Color(0xFF090B10),
            surfaceVariant = CyberColors.SurfaceElevated,
            onSurfaceVariant = CyberColors.TextSecondary,
            surfaceTint = CyberColors.AccentBlue,
            inverseSurface = CyberColors.BackgroundDeep,
            inverseOnSurface = Color(0xFFFFFFFF),
            inversePrimary = CyberColors.AccentBlue,
            outline = CyberColors.BorderGlass,
            outlineVariant = CyberColors.BorderGlass.copy(alpha = 0.5f),
            scrim = Color(0xFF000000).copy(alpha = 0.5f),
            surfaceBright = CyberColors.SurfaceElevated,
            surfaceDim = CyberColors.BackgroundDeep,
            surfaceContainer = CyberColors.SurfaceGlass.copy(alpha = 0.95f),
            surfaceContainerHigh = CyberColors.SurfaceElevated,
            surfaceContainerHighest = Color(0xFF2A2A40),
            surfaceContainerLow = Color(0xFF12121E),
            surfaceContainerLowest = Color(0xFF090B10),
        )
    }

    val shapes = Shapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(20.dp),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CyberTypography(),
        shapes = shapes,
        content = content
    )
}
