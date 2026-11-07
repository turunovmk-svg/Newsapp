package com.mikhail.newsapp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mikhail.newsapp.data.Source
import com.mikhail.newsapp.data.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        getApplication<Application>().getSharedPreferences("NewsAppPrefs", Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .writeTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _sources = MutableStateFlow<List<Source>>(emptyList())
    val sources: StateFlow<List<Source>> = _sources.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _favorites = MutableStateFlow<List<Post>>(emptyList())
    val favorites: StateFlow<List<Post>> = _favorites.asStateFlow()

    private val _currentShortsIndex = MutableStateFlow(0)
    val currentShortsIndex: StateFlow<Int> = _currentShortsIndex.asStateFlow()

    fun setCurrentShortsIndex(index: Int) {
        _currentShortsIndex.value = index
    }

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val DEFAULT_SOURCES = listOf(
        Source("RSS", "https://feeds.bbci.co.uk/ukrainian/rss.xml"),
        Source("RSS", "https://www.pravda.com.ua/rss/"),
        Source("REDDIT", "https://www.reddit.com/r/worldnews/.rss"),
        Source("YOUTUBE", "https://www.youtube.com/feeds/videos.xml?channel_id=UC_x5XG1OV2P6uZZ5FSM9Ttw")
    )

    init {
        loadSources()
        loadFavorites()
        ensureDefaultSources()
        loadPosts()
    }

    /** Загружает сохранённые источники из SharedPreferences. */
    fun loadSources() {
        val json = prefs.getString("sources_json", null) ?: return
        _sources.value = Source.fromJsonList(json)
    }

    /** Добавляет предустановленные источники, если пользователь ещё не добавил ни одного. */
    fun ensureDefaultSources() {
        val current = _sources.value
        if (current.isEmpty()) {
            _sources.value = DEFAULT_SOURCES
            saveSources(DEFAULT_SOURCES)
        }
    }

    /** Сохраняет источники в SharedPreferences. */
    fun saveSources(sources: List<Source>) {
        val json = Source.toJsonList(sources)
        prefs.edit().putString("sources_json", json).apply()
        _sources.value = sources
    }

    /** Добавляет источник по вставленной ссылке: тип определяется автоматически. */
    fun addSource(type: String, url: String) {
        val detected = Source.detect(url, type)
        if (detected == null) {
            _error.value = "Не удалось распознать ссылку"
            return
        }
        val trimmedUrl = detected.url.trim()
        if (trimmedUrl.isEmpty()) {
            _error.value = "Введите URL источника"
            return
        }
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            _error.value = "URL должен начинаться с http:// или https://"
            return
        }
        val current = _sources.value
        if (current.any { it.url == trimmedUrl }) {
            _error.value = "Источник уже добавлен"
            return
        }
        val newSource = Source(detected.type, trimmedUrl)
        val updated = current + newSource
        saveSources(updated)
        clearError()
        loadPosts()
    }

    /** Загружает посты из всех сохранённых источников. */
    fun loadPosts() {
        viewModelScope.launch {
            val sources = _sources.value
            if (sources.isEmpty()) {
                _posts.value = emptyList()
                _isLoading.value = false
                return@launch
            }
            _isLoading.value = true
            _error.value = null
            try {
                val allPosts = mutableListOf<Post>()
                var success = false
                for (source in sources) {
                    try {
                        val feedUrl = if (source.type == "YOUTUBE") resolveYoutubeFeedUrl(source.url) else source.url
                        val xml = withContext(Dispatchers.Default) {
                            fetchXml(feedUrl)
                        }
                        val parsed = withContext(Dispatchers.Default) {
                            parseFeed(source.type, xml)
                        }
                        allPosts.addAll(parsed)
                        success = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (allPosts.isEmpty() && _posts.value.isNotEmpty()) {
                    // Сохраняем предыдущие посты, если новая загрузка не вернула ничего
                } else {
                    // Дедупликация по sourceUrl (или title если URL пуст)
                    val deduplicatedPosts = allPosts.distinctBy { it.sourceUrl ?: it.title }
                    _posts.value = deduplicatedPosts.sortedByDescending { it.publishedAt ?: "" }
                }
                if (!success && _posts.value.isEmpty()) {
                    _error.value = "Не удалось загрузить новости ни из одного источника"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchXml(url: String): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    throw Exception("HTTP ${response.code}")
                }
                body.string()
            }
        }
    }

    private suspend fun parseFeed(type: String, xml: String): List<Post> {
        return withContext(Dispatchers.Default) {
            when (type) {
                "RSS" -> parseRss(xml)
                "REDDIT" -> parseRss(xml) // Reddit .rss тоже валидный RSS
                "TWITTER" -> parseRss(xml) // RSSHub отдаёт стандартный RSS
                "YOUTUBE" -> parseYouTube(xml)
                "TELEGRAM" -> parseTelegram(xml)
                else -> emptyList()
            }
        }
    }

    /**
     * YouTube: при вставке ссылки на канал/видео (@handle, youtu.be, watch)
     * надо сначала достать channel_id из HTML страницы и превратить ссылку в Atom-фид.
     */
    private suspend fun resolveYoutubeFeedUrl(url: String): String {
        if (url.contains("/feeds/videos.xml")) return url
        if (!(url.contains("youtube.com/@") || url.contains("youtube.com/watch") || url.contains("youtu.be/"))) return url
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    val html = response.body?.string() ?: return@withContext url
                    // channel_id ищем в JSON-описании канала
                    val channelId = Regex("\"channel_id\"\\s*:\\s*\"(UC[\\w-]+)\"").find(html)?.groupValues?.get(1)
                        ?: Regex("channel_id=(UC[\\w-]+)").find(html)?.groupValues?.get(1)
                        ?: Regex("\"externalId\"\\s*:\\s*\"(UC[\\w-]+)\"").find(html)?.groupValues?.get(1)
                    if (channelId != null) {
                        "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
                    } else {
                        url
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                url
            }
        }
    }

    private fun parseRss(xml: String): List<Post> {
        val posts = mutableListOf<Post>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            val input = java.io.StringReader(xml)
            parser.setInput(input)

            var eventType = parser.eventType
            var currentTag: String? = null
            var currentTitle: String? = null
            var currentDesc: String? = null
            var currentDate: String? = null
            var currentLink: String? = null
            var currentImageUrl: String? = null
            var inItem = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tag = parser.name
                        currentTag = tag
                        if (tag == "item") {
                            inItem = true
                            currentTitle = null
                            currentDesc = null
                            currentDate = null
                            currentLink = null
                            currentImageUrl = null
                        } else if (inItem && (tag == "enclosure" || tag == "content" || tag == "thumbnail"
                                || tag == "media:content" || tag == "media:thumbnail" || tag == "itunes:image")) {
                            val imgUrl = parser.getAttributeValue(null, "url")
                                ?: parser.getAttributeValue(null, "href")
                                ?: parser.getAttributeValue(null, "image")
                            if (imgUrl != null && imgUrl.isNotEmpty()) {
                                currentImageUrl = imgUrl
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty() && inItem) {
                            when (currentTag) {
                                "title" -> currentTitle = text
                                "description" -> currentDesc = text
                                "pubDate" -> currentDate = text
                                "link" -> currentLink = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tag = parser.name
                        if (tag == "item") {
                            inItem = false
                            if (currentTitle != null) {
                                // Fallback: ищем <img src="..."> в описании, если нет явного imageUrl
                                val finalImageUrl = currentImageUrl ?: run {
                                    val desc = currentDesc ?: ""
                                    val imgRegex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                                    imgRegex.find(desc)?.groupValues?.get(1)
                                }
                                posts.add(Post(
                                    id = currentLink ?: currentTitle.hashCode().toString(),
                                    title = currentTitle ?: "",
                                    description = currentDesc,
                                    publishedAt = currentDate,
                                    sourceUrl = currentLink,
                                    imageUrl = finalImageUrl
                                ))
                            }
                            currentTitle = null
                            currentDesc = null
                            currentDate = null
                            currentLink = null
                        }
                        currentTag = null
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return posts
    }

    private fun parseYouTube(xml: String): List<Post> {
        val posts = mutableListOf<Post>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            val input = java.io.StringReader(xml)
            parser.setInput(input)

            var eventType = parser.eventType
            var currentTag: String? = null
            var currentTitle: String? = null
            var currentDesc: String? = null
            var currentDate: String? = null
            var currentLink: String? = null
            var currentImageUrl: String? = null
            var inEntry = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tag = parser.name
                        currentTag = tag
                        if (tag == "entry") {
                            inEntry = true
                            currentTitle = null
                            currentDesc = null
                            currentDate = null
                            currentLink = null
                            currentImageUrl = null
                        } else if (inEntry && tag == "link") {
                            // YouTube Atom: <link rel="alternate" href="..."/>
                            val href = parser.getAttributeValue(null, "href")
                            if (href != null && (parser.getAttributeValue(null, "rel") == null || parser.getAttributeValue(null, "rel") == "alternate")) {
                                currentLink = href
                            }
                        } else if (inEntry && (tag == "thumbnail" || tag == "media:thumbnail")) {
                            val thumbUrl = parser.getAttributeValue(null, "url")
                            if (thumbUrl != null && thumbUrl.isNotEmpty()) {
                                currentImageUrl = thumbUrl
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty() && inEntry) {
                            when (currentTag) {
                                "title" -> currentTitle = text
                                "summary" -> currentDesc = text
                                "published" -> currentDate = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tag = parser.name
                        if (tag == "entry") {
                            inEntry = false
                            if (currentTitle != null) {
                                posts.add(Post(
                                    id = currentLink ?: currentTitle.hashCode().toString(),
                                    title = currentTitle ?: "",
                                    description = currentDesc,
                                    publishedAt = currentDate,
                                    sourceUrl = currentLink,
                                    imageUrl = currentImageUrl
                                ))
                            }
                            currentTitle = null
                            currentDesc = null
                            currentDate = null
                            currentLink = null
                        }
                        currentTag = null
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return posts
    }

    /**
     * Telegram: парсим публичное превью канала t.me/s/<channel> (без API-ключа).
     * Каждый пост — .tgme_widget_message внутри message_bubble.
     */
    private fun parseTelegram(html: String): List<Post> {
        val posts = mutableListOf<Post>()
        try {
            val doc = org.jsoup.Jsoup.parse(html)
            val channelTitle = doc.selectFirst("div.tgme_channel_header_title, div.tgme_header_title")?.text() ?: "Telegram"
            val messages = doc.select("div.tgme_widget_message")
            for (msg in messages) {
                val permalink = msg.attr("data-post").takeIf { it.isNotEmpty() }?.let { "https://t.me/$it" }
                    ?: msg.selectFirst("a.tgme_widget_message_date")?.attr("href")?.takeIf { it.isNotEmpty() }
                val date = msg.selectFirst("a.tgme_widget_message_date time")?.attr("datetime")
                    ?: msg.selectFirst("time")?.attr("datetime")
                    ?: msg.selectFirst("a.tgme_widget_message_date time")?.text()
                val textEl = msg.selectFirst("div.tgme_widget_message_text")
                val text = textEl?.text()?.trim()
                val imageUrl = msg.selectFirst("a.tgme_widget_message_photo_wrap")?.attr("style")
                    ?.let { Regex("url\\(['\"]?([^'\")]+)['\"]?\\)").find(it)?.groupValues?.get(1) }
                    ?: msg.selectFirst("img.tgme_widget_message_photo_image")?.attr("src")
                    ?: msg.selectFirst("img")?.attr("src")
                if (text.isNullOrEmpty() && permalink == null) continue
                val title = (text ?: channelTitle).take(120).let { if ((text?.length ?: 0) > 120) "$it…" else it }
                val id = permalink ?: text?.hashCode()?.toString() ?: channelTitle
                posts.add(
                    Post(
                        id = id,
                        title = title,
                        description = text,
                        publishedAt = date,
                        sourceUrl = permalink ?: "https://t.me/s/${channelTitle}",
                        imageUrl = imageUrl
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return posts
    }

    /** Добавляет или удаляет публикацию из избранного с сохранением в SharedPreferences. */
    fun toggleFavorite(post: Post) {
        val current = _favorites.value
        if (current.any { it.id == post.id }) {
            _favorites.value = current.filter { it.id != post.id }
        } else {
            _favorites.value = current + post
        }
        saveFavorites()
    }

    /** Сохраняет избранное в SharedPreferences. */
    private fun saveFavorites() {
        val json = Post.toJsonList(_favorites.value)
        prefs.edit().putString("favorites_json", json).apply()
    }

    /** Загружает избранное из SharedPreferences. */
    private fun loadFavorites() {
        val json = prefs.getString("favorites_json", null) ?: return
        _favorites.value = Post.fromJsonList(json)
    }

    /** Удаляет источник по URL. */
    fun removeSource(url: String) {
        val current = _sources.value
        val updated = current.filter { it.url != url }
        if (updated.size != current.size) {
            saveSources(updated)
            // Удаляем посты этого источника
            _posts.value = _posts.value.filter { it.sourceUrl != url }
            clearError()
        }
    }

    /** Проверяет, находится ли публикация в избранном. */
    fun isFavorite(post: Post): Boolean {
        return _favorites.value.any { it.id == post.id }
    }

    /** Сбрасывает сообщение об ошибке. */
    fun clearError() {
        _error.value = null
    }
}
