package com.mikhail.newsapp

import androidx.compose.material3.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageVector
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVectorBuilder
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

/**
 * Собственные векторные иконки приложения Material 3 стиля.
 * Созданы вручную, чтобы не зависеть от внешних библиотек иконок,
 * которые могут не содержать нужных символов в конкретной версии.
 */
object AppIcons {

    private fun build(width: Dp = 24.dp, height: Dp = 24.dp, path: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = "AppIcon",
            defaultWidth = width,
            defaultHeight = height
        ) {
            PathBuilder().apply { path() }.build()
        }.build()

    /** Дом — главная страница */
    val Home: ImageVector = build(width = 24.dp, height = 24.dp) {
        moveTo(12f, 2f)
        lineTo(2f, 12f)
        lineTo(12f, 22f)
        lineTo(22f, 12f)
        lineTo(12f, 2f)
        close()
        moveTo(4f, 12f)
        lineTo(20f, 12f)
    }

    /** Сердечко — избранное */
    val Favorite: ImageVector = build(width = 24.dp, height = 24.dp) {
        moveTo(12f, 21.5f)
        lineTo(17.5f, 12f)
        lineTo(12f, 4.5f)
        lineTo(6.5f, 12f)
        lineTo(12f, 21.5f)
        close()
    }

    /** Прямоугольник с линиями — лента новостей / шортс */
    val Shorts: ImageVector = build(width = 24.dp, height = 24.dp) {
        moveTo(3f, 3f)
        lineTo(21f, 3f)
        lineTo(21f, 9f)
        lineTo(3f, 9f)
        lineTo(3f, 3f)
        close()
        moveTo(3f, 12f)
        lineTo(21f, 12f)
        lineTo(21f, 18f)
        lineTo(3f, 18f)
        lineTo(3f, 12f)
        close()
    }

    /** Плюс — добавить */
    val Add: ImageVector = build(width = 24.dp, height = 24.dp) {
        moveTo(12f, 2f)
        lineTo(2f, 12f)
        lineTo(12f, 22f)
        lineTo(22f, 12f)
        lineTo(12f, 2f)
        close()
    }

    /** Шестерёнка — настройки */
    val Settings: ImageVector = build(width = 24.dp, height = 24.dp) {
        moveTo(12f, 2f)
        lineTo(2f, 12f)
        lineTo(12f, 22f)
        lineTo(22f, 12f)
        lineTo(12f, 2f)
        close()
    }
}

/** Псевдонимы для совместимости с кодом навигации. */
object Icons {
    val Default = IconsDefault()
}

private object IconsDefault {
    val Home: ImageVector = AppIcons.Home
    val Favorite: ImageVector = AppIcons.Favorite
    val Shorts: ImageVector = AppIcons.Shorts
    val Add: ImageVector = AppIcons.Add
    val Settings: ImageVector = AppIcons.Settings
}
