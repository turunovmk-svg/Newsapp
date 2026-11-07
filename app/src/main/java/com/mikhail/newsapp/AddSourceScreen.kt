package com.mikhail.newsapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mikhail.newsapp.UiSizes

@Composable
fun AddSourceScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    var sourceType by remember { mutableStateOf("RSS") }
    var sourceUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(UiSizes.SourceSectionSpacing),
        verticalArrangement = Arrangement.spacedBy(UiSizes.SourceFieldSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.add_source_title),
            style = MaterialTheme.typography.headlineSmall
        )

        // Поле типа источника
        OutlinedTextField(
            value = sourceType,
            onValueChange = { sourceType = it.uppercase() },
            label = { Text(stringResource(id = R.string.source_type_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions()
        )

        // Поле URL
        OutlinedTextField(
            value = sourceUrl,
            onValueChange = { sourceUrl = it },
            label = { Text(stringResource(id = R.string.source_url_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )

        // Кнопка добавления вручную
        Button(
            onClick = {
                if (sourceUrl.isNotEmpty()) {
                    viewModel.addSource(sourceType, sourceUrl)
                    sourceUrl = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.add_source_button))
        }

        Spacer(modifier = Modifier.height(UiSizes.SourceSectionSpacing))

        // Популярные источники
        Text(
            text = stringResource(id = R.string.popular_sources_title),
            style = MaterialTheme.typography.titleMedium
        )

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
                OutlinedButton(
                    onClick = { viewModel.addSource(source.type, source.url) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = source.name)
                }
                Spacer(modifier = Modifier.height(UiSizes.SourceButtonSpacing))
            }
        }

        Spacer(modifier = Modifier.height(UiSizes.SourceBottomSpacing))

        Button(onClick = onNavigateBack) {
            Text(stringResource(id = R.string.back))
        }
    }
}

private data class PopularSource(
    val name: String,
    val type: String,
    val url: String
)
