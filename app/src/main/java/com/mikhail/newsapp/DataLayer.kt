package com.mikhail.newsapp

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "news_app")

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
            } catch (e: Exception) { emptyList() }
        }
    }
}

object DataStoreManager {
    lateinit var dataStore: DataStore<Preferences>
    
    fun init(context: Context) {
        dataStore = context.dataStore
    }
    
    suspend fun saveSources(sources: List<SourceItem>) {
        val json = Gson().toJson(sources)
        dataStore.edit { it[stringPreferencesKey("sources")] = json }
    }
    
    fun getSources(): Flow<List<SourceItem>> {
        return dataStore.data.map { prefs ->
            val json = prefs[stringPreferencesKey("sources")] ?: "[]"
            Gson().fromJson(json, object : TypeToken<List<SourceItem>>() {}.type) ?: emptyList()
        }
    }
    
    suspend fun saveFavorites(favorites: Set<String>) {
        val json = Gson().toJson(favorites)
        dataStore.edit { it[stringPreferencesKey("favorites")] = json }
    }
    
    fun getFavorites(): Flow<Set<String>> {
        return dataStore.data.map { prefs ->
            val json = prefs[stringPreferencesKey("favorites")] ?: "[]"
            Gson().fromJson(json, object : TypeToken<Set<String>>() {}.type) ?: emptySet()
        }
    }
}

class MainViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val _posts = MutableStateFlow<List<UnifiedPost>>(emptyList())
    val posts: StateFlow<List<UnifiedPost>> = _posts
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    
    val userSources = DataStoreManager.getSources().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val favorites = DataStoreManager.getFavorites().stateIn(viewModelScope, SharingStarted.Lazily, emptySet())
    
    init { loadPosts() }
    
    fun loadPosts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val sources = userSources.value
            _posts.value = if (sources.isEmpty()) emptyList() else {
                sources.map { NetworkClient.fetchRssAsJson(it.url, it.type) }.flatten().shuffled()
            }
            _isRefreshing.value = false
        }
    }
    
    fun toggleFavorite(postId: String) {
        viewModelScope.launch {
            val current = favorites.value
            val newFav = if (current.contains(postId)) current - postId else current + postId
            DataStoreManager.saveFavorites(newFav)
        }
    }
    
    fun isFavorite(postId: String): Boolean = favorites.value.contains(postId)
    
    fun addSource(type: SourceType, url: String) {
        viewModelScope.launch {
            val current = userSources.value
            val newSource = SourceItem("${type.name}_${System.currentTimeMillis()}", type, url)
            DataStoreManager.saveSources(current + newSource)
            loadPosts()
        }
    }
    
    fun removeSource(sourceId: String) {
        viewModelScope.launch {
            val current = userSources.value
            DataStoreManager.saveSources(current.filter { it.id != sourceId })
            loadPosts()
        }
    }
}