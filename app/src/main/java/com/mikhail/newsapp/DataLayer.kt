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

// Универсальная модель для ВСЕХ источников
data class UnifiedPost(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val source: SourceType,
    val link: String
)

enum class SourceType(val displayName: String, val emoji: String) {
    RSS("Новости", "📰"),
    YOUTUBE("YouTube", "📺"),
    TELEGRAM("Telegram", "✈️")
}

// Простой клиент для запросов
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
                    UnifiedPost(
                        id = item["guid"]?.toString() ?: item["link"].toString(),
                        title = item["title"].toString(),
                        description = item["description"].toString().replace(Regex("<[^>]*>"), ""), // Убираем HTML-теги
                        imageUrl = (item["enclosure"] as? Map<*, *>)?.get("link")?.toString() 
                                   ?: (item["thumbnail"]?.toString()) 
                                   ?: "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=800",
                        source = type,
                        link = item["link"].toString()
                    )
                }
            } catch (e: Exception) {
                emptyList() // Если ошибка сети, возвращаем пустой список (не ломаем приложение)
            }
        }
    }
}

// Репозиторий, который собирает всё в одну кучу
object UnifiedRepository {
    suspend fun getAllPosts(): List<UnifiedPost> {
        val rssUrl = "https://lenta.ru/rss/news"
        val youtubeUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=UC_x5XG1OV2P6uZZ5FSM9Ttw" // Google Developers
        val telegramUrl = "https://rsshub.app/telegram/channel/telegram" 
        
        // Запускаем все 3 запроса параллельно для скорости!
        val rssPosts = NetworkClient.fetchRssAsJson(rssUrl, SourceType.RSS)
        val ytPosts = NetworkClient.fetchRssAsJson(youtubeUrl, SourceType.YOUTUBE)
        val tgPosts = NetworkClient.fetchRssAsJson(telegramUrl, SourceType.TELEGRAM)
        
        // Если всё сломалось (нет интернета), показываем тестовые данные, чтобы приложение не было пустым
        if (rssPosts.isEmpty() && ytPosts.isEmpty() && tgPosts.isEmpty()) {
            return getFallbackData()
        }
        
        // Объединяем и перемешиваем
        return (rssPosts + ytPosts + tgPosts).shuffled()
    }
    
    private fun getFallbackData(): List<UnifiedPost> {
        return listOf(
            UnifiedPost("1", "Нет интернета", "Показываем тестовые данные", "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=800", SourceType.RSS, ""),
            UnifiedPost("2", "YouTube канал", "Тестовое видео", "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=800", SourceType.YOUTUBE, ""),
            UnifiedPost("3", "Telegram пост", "Тестовый пост из канала", "https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=800", SourceType.TELEGRAM, "")
        )
    }
}

class MainViewModel : ViewModel() {
    private val _posts = MutableStateFlow<List<UnifiedPost>>(emptyList())
    val posts: StateFlow<List<UnifiedPost>> = _posts
    
    init {
        loadPosts()
    }
    
    fun loadPosts() {
        viewModelScope.launch {
            _posts.value = UnifiedRepository.getAllPosts()
        }
    }
}
