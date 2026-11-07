package com.mikhail.newsapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mikhail.newsapp.data.Post
import com.mikhail.newsapp.theme.CyberColors
import com.mikhail.newsapp.theme.CyberTypography
import com.mikhail.newsapp.theme.NeonButton
import com.mikhail.newsapp.UiSizes

/**
 * Иммерсивный экран чтения статьи:
 * — крупное фото, мета-информация, читаемая типографика с масштабом из настроек,
 * — свайпы влево/вправо — предыдущая/следующая статья,
 * — кнопка «Открыть оригинал» ведёт в браузер.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    post: Post,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fontScale by viewModel.fontScale.collectAsState()

    // Помечаем прочитанным при открытии
    LaunchedEffect(post.id) {
        viewModel.markAsRead(post)
    }

    val bodyStyle = CyberTypography.BodyLarge.copy(
        fontSize = (17 * fontScale).sp,
        lineHeight = (27 * fontScale).sp
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(onPrevious, onNext) {
                var accumulatedX = 0f
                detectDragGestures(
                    onDragStart = { accumulatedX = 0f },
                    onDrag = { _, dragAmount -> accumulatedX += dragAmount.x },
                    onDragEnd = {
                        if (accumulatedX < -80 && onNext != null) {
                            onNext()
                        } else if (accumulatedX > 80 && onPrevious != null) {
                            onPrevious()
                        }
                        accumulatedX = 0f
                    },
                )
            },
    ) {
        item {
            // Верхняя панель
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiSizes.SpacerSmall, vertical = UiSizes.SpacerSmall)
                    .height(UiSizes.ButtonHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Назад",
                        tint = CyberColors.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(UiSizes.SpacerSmall))
                post.sourceUrl?.let { url ->
                    val host = url
                        .removePrefix("https://")
                        .removePrefix("http://")
                        .removePrefix("www.")
                        .substringBefore('/')
                        .take(32)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(CyberColors.AccentBlue.copy(alpha = 0.12f))
                            .padding(horizontal = UiSizes.SpacerSmall, vertical = UiSizes.SpacerXSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = host,
                            style = CyberTypography.BodySmall,
                            color = CyberColors.AccentBlueBright,
                        )
                    }
                }
            }
        }

        item {
            // Фото статьи
            post.imageUrl?.let { imageUrl ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = UiSizes.ArticlePadding)
                        .height(UiSizes.ArticleImageMaxHeight)
                        .clip(RoundedCornerShape(UiSizes.ArticleCornerRadius))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        CyberColors.BackgroundDeep.copy(alpha = 0.25f)
                                    )
                                )
                            )
                    )
                }
                Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
            }
        }

        item {
            // Заголовок и дата
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiSizes.ArticlePadding)
            ) {
                Text(
                    text = post.title,
                    style = CyberTypography.HeadlineLarge,
                    color = CyberColors.TextPrimary,
                )
                post.publishedAt?.let { date ->
                    Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                    Text(
                        text = date,
                        style = CyberTypography.BodySmall,
                        color = CyberColors.TextSecondary,
                    )
                }
                Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CyberColors.Divider)
                )
            }
        }

        item {
            // Текст статьи
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiSizes.ArticlePadding)
            ) {
                post.description?.let { desc ->
                    val cleanDesc = Post.stripHtml(desc)
                    if (!cleanDesc.isNullOrEmpty()) {
                        Text(
                            text = cleanDesc,
                            style = bodyStyle,
                            color = CyberColors.TextPrimary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.article_no_content),
                            style = CyberTypography.BodyMedium,
                            color = CyberColors.TextSecondary,
                        )
                    }
                } ?: run {
                    Text(
                        text = stringResource(id = R.string.article_no_content),
                        style = CyberTypography.BodyMedium,
                        color = CyberColors.TextSecondary,
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
        }

        item {
            // Кнопка открытия оригинала
            NeonButton(
                onClick = {
                    post.sourceUrl?.let { url ->
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(url)
                        }
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiSizes.ArticlePadding),
                text = stringResource(id = R.string.open_original),
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
        }

        item {
            // Навигация prev/next
            if (onPrevious != null || onNext != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = UiSizes.ArticlePadding,
                            end = UiSizes.ArticlePadding,
                            bottom = UiSizes.SpacerLarge
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "← Предыдущая",
                        style = CyberTypography.BodyMedium,
                        color = if (onPrevious != null) CyberColors.AccentBlueBright else CyberColors.TextMuted,
                    )
                    Text(
                        text = "Следующая →",
                        style = CyberTypography.BodyMedium,
                        color = if (onNext != null) CyberColors.AccentBlueBright else CyberColors.TextMuted,
                    )
                }
            }
        }
    }
}
