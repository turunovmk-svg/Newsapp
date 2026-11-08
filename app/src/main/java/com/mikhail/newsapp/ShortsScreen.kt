package com.mikhail.newsapp

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mikhail.newsapp.data.Post
import com.mikhail.newsapp.theme.CyberColors
import com.mikhail.newsapp.theme.CyberTypography
import com.mikhail.newsapp.theme.NeonButton
import com.mikhail.newsapp.theme.SourceGradientPlaceholder
import com.mikhail.newsapp.UiSizes

/**
 * Shorts — полноэкранная вертикальная лента в стиле TikTok:
 * страницы перелистываются свайпом вверх/вниз (VerticalPager со snap).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    viewModel: MainViewModel,
    onAddSource: () -> Unit,
    onArticleClick: (Post) -> Unit,
    modifier: Modifier = Modifier,
) {
    val posts by viewModel.posts.collectAsState()
    val isRefreshing by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val currentIndexState by viewModel.currentShortsIndex.collectAsState()
    val ctx = LocalContext.current

    // Фильтр по выбранной папке (как на Главной)
    val folderUrls = remember(selectedFolder, sources) {
        if (selectedFolder.isBlank()) null
        else sources.filter { it.folder == selectedFolder }.map { it.url }.toSet()
    }
    val allPosts = if (folderUrls == null) posts else {
        posts.filter { post ->
            // GNEWS-источники матчим по домену из siteUrl
            val rawUrl = post.siteUrl ?: post.sourceUrl ?: ""
            folderUrls.any { src ->
                val srcDomain = src.removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore('/')
                rawUrl.contains(srcDomain, ignoreCase = true)
            }
        }
    }
    val shortPosts = allPosts.take(20)

    if (shortPosts.isEmpty() && error == null && !isRefreshing) {
        EmptyShortsState(onAddSource = onAddSource, modifier = modifier)
        return
    }

    if (error != null && shortPosts.isEmpty()) {
        ErrorShortsState(
            error = error ?: "",
            onRetry = { viewModel.loadPosts(); viewModel.clearError() },
            onAddSource = onAddSource,
            modifier = modifier,
        )
        return
    }

    val safeStart = currentIndexState.coerceIn(0, shortPosts.size - 1)
    val pagerState = rememberPagerState(initialPage = safeStart) { shortPosts.size }

    // Сохраняем позицию в ViewModel при смене страницы
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentShortsIndex(pagerState.currentPage)
    }

    PullToRefreshBox(
        state = rememberPullToRefreshState(),
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.loadPosts() },
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberColors.BackgroundDeep)
        ) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = UiSizes.SpacerSmall,
            ) { page ->
                val post = shortPosts[page]
                ShortsFullScreenCard(
                    post = post,
                    isFavorite = viewModel.isFavorite(post),
                    isRead = viewModel.isRead(post),
                    onFavoriteClick = { viewModel.toggleFavorite(post) },
                    onShareClick = { sharePost(ctx, post) },
                    onOpenArticle = { onArticleClick(post) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Индикатор страниц убран по требованию — чистая TikTok-лента
        }
    }
}

/** Полноэкранная карточка Shorts: фото, градиент, источник, заголовок, панель действий. */
@Composable
fun ShortsFullScreenCard(
    post: Post,
    isFavorite: Boolean,
    isRead: Boolean,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onOpenArticle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onOpenArticle)
    ) {
        // Фон: фото или заглушка
        post.imageUrl?.let { imageUrl ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(150)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } ?: run {
            SourceGradientPlaceholder(
                sourceUrl = post.siteUrl ?: post.sourceUrl,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 0.dp,
            )
        }

        // Верхний и нижний градиенты для читаемости
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CyberColors.BackgroundDeep.copy(alpha = 0.55f),
                            Color.Transparent,
                            Color.Transparent,
                            CyberColors.BackgroundDeep.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Источник сверху слева
        val chipName = post.siteName
            ?: post.siteUrl?.removePrefix("https://")?.removePrefix("www.")?.removePrefix("http://")?.substringBefore('/')
            ?: post.sourceUrl
                ?.removePrefix("https://")
                ?.removePrefix("http://")
                ?.removePrefix("www.")
                ?.substringBefore('/')
                ?.take(28)
        if (chipName != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(UiSizes.ScreenPadding)
                    .clip(RoundedCornerShape(50))
                    .background(CyberColors.SurfaceGlassStrong.copy(alpha = 0.7f))
                    .padding(horizontal = UiSizes.SpacerSmall, vertical = UiSizes.SpacerXSmall)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(CyberColors.AccentBlue)
                )
                Spacer(modifier = Modifier.width(UiSizes.SpacerSmall))
                Text(
                    text = chipName,
                    style = CyberTypography.BodySmall,
                    color = CyberColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Вертикальная панель действий справа (TikTok-style)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = UiSizes.SpacerSmall, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UiSizes.SpacerMedium)
        ) {
            ShortsActionButton(
                icon = R.drawable.ic_favorite,
                label = if (isFavorite) "В избранном" else "В избранное",
                tint = if (isFavorite) CyberColors.AccentBlueBright else CyberColors.TextPrimary,
                onClick = onFavoriteClick
            )
            ShortsActionButton(
                icon = R.drawable.ic_share,
                label = "Поделиться",
                tint = CyberColors.TextPrimary,
                onClick = onShareClick
            )
        }

        // Текстовая часть снизу слева
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    start = UiSizes.ScreenPadding,
                    end = 72.dp,
                    bottom = UiSizes.SpacerLarge
                )
        ) {
            Text(
                text = post.title,
                style = CyberTypography.HeadlineMedium,
                color = if (isRead) CyberColors.TextSecondary else CyberColors.TextPrimary,
                maxLines = 3,
            )
            post.publishedAt?.let { date ->
                Spacer(modifier = Modifier.height(UiSizes.SpacerXSmall))
                Text(
                    text = date,
                    style = CyberTypography.BodySmall,
                    color = CyberColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            post.description?.let { desc ->
                val cleanDesc = Post.stripHtml(desc)
                if (!cleanDesc.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                    Text(
                        text = cleanDesc,
                        style = CyberTypography.BodyMedium,
                        color = CyberColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortsActionButton(
    icon: Int,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            style = CyberTypography.BodySmall,
            color = tint,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyShortsState(onAddSource: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(UiSizes.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.empty_shorts),
            style = CyberTypography.HeadlineMedium,
            color = CyberColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
        Text(
            text = stringResource(id = R.string.add_sources_hint),
            style = CyberTypography.BodyMedium,
            color = CyberColors.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
        NeonButton(
            onClick = onAddSource,
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.add_source_button),
        )
    }
}

@Composable
private fun ErrorShortsState(
    error: String,
    onRetry: () -> Unit,
    onAddSource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(UiSizes.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.error_loading_news, error),
            style = CyberTypography.TitleMedium,
            color = CyberColors.Error
        )
        Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
        NeonButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.retry_button),
        )
        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
        NeonButton(
            onClick = onAddSource,
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.add_source_button),
        )
    }
}

/** Поделиться статьёй через системный Intent. */
private fun sharePost(context: android.content.Context, post: Post) {
    post.sourceUrl?.let { url ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться"))
    }
}
