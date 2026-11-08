package com.mikhail.newsapp

import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikhail.newsapp.data.Post
import com.mikhail.newsapp.theme.CyberColors
import com.mikhail.newsapp.theme.CyberTypography
import com.mikhail.newsapp.theme.NeonButton
import com.mikhail.newsapp.theme.SourceGradientPlaceholder
import com.mikhail.newsapp.UiSizes

@Composable
fun FavoritesScreen(
    viewModel: MainViewModel,
    onArticleClick: (Post) -> Unit,
    modifier: Modifier = Modifier
) {
    val favorites by viewModel.favorites.collectAsState()
    val readLater by viewModel.readLater.collectAsState()
    var tab by remember { mutableStateOf(0) } // 0 = Избранное, 1 = Читать позже

    val currentList = if (tab == 0) favorites else readLater

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberColors.BackgroundDeep)
    ) {
        // Табы
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiSizes.ScreenPadding, vertical = UiSizes.SpacerSmall),
            horizontalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
        ) {
            TabChip("Избранное (${favorites.size})", tab == 0) { tab = 0 }
            TabChip("Читать позже (${readLater.size})", tab == 1) { tab = 1 }
        }

        if (currentList.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = if (tab == 0) "♥" else "🔖", style = CyberTypography.HeadlineLarge)
                Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
                Text(
                    text = if (tab == 0) stringResource(id = R.string.favorites_empty)
                    else "Очередь чтения пуста",
                    style = CyberTypography.TitleMedium,
                    color = CyberColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                Text(
                    text = if (tab == 0) "Нажмите ♥ на карточке новости"
                    else "Добавьте статьи кнопкой закладки в статье или на карточке",
                    style = CyberTypography.BodySmall,
                    color = CyberColors.TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = UiSizes.ScreenPadding,
                    vertical = UiSizes.SpacerSmall
                ),
                verticalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
            ) {
                items(items = currentList, key = { it.id }) { post ->
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    CompactNewsCard(
                        post = post,
                        isRead = viewModel.isRead(post),
                        onClick = { onArticleClick(post) },
                        onRemove = {
                            if (tab == 0) viewModel.toggleFavorite(post)
                            else viewModel.toggleReadLater(post)
                        },
                        onShare = {
                            post.sourceUrl?.let { url ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "$url")
                                }
                                ctx.startActivity(Intent.createChooser(intent, "Поделиться"))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) CyberColors.AccentBlue else CyberColors.SurfaceElevated.copy(alpha = 0.6f),
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = CyberTypography.BodySmall,
            color = if (selected) CyberColors.TextInverse else CyberColors.TextSecondary
        )
    }
}

/** Компактная карточка для списков (избранное / читать позже). */
@Composable
fun CompactNewsCard(
    post: Post,
    isRead: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UiSizes.CardCornerRadius))
            .background(CyberColors.SurfaceElevated.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(UiSizes.CardPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Мини-превью
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            if (post.imageUrl != null) {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(post.imageUrl)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                SourceGradientPlaceholder(
                    sourceUrl = post.siteUrl ?: post.sourceUrl,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 10.dp
                )
            }
        }
        Spacer(modifier = Modifier.width(UiSizes.SpacerMedium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = post.title,
                style = CyberTypography.BodyMedium,
                color = if (isRead) CyberColors.TextSecondary else CyberColors.TextPrimary,
                maxLines = 3,
            )
            post.publishedAt?.let {
                Spacer(modifier = Modifier.height(UiSizes.SpacerXSmall))
                Text(
                    text = it,
                    style = CyberTypography.BodySmall,
                    color = CyberColors.TextMuted,
                    maxLines = 1
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(id = R.drawable.ic_bookmark),
                contentDescription = "Убрать",
                tint = CyberColors.AccentBlueBright,
                modifier = Modifier.width(20.dp).height(20.dp)
            )
        }
        IconButton(onClick = onShare) {
            Icon(
                painter = painterResource(id = R.drawable.ic_share),
                contentDescription = "Поделиться",
                tint = CyberColors.TextSecondary,
                modifier = Modifier.width(20.dp).height(20.dp)
            )
        }
    }
}
