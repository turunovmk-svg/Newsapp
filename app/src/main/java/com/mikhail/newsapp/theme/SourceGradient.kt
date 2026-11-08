package com.mikhail.newsapp.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Градиентный плейсхолдер источника — вместо надписи «Изображение недоступно».
 * Цвет градиента детерминированно derived от домена: у каждого источника свой оттенок
 * (синий → бирюзовый → фиолетовый), первая буква домена — крупная, полупрозрачная.
 */
@Composable
fun SourceGradientPlaceholder(
    sourceUrl: String?,
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 12.dp,
    letter: String? = null,
) {
    val host = (sourceUrl ?: "")
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .substringBefore('/')
    val initial = letter ?: host.firstOrNull()?.uppercase() ?: "N"

    // Детерминированный оттенок из хеша домена: 0..2
    val variant = abs(host.hashCode()) % 3
    val startColor = when (variant) {
        0 -> CyberColors.AccentBlue                       // электрический синий
        1 -> CyberColors.AccentBlueBright                 // яркий синий
        else -> Color(0xFF6C5CE7)                         // фиолетовый акцент
    }
    val endColor = when (variant) {
        0 -> CyberColors.AccentBlueBright
        1 -> Color(0xFF3EC6FF)                            // бирюзовый
        else -> CyberColors.AccentBlueDim
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        startColor.copy(alpha = 0.85f),
                        endColor.copy(alpha = 0.55f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1f, 1f)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
