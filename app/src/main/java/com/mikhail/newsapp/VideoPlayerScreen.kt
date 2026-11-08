package com.mikhail.newsapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mikhail.newsapp.theme.CyberTypography
import com.mikhail.newsapp.theme.CyberColors

/**
 * Встроенный плеер для mp4-видео из Telegram-постов (Media3 ExoPlayer).
 * Автоплей при появлении, корректный release при уходе с экрана.
 */
@Composable
fun TelegramVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = autoPlay
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier.background(Color.Black),
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                controllerAutoShow = true
            }
        },
        update = { view ->
            view.player?.playWhenReady = autoPlay
        },
        onRelease = { view ->
            view.player = null
        }
    )
}

/**
 * Видео-блок для Shorts: плеер с запасным постером, если плеер не стартовал.
 */
@Composable
fun ShortsVideoBox(
    videoUrl: String,
    posterUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(modifier = modifier) {
        // Постер-фон (виден до старта буферизации)
        if (posterUrl != null) {
            coil.compose.AsyncImage(
                model = coil.request.ImageRequest.Builder(context)
                    .data(posterUrl)
                    .crossfade(100)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(CyberColors.SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "▶", style = CyberTypography.HeadlineLarge, color = CyberColors.TextSecondary)
            }
        }
        TelegramVideoPlayer(
            videoUrl = videoUrl,
            modifier = Modifier.fillMaxSize()
        )
    }
}
