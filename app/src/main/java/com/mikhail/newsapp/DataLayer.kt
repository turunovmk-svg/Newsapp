package com.mikhail.newsapp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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

object AppPrefs {
    private const val PREFS_NAME = "news_app_prefs"
    private const val KEY_SOURCES = "sources"
    private const val KEY_FAVORITES = "favorites"
    
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun saveSources(sources: List<SourceItem>) {
        try {
            val json = Gson().toJson(sources)
            prefs.edit().putString(KEY_SOURCES, json).apply()
        } catch (e: Exception) {
            Log.e("AppPrefs", "Save sources error: ${e.message}")
        }
    }
    
    fun getSources(): List<SourceItem> {
        return try {
            val json = prefs.getString(KEY_SOURCES, "[]") ?: "[]"
            val type = object : TypeToken<List<SourceItem>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("AppPrefs", "Read sources error: ${e.message}")
            emptyList()
        }
    }
    
    fun saveFavorites(favorites: Set<String>) {
        try {
            prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
        } catch (e: Exception) {
            Log.e("AppPrefs", "Save favorites error: ${e.message}")
        }
    }
    
    fun getFavorites(): Set<String> {
        return try {
            prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        } catch (e: Exception) {
            Log.e("AppPrefs", "Read favorites error: ${e.message}")
            emptySet()
        }
    }
}

class MainViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val _posts = MutableStateFlow<List<UnifiedPost>>(emptyList())
    val posts: StateFlow<List<UnifiedPost>> = _posts
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    
    init { 
        AppPrefs.init(application)
        loadPosts() 
    }
    
    fun loadPosts() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                val sources = AppPrefs.getSources()
                Log.d("NewsApp", "Loading posts for ${sources.size} sources")
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
            val current = AppPrefs.getFavorites()
            val newFav = if (current.contains(postId)) current - postId else current + postId
            AppPrefs.saveFavorites(newFav)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Toggle favorite error: ${e.message}")
        }
    }
    
    fun isFavorite(postId: String): Boolean = AppPrefs.getFavorites().contains(postId)
    
    fun addSource(type: SourceType, url: String) {
        viewModelScope.launch {
            try {
                Log.d("NewsApp", "Adding source: $type - $url")
                val current = AppPrefs.getSources()
                val newSource = SourceItem("${type.name}_${System.currentTimeMillis()}", type, url)
                AppPrefs.saveSources(current + newSource)
                Log.d("NewsApp", "Source saved, reloading posts")
                loadPosts()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Add source error: ${e.message}", e)
            }
        }
    }
    
    fun removeSource(sourceId: String) {
        viewModelScope.launch {
            try {
                val current = AppPrefs.getSources()
                AppPrefs.saveSources(current.filter { it.id != sourceId })
                loadPosts()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Remove source error: ${e.message}", e)
            }
        }
    }
}