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
                val apiUrl = "https://api.rss2json.com/v1/api.json?rss_url=${java.net.URLEncoder.encode(rssUrl, "UTF-8")}"
                val json = URL(apiUrl).readText()
                val root = gson.fromJson(json, Map::class.java)
                val items = root["items"] as? List<Map<String, Any>> ?: emptyList()
                
                items.take(8).mapNotNull { item ->
                    try {
                        val title = item["title"]?.toString() ?: return@mapNotNull null
                        val link = item["link"]?.toString() ?: return@mapNotNull null
                        val description = item["description"]?.toString() ?: ""
                        val enclosure = item["enclosure"] as? Map<*, *>
                        val thumbnail = item["thumbnail"]?.toString()
                        val rawImageUrl = enclosure?.get("link")?.toString() ?: thumbnail ?: extractImageUrlFromHtml(description)
                        val imageUrl = if (!rawImageUrl.isNullOrEmpty() && rawImageUrl.startsWith("http")) {
                            rawImageUrl.trim().replace(" ", "%20")
                        } else { getFallbackImage(type) }
                        
                        UnifiedPost(
                            id = item["guid"]?.toString() ?: link,
                            title = title,
                            description = description.replace(Regex("<[^>]*>"), "").take(200),
                            imageUrl = imageUrl,
                            source = type,
                            link = link,
                            videoUrl = if (type == SourceType.YOUTUBE) link else null
                        )
                    } catch (e: Exception) { null }
                }
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
                Log.e("NewsApp", "Error in addSource: ${e.message}", e)
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
                Log.e("MainViewModel", "Remove source error: ${e.message}")
            }
        }
    }
}