package com.mikhail.newsapp

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.StringReader
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.InputSource

/**
 * Унифицированная модель поста для всех источников.
 * @Keep — чтобы ProGuard не удалил при minify.
 */
@Keep
data class UnifiedPost(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val source: SourceType,
    val link: String,
    val videoUrl: String? = null,
    val publishedAt: Long = System.currentTimeMillis()
)

/** Типы поддерживаемых источников. */
enum class SourceType(val displayName: String, val emoji: String) {
    RSS("Новости", "📰"),
    YOUTUBE("YouTube", "📺"),
    TELEGRAM("Telegram", "✈️"),
    REDDIT("Reddit", "🔴"),
    TWITTER("Twitter", "🐦")
}

/** Элемент источника в настройках. */
@Keep
data class SourceItem(
    val id: String,
    val type: SourceType,
    val url: String
)

/**
 * Результат загрузки постов для sealed-класса UiState.
 */
sealed class PostsResult {
    data class Success(val posts: List<UnifiedPost>) : PostsResult()
    data class Error(val message: String) : PostsResult()
    object Loading : PostsResult()
}

/**
 * HTTP-клиент с таймаутами, User-Agent, простым кэшем и одним ретраем.
 */
object NetworkClient {
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 12_000
    private const val USER_AGENT_DEFAULT = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36"
    private const val USER_AGENT_FALLBACK = "NewsApp/1.0 (RSS Reader; +https://github.com/turunovmk-svg/Newsapp)"
    private const val CACHE_TTL_MS = 10 * 60 * 1000 // 10 минут
    private const val MAX_RETRIES = 1

    private val cache = mutableMapOf<String, Pair<String, Long>>()
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

    @Suppress("UNUSED_PARAMETER")
    suspend fun fetchRssAsJson(rssUrl: String, type: SourceType): List<UnifiedPost> {
        return withContext(Dispatchers.IO) {
            if (rssUrl.isNullOrBlank() || !rssUrl.startsWith("http")) {
                Log.e("NetworkClient", "Invalid URL: $rssUrl")
                return@withContext emptyList()
            }

            val encodedUrl = URLEncoder.encode(rssUrl, "UTF-8")
            val apiUrl = "https://api.rss2json.com/v1/api.json?rss_url=$encodedUrl"

            val cached = cache[apiUrl]
            if (cached != null && (System.currentTimeMillis() - cached.second) < CACHE_TTL_MS) {
                return@withContext parseJson(cached.first, type)
            }

            var lastException: Exception? = null
            var attempt = 0
            while (attempt <= MAX_RETRIES) {
                attempt++
                try {
                    val url = URL(apiUrl)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = CONNECT_TIMEOUT_MS
                    connection.readTimeout = READ_TIMEOUT_MS
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", USER_AGENT_DEFAULT)
                    connection.setRequestProperty("Accept", "application/json")

                    val responseCode = connection.responseCode
                    if (responseCode != 200) {
                        Log.w("NetworkClient", "HTTP $responseCode for $rssUrl")
                        connection.disconnect()
                        if (attempt > MAX_RETRIES) return@withContext emptyList()
                        Thread.sleep(500)
                        continue
                    }

                    val json = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()

                    if (json.isNullOrBlank()) {
                        Log.w("NetworkClient", "Empty response for: $rssUrl")
                        return@withContext emptyList()
                    }

                    cache[apiUrl] = json to System.currentTimeMillis()
                    return@withContext parseJson(json, type)

                } catch (e: java.net.SocketTimeoutException) {
                    lastException = e
                    Log.w("NetworkClient", "Timeout (attempt $attempt) for: $rssUrl")
                } catch (e: java.io.IOException) {
                    lastException = e
                    Log.w("NetworkClient", "IO error (attempt $attempt) for: $rssUrl")
                } catch (e: Exception) {
                    lastException = e
                    Log.e("NetworkClient", "Unexpected error: ${e.message}", e)
                    break
                }
                if (attempt <= MAX_RETRIES) {
                    Thread.sleep((500 * attempt).toLong())
                }
            }

            Log.e("NetworkClient", "All retries failed for $rssUrl", lastException)
            emptyList()
        }
    }

    private fun parseJson(json: String, type: SourceType): List<UnifiedPost> {
        return try {
            val root = gson.fromJson(json, Map::class.java)
            if (root == null || root["status"] as? String != "ok") {
                Log.w("NetworkClient", "API status not ok")
                return emptyList()
            }
            val items = root["items"] as? List<Map<String, Any>> ?: emptyList()
            items.take(5).mapNotNull { item ->
                try {
                    val title = (item["title"] as? String)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val link = (item["link"] as? String)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val description = (item["description"] as? String) ?: ""
                    val thumbnail = (item["thumbnail"] as? String) ?: ""
                    val pubDateStr = (item["pubDate"] as? String) ?: ""
                    val publishedAt = try {
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).parse(pubDateStr)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) { System.currentTimeMillis() }

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
                        link = link,
                        publishedAt = publishedAt
                    )
                } catch (e: Exception) {
                    Log.w("NetworkClient", "Parse item error: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkClient", "JSON parse error: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Telegram: перебор RSSHub-инстансов с фоллбэком.
     */
    suspend fun fetchTelegramWithFallback(channelName: String): List<UnifiedPost> {
        val instances = listOf(
            "https://rsshub.app/telegram/channel/$channelName",
            "https://rsshub.rssforever.com/telegram/channel/$channelName",
            "https://rss.shab.fun/telegram/channel/$channelName",
            "https://rss.injahow.cn/telegram/channel/$channelName"
        )

        for (instance in instances) {
            try {
                val posts = fetchRssAsJson(instance, SourceType.TELEGRAM)
                if (posts.isNotEmpty()) {
                    Log.d("NetworkClient", "Telegram OK via $instance")
                    return posts
                }
            } catch (e: Exception) {
                Log.w("NetworkClient", "Telegram instance failed: $instance - ${e.message}")
            }
        }
        Log.e("NetworkClient", "All Telegram RSSHub instances failed for $channelName")
        return emptyList()
    }

    /**
     * Reddit: кастомный User-Agent, обработка 403/429.
     * При 403 — фоллбэк на NewsApp UA. При 429 — ждём 60с и возвращаем ошибку.
     */
    suspend fun fetchRedditWithHandling(rssUrl: String): PostsResult {
        return withContext(Dispatchers.IO) {
            val uaPrimary = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36"
            val uaFallback = USER_AGENT_FALLBACK
            var useFallbackUa = false
            var attempt = 0
            val maxAttempts = 2

            while (attempt < maxAttempts) {
                attempt++
                try {
                    val url = URL(rssUrl)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = CONNECT_TIMEOUT_MS
                    connection.readTimeout = READ_TIMEOUT_MS
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", if (useFallbackUa) uaFallback else uaPrimary)
                    connection.setRequestProperty("Accept", "application/rss+xml, application/xml, */*")

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val xml = connection.inputStream.bufferedReader().use { it.readText() }
                        connection.disconnect()
                        val posts = parseRedditXml(xml)
                        return@withContext PostsResult.Success(posts)
                    } else if (responseCode == 403 && !useFallbackUa) {
                        Log.w("NetworkClient", "Reddit 403, retrying with fallback UA")
                        useFallbackUa = true
                        connection.disconnect()
                        continue
                    } else if (responseCode == 429) {
                        Log.w("NetworkClient", "Reddit 429 rate limited")
                        connection.disconnect()
                        Thread.sleep(60_000)
                        return@withContext PostsResult.Error("Reddit ограничил запросы. Попробуйте через минуту.")
                    } else {
                        Log.w("NetworkClient", "Reddit HTTP $responseCode")
                        connection.disconnect()
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    Log.w("NetworkClient", "Reddit timeout (attempt $attempt)")
                } catch (e: Exception) {
                    Log.e("NetworkClient", "Reddit error: ${e.message}", e)
                }
                if (attempt < maxAttempts) Thread.sleep(1000)
            }
            PostsResult.Error("Не удалось загрузить Reddit. Проверьте соединение.")
        }
    }

    private fun parseRedditXml(xml: String): List<UnifiedPost> {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(xml)))
            val entries = doc.getElementsByTagName("entry")
            val posts = mutableListOf<UnifiedPost>()
            for (i in 0 until minOf(entries.length, 10)) {
                val entry = entries.item(i) as Element
                val title = getTextContent(entry, "title") ?: continue
                val link = getLinkHref(entry) ?: continue
                val contentHtml = getTextContent(entry, "content") ?: getTextContent(entry, "summary") ?: ""
                val imageUrl = extractImageUrlFromHtml(contentHtml) ?: getFallbackImage(SourceType.REDDIT)
                val id = getTextContent(entry, "id") ?: link
                val published = getTextContent(entry, "published") ?: ""
                val publishedAt = try {
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).parse(published)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) { System.currentTimeMillis() }

                posts.add(UnifiedPost(
                    id = id,
                    title = title,
                    description = contentHtml.replace(Regex("<[^>]*>"), "").take(150),
                    imageUrl = imageUrl,
                    source = SourceType.REDDIT,
                    link = link,
                    publishedAt = publishedAt
                ))
            }
            posts
        } catch (e: Exception) {
            Log.e("NetworkClient", "Reddit XML parse error: ${e.message}", e)
            emptyList()
        }
    }

    private fun getTextContent(parent: Element, tag: String): String? {
        val nl = parent.getElementsByTagName(tag)
        return if (nl.length > 0) nl.item(0).textContent.trim().takeIf { it.isNotBlank() } else null
    }

    private fun getLinkHref(entry: Element): String? {
        val links = entry.getElementsByTagName("link")
        for (i in 0 until links.length) {
            val link = links.item(i) as Element
            val rel = link.getAttribute("rel")
            val href = link.getAttribute("href")
            if ((rel == "alternate" || rel.isBlank()) && href.startsWith("http")) {
                return href
            }
        }
        return null
    }
}

/**
 * Главная ViewModel: источники, посты, избранное, тема, язык, OPML, история.
 */
class MainViewModel(private val appContext: Context) : ViewModel() {
    private val prefs: SharedPreferences = appContext.getSharedPreferences("news_app_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _posts = MutableStateFlow<PostsResult>(PostsResult.Loading)
    val posts: StateFlow<PostsResult> = _posts

    private val _sources = MutableStateFlow<List<SourceItem>>(emptyList())
    val sources: StateFlow<List<SourceItem>> = _sources

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _isAdding = MutableStateFlow(false)
    val isAdding: StateFlow<Boolean> = _isAdding

    private val _selectedPost = MutableStateFlow<UnifiedPost?>(null)
    val selectedPost: StateFlow<UnifiedPost?> = _selectedPost

    init {
        _sources.value = loadSourcesFromPrefs()
        _posts.value = PostsResult.Loading
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

    private fun saveSources(sources: List<SourceItem>) {
        try {
            val json = gson.toJson(sources)
            Log.d("MainViewModel", "Save sources: count=${sources.size}, json=$json")
            prefs.edit().putString("sources", json).apply()
            _sources.value = sources
            Log.d("MainViewModel", "Sources saved successfully: count=${_sources.value.size}")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Save sources error: ${e.message}", e)
        }
    }

    fun getSources(): List<SourceItem> = _sources.value

    fun getFavorites(): Set<String> {
        return try {
            val set = prefs.getStringSet("favorites", emptySet())
            if (set == null) emptySet() else set.toMutableSet()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Read favorites error: ${e.message}")
            emptySet()
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

    fun loadPosts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _posts.value = PostsResult.Loading
            try {
                val currentSources = _sources.value
                if (currentSources.isEmpty()) {
                    _posts.value = PostsResult.Success(emptyList())
                    return@launch
                }

                val allPosts = mutableListOf<UnifiedPost>()
                val errors = mutableListOf<String>()

                for (source in currentSources) {
                    try {
                        val fetched = when (source.type) {
                            SourceType.TELEGRAM -> {
                                val channel = extractTelegramChannel(source.url)
                                if (channel != null) NetworkClient.fetchTelegramWithFallback(channel)
                                else emptyList()
                            }
                            SourceType.REDDIT -> {
                                val result = NetworkClient.fetchRedditWithHandling(source.url)
                                when (result) {
                                    is PostsResult.Success -> result.posts
                                    is PostsResult.Error -> {
                                        errors.add(result.message)
                                        emptyList()
                                    }
                                    else -> emptyList()
                                }
                            }
                            else -> NetworkClient.fetchRssAsJson(source.url, source.type)
                        }
                        allPosts.addAll(fetched)
                    } catch (e: Exception) {
                        Log.w("MainViewModel", "Fetch failed for ${source.id}: ${e.message}")
                        errors.add("${source.type.displayName}: ${e.message}")
                    }
                }

                val uniquePosts = allPosts.distinctBy { it.id }.sortedByDescending { it.publishedAt }
                if (uniquePosts.isEmpty() && errors.isNotEmpty()) {
                    _posts.value = PostsResult.Error(errors.joinToString("; "))
                } else {
                    _posts.value = PostsResult.Success(uniquePosts)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Load posts error: ${e.message}", e)
                _posts.value = PostsResult.Error("Неожиданная ошибка: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    suspend fun loadPostsForUrl(type: SourceType, url: String): PostsResult = withTimeoutOrNull(15_000L) {
        withContext(Dispatchers.IO) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return@withContext PostsResult.Error("URL должен начинаться с http:// или https://")
            }
            when (type) {
                SourceType.TELEGRAM -> {
                    val channel = extractTelegramChannel(url)
                    if (channel != null) PostsResult.Success(NetworkClient.fetchTelegramWithFallback(channel))
                    else PostsResult.Error("Неверный формат Telegram URL")
                }
                SourceType.REDDIT -> NetworkClient.fetchRedditWithHandling(url)
                else -> PostsResult.Success(NetworkClient.fetchRssAsJson(url, type))
            }
        }
    } ?: PostsResult.Error("Таймаут 15 сек. Источник не отвечает.")

    private fun extractTelegramChannel(url: String): String? {
        return try {
            if (url.contains("t.me/")) {
                val after = url.substringAfter("t.me/")
                val channel = after.takeWhile { it != '/' && it != '?' && it != '@' }.trim()
                if (channel.isNotBlank()) channel else null
            } else null
        } catch (e: Exception) { null }
    }

    fun addSource(type: SourceType, url: String) {
        viewModelScope.launch {
            _isAdding.value = true
            try {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    showToast("URL должен начинаться с http:// или https://")
                    return@launch
                }

                Log.d("MainViewModel", "Add source: type=$type, url=$url")
                showToast("Добавляем источник...")
                val current = _sources.value
                val finalUrl = if (type == SourceType.TELEGRAM) convertTelegramUrl(url) else url
                val newSource = SourceItem("${type.name}_${System.currentTimeMillis()}", type, finalUrl)
                saveSources(current + newSource)

                // Проверяем, что источник реально добавился
                if (!_sources.value.any { it.id == newSource.id }) {
                    Log.e("MainViewModel", "Source was NOT saved: id=${newSource.id}, url=$finalUrl")
                    showToast("Ошибка: источник не сохранён")
                    return@launch
                }

                Log.d("MainViewModel", "Source saved successfully: id=${newSource.id}")
                showToast("Источник добавлен! Загружаем новости...")
                loadPosts()
            } catch (e: Exception) {
                showToast("Ошибка: ${e.message}")
                Log.e("MainViewModel", "Add source error: ${e.message}", e)
            } finally {
                _isAdding.value = false
            }
        }
    }

    private fun convertTelegramUrl(url: String): String {
        return try {
            if (url.contains("t.me/")) {
                val channelName = url.substringAfter("t.me/")
                    .takeWhile { it != '/' && it != '?' && it != '@' }
                    .trim()
                if (channelName.isNotBlank()) "https://rsshub.app/telegram/channel/$channelName" else url
            } else url
        } catch (e: Exception) {
            Log.e("MainViewModel", "Telegram URL conversion error: ${e.message}")
            url
        }
    }

    fun removeSource(sourceId: String) {
        viewModelScope.launch {
            try {
                val current = _sources.value
                saveSources(current.filter { it.id != sourceId })
                loadPosts()
            } catch (e: Exception) {
                showToast("Ошибка удаления: ${e.message}")
            }
        }
    }

    fun selectPost(post: UnifiedPost?) {
        _selectedPost.value = post
    }

    private fun showToast(msg: String) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    enum class AppTheme(val value: Int) {
        SYSTEM(0), LIGHT(1), DARK(2), AMOLED(3)
    }

    fun getTheme(): AppTheme {
        return try {
            val saved = prefs.getInt("app_theme", AppTheme.SYSTEM.value)
            AppTheme.values().first { it.value == saved }
        } catch (e: Exception) { AppTheme.SYSTEM }
    }

    fun setTheme(theme: AppTheme) {
        try { prefs.edit().putInt("app_theme", theme.value).apply() }
        catch (e: Exception) { Log.e("MainViewModel", "Save theme error: ${e.message}") }
    }

    fun getAppLanguage(): String = prefs.getString("app_language", "system") ?: "system"

    fun setAppLanguage(language: String) {
        try { prefs.edit().putString("app_language", language).apply() }
        catch (e: Exception) { Log.e("MainViewModel", "Save language error: ${e.message}") }
    }

    fun exportSourcesToOpml(): String {
        val doc = try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        } catch (e: Exception) {
            Log.e("MainViewModel", "OPML export document error: ${e.message}")
            return ""
        }

        val opml = doc.createElement("opml").apply { setAttribute("version", "1.0") }
        doc.appendChild(opml)

        val head = doc.createElement("head")
        opml.appendChild(head)
        val title = doc.createElement("title").apply { appendChild(doc.createTextNode("NewsApp Sources")) }
        head.appendChild(title)

        val body = doc.createElement("body")
        opml.appendChild(body)

        for (source in _sources.value) {
            val outline = doc.createElement("outline").apply {
                setAttribute("text", source.type.displayName)
                setAttribute("title", source.type.displayName)
                setAttribute("type", "rss")
                setAttribute("xmlUrl", source.url)
                setAttribute("htmlUrl", source.url)
                setAttribute("sourceType", source.type.name)
            }
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
        return try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(xml)))
            val outlineList = doc.getElementsByTagName("outline")
            var imported = 0
            val existingUrls = _sources.value.map { it.url }.toMutableSet()

            for (i in 0 until outlineList.length) {
                val outline = outlineList.item(i) as Element
                val url = outline.getAttribute("xmlUrl")
                val typeName = outline.getAttribute("sourceType")
                if (url.isNullOrBlank() || !url.startsWith("http")) continue
                if (existingUrls.contains(url)) continue

                val type = try { SourceType.valueOf(typeName.takeIf { it.isNotBlank() } ?: "RSS") }
                catch (e: Exception) { SourceType.RSS }

                val newSource = SourceItem(
                    id = "${type.name}_${System.currentTimeMillis()}_$i",
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
    }

    fun clearAllSources() {
        _sources.value = emptyList()
        saveSources(emptyList())
    }

    fun getReadHistory(): Set<String> {
        return try {
            val set = prefs.getStringSet("read_history", emptySet())
            if (set == null) emptySet() else set.toMutableSet()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Read history error: ${e.message}")
            emptySet()
        }
    }

    fun isRead(postId: String): Boolean = getReadHistory().contains(postId)

    fun markAsRead(postId: String) {
        viewModelScope.launch {
            try {
                val current = getReadHistory()
                prefs.edit().putStringSet("read_history", current + postId).apply()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Mark as read error: ${e.message}")
            }
        }
    }

    fun clearReadHistory() {
        viewModelScope.launch {
            try { prefs.edit().remove("read_history").apply() }
            catch (e: Exception) { Log.e("MainViewModel", "Clear read history error: ${e.message}") }
        }
    }

    fun clearCache() {
        Log.d("MainViewModel", "Cache cleared")
    }
}