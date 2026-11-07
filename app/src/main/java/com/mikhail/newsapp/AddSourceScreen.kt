package com.mikhail.newsapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikhail.newsapp.theme.CyberColors
import com.mikhail.newsapp.theme.GlassCard
import com.mikhail.newsapp.theme.NeonButton
import com.mikhail.newsapp.UiSizes

@Composable
fun AddSourceScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    var sourceUrl by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }
    var addedSource by remember { mutableStateOf<Boolean?>(null) } // null = не пытались, true = успех, false = ошибка

    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    val urlValid = sourceUrl.isNotBlank() &&
        (sourceUrl.startsWith("http://") || sourceUrl.startsWith("https://")) &&
        sourceUrl.length > 10

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(UiSizes.ScreenPadding)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Кнопка назад
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(UiSizes.SpacerMedium))
            Text(
                text = stringResource(id = R.string.add_source_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))

        // Подзаголовок
        Text(
            text = stringResource(id = R.string.add_source_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))

        // Карточка ввода с стеклянным эффектом
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiSizes.SpacerSmall)
        ) {
            Column(modifier = Modifier.padding(UiSizes.CardPadding)) {
                // Поле URL
                OutlinedTextField(
                    value = sourceUrl,
                    onValueChange = { sourceUrl = it; urlError = null },
                    label = { Text(stringResource(id = R.string.source_url_label)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = "URL",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UiSizes.ButtonHeight),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))

                // Ошибка валидации
                if (urlError != null) {
                    Text(
                        text = urlError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                }

                // Подсказка по формату
                if (sourceUrl.isNotEmpty() && !urlValid && urlError == null) {
                    Text(
                        text = stringResource(id = R.string.url_format_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                }

                // Основная кнопка добавления
                NeonButton(
                    onClick = {
                        if (sourceUrl.isBlank()) {
                            urlError = stringResource(id = R.string.url_empty_error)
                        } else if (!urlValid) {
                            urlError = stringResource(id = R.string.url_format_error)
                        } else {
                            val current = viewModel.sources.value
                            if (current.any { it.url == sourceUrl }) {
                                urlError = stringResource(id = R.string.url_duplicate_error)
                            } else {
                                urlError = null
                                isAdding = true
                                viewModel.addSource("RSS", sourceUrl)
                                addedSource = true
                                sourceUrl = ""
                                isAdding = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    text = if (isAdding) stringResource(id = R.string.adding_source) else stringResource(id = R.string.add_source_button),
                    enabled = urlValid && !isAdding,
                )
                Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))

                // Сообщение об успешном добавлении
                if (addedSource == true) {
                    Text(
                        text = stringResource(id = R.string.source_added_success),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberColors.AccentBlueBright,
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = UiSizes.SpacerSmall),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
                }

                // Общая ошибка экрана
                if (error != null && addedSource != true) {
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                }
            }
        }

        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))

        // Популярные источники — отдельные элементы
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.popular_sources_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))

            val popularSources = listOf(
                PopularSource(stringResource(id = R.string.source_bbc_ukraine), "RSS", "https://feeds.bbci.co.uk/ukrainian/rss.xml"),
                PopularSource(stringResource(id = R.string.source_pravda), "RSS", "https://www.pravda.com.ua/rss/"),
                PopularSource(stringResource(id = R.string.source_reddit_world), "REDDIT", "https://www.reddit.com/r/worldnews/.rss"),
                PopularSource(stringResource(id = R.string.source_youtube_tech), "YOUTUBE", "https://www.youtube.com/feeds/videos.xml?channel_id=UC_x5XG1OV2P6uZZ5FSM9Ttw")
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(popularSources) { source ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = UiSizes.SpacerSmall, vertical = 4.dp),
                        borderColor = CyberColors.BorderGlass,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled = !isAdding,
                                    onClick = {
                                        val current = viewModel.sources.value
                                        if (!current.any { it.url == source.url }) {
                                            isAdding = true
                                            viewModel.addSource(source.type, source.url)
                                            addedSource = true
                                            isAdding = false
                                        }
                                    }
                                )
                                .padding(horizontal = UiSizes.CardPadding, vertical = UiSizes.SpacerSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_add),
                                contentDescription = source.name,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(UiSizes.SpacerMedium))
                            Column {
                                Text(
                                    text = source.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = source.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            Spacer(modifier = Modifier.width(UiSizes.SpacerMedium))
                            if (isAdding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))

        // Кнопка назад к ленте
        NeonButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.back_to_feed),
            enabled = !isAdding,
        )
    }
}

private data class PopularSource(
    val name: String,
    val type: String,
    val url: String
)
