package com.mikhail.newsapp.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Цветовая палитра в стиле Cyberpunk / Futuristic Glass UI.
 *
 * Фон: глубокий графит (#0A0A0F)
 * Поверхность: полупрозрачное стекло (#14141E)
 * Акценты: неон синий (#00F2FF), неон фиолетовый (#BC13FE)
 */
object CyberColors {

    // ---------- Основные цвета ----------
    val BackgroundDark   = Color(0xFF0A0A0F)   // глубокий графит
    val BackgroundDark2  = Color(0xFF12121E)   // чуть светлее для слоёв
    val SurfaceGlass     = Color(0xFF1A1A2A)   // стекло-поверхность
    val SurfaceGlassHigh = Color(0xFF22223A)   // подсвеченное стекло

    // ---------- Неоновые акценты ----------
    val NeonCyan    = Color(0xFF00F2FF)   // неон синий
    val NeonPurple  = Color(0xFFBC13FE)   // неон фиолетовый
    val NeonPink    = Color(0xFFFF007F)   // неон розовый (для акцентов)
    val NeonGreen   = Color(0xFF00FF88)   // неон зелёный (статус OK)

    // ---------- Текст ----------
    val TextOnSurface = Color(0xFFFFFFFF)
    val TextMuted     = Color(0xFF8888AA)
    val TextDim       = Color(0xFF444455)

    // ---------- Границы и свечения ----------
    val BorderGlass   = Color(0x3300F2FF)   // полупрозрачная граница
    val GlowCyan      = Color(0x1A00F2FF)   // мягкое свечение
    val GlowPurple    = Color(0x1ABC13FE)   // мягкое свечение
}

/**
 * Создаёт ColorScheme в стиле cyberpunk.
 * Все обязательные параметры ColorScheme заполнены.
 */
fun CyberDarkColorScheme(): ColorScheme = ColorScheme(
    primary = CyberColors.NeonCyan,
    onPrimary = Color(0xFF000000),
    primaryContainer = CyberColors.NeonCyan.copy(alpha = 0.2f),
    onPrimaryContainer = Color(0xFF000000),
    inversePrimary = CyberColors.NeonCyan,
    secondary = CyberColors.NeonPurple,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = CyberColors.NeonPurple.copy(alpha = 0.2f),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = CyberColors.NeonPink,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = CyberColors.NeonPink.copy(alpha = 0.2f),
    onTertiaryContainer = Color(0xFF000000),
    background = CyberColors.BackgroundDark,
    onBackground = CyberColors.TextOnSurface,
    surface = CyberColors.SurfaceGlass,
    onSurface = CyberColors.TextOnSurface,
    surfaceVariant = CyberColors.SurfaceGlassHigh,
    onSurfaceVariant = CyberColors.TextMuted,
    surfaceTint = CyberColors.NeonCyan,
    inverseSurface = CyberColors.BackgroundDark,
    inverseOnSurface = Color(0xFFFFFFFF),
    error = Color(0xFFFF3B30),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFF3B30).copy(alpha = 0.2f),
    onErrorContainer = Color(0xFF000000),
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
