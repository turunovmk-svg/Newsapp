package com.mikhail.newsapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
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
    TELEGRAM("Telegram", "✈️")
}

object NetworkClient {
    private val gson = Gson()
    
    private fun extractImageUrlFromHtml(html: String?): String? {
        if (html.isNullOrEmpty()) return null
        
        return try {
            val regex = Regex("""<img[^>]+src=["']([^"']+)["']""")
            regex.find(html)?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            Log.e("RSS_DEBUG", "Error extracting image: ${e.message}")
            null
        }
    }
    
    private fun getFallbackImage(type: SourceType): String {
        return when (type) {
            SourceType.RSS -> "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=800"
            SourceType.YOUTUBE -> "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=800"
            SourceType.TELEGRAM -> "https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=800"
        }
    }
    
    suspend fun fetchRssAsJson(rssUrl: String, type: SourceType): List<UnifiedPost> {
        return withContext(Dispatchers.IO) {
            try {
                val apiUrl = "https://api.rss2json.com/v1/api.json?rss_url=${java.net.URLEncoder.encode(rssUrl, "UTF-8")}"
                Log.d("RSS_DEBUG", "Fetching: $apiUrl")
                
                val json = URL(apiUrl).readText()
                val root = gson.fromJson(json, Map::class.java)
                val items = root["items"] as? List<Map<String, Any>> ?: emptyList()
                
                Log.d("RSS_DEBUG", "Found ${items.size} items for $type")
                
                items.take(8).mapNotNull { item ->
                    try {
                        val title = item["title"]?.toString() ?: return@mapNotNull null
                        val link = item["link"]?.toString() ?: return@mapNotNull null
                        val description = item["description"]?.toString() ?: ""
                        
                        val enclosure = item["enclosure"] as? Map<*, *>
                        val thumbnail = item["thumbnail"]?.toString()
                        
                        val rawImageUrl = enclosure?.get("link")?.toString()
                            ?: thumbnail
                            ?: extractImageUrlFromHtml(description)
                        
                        val imageUrl = if (!rawImageUrl.isNullOrEmpty() && rawImageUrl.startsWith("http")) {
                            rawImageUrl.trim().replace(" ", "%20")
                        } else {
                            getFallbackImage(type)
                        }
                        
                        Log.d("RSS_DEBUG", "Title: $title")
                        Log.d("RSS_DEBUG", "Image URL: $imageUrl")
                        
                        UnifiedPost(
                            id = item["guid"]?.toString() ?: link,
                            title = title,
                            description = description.replace(Regex("<[^>]*>"), "").take(200),
                            imageUrl = imageUrl,
                            source = type,
                            link = link,
                            videoUrl = if (type == SourceType.YOUTUBE) link else null
                        )
                    } catch (e: Exception) {
                        Log.e("RSS_DEBUG", "Error parsing item: ${e.message}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("RSS_DEBUG", "Error fetching RSS: ${e.message}")
                emptyList()
            }
        }
    }
}

object UnifiedRepository {
    suspend fun getAllPosts(): List<UnifiedPost> {
        val rssUrl = "https://lenta.ru/rss/news"
        val youtubeUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=UC_x5XG1OV2P6uZZ5FSM9Ttw"
        
        val telegramUrls = listOf(
            "https://rsshub.app/telegram/channel/telegram",
            "https://rsshub.rssforever.com/telegram/channel/telegram",
            "https://rss.fatpandac.com/telegram/channel/telegram"
        )
        
        Log.d("RSS_DEBUG", "Starting to fetch all posts...")
        
        val rssPosts = NetworkClient.fetchRssAsJson(rssUrl, SourceType.RSS)
        val ytPosts = NetworkClient.fetchRssAsJson(youtubeUrl, SourceType.YOUTUBE)
        
        var tgPosts = emptyList<UnifiedPost>()
        for (url in telegramUrls) {
            tgPosts = NetworkClient.fetchRssAsJson(url, SourceType.TELEGRAM)
            if (tgPosts.isNotEmpty()) break
        }
        
        Log.d("RSS_DEBUG", "RSS: ${rssPosts.size}, YouTube: ${ytPosts.size}, Telegram: ${tgPosts.size}")
        
        if (rssPosts.isEmpty() && ytPosts.isEmpty() && tgPosts.isEmpty()) {
            Log.d("RSS_DEBUG", "All sources empty, using fallback")
            return getFallbackData()
        }
        
        return (rssPosts + ytPosts + tgPosts).shuffled()
    }
    
    private fun getFallbackData(): List<UnifiedPost> {
        return listOf(
            UnifiedPost("1", "Нет интернета", "Показываем тестовые данные", 
                "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=800", 
                SourceType.RSS, ""),
            UnifiedPost("2", "YouTube канал", "Тестовое видео", 
                "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=800", 
                SourceType.YOUTUBE, ""),
            UnifiedPost("3", "Telegram пост", "Тестовый пост", 
                "https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=800", 
                SourceType.TELEGRAM, "")
        )
    }
}

class MainViewModel : ViewModel() {
    private val _posts = MutableStateFlow<List<UnifiedPost>>(emptyList())
    val posts: StateFlow<List<UnifiedPost>> = _posts
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    
    init {
        loadPosts()
    }
    
    fun loadPosts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _posts.value = UnifiedRepository.getAllPosts()
            _isRefreshing.value = false
        }
    }
}
