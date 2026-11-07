package com.mikhail.newsapp.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Типографика в стиле cyberpunk.
 * Моноширинные акценты для заголовков, чистый sans-serif для тела.
 */
object CyberTypography {

    val HeadlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 0.5.sp,
    )

    val HeadlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 0.4.sp,
    )

    val TitleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        letterSpacing = 0.2.sp,
    )

    val TitleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
    )

    val BodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.1.sp,
    )

    val BodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
    )

    val BodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.1.sp,
    )

    val LabelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
    )
}

/**
 * Создаёт Typography в стиле cyberpunk.
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
