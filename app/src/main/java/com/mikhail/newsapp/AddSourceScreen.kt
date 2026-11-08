package com.mikhail.newsapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikhail.newsapp.data.Source
import com.mikhail.newsapp.data.SourceType
import com.mikhail.newsapp.theme.CyberColors
import com.mikhail.newsapp.theme.CyberTypography
import com.mikhail.newsapp.theme.GlassCard
import com.mikhail.newsapp.theme.NeonButton
import com.mikhail.newsapp.UiSizes

@Composable
fun AddSourceScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    var sourceUrl by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf(SourceType.RSS) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }
    var addedSource by remember { mutableStateOf<Boolean?>(null) } // null = не пытались, true = успех, false = ошибка

    val error by viewModel.error.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val sourceErrors by viewModel.sourceErrors.collectAsState()
    val context = LocalContext.current

    // Предзагружаем строковые ресурсы для использования в onClick лямбдах
    val urlEmptyError = stringResource(id = R.string.url_empty_error)
    val urlFormatError = stringResource(id = R.string.url_format_error)
    val urlDuplicateError = stringResource(id = R.string.url_duplicate_error)
    val addingSource = stringResource(id = R.string.adding_source)
    val addSourceButton = stringResource(id = R.string.add_source_button)
    val sourceAddedSuccess = stringResource(id = R.string.source_added_success)
    val backToFeed = stringResource(id = R.string.back_to_feed)

    val urlValid = sourceUrl.isNotBlank() &&
        (sourceUrl.startsWith("http://") || sourceUrl.startsWith("https://") || sourceUrl.contains('.')) &&
        sourceUrl.length > 6

    // Автоопределение типа по вставленной ссылке
    val detectedType = remember(sourceUrl) {
        if (sourceUrl.isBlank()) null
        else Source.detect(sourceUrl)?.type
    }

    fun addSource(type: String, url: String) {
        isAdding = true
        viewModel.addSource(type, url)
        addedSource = true
        isAdding = false
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberColors.BackgroundDeep)
            .padding(horizontal = UiSizes.ScreenPadding)
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(WindowInsets.ime.asPaddingValues())
            .padding(bottom = UiSizes.SpacerLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Кнопка назад
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = UiSizes.SpacerSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Назад",
                        tint = CyberColors.TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(UiSizes.SpacerMedium))
                Text(
                    text = stringResource(id = R.string.add_source_title),
                    style = CyberTypography.HeadlineMedium,
                    color = CyberColors.TextPrimary
                )
            }
        }

        item {
            // Подзаголовок
            Text(
                text = stringResource(id = R.string.add_source_subtitle),
                style = CyberTypography.BodyMedium,
                color = CyberColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
        }

        item {
            // Карточка ввода с стеклянным эффектом
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = CyberColors.BorderGlassBright,
            ) {
                Column(modifier = Modifier.padding(UiSizes.CardPadding)) {
                    // Поле URL
                    OutlinedTextField(
                        value = sourceUrl,
                        onValueChange = { sourceUrl = it; urlError = null },
                        label = { Text(stringResource(id = R.string.source_url_label)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_link),
                                contentDescription = "URL",
                                tint = CyberColors.TextSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))

                    // Тип источника: автоопределение + ручной выбор
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.source_type_label),
                            style = CyberTypography.BodySmall,
                            color = CyberColors.TextSecondary,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SourceTypeChip("RSS", sourceType, CyberColors.AccentBlue) { sourceType = SourceType.RSS }
                            SourceTypeChip("Reddit", sourceType, CyberColors.AccentBlue) { sourceType = SourceType.REDDIT }
                            SourceTypeChip("YouTube", sourceType, CyberColors.AccentBlue) { sourceType = SourceType.YOUTUBE }
                            SourceTypeChip("TG", sourceType, CyberColors.AccentBlue) { sourceType = SourceType.TELEGRAM }
                            SourceTypeChip("X", sourceType, CyberColors.AccentBlue) { sourceType = SourceType.TWITTER }
                        }
                    }
                    Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))

                    // Подсказка автоопределения типа
                    if (detectedType != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check),
                                contentDescription = null,
                                tint = CyberColors.AccentBlueBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(UiSizes.SpacerSmall))
                            Text(
                                text = stringResource(id = R.string.type_detected) + typeLabel(detectedType),
                                style = CyberTypography.BodySmall,
                                color = CyberColors.AccentBlueBright,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
                    }
                    Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))

                    // Ошибка валидации
                    if (urlError != null) {
                        Text(
                            text = urlError ?: "",
                            style = CyberTypography.BodySmall,
                            color = CyberColors.Error,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                    }

                    // Подсказка по формату
                    if (sourceUrl.isNotEmpty() && !urlValid && urlError == null) {
                        Text(
                            text = stringResource(id = R.string.url_format_hint),
                            style = CyberTypography.BodySmall,
                            color = CyberColors.TextSecondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                    }

                    // Основная кнопка добавления
                    NeonButton(
                        onClick = {
                            if (sourceUrl.isBlank()) {
                                urlError = urlEmptyError
                            } else if (!urlValid) {
                                urlError = urlFormatError
                            } else {
                                val current = viewModel.sources.value
                                if (current.any { it.url == sourceUrl }) {
                                    urlError = urlDuplicateError
                                } else {
                                    urlError = null
                                    addSource(sourceType, sourceUrl)
                                    sourceUrl = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        text = if (isAdding) addingSource else addSourceButton,
                        enabled = urlValid && !isAdding,
                    )
                    Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))

                    // Сообщение об успешном добавлении
                    if (addedSource == true) {
                        Text(
                            text = sourceAddedSuccess,
                            style = CyberTypography.BodyMedium,
                            color = CyberColors.AccentBlueBright,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
                    }

                    // Общая ошибка экрана
                    if (error != null && addedSource != true) {
                        Text(
                            text = error ?: "",
                            style = CyberTypography.BodySmall,
                            color = CyberColors.Error,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                    }
                }
            }
        }

        // ---------- Ошибки загрузки источников ----------
        if (sourceErrors.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
                GlassCard(borderColor = CyberColors.Error.copy(alpha = 0.4f)) {
                    Column(modifier = Modifier.padding(UiSizes.CardPadding)) {
                        Text(
                            text = "Проблемы с источниками:",
                            style = CyberTypography.BodyMedium,
                            color = CyberColors.Error,
                        )
                        sourceErrors.forEach { (url, msg) ->
                            Spacer(modifier = Modifier.height(UiSizes.SpacerXSmall))
                            Text(
                                text = "• ${url.removePrefix("https://").removePrefix("http://").take(36)}: $msg",
                                style = CyberTypography.BodySmall,
                                color = CyberColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        }

        // ---------- Мои источники ----------
        if (sources.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
                Text(
                    text = "Мои источники (${sources.size})",
                    style = CyberTypography.TitleMedium,
                    color = CyberColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
            }
            items(sources.size) { i ->
                val src = sources[i]
                val isAdded = true
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = UiSizes.SpacerXSmall),
                    borderColor = CyberColors.AccentBlue.copy(alpha = 0.3f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = UiSizes.CardPadding, vertical = UiSizes.SpacerSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = null,
                            tint = CyberColors.AccentBlueBright,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(UiSizes.SpacerMedium))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = src.url
                                    .removePrefix("https://")
                                    .removePrefix("http://")
                                    .take(48),
                                style = CyberTypography.BodyMedium,
                                color = CyberColors.TextPrimary,
                                maxLines = 1,
                            )
                            Text(
                                text = typeLabel(src.type),
                                style = CyberTypography.BodySmall,
                                color = CyberColors.TextSecondary,
                            )
                        }
                        IconButton(
                            onClick = { viewModel.removeSource(src.url); addedSource = false },
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete),
                                contentDescription = "Удалить источник",
                                tint = CyberColors.Error,
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))

            // Кнопка назад к ленте
            NeonButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
                text = backToFeed,
                enabled = !isAdding,
            )
        }
    }
}

/** Круглый чип выбора типа источника. */
@Composable
private fun SourceTypeChip(
    label: String,
    selectedType: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val selected = selectedType == when (label) {
        "Reddit" -> SourceType.REDDIT
        "YouTube" -> SourceType.YOUTUBE
        "TG" -> SourceType.TELEGRAM
        "X" -> SourceType.TWITTER
        else -> SourceType.RSS
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) accentColor else CyberColors.SurfaceElevated,
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = CyberTypography.BodySmall,
            color = if (selected) CyberColors.TextInverse else CyberColors.TextSecondary,
        )
    }
}

/** Человекочитаемая подпись типа источника. */
private fun typeLabel(type: String): String = when (type) {
    SourceType.REDDIT -> "Reddit"
    SourceType.YOUTUBE -> "YouTube"
    SourceType.TELEGRAM -> "Telegram"
    SourceType.TWITTER -> "X (Twitter)"
    else -> "RSS"
}
