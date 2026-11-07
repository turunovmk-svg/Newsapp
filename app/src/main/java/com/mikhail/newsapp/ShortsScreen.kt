package com.mikhail.newsapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mikhail.newsapp.theme.CyberColors
import com.mikhail.newsapp.UiSizes
import com.mikhail.newsapp.data.Post

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    viewModel: MainViewModel,
    onAddSource: () -> Unit,
    onArticleClick: (Post) -> Unit,
) {
    val posts by viewModel.posts.collectAsState()
    val isRefreshing by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val shortPosts = posts.take(10)

    val refreshState = rememberPullToRefreshState()
    var currentIndex by remember { mutableIntStateOf(0) }

    if (shortPosts.isEmpty() && error == null && !isRefreshing) {
        EmptyShortsState(onAddSource = onAddSource)
        return
    }

    if (error != null && shortPosts.isEmpty()) {
        ErrorShortsState(
            error = error ?: "",
            onRetry = { viewModel.loadPosts(); viewModel.clearError() },
            onAddSource = onAddSource,
        )
        return
    }

    val currentPost = shortPosts[currentIndex]

    val onSwipeUp = {
        if (currentIndex < shortPosts.size - 1) {
            currentIndex += 1
        }
    }
    val onSwipeDown = {
        if (currentIndex > 0) {
            currentIndex -= 1
        }
    }

    PullToRefreshBox(
        state = refreshState,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.loadPosts() },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { _, dragAmount ->
                            if (dragAmount.y < -80) {
                                onSwipeUp()
                            } else if (dragAmount.y > 80) {
                                onSwipeDown()
                            }
                        },
                    )
                },
        ) {
            // Полноэкранная карточка новости
            ShortsNewsCard(
                post = currentPost,
                isFavorite = viewModel.isFavorite(currentPost),
                onFavoriteClick = { viewModel.toggleFavorite(currentPost) },
                onShareClick = { /* TODO: реализовать поделиться */ },
                onClick = { onArticleClick(currentPost) },
                modifier = Modifier.fillMaxSize(),
            )

            // Индикатор страниц (точки)
            Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                shortPosts.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (index == currentIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))

            // Действия внизу (одна рука)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiSizes.ScreenPadding),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { viewModel.toggleFavorite(currentPost) },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_favorite),
                        contentDescription = if (viewModel.isFavorite(currentPost)) "Убрать из избранного" else "Добавить в избранное",
                        tint = if (viewModel.isFavorite(currentPost)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { /* TODO: поделиться */ },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = "Поделиться",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { onArticleClick(currentPost) },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = "Открыть статью",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ShortsNewsCard(
    post: Post,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Изображение новости (занимает существенную часть экрана)
        post.imageUrl?.let { imageUrl ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UiSizes.ShortsImageHeight)
                    .clip(RoundedCornerShape(UiSizes.ShortsCornerRadius))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = UiSizes.ShortsPadding)
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
        } ?: run {
            // Типографическая заглушка без изображения
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UiSizes.ShortsImageHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clip(RoundedCornerShape(UiSizes.ShortsCornerRadius))
                    .padding(horizontal = UiSizes.ShortsPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📰",
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                Text(
                    text = stringResource(id = R.string.no_image),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
        }

        // Источник и время
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiSizes.ShortsPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            post.sourceUrl?.let { url ->
                Text(
                    text = url.replace("https://", "").replace("http://", "").take(40),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.width(UiSizes.SpacerSmall))
            post.publishedAt?.let { date ->
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))

        // Заголовок (крупный и читаемый)
        Text(
            text = post.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiSizes.ShortsPadding),
            maxLines = 3,
        )
        Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))

        // Описание (разумная длина)
        post.description?.let { desc ->
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiSizes.ShortsPadding),
                maxLines = 6,
            )
        }

        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyShortsState(onAddSource: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(UiSizes.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
        Text(
            text = stringResource(id = R.string.empty_shorts),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
        Text(
            text = stringResource(id = R.string.add_sources_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
        Button(
            onClick = onAddSource,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.add_source_button))
        }
    }
}

@Composable
private fun ErrorShortsState(
    error: String,
    onRetry: () -> Unit,
    onAddSource: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(UiSizes.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.error_loading_news, error),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.retry_button))
        }
        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
        Button(
            onClick = onAddSource,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.add_source_button))
        }
    }
}
