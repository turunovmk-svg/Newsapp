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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikhail.newsapp.data.Source
import com.mikhail.newsapp.data.SourceCatalog
import com.mikhail.newsapp.data.SourceType
import com.mikhail.newsapp.theme.CyberColors
import com.mikhail.newsapp.theme.CyberTypography
import com.mikhail.newsapp.theme.GlassCard
import com.mikhail.newsapp.theme.NeonButton
import com.mikhail.newsapp.theme.RefCatalogRow
import com.mikhail.newsapp.theme.RefChip
import com.mikhail.newsapp.theme.RefSectionHeader
import com.mikhail.newsapp.UiSizes

@Composable
fun AddSourceScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    var sourceUrl by remember { mutableStateOf("") }
    var sourceFolder by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }
    var addedSource by remember { mutableStateOf<Boolean?>(null) }

    // Поиск и фильтр каталога
    var catalogQuery by remember { mutableStateOf("") }
    var catalogCategory by remember { mutableStateOf<String?>(null) }

    val error by viewModel.error.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val sourceErrors by viewModel.sourceErrors.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val context = LocalContext.current

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

    val catalogResults = remember(catalogQuery, catalogCategory, sources) {
        SourceCatalog.search(catalogQuery, catalogCategory)
    }

    val categoryTabs = listOf(
        null to "Все",
        SourceCatalog.CAT_WEBSITE to "Сайты",
        SourceCatalog.CAT_YOUTUBE to "YouTube",
        SourceCatalog.CAT_REDDIT to "Reddit",
        SourceCatalog.CAT_TELEGRAM to "Telegram",
    )

    fun addFromCatalog(url: String) {
        isAdding = true
        viewModel.addSource(SourceType.RSS, url, sourceFolder)
        addedSource = true
        isAdding = false
    }

    fun addManual(type: String, url: String) {
        val normalized = Source.detect(url, type)?.url?.trim()
        if (normalized != null && viewModel.sources.value.any { it.url == normalized }) {
            urlError = urlDuplicateError
            isAdding = false
            return
        }
        isAdding = true
        viewModel.addSource(type, url, sourceFolder)
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
        // ---------- Шапка ----------
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = UiSizes.SpacerSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = stringResource(id = R.string.back),
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

        // ---------- Поиск по каталогу ----------
        item {
            OutlinedTextField(
                value = catalogQuery,
                onValueChange = { catalogQuery = it },
                label = { Text("Найти источник...") },
                placeholder = { Text("Название или описание", style = CyberTypography.BodySmall) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = "Поиск",
                        tint = CyberColors.TextSecondary
                    )
                },
                trailingIcon = {
                    if (catalogQuery.isNotEmpty()) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Очистить",
                            tint = CyberColors.TextSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { catalogQuery = "" }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))

            // Фильтры категорий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
            ) {
                categoryTabs.take(3).forEach { (cat, label) ->
                    RefChip(
                        label = label,
                        selected = catalogCategory == cat,
                        onClick = { catalogCategory = cat },
                    )
                }
            }
            Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UiSizes.SpacerSmall)
            ) {
                categoryTabs.drop(3).forEach { (cat, label) ->
                    RefChip(
                        label = label,
                        selected = catalogCategory == cat,
                        onClick = { catalogCategory = cat },
                    )
                }
            }
        }

        // ---------- Результаты каталога ----------
        item {
            Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
            RefSectionHeader(
                title = if (catalogQuery.isBlank()) "Каталог источников" else "Результаты поиска",
                trailing = "${catalogResults.size}"
            )
        }

        items(
            items = catalogResults,
            key = { it.url }
        ) { catalogSource ->
            val following = remember(sources) {
                SourceCatalog.isFollowing(catalogSource, sources)
            }
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                borderColor = if (following)
                    CyberColors.Success.copy(alpha = 0.35f)
                else
                    CyberColors.BorderGlass,
            ) {
                RefCatalogRow(
                    source = catalogSource,
                    isFollowing = following,
                    onFollowClick = { addFromCatalog(catalogSource.url) }
                )
            }
        }

        if (catalogResults.isEmpty()) {
            item {
                Text(
                    text = "Ничего не найдено. Попробуйте другой запрос или добавьте ссылку вручную ниже.",
                    style = CyberTypography.BodyMedium,
                    color = CyberColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = UiSizes.SpacerMedium)
                )
            }
        }

        // ---------- Ручной ввод ----------
        item {
            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
            RefSectionHeader(title = "Или вставьте ссылку")
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = CyberColors.BorderGlassBright,
            ) {
                Column(modifier = Modifier.padding(UiSizes.CardPadding)) {
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
                    val detectedType = remember(sourceUrl) {
                        if (sourceUrl.isBlank()) null else Source.detect(sourceUrl)?.type
                    }
                    if (detectedType != null) {
                        Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                        Text(
                            text = stringResource(id = R.string.type_detected) + typeLabel(detectedType),
                            style = CyberTypography.BodySmall,
                            color = CyberColors.AccentBlueBright,
                        )
                    }
                    Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
                    OutlinedTextField(
                        value = sourceFolder,
                        onValueChange = { sourceFolder = it },
                        label = { Text("Папка (необязательно)") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_link),
                                contentDescription = "Папка",
                                tint = CyberColors.TextSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    if (urlError != null) {
                        Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                        Text(
                            text = urlError ?: "",
                            style = CyberTypography.BodySmall,
                            color = CyberColors.Error,
                        )
                    }
                    if (sourceUrl.isNotEmpty() && !urlValid && urlError == null) {
                        Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                        Text(
                            text = stringResource(id = R.string.url_format_error),
                            style = CyberTypography.BodySmall,
                            color = CyberColors.TextMuted,
                        )
                    }
                    Spacer(modifier = Modifier.height(UiSizes.SpacerMedium))
                    NeonButton(
                        onClick = {
                            when {
                                sourceUrl.isBlank() -> urlError = urlEmptyError
                                !urlValid -> urlError = urlFormatError
                                else -> {
                                    urlError = null
                                    addManual(SourceType.RSS, sourceUrl)
                                    sourceUrl = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        text = if (isAdding) addingSource else addSourceButton,
                        enabled = urlValid && !isAdding,
                    )
                    if (addedSource == true) {
                        Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                        Text(
                            text = sourceAddedSuccess,
                            style = CyberTypography.BodySmall,
                            color = CyberColors.Success,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (error != null && addedSource != true) {
                        Spacer(modifier = Modifier.height(UiSizes.SpacerSmall))
                        Text(
                            text = error ?: "",
                            style = CyberTypography.BodySmall,
                            color = CyberColors.Error,
                        )
                    }
                }
            }
        }

        // ---------- Ошибки источников ----------
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
                RefSectionHeader(title = "Мои источники", trailing = "${sources.size}")
            }
            items(
                items = sources,
                key = { it.url }
            ) { src ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    borderColor = CyberColors.AccentBlue.copy(alpha = 0.3f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = UiSizes.SpacerMedium, vertical = UiSizes.SpacerSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(UiSizes.RefLogoCircle)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CyberColors.AccentBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (src.label ?: src.url).take(1).uppercase(),
                                style = CyberTypography.TitleMedium,
                                color = CyberColors.AccentBlue
                            )
                        }
                        Spacer(modifier = Modifier.width(UiSizes.SpacerMedium))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = src.label ?: src.url.removePrefix("https://").removePrefix("http://"),
                                style = CyberTypography.BodyLarge,
                                color = CyberColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = src.url.removePrefix("https://").removePrefix("http://"),
                                style = CyberTypography.BodySmall,
                                color = CyberColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "Удалить источник",
                            tint = CyberColors.Error,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { viewModel.removeSource(src.url) }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
            NeonButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
                text = backToFeed,
                enabled = !isAdding,
            )
            Spacer(modifier = Modifier.height(UiSizes.SpacerLarge))
        }
    }
}

/** Человекочитаемое название типа источника. */
internal fun typeLabel(type: String): String = when (type) {
    SourceType.REDDIT -> "Reddit"
    SourceType.YOUTUBE -> "YouTube"
    SourceType.TELEGRAM -> "Telegram-канал"
    SourceType.TWITTER -> "X (Twitter)"
    SourceType.GNEWS -> "Сайт"
    else -> "RSS"
}
