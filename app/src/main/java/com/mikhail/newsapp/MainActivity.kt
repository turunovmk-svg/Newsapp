package com.mikhail.newsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Arrangement
import androidx.compose.foundation.layout.Arrangement.Horizontal
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.runtime.experimental.optin.OptIn
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

// Data model for news article
data class NewsArticle(
    val id: String,
    val title: String,
    val description: String?,
    val publishedAt: String?
)



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// ... (остальная часть класса MainActivity остается без изменений)

// Composable screen that displays news with PullToRefresh
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsAppScreen() {
    var scrollState by rememberScrollState()
    val context = LocalContext.current
    val articles by remember { mutableStateOf<List<NewsArticle>?>(null) }
    val prefsManager = remember { SharedPreferencesManager(context) }
    
    // ... (DisposableEffect остается без изменений)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.background
    ) {
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            defaultDefaults = PullToRefreshDefaults.Default,
            // Header shown when pulled down
            header = { pullToRefresh ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(MaterialTheme.primaryColor),
                    color = MaterialTheme.primaryColor
                ) {
                    Text(
                        text = "Pull down to refresh",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.onPrimary
                    )
                }
            }
        ) {
            ScrollColumn(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                if (articles == null || articles.isEmpty()) {
                    Text(
                        text = "Loading news...",
                        modifier = Modifier.padding(32.dp)
                    )
                } else {
                    articles.take(10).forEach { article ->
                        // --- НОВАЯ ЛОГИКА ИЗБРАННОГО ---
                        val isFav = prefsManager.isFavorite(article.id)
                        
                        Surface(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                                .background(MaterialTheme.surfaceColor),
                            color = MaterialTheme.background
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = article.title,
                                    style = MaterialTheme.typography.h6,
                                    modifier = Modifier.wrapContentWidth(100.dp)
                                )
                                Text(
                                    text = article.description ?: "No description",
                                    style = MaterialTheme.typography.body2,
                                    maxLines = 3,
                                    overflow = Overflow.Clip
                                )
                                Text(
                                    text = "Published: " + article.publishedAt,
                                    style = MaterialTheme.typography.caption,
                                    textColor = MaterialTheme.onSurfaceVariant
                                )
                                // Кнопка избранного (простая)
                                IconButton(onClick = { prefsManager.toggleFavorite(article.id) }) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Toggle Favorite",
                                        tint = if (isFav) MaterialTheme.colorScheme.Primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simple article parser using string operations
fun parseArticles(json: String): List<NewsArticle> {
    val result = mutableListOf<NewsArticle>()
    // Very simple parsing - extract title, description, publishedAt between known markers
    val titleStart = "\"title\":\""
    val descStart = "\"description\":\""
    val dateStart = "\"publishedAt\":\""
    var pos = 0

    while (pos < json.length && result.size < 20) {
        var titleEnd = json.indexOf("\"", json.indexOf(titleStart, pos) + titleStart.length)
        if (titleEnd < 0) break
        val title = json.substring(json.indexOf(titleStart, pos) + titleStart.length, titleEnd)

        var descEnd = json.indexOf("\"", json.indexOf(descStart, pos) + descStart.length)
        val description = if (descEnd > 0) {
            json.substring(json.indexOf(descStart, pos) + descStart.length, descEnd)
        } else {
            ""
        }

        var dateEnd = json.indexOf("\"", json.indexOf(dateStart, pos) + dateStart.length)
        val publishedAt = if (dateEnd > 0) {
            json.substring(json.indexOf(dateStart, pos) + dateStart.length, dateEnd)
        } else {
            ""
        }

        result.add(NewsArticle(title = title, description = description, publishedAt = publishedAt))

        pos = titleEnd + 1
    }
    return result
}