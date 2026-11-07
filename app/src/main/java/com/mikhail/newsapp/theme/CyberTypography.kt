package com.mikhail.newsapp.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Типографика в стиле OBSIDIAN / ELECTRIC BLUE.
 * Редакционные крупные заголовки, чёткая иерархия, комфортный основной текст.
 */
object CyberTypography {

    // ---------- Редакционные заголовки ----------
    val HeadlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        letterSpacing = -0.5.sp,
    )

    val HeadlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = -0.25.sp,
    )

    val TitleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.sp,
    )

    val TitleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        letterSpacing = 0.sp,
    )

    // ---------- Основной текст ----------
    val BodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.25.sp,
        lineHeight = 24.sp,
    )

    val BodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
        lineHeight = 21.sp,
    )

    val BodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
        lineHeight = 18.sp,
    )

    // ---------- Метки и подписи ----------
    val LabelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp,
    )

    val Caption = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
    )
}

/**
 * Создаёт Typography в стиле OBSIDIAN / ELECTRIC BLUE.
 */
fun CyberTypography(): Typography = Typography(
    headlineLarge = CyberTypography.HeadlineLarge,
    headlineMedium = CyberTypography.HeadlineMedium,
    titleLarge = CyberTypography.TitleLarge,
    titleMedium = CyberTypography.TitleMedium,
    bodyLarge = CyberTypography.BodyLarge,
    bodyMedium = CyberTypography.BodyMedium,
    bodySmall = CyberTypography.BodySmall,
    labelLarge = CyberTypography.LabelLarge,
)
