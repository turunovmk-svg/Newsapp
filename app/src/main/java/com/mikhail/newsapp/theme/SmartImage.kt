package com.mikhail.newsapp.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Адаптивная картинка новости.
 *
 * Если источник отдаёт маленький thumbnail (как BBC RSS: 240px) — приложение
 * не растягивает его пиксельно, а строит «премиальную» композицию:
 *  — фон: та же картинка, растянута и размыта (bokeh-эффект);
 *  — объект: оригинал contain в пределах контейнера без деформации.
 *
 * Для высокого разрешения фон тоже остаётся — фон выглядит как кинематографичный
 * виньетка и работает на всех экранах (Shorts, Featured, карточки).
 */
@Composable
fun SmartNewsImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    cornerRadius: Dp = 0.dp,
    blurRadius: Dp = 28.dp
) {
    if (imageUrl.isNullOrBlank()) {
        SourceGradientPlaceholder(
            sourceUrl = null,
            modifier = modifier,
            cornerRadius = cornerRadius
        )
        return
    }

    val context = LocalContext.current
    val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(CyberColors.SurfaceElevated)
    ) {
        // Слой 1: размытый фон (bokeh). На API < 31 — без размытия, просто затемнённое фото.
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(120)
                .build(),
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (blurSupported) {
                        Modifier.blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                    } else {
                        Modifier
                    }
                ),
            alpha = if (blurSupported) 1f else 0.25f
        )

        // Слой 2: резкий объект (contain — без пиксельного апскейла)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(120)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** Обёртка для карточки фиксированной высоты с фон-боке композицией. */
@Composable
fun SmartNewsImageBox(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    cornerRadius: Dp = 12.dp
) {
    SmartNewsImage(
        imageUrl = imageUrl,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        cornerRadius = cornerRadius
    )
}
