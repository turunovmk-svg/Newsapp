package com.mikhail.newsapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedShape
import androidx.compose.foundation.shape.CornerBasedShape

/**
 * Композируемая функция темы в стиле Cyberpunk.
 * Применяет тёмную цветовую схему, кибертипографику и угловые формы.
 */
@Composable
fun CyberTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        CyberDarkColorScheme()
    } else {
        dynamicLightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CyberTypography(),
        shapes = CyberShapes(),
        content = content
    )
}

/**
 * Формы в стиле cyberpunk — угловатые, с небольшим скруглением.
 */
private object CyberShapes {
    val Small = RoundedShape(4.dp)
    val Medium = RoundedShape(8.dp)
    val Large = RoundedShape(16.dp)
    val ExtraLarge = RoundedShape(24.dp)
}
