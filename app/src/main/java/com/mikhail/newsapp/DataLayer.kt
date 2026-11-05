package com.mikhail.newsapp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.InputSource

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
                if (rssUrl.isNullOrBlank() || !rssUrl.startsWith("http")) {
                    Log.e("NetworkClient", "Invalid URL: $rssUrl")
                    return@withContext emptyList()
                }
                
                val encodedUrl = java.net.URLEncoder.encode(rssUrl, "UTF-8")
                val apiUrl = "https://api.rss2json.com/v1/api.json?rss_url=$encodedUrl"
                
                val url = URL(apiUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"
                connection.addRequestProperty("User-Agent", "Mozilla/5.0")
                
                if (connection.responseCode != 200) {
                    Log.e("NetworkClient", "HTTP error: ${connection.responseCode} for $rssUrl")
                    connection.disconnect()
                    return@withContext emptyList()
                }
                
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                
                if (json.isNullOrBlank()) {
                    Log.e("NetworkClient", "Empty response for: $rssUrl")
                    return@withContext emptyList()
                }
                
                val root = gson.fromJson(json, Map::class.java)
                if (root == null || root["status"] as? String != "ok") {
                    Log.e("NetworkClient", "API status not ok for: $rssUrl")
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
                        
                        val postId = (item["guid"] as? String) ?: link
                        
                        UnifiedPost(
                            id = postId,
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
            } catch (e: java.net.MalformedURLException) {
                Log.e("NetworkClient", "Malformed URL: $rssUrl")
                emptyList()
            } catch (e: Exception) {
                Log.e("NetworkClient", "Fetch error: ${e.message}")
                emptyList()
            }
        }
    }
}

class MainViewModel(private val appContext: Context) : ViewModel() {
    private val prefs: SharedPreferences = appContext.getSharedPreferences("news_app_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val _sources = MutableStateFlow<List<SourceItem>>(emptyList())
    val sources: StateFlow<List<SourceItem>> = _sources
    
    private val _posts = MutableStateFlow<List<UnifiedPost>>(emptyList())
    val posts: StateFlow<List<UnifiedPost>> = _posts
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    
    private val _selectedPost = MutableStateFlow<UnifiedPost?>(null)
    val selectedPost: StateFlow<UnifiedPost?> = _selectedPost
    
    init { 
        _sources.value = loadSourcesFromPrefs()
        _posts.value = emptyList()
        _isRefreshing.value = false
        _selectedPost.value = null
    }
    
    private fun loadSourcesFromPrefs(): List<SourceItem> {
        return try {
            val json = prefs.getString("sources", "[]") ?: "[]"
            if (json.isNullOrBlank()) return emptyList()
            val type = object : TypeToken<List<SourceItem>>() {}.type
            val result = gson.fromJson(json, type) as? List<SourceItem>
            if (result == null) emptyList() else result
        } catch (e: Exception) {
            Log.e("MainViewModel", "Read sources error: ${e.message}")
            emptyList()
        }
    }
    
    fun getSources(): List<SourceItem> = _sources.value
    
    private fun saveSources(sources: List<SourceItem>) {
        try {
            val json = gson.toJson(sources)
            prefs.edit().putString("sources", json).apply()
            _sources.value = sources
        } catch (e: Exception) {
            Log.e("MainViewModel", "Save sources error: ${e.message}")
        }
    }
    
    fun getFavorites(): Set<String> {
        return try {
            val set = prefs.getStringSet("favorites", emptySet())
            if (set == null) emptySet() else set.toMutableSet()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Read favorites error: ${e.message}")
            emptySet()
        }
    }
    
    fun loadPosts() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                val currentSources = _sources.value
                if (currentSources.isEmpty()) {
                    _posts.value = emptyList()
                    return@launch
                }
                
                val allPosts = mutableListOf<UnifiedPost>()
                for (source in currentSources) {
                    try {
                        val fetched = NetworkClient.fetchRssAsJson(source.url, source.type)
                        allPosts.addAll(fetched)
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Fetch failed for source ${source.id}: ${e.message}")
                    }
                }
                
                _posts.value = allPosts.distinctBy { it.id }.shuffled()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Load posts error: ${e.message}")
                _posts.value = emptyList()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    fun toggleFavorite(postId: String) {
        viewModelScope.launch {
            try {
                val current = getFavorites()
                val newFav = if (current.contains(postId)) current - postId else current + postId
                if (newFav.isNotEmpty()) {
                    prefs.edit().putStringSet("favorites", newFav).apply()
                } else {
                    prefs.edit().remove("favorites").apply()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Toggle favorite error: ${e.message}")
            }
        }
    }
    
    fun isFavorite(postId: String): Boolean = getFavorites().contains(postId)
    
    private fun convertTelegramUrl(url: String): String {
        return try {
            if (url.contains("t.me/")) {
                val channelName = url.substringAfter("t.me/")
                    .takeIf { it.isNotBlank() }
                    ?.takeWhile { it != '/' && it != '?' }
                    ?.trim()
                    ?: url
                "https://rsshub.app/telegram/channel/$channelName"
            } else {
                url
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Telegram URL conversion error: ${e.message}")
            url
        }
    }
    
    fun addSource(type: SourceType, url: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Добавляем источник...", Toast.LENGTH_SHORT).show()
                }
                val current = _sources.value
                val finalUrl = if (type == SourceType.TELEGRAM) convertTelegramUrl(url) else url
                val newSource = SourceItem("${type.name}_${System.currentTimeMillis()}", type, finalUrl)
                saveSources(current + newSource)
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Источник добавлен! Загружаем новости...", Toast.LENGTH_SHORT).show()
                }
                loadPosts()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "ОШИБКА: ${e.message}", Toast.LENGTH_LONG).show()
                }
                Log.e("MainViewModel", "Add source CRASH: ${e.message}", e)
            }
        }
    }
    
    fun removeSource(sourceId: String) {
        viewModelScope.launch {
            try {
                val current = _sources.value
                saveSources(current.filter { it.id != sourceId })
                loadPosts()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Ошибка удаления: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    fun selectPost(post: UnifiedPost?) {
        _selectedPost.value = post
    }

    // MARK: - Theme

    enum class AppTheme(val value: Int) {
        SYSTEM(0), LIGHT(1), DARK(2), AMOLED(3)
    }

    fun getTheme(): AppTheme {
        return try {
            val saved = prefs.getInt("app_theme", AppTheme.SYSTEM.value)
            AppTheme.values().first { it.value == saved }
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    fun setTheme(theme: AppTheme) {
        try {
            prefs.edit().putInt("app_theme", theme.value).apply()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Save theme error: ${e.message}")
        }
    }

    // MARK: - OPML Export/Import

    fun exportSourcesToOpml(): String {
        val doc = try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        } catch (e: Exception) {
            Log.e("MainViewModel", "OPML export document error: ${e.message}")
            return ""
        }

        val opml = doc.createElement("opml")
        opml.setAttribute("version", "1.0")
        doc.appendChild(opml)

        val head = doc.createElement("head")
        opml.appendChild(head)
        val title = doc.createElement("title")
        title.appendChild(doc.createTextNode("NewsApp Sources"))
        head.appendChild(title)

        val body = doc.createElement("body")
        opml.appendChild(body)

        for (source in _sources.value) {
            val outline = doc.createElement("outline")
            outline.setAttribute("text", source.type.displayName)
            outline.setAttribute("title", source.type.displayName)
            outline.setAttribute("type", "rss")
            outline.setAttribute("xmlUrl", source.url)
            outline.setAttribute("htmlUrl", source.url)
            outline.setAttribute("sourceType", source.type.name)
            body.appendChild(outline)
        }

        return try {
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "no")
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8")
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes")
            val writer = java.io.StringWriter()
            transformer.transform(DOMSource(doc), StreamResult(writer))
            writer.toString()
        } catch (e: Exception) {
            Log.e("MainViewModel", "OPML export transform error: ${e.message}")
            ""
        }
    }

    fun importSourcesFromOpml(xml: String): Int {
        val count = try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(xml)))

            val outlineList = doc.getElementsByTagName("outline")
            var imported = 0
            val existingUrls = _sources.value.map { it.url }.toMutableSet()

            for (i in 0 until outlineList.length) {
                val outline = outlineList.item(i) as Element
                val url = outline.getAttribute("xmlUrl")
                val typeName = outline.getAttribute("sourceType")
                val title = outline.getAttribute("title").takeIf { it.isNotBlank() } ?: url

                if (url.isNullOrBlank() || !url.startsWith("http")) continue
                if (existingUrls.contains(url)) continue

                val type = try {
                    SourceType.valueOf(typeName.takeIf { it.isNotBlank() } ?: "RSS")
                } catch (e: Exception) {
                    SourceType.RSS
                }

                val newSource = SourceItem(
                    id = "${type.name}_${System.currentTimeMillis()}_${i}",
                    type = type,
                    url = url
                )

                _sources.value = _sources.value + newSource
                existingUrls.add(url)
                imported++
            }

            saveSources(_sources.value)
            imported
        } catch (e: Exception) {
            Log.e("MainViewModel", "OPML import error: ${e.message}")
            -1
        }

        return count
    }

    fun clearAllSources() {
        _sources.value = emptyList()
        saveSources(emptyList())
    }
}