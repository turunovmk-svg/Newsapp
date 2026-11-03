package com.mikhail.newsapp

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
    
    suspend fun fetchRssAsJson(rssUrl: String, type: SourceType): List<UnifiedPost> {
        return withContext(Dispatchers.IO) {
            try {
                val apiUrl = "https://api.rss2json.com/v1/api.json?rss_url=${java.net.URLEncoder.encode(rssUrl, "UTF-8")}"
                val json = URL(apiUrl).readText()
                val root = gson.fromJson(json, Map::class.java)
                val items = root["items"] as? List<Map<String, Any>> ?: emptyList()
                
                items.take(5).map { item ->
                    val enclosure = item["enclosure"] as? Map<*, *>
                    val thumbnail = item["thumbnail"]?.toString()
                    val imageUrl = enclosure?.get("link")?.toString() 
                        ?: thumbnail 
                        ?: "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=800"
                    
                    UnifiedPost(
                        id = item["guid"]?.toString() ?: item["link"].toString(),
                        title = item["title"].toString(),
                        description = item["description"].toString().replace(Regex("<[^>]*>"), ""),
                        imageUrl = imageUrl,
                        source = type,
                        link = item["link"].toString(),
                        videoUrl = if (type == SourceType.YOUTUBE) item["link"].toString() else null
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

object UnifiedRepository {
    suspend fun getAllPosts(): List<UnifiedPost> {
        val rssUrl = "https://lenta.ru/rss/news"
        val youtubeUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=UC_x5XG1OV2P6uZZ5FSM9Ttw"
        val telegramUrl = "https://rsshub.app/telegram/channel/telegram"
        
        val rssPosts = NetworkClient.fetchRssAsJson(rssUrl, SourceType.RSS)
        val ytPosts = NetworkClient.fetchRssAsJson(youtubeUrl, SourceType.YOUTUBE)
        val tgPosts = NetworkClient.fetchRssAsJson(telegramUrl, SourceType.TELEGRAM)
        
        if (rssPosts.isEmpty() && ytPosts.isEmpty() && tgPosts.isEmpty()) {
            return getFallbackData()
        }
        
        return (rssPosts + ytPosts + tgPosts).shuffled()
    }
    
    private fun getFallbackData(): List<UnifiedPost> {
        return listOf(
            UnifiedPost("1", "Нет интернета", "Показываем тестовые данные", "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=800", SourceType.RSS, ""),
            UnifiedPost("2", "YouTube канал", "Тестовое видео", "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=800", SourceType.YOUTUBE, ""),
            UnifiedPost("3", "Telegram пост", "Тестовый пост", "https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=800", SourceType.TELEGRAM, "")
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
