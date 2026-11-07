package com.mikhail.newsapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddSourceScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    var sourceType by remember { mutableStateOf("RSS") }
    var sourceUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Добавить источник",
            style = MaterialTheme.typography.headlineSmall
        )

        // Поле типа источника
        OutlinedTextField(
            value = sourceType,
            onValueChange = { sourceType = it.toUpperCase() },
            label = { Text("Тип (RSS / REDDIT / YOUTUBE)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions()
        )

        // Поле URL
        OutlinedTextField(
            value = sourceUrl,
            onValueChange = { sourceUrl = it },
            label = { Text("URL источника") },
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
            Text("Добавить источник")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Популярные источники
        Text(
            text = "Популярные источники:",
            style = MaterialTheme.typography.titleMedium
        )

        val popularSources = listOf(
            PopularSource("BBC Украина", "RSS", "https://feeds.bbci.co.uk/ukrainian/rss.xml"),
            PopularSource("Украинская правда", "RSS", "https://www.pravda.com.ua/rss/"),
            PopularSource("Reddit World News", "REDDIT", "https://www.reddit.com/r/worldnews/.rss"),
            PopularSource("YouTube Tech", "YOUTUBE", "https://www.youtube.com/feeds/videos.xml?channel_id=UC_x5XG1OV2P6uZZ5FSM9Ttw")
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
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onNavigateBack) {
            Text("Назад")
        }
    }
}

private data class PopularSource(
    val name: String,
    val type: String,
    val url: String
)
