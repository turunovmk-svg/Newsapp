package com.mikhail.newsapp.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Дизайн-система OBSIDIAN / ELECTRIC BLUE.
 * Централизованные токены цветов. Все компоненты ссылаются на эти значения.
 *
 * Две темы: тёмная (OBSIDIAN) и светлая (PRISM).
 * Переключение — через setTheme(dark), вызывается из CyberTheme.
 * Акцентный электрический синий единый в обеих темах — «якорь» бренда.
 */
object CyberColors {

    // ---------- Тёмная тема (OBSIDIAN) ----------
    internal val Dark = Color(0xFF090B10)
    internal val Dark = Color(0xFF111722)
    internal val Dark = Color(0xFF1A1F2B)
    internal val Dark = Color(0xFF222A3A)
    internal val Dark = Color(0xFF0E131E)
    internal val Dark = Color(0xFFF4F7FC)
    internal val Dark = Color(0xFFA1ADBF)
    internal val Dark = Color(0xFF6E7A8C)
    internal val Dark = Color(0xFF090B10)
    internal val Dark = Color(0xFF2A3442)
    internal val Dark = Color(0xFF3478F6)
    internal val Dark = Color(0xFF1E2632)
    internal val Dark = Color(0xFF1A1F2B)
    internal val Dark = Color(0xFF3478F6)

    // ---------- Светлая тема (PRISM) ----------
    internal val Light = Color(0xFFF4F6FA)
    internal val Light = Color(0xFFFFFFFF)
    internal val Light = Color(0xFFFFFFFF)
    internal val Light = Color(0xFFEAEFF6)
    internal val Light = Color(0xFFFFFFFF)
    internal val Light = Color(0xFF0D1117)
    internal val Light = Color(0xFF4A5568)
    internal val Light = Color(0xFF8A94A6)
    internal val Light = Color(0xFFFFFFFF)
    internal val Light = Color(0xFFD5DCE6)
    internal val Light = Color(0xFF3478F6)
    internal val Light = Color(0xFFE2E8F0)
    internal val Light = Color(0xFFFFFFFF)
    internal val Light = Color(0xFF3478F6)

    // ---------- Акценты (едины в обеих темах) ----------
    val AccentBlue = Color(0xFF3478F6)
    val AccentBlueBright = Color(0xFF4B91FF)
    val AccentBlueDim = Color(0xFF1E5AD9)
    val Error = Color(0xFFE5484D)
    val Success = Color(0xFF12A150)
    val Warning = Color(0xFFF5A524)

    // ---------- Тематические (переключаются) ----------
    var IsDark: Boolean = true
        private set

    var BackgroundDeep: Color = DarkBackground
        private set
    var SurfaceElevated: Color = DarkSurfaceElevated
        private set
    var SurfaceGlass: Color = DarkSurfaceGlass
        private set
    var SurfaceGlassLight: Color = DarkSurfaceGlassLight
        private set
    var SurfaceGlassStrong: Color = DarkSurfaceGlassStrong
        private set
    var TextPrimary: Color = DarkTextPrimary
        private set
    var TextSecondary: Color = DarkTextSecondary
        private set
    var TextMuted: Color = DarkTextMuted
        private set
    var TextInverse: Color = DarkTextInverse
        private set
    var BorderGlass: Color = DarkBorderGlass
        private set
    var BorderGlassBright: Color = DarkBorderGlassBright
        private set
    var Divider: Color = DarkDivider
        private set
    var GlassOverlay: Color = DarkGlassOverlay
        private set
    var GlowBlue: Color = DarkGlowBlue
        private set

    /** Переключает палитру. Вызывается из CyberTheme до отрисовки контента. */
    fun setTheme(dark: Boolean) {
        IsDark = dark
        if (dark) {
            BackgroundDeep = DarkBackground
            SurfaceElevated = DarkSurfaceElevated
            SurfaceGlass = DarkSurfaceGlass
            SurfaceGlassLight = DarkSurfaceGlassLight
            SurfaceGlassStrong = DarkSurfaceGlassStrong
            TextPrimary = DarkTextPrimary
            TextSecondary = DarkTextSecondary
            TextMuted = DarkTextMuted
            TextInverse = DarkTextInverse
            BorderGlass = DarkBorderGlass
            BorderGlassBright = DarkBorderGlassBright
            Divider = DarkDivider
            GlassOverlay = DarkGlassOverlay
            GlowBlue = DarkGlowBlue
        } else {
            BackgroundDeep = LightBackground
            SurfaceElevated = LightSurfaceElevated
            SurfaceGlass = LightSurfaceGlass
            SurfaceGlassLight = LightSurfaceGlassLight
            SurfaceGlassStrong = LightSurfaceGlassStrong
            TextPrimary = LightTextPrimary
            TextSecondary = LightTextSecondary
            TextMuted = LightTextMuted
            TextInverse = LightTextInverse
            BorderGlass = LightBorderGlass
            BorderGlassBright = LightBorderGlassBright
            Divider = LightDivider
            GlassOverlay = LightGlassOverlay
            GlowBlue = LightGlowBlue
        }
    }
}

/** Тёмная схема OBSIDIAN. */
fun CyberDarkColorScheme(): ColorScheme = ColorScheme(
    primary = CyberColors.AccentBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = CyberColors.AccentBlue.copy(alpha = 0.15f),
    onPrimaryContainer = CyberColors.DarkTextPrimary,
    secondary = CyberColors.AccentBlueBright,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = CyberColors.AccentBlueBright.copy(alpha = 0.15f),
    onSecondaryContainer = CyberColors.DarkTextPrimary,
    tertiary = CyberColors.AccentBlueDim,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = CyberColors.AccentBlueDim.copy(alpha = 0.15f),
    onTertiaryContainer = CyberColors.DarkTextPrimary,
    background = CyberColors.DarkBackground,
    onBackground = CyberColors.DarkTextPrimary,
    surface = CyberColors.DarkSurfaceGlass,
    onSurface = CyberColors.DarkTextPrimary,
    surfaceVariant = CyberColors.DarkSurfaceElevated,
    onSurfaceVariant = CyberColors.DarkTextSecondary,
    surfaceTint = CyberColors.AccentBlue,
    inverseSurface = CyberColors.DarkSurfaceElevated,
    inverseOnSurface = CyberColors.DarkTextPrimary,
    inversePrimary = CyberColors.AccentBlueBright,
    error = CyberColors.Error,
    onError = Color(0xFFFFFFFF),
    errorContainer = CyberColors.Error.copy(alpha = 0.15f),
    onErrorContainer = Color(0xFFFFFFFF),
    outline = CyberColors.DarkBorderGlass,
    outlineVariant = CyberColors.DarkBorderGlass.copy(alpha = 0.3f),
    scrim = Color(0xFF000000).copy(alpha = 0.6f),
    surfaceBright = CyberColors.DarkSurfaceElevated,
    surfaceDim = CyberColors.DarkBackground,
    surfaceContainer = CyberColors.DarkSurfaceGlass,
    surfaceContainerHigh = CyberColors.DarkSurfaceElevated,
    surfaceContainerHighest = CyberColors.DarkSurfaceGlassLight,
    surfaceContainerLow = CyberColors.DarkBackground,
    surfaceContainerLowest = CyberColors.DarkBackground,
)

/** Светлая схема PRISM. */
fun CyberLightColorScheme(): ColorScheme = ColorScheme(
    primary = CyberColors.AccentBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = CyberColors.AccentBlue.copy(alpha = 0.12f),
    onPrimaryContainer = CyberColors.LightTextPrimary,
    secondary = CyberColors.AccentBlueBright,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = CyberColors.AccentBlueBright.copy(alpha = 0.12f),
    onSecondaryContainer = CyberColors.LightTextPrimary,
    tertiary = CyberColors.AccentBlueDim,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = CyberColors.AccentBlueDim.copy(alpha = 0.12f),
    onTertiaryContainer = CyberColors.LightTextPrimary,
    background = CyberColors.LightBackground,
    onBackground = CyberColors.LightTextPrimary,
    surface = CyberColors.LightSurfaceGlass,
    onSurface = CyberColors.LightTextPrimary,
    surfaceVariant = CyberColors.LightSurfaceGlassLight,
    onSurfaceVariant = CyberColors.LightTextSecondary,
    surfaceTint = CyberColors.AccentBlue,
    inverseSurface = Color(0xFF202633),
    inverseOnSurface = Color(0xFFF4F7FC),
    inversePrimary = CyberColors.AccentBlueBright,
    error = CyberColors.Error,
    onError = Color(0xFFFFFFFF),
    errorContainer = CyberColors.Error.copy(alpha = 0.12f),
    onErrorContainer = CyberColors.Error,
    outline = CyberColors.LightBorderGlass,
    outlineVariant = CyberColors.LightBorderGlass.copy(alpha = 0.5f),
    scrim = Color(0xFF000000).copy(alpha = 0.4f),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = CyberColors.LightSurfaceGlassLight,
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = CyberColors.LightSurfaceGlassLight,
    surfaceContainerHighest = Color(0xFFDCE3EC),
    surfaceContainerLow = Color(0xFFFDFEFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
)
