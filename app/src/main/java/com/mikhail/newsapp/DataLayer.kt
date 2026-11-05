package com.mikhail.newsapp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

data class UnifiedPost(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val source: SourceType,
    val link: String,
    val videoUrl: String? = null
)

enum class SourceType(val displayName: String, val emoji: String) {
    RSS("Новости", "📰"),
    YOUTUBE("YouTube", "📺"),
    TELEGRAM("Telegram", "✈️"),
    REDDIT("Reddit", "🔴"),
    TWITTER("Twitter", "🐦")
}

data class SourceItem(
    val id: String,
    val type: SourceType,
    val url: String
)

object NetworkClient {
    private val gson = Gson()
    
    private fun extractImageUrlFromHtml(html: String?): String? {
        if (html.isNullOrEmpty()) return null
        return try {
            Regex("""<img[^>]+src=["']([^"']+)["']""")
                .find(html)?.groupValues?.get(1)?.trim()
                ?.takeIf { it.startsWith("http") }
        } catch (e: Exception) { null }
    }
    
    private fun getFallbackImage(type: SourceType): String {
        return when (type) {
            SourceType.RSS -> "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=800"
            SourceType.YOUTUBE -> "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=800"
            SourceType.TELEGRAM -> "https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=800"
            SourceType.REDDIT -> "https://images.unsplash.com/photo-1611162616305-c69b3fa7fbe0?w=800"
            SourceType.TWITTER -> "https://images.unsplash.com/photo-1611605698335-8b85b3781d76?w=800"
        }
    }
    
    suspend fun fetchRssAsJson(rssUrl: String, type: SourceType): List<UnifiedPost> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedUrl = java.net.URLEncoder.encode(rssUrl, "UTF-8")
                val apiUrl = "https://api.rss2json.com/v1/api.json?rss_url=$encodedUrl"
                
                val url = URL(apiUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"
                
                if (connection.responseCode != 200) {
                    Log.e("NetworkClient", "HTTP error: ${connection.responseCode}")
                    connection.disconnect()
                    return@withContext emptyList()
                }
                
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                
                val root = gson.fromJson(json, Map::class.java)
                if (root["status"] as? String != "ok") {
                    return@withContext emptyList()
                }
                
                val items = root["items"] as? List<Map<String, Any>> ?: emptyList()
                
                items.take(5).mapNotNull { item ->
                    try {
                        val title = (item["title"] as? String)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val link = (item["link"] as? String)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val description = (item["description"] as? String) ?: ""
                        val thumbnail = (item["thumbnail"] as? String) ?: ""
                        
                        val rawImageUrl = thumbnail ?: extractImageUrlFromHtml(description)
                        val imageUrl = if (!rawImageUrl.isNullOrEmpty() && rawImageUrl.startsWith("http")) {
                            rawImageUrl.trim().replace(" ", "%20")
                        } else {
                            getFallbackImage(type)
                        }
                        
                        UnifiedPost(
                            id = (item["guid"] as? String) ?: link,
                            title = title,
                            description = description.replace(Regex("<[^>]*>"), "").take(150),
                            imageUrl = imageUrl,
                            source = type,
                            link = link
                        )
                    } catch (e: Exception) {
                        Log.e("NetworkClient", "Parse item error: ${e.message}")
                        null
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                Log.e("NetworkClient", "Connection timed out for: $rssUrl")
                emptyList()
            } catch (e: Exception) {
                Log.e("NetworkClient", "Fetch error: ${e.message}")
                emptyList()
            }
        }
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    // БЕЗОПАСНАЯ ИНИЦИАЛИЗАЦИЯ: берем контекст напрямую из Application
    private val context = getApplication<Application>().applicationContext
    private val prefs: SharedPreferences = context.getSharedPreferences("news_app_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val _posts = MutableStateFlow<List<UnifiedPost>>(emptyList())
    val posts: StateFlow<List<UnifiedPost>> = _posts
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    
    init { 
        loadPosts() 
    }
    
    fun getSources(): List<SourceItem> {
        return try {
            val json = prefs.getString("sources", "[]") ?: "[]"
            val type = object : TypeToken<List<SourceItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Read sources error: ${e.message}")
            emptyList()
        }
    }
    
    private fun saveSources(sources: List<SourceItem>) {
        try {
            val json = gson.toJson(sources)
            prefs.edit().putString("sources", json).apply()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Save sources error: ${e.message}")
        }
    }
    
    fun getFavorites(): Set<String> {
        return try {
            prefs.getStringSet("favorites", emptySet()) ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    fun loadPosts() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                val sources = getSources()
                _posts.value = if (sources.isEmpty()) emptyList() else {
                    sources.map { NetworkClient.fetchRssAsJson(it.url, it.type) }.flatten().shuffled()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Load posts error: ${e.message}")
                _posts.value = emptyList()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    fun toggleFavorite(postId: String) {
        try {
            val current = getFavorites()
            val newFav = if (current.contains(postId)) current - postId else current + postId
            prefs.edit().putStringSet("favorites", newFav).apply()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Toggle favorite error: ${e.message}")
        }
    }
    
    fun isFavorite(postId: String): Boolean = getFavorites().contains(postId)
    
    fun addSource(type: SourceType, url: String) {
        viewModelScope.launch {
            try {
                Toast.makeText(context, "Добавляем источник...", Toast.LENGTH_SHORT).show()
                val current = getSources()
                val newSource = SourceItem("${type.name}_${System.currentTimeMillis()}", type, url)
                saveSources(current + newSource)
                Toast.makeText(context, "Источник добавлен! Загружаем новости...", Toast.LENGTH_SHORT).show()
                loadPosts()
            } catch (e: Exception) {
                // ЕСЛИ ВЫЛЕТ ЗДЕСЬ, МЫ УВИДИМ ТЕКСТ ОШИБКИ НА ЭКРАНЕ!
                Toast.makeText(context, "ОШИБКА: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("MainViewModel", "Add source CRASH: ${e.message}", e)
            }
        }
    }
    
    fun removeSource(sourceId: String) {
        viewModelScope.launch {
            try {
                val current = getSources()
                saveSources(current.filter { it.id != sourceId })
                loadPosts()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка удаления: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}