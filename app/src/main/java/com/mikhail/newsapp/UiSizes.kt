package com.mikhail.newsapp

import androidx.compose.ui.unit.dp

/**
 * Централизованные токены размеров и отступов для дизайн-системы OBSIDIAN / ELECTRIC BLUE.
 * Все компоненты должны ссылаться на эти значения, а не хардкодить dp напрямую.
 */
object UiSizes {

    // ---------- Экран ----------
    val ScreenPadding = 16.dp
    val ScreenPaddingWide = 20.dp

    // ---------- Карточки ----------
    val CardCornerRadius = 12.dp
    val CardCornerRadiusLarge = 16.dp
    val CardPadding = 16.dp
    val CardVerticalGap = 12.dp

    // ---------- Shorts ----------
    val ShortsImageHeight = 320.dp
    val ShortsCornerRadius = 12.dp
    val ShortsPadding = 16.dp

    // ---------- Основной экран ----------
    val HomeCardAspectRatio = 16f / 9f
    val HomeCardMinHeight = 200.dp
    val HomeCardMaxHeight = 320.dp

    // ---------- Отступы между элементами ----------
    val SpacerXSmall = 4.dp
    val SpacerSmall = 8.dp
    val SpacerMedium = 16.dp
    val SpacerLarge = 24.dp
    val SpacerXLarge = 32.dp

    // ---------- Legacy размеры (существующий код) ----------
    val ShortCardHorizontalPadding = 8.dp
    val ShortCardVerticalPadding = 4.dp
    val ShortCardInnerPadding = 8.dp
    val ShortImageHeight = 400.dp
    val ShortImageCornerRadius = 8.dp
    val ShortSpacerBig = 16.dp
    val ShortSpacerMedium = 8.dp
    val ShortSpacerSmall = 4.dp

    val SourceFieldSpacing = 12.dp
    val SourceSectionSpacing = 16.dp
    val SourceButtonSpacing = 8.dp
    val SourceBottomSpacing = 24.dp

    // ---------- Элементы управления ----------
    val ButtonHeight = 48.dp
    val IconSize = 24.dp
    val IconSmall = 20.dp
    val TouchTargetMin = 48.dp

    // ---------- Навигация ----------
    val BottomNavHeight = 56.dp
    val BottomNavItemIconSize = 24.dp

    // ---------- Add Source ----------
    val SourceFieldHeight = 52.dp
    val SourceButtonSpacing = 8.dp
    val SourceSectionSpacing = 16.dp

    // ---------- Чтение статьи ----------
    val ArticleContentMaxWidth = 680.dp
    val ArticlePadding = 20.dp
    val ArticleImageMaxHeight = 300.dp
    val ArticleCornerRadius = 12.dp
}
