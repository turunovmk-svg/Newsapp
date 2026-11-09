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
    internal val DarkBackground = Color(0xFF090B10)
    internal val DarkSurfaceElevated = Color(0xFF111722)
    internal val DarkSurfaceGlass = Color(0xFF1A1F2B)
    internal val DarkSurfaceGlassLight = Color(0xFF222A3A)
    internal val DarkSurfaceGlassStrong = Color(0xFF0E131E)
    internal val DarkTextPrimary = Color(0xFFF4F7FC)
    internal val DarkTextSecondary = Color(0xFFA1ADBF)
    internal val DarkTextMuted = Color(0xFF6E7A8C)
    internal val DarkTextInverse = Color(0xFF090B10)
    internal val DarkBorderGlass = Color(0xFF2A3442)
    internal val DarkBorderGlassBright = Color(0xFF3478F6)
    internal val DarkDivider = Color(0xFF1E2632)
    internal val DarkGlassOverlay = Color(0xFF1A1F2B)
    internal val DarkGlowBlue = Color(0xFF3478F6)

    // ---------- Светлая тема (PRISM / GLASS HARMONY) ----------
    internal val LightBackground = Color(0xFFF4F6FA)
    internal val LightSurfaceElevated = Color(0xFFFFFFFF)
    internal val LightSurfaceGlass = Color(0xFFFFFFFF)
    internal val LightSurfaceGlassLight = Color(0xFFEAEFF6)
    internal val LightSurfaceGlassStrong = Color(0xFFFFFFFF)
    internal val LightTextPrimary = Color(0xFF0D1117)
    internal val LightTextSecondary = Color(0xFF4A5568)
    internal val LightTextMuted = Color(0xFF8A94A6)
    internal val LightTextInverse = Color(0xFFFFFFFF)
    internal val LightBorderGlass = Color(0xFFD5DCE6)
    internal val LightBorderGlassBright = Color(0xFF3478F6)
    internal val LightDivider = Color(0xFFE2E8F0)
    internal val LightGlassOverlay = Color(0xFFFFFFFF)
    internal val LightGlowBlue = Color(0xFF3478F6)

    // ---------- Неоновая тема (NEON GLASS) ----------
    internal val NeonBackground = Color(0xFF0B0718)
    internal val NeonSurfaceElevated = Color(0xFF171233)
    internal val NeonSurfaceGlass = Color(0xFF1E1740)
    internal val NeonSurfaceGlassLight = Color(0xFF2A1F52)
    internal val NeonSurfaceGlassStrong = Color(0xFF120C28)
    internal val NeonTextPrimary = Color(0xFFF6F2FF)
    internal val NeonTextSecondary = Color(0xFFC5B8E8)
    internal val NeonTextMuted = Color(0xFF8B7BB0)
    internal val NeonTextInverse = Color(0xFF0B0718)
    internal val NeonBorderGlass = Color(0xFF3D2E6B)
    internal val NeonBorderGlassBright = Color(0xFFB44BFF)
    internal val NeonDivider = Color(0xFF2A1F52)
    internal val NeonGlassOverlay = Color(0xFF1E1740)
    internal val NeonGlowBlue = Color(0xFF8A3BFF)

    // ---------- Акценты (зависят от темы) ----------
    var AccentBlue: Color = Color(0xFF3478F6)
        private set
    var AccentBlueBright: Color = Color(0xFF4B91FF)
        private set
    var AccentBlueDim: Color = Color(0xFF1E5AD9)
        private set
    val Error = Color(0xFFE5484D)
    val Success = Color(0xFF12A150)
    val Warning = Color(0xFFF5A524)

    // ---------- Тематические (переключаются) ----------
    var IsDark: Boolean = true
        private set
    var ThemeMode: String = "obsidian"
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
        setThemeMode(if (dark) "obsidian" else "harmony")
    }

    /**
     * Переключает полную тему оформления:
     * "obsidian" — OBSIDIAN PRO (тёмный графит + электрический синий),
     * "neon"     — NEON GLASS (тёмный сине-фиолетовый + неоновое свечение),
     * "harmony"  — GLASS HARMONY (светлое стекло + холодный голубой).
     */
    fun setThemeMode(mode: String) {
        ThemeMode = mode
        when (mode) {
            "neon" -> applyNeon()
            "harmony" -> applyLight()
            else -> applyDark()  // obsidian по умолчанию
        }
    }

    private fun applyDark() {
        IsDark = true
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
        AccentBlue = Color(0xFF3478F6)
        AccentBlueBright = Color(0xFF4B91FF)
        AccentBlueDim = Color(0xFF1E5AD9)
    }

    private fun applyNeon() {
        IsDark = true
        BackgroundDeep = NeonBackground
        SurfaceElevated = NeonSurfaceElevated
        SurfaceGlass = NeonSurfaceGlass
        SurfaceGlassLight = NeonSurfaceGlassLight
        SurfaceGlassStrong = NeonSurfaceGlassStrong
        TextPrimary = NeonTextPrimary
        TextSecondary = NeonTextSecondary
        TextMuted = NeonTextMuted
        TextInverse = NeonTextInverse
        BorderGlass = NeonBorderGlass
        BorderGlassBright = NeonBorderGlassBright
        Divider = NeonDivider
        GlassOverlay = NeonGlassOverlay
        GlowBlue = NeonGlowBlue
        AccentBlue = Color(0xFF8A3BFF)
        AccentBlueBright = Color(0xFFB44BFF)
        AccentBlueDim = Color(0xFF6A1FD0)
    }

    private fun applyLight() {
        IsDark = false
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
        AccentBlue = Color(0xFF3478F6)
        AccentBlueBright = Color(0xFF4B91FF)
        AccentBlueDim = Color(0xFF1E5AD9)
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

/** Неоновая схема NEON GLASS. */
fun CyberNeonColorScheme(): ColorScheme = ColorScheme(
    primary = CyberColors.AccentBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = CyberColors.AccentBlue.copy(alpha = 0.18f),
    onPrimaryContainer = CyberColors.NeonTextPrimary,
    secondary = CyberColors.AccentBlueBright,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = CyberColors.AccentBlueBright.copy(alpha = 0.18f),
    onSecondaryContainer = CyberColors.NeonTextPrimary,
    tertiary = Color(0xFF00E5FF),
    onTertiary = Color(0xFF06021A),
    tertiaryContainer = Color(0xFF00E5FF).copy(alpha = 0.15f),
    onTertiaryContainer = CyberColors.NeonTextPrimary,
    background = CyberColors.NeonBackground,
    onBackground = CyberColors.NeonTextPrimary,
    surface = CyberColors.NeonSurfaceGlass,
    onSurface = CyberColors.NeonTextPrimary,
    surfaceVariant = CyberColors.NeonSurfaceElevated,
    onSurfaceVariant = CyberColors.NeonTextSecondary,
    surfaceTint = CyberColors.AccentBlue,
    inverseSurface = CyberColors.NeonSurfaceElevated,
    inverseOnSurface = CyberColors.NeonTextPrimary,
    inversePrimary = CyberColors.AccentBlueBright,
    error = CyberColors.Error,
    onError = Color(0xFFFFFFFF),
    errorContainer = CyberColors.Error.copy(alpha = 0.15f),
    onErrorContainer = Color(0xFFFFFFFF),
    outline = CyberColors.NeonBorderGlass,
    outlineVariant = CyberColors.NeonBorderGlass.copy(alpha = 0.4f),
    scrim = Color(0xFF000000).copy(alpha = 0.6f),
    surfaceBright = CyberColors.NeonSurfaceElevated,
    surfaceDim = CyberColors.NeonBackground,
    surfaceContainer = CyberColors.NeonSurfaceGlass,
    surfaceContainerHigh = CyberColors.NeonSurfaceElevated,
    surfaceContainerHighest = CyberColors.NeonSurfaceGlassLight,
    surfaceContainerLow = CyberColors.NeonBackground,
    surfaceContainerLowest = CyberColors.NeonBackground,
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
