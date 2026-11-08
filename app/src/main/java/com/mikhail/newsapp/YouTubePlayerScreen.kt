package com.mikhail.newsapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.mikhail.newsapp.data.Post
import com.mikhail.newsapp.theme.CyberColors
import com.mikhail.newsapp.theme.CyberTypography
import com.mikhail.newsapp.theme.NeonButton
import com.mikhail.newsapp.UiSizes
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

/**
 * Извлекает video-id из ссылки YouTube (watch?v=, youtu.be/, shorts/).
 */
fun extractYouTubeId(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return try {
        when {
            url.contains("watch") && url.contains("v=") ->
                url.substringAfter("v=").substringBefore('&').substringBefore('#')
            url.contains("youtu.be/") ->
                url.substringAfter("youtu.be/").substringBefore('?').substringBefore('#')
            url.contains("/shorts/") ->
                url.substringAfter("/shorts/").substringBefore('?').substringBefore('#')
            url.contains("/embed/") ->
                url.substringAfter("/embed/").substringBefore('?').substringBefore('#')
            else -> null
        }?.takeIf { it.isNotBlank() && it.length >= 6 }
    } catch (e: Exception) {
        null
    }
}

/** Является ли пост YouTube-видео (для выбора плеера вместо читалки). */
fun isYouTubePost(post: Post): Boolean {
    val u = post.sourceUrl ?: ""
    return (u.contains("youtube.com") || u.contains("youtu.be")) && extractYouTubeId(u) != null
}

/**
 * Встроенный YouTube-плеер + метаданные поста. Не переходит в приложение YouTube.
 */
@Composable
fun YouTubePlayerScreen(
    post: Post,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoId = extractYouTubeId(post.sourceUrl)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberColors.BackgroundDeep)
            .verticalScroll(rememberScrollState())
    ) {
        // Верхняя панель
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiSizes.SpacerSmall),
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Назад",
                    tint = CyberColors.TextPrimary
                )
            }
        }

        // Плеер
        if (videoId != null) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color.Black),
                factory = { ctx: Context ->
                    YouTubePlayerView(ctx).also { view ->
                        view.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                youTubePlayer.loadVideo(videoId, 0f)
                            }
                        })
                    }
                },
                onRelease = { view -> view.release() }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(CyberColors.SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Видео недоступно",
                    style = CyberTypography.BodyMedium,
                    color = CyberColors.TextSecondary
                )
            }
        }

        Column(modifier = Modifier.padding(UiSizes.ArticlePadding)) {
            Text(
                text = post.title,
                style = CyberTypography.HeadlineMedium,
                color = CyberColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
            post.publishedAt?.let {
                Text(
                    text = it,
                    style = CyberTypography.BodySmall,
                    color = CyberColors.TextSecondary,
                )
            }
            Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
            post.description?.let { desc ->
                val clean = Post.stripHtml(desc)
                if (!clean.isNullOrEmpty()) {
                    Text(
                        text = clean,
                        style = CyberTypography.BodyLarge,
                        color = CyberColors.TextPrimary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))

            // Кнопка «Открыть в YouTube» — остаётся как опция
            NeonButton(
                onClick = {
                    post.sourceUrl?.let { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                text = "Открыть в YouTube",
                primaryColor = CyberColors.SurfaceElevated,
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
        }
    }
}
