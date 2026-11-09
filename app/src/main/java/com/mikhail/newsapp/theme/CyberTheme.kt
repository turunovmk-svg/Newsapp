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
    themeMode: String? = null,
    content: @Composable () -> Unit
) {
    // Переключаем адаптивную палитру ДО отрисовки контента.
    // Если передан themeMode — используем его, иначе флаг darkTheme.
    if (themeMode != null) {
        CyberColors.setThemeMode(themeMode)
    } else {
        CyberColors.setTheme(darkTheme)
    }

    val colorScheme = when (CyberColors.ThemeMode) {
        "neon" -> CyberNeonColorScheme()
        "harmony" -> CyberLightColorScheme()
        else -> CyberDarkColorScheme()
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
