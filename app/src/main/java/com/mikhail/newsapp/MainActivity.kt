package com.mikhail.newsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.IconButton
import android.content.Context
import android.content.SharedPreferences

// Data model for news article
data class NewsArticle(
    val id: String,
    val title: String,
    val description: String?,
    val publishedAt: String?
)

// Main Activity with Jetpack Compose UI
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewsAppScreen()
        }
    }
}

// Composable screen that displays news with PullToRefresh
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsAppScreen() {
    var scrollState by rememberScrollState()
    val context = LocalContext.current
    val articles by remember { mutableStateOf<List<NewsArticle>?>(null) }
    val prefsManager = remember { SharedPreferencesManager(context) }

    // Load news from API on first composition
    DisposableEffect(Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://newsapi.org/v2/top-headlines?country=us&category=technology")
            .build()
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val json = response.body.string()
                articles = parseArticles(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
                                    overflow = androidx.compose.ui.text.style.Overflow.Clip
                                )
                                Text(
                                    text = "Published: " + article.publishedAt,
                                    style = MaterialTheme.typography.caption,
                                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // Кнопка избранного (простая)
                                IconButton(onClick = { prefsManager.toggleFavorite(article.id) }) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Toggle Favorite",
                                        tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
        // *** ВАЖНО: Присваиваем ID, используя комбинацию полей, так как реального ID в парсере нет ***
        val articleId = title.hashCode().toString() 
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

        result.add(NewsArticle(id = articleId, title = title, description = description, publishedAt = publishedAt))

        pos = titleEnd + 1
    }
    return result
}