package com.mikhail.newsapp.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Дизайн-система OBSIDIAN / ELECTRIC BLUE.
 *
 * Централизованные токены цветов. Все компоненты должны ссылаться на эти значения,
 * а не хардкодить цвета напрямую.
 *
 * Цветовая палитра:
 * - Основной фон: #090B10 — глубокий тёмный графит
 * - Вторичная поверхность: #111722 — немного светлее для приподнятых элементов
 * - Основной акцент: #3478F6 — электрический синий
 * - Яркий акцент: #4B91FF — для активных и интерактивных элементов
 * - Основной текст: #F4F7FC — почти белый для высокой читаемости
 * - Вторичный текст: #A1ADBF — приглушённый для подписей и мета-данных
 */
object CyberColors {

    // ---------- Фон и поверхности ----------
    val BackgroundDeep = Color(0xFF090B10)   // главный фон приложения
    val SurfaceElevated = Color(0xFF111722)   // приподнятая поверхность (карточки)
    val SurfaceGlass = Color(0xFF1A1F2B).copy(alpha = 0.92f)   // полупрозрачное стекло
    val SurfaceGlassLight = Color(0xFF222A3A).copy(alpha = 0.85f)   // более светлое стекло
    val SurfaceGlassStrong = Color(0xFF0E131E).copy(alpha = 0.96f)   // тёмное стекло для оверлеев

    // ---------- Акцентные цвета ----------
    val AccentBlue = Color(0xFF3478F6)   // основной фирменный акцент
    val AccentBlueBright = Color(0xFF4B91FF)   // яркий акцент для активных элементов
    val AccentBlueDim = Color(0xFF1E5AD9)   // приглушённый синий для неактивных состояний

    // ---------- Текст ----------
    val TextPrimary = Color(0xFFF4F7FC)   // основной текст, высокая контрастность
    val TextSecondary = Color(0xFFA1ADBF)   // вторичный текст, подписи, даты
    val TextMuted = Color(0xFF6E7A8C)   // приглушённый текст для auxiliary информации
    val TextInverse = Color(0xFF090B10)   // текст на светлом фоне (акцент)

    // ---------- Границы и обводки ----------
    val BorderGlass = Color(0xFF2A3442).copy(alpha = 0.5f)   // тонкая граница стекла
    val BorderGlassBright = Color(0xFF3478F6).copy(alpha = 0.3f)   // акцентная граница
    val Divider = Color(0xFF1E2632).copy(alpha = 0.6f)   // разделитель между элементами

    // ---------- Состояния ----------
    val Error = Color(0xFFFF4D4D)
    val Success = Color(0xFF00FF88)
    val Warning = Color(0xFFFFB800)

    // ---------- Стекло и эффекты ----------
    val GlassOverlay = Color(0xFF1A1F2B).copy(alpha = 0.75f)   //overlay для стеклянных панелей
    val GlowBlue = Color(0xFF3478F6).copy(alpha = 0.15f)   // мягкое синее свечение
}

/**
 * Создаёт ColorScheme в стиле OBSIDIAN / ELECTRIC BLUE.
 * Все обязательные параметры ColorScheme заполнены централизованными токенами.
 */
fun CyberDarkColorScheme(): ColorScheme = ColorScheme(
    primary = CyberColors.AccentBlue,
    onPrimary = CyberColors.TextInverse,
    primaryContainer = CyberColors.AccentBlue.copy(alpha = 0.15f),
    onPrimaryContainer = CyberColors.TextPrimary,
    secondary = CyberColors.AccentBlueBright,
    onSecondary = CyberColors.TextInverse,
    secondaryContainer = CyberColors.AccentBlueBright.copy(alpha = 0.15f),
    onSecondaryContainer = CyberColors.TextPrimary,
    tertiary = CyberColors.AccentBlueDim,
    onTertiary = CyberColors.TextInverse,
    tertiaryContainer = CyberColors.AccentBlueDim.copy(alpha = 0.15f),
    onTertiaryContainer = CyberColors.TextPrimary,
    background = CyberColors.BackgroundDeep,
    onBackground = CyberColors.TextPrimary,
    surface = CyberColors.SurfaceGlass,
    onSurface = CyberColors.TextPrimary,
    surfaceVariant = CyberColors.SurfaceElevated,
    onSurfaceVariant = CyberColors.TextSecondary,
    surfaceTint = CyberColors.AccentBlue,
    inverseSurface = CyberColors.SurfaceElevated,
    inverseOnSurface = CyberColors.TextPrimary,
    inversePrimary = CyberColors.AccentBlueBright,
    error = CyberColors.Error,
    onError = Color(0xFFFFFFFF),
    errorContainer = CyberColors.Error.copy(alpha = 0.15f),
    onErrorContainer = Color(0xFFFFFFFF),
    outline = CyberColors.BorderGlass,
    outlineVariant = CyberColors.BorderGlass.copy(alpha = 0.3f),
    scrim = Color(0xFF000000).copy(alpha = 0.6f),
    surfaceBright = CyberColors.SurfaceElevated,
    surfaceDim = CyberColors.BackgroundDeep,
    surfaceContainer = CyberColors.SurfaceGlass,
    surfaceContainerHigh = CyberColors.SurfaceElevated,
    surfaceContainerHighest = Color(0xFF222A3A),
    surfaceContainerLow = CyberColors.BackgroundDeep,
    surfaceContainerLowest = CyberColors.BackgroundDeep,
)
