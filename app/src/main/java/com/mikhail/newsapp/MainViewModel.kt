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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
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

    // ---------- Читать позже (persisted) ----------
    private val _readLater = MutableStateFlow<List<Post>>(emptyList())
    val readLater: StateFlow<List<Post>> = _readLater.asStateFlow()

    private val _currentShortsIndex = MutableStateFlow(0)
    val currentShortsIndex: StateFlow<Int> = _currentShortsIndex.asStateFlow()

    fun setCurrentShortsIndex(index: Int) {
        _currentShortsIndex.value = index
    }

    // ---------- Папки источников ----------
    private val _selectedFolder = MutableStateFlow("")   // "" = все источники
    val selectedFolder: StateFlow<String> = _selectedFolder.asStateFlow()

    /** Список непустых папок из источников. */
    val folders: StateFlow<List<String>> = _sources.map { list ->
        list.map { it.folder }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setSelectedFolder(folder: String) {
        _selectedFolder.value = folder
        loadPosts()
    }

    // ---------- Ошибки конкретных источников (url → текст) ----------
    private val _sourceErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val sourceErrors: StateFlow<Map<String, String>> = _sourceErrors.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ---------- Прочитанные посты (persisted) ----------
    private val _readIds = MutableStateFlow<Set<String>>(emptySet())
    val readIds: StateFlow<Set<String>> = _readIds.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // ---------- Настройки чтения (persisted) ----------
    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val _darkTheme = MutableStateFlow(true)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()


        init {
        loadSources()
        loadFavorites()
        loadReadLater()
        loadReadIds()
        loadSettings()
        loadCachedPosts()
        loadPosts()
        // Пересчёт непрочитанных
        viewModelScope.launch {
            combine(_posts, _readIds) { posts, ids ->
                posts.count { it.id !in ids }
            }.collect { count ->
                _unreadCount.value = count
            }
        }
    }

    // ---------- Кэш постов (офлайн) ----------
    private fun loadCachedPosts() {
        val json = prefs.getString("posts_json", null) ?: return
        val cached = Post.fromJsonList(json)
        if (cached.isNotEmpty()) {
            _posts.value = cached.sortedByDescending { it.publishedAt ?: "" }
        }
    }

    private fun saveCachedPosts(posts: List<Post>) {
        // храним не более 200 последних постов, чтобы не раздувать prefs
        val toSave = posts.take(200)
        prefs.edit().putString("posts_json", Post.toJsonList(toSave)).apply()
    }

    /** Отмечает пост прочитанным (сохраняется между запусками). */
    fun markAsRead(post: Post) {
        if (_readIds.value.contains(post.id)) return
        _readIds.value = _readIds.value + post.id
        prefs.edit().putStringSet("read_ids", _readIds.value).apply()
        // временные метки для статистики
        val ts = prefs.getString("read_ts_json", null)
        val map = org.json.JSONObject(ts ?: "{}")
        map.put(post.id, System.currentTimeMillis())
        prefs.edit().putString("read_ts_json", map.toString()).apply()
    }

    fun isRead(post: Post): Boolean = _readIds.value.contains(post.id)

    /** Отмечает все загруженные посты прочитанными. */
    fun markAllRead() {
        val ids = _posts.value.map { it.id }.toSet() + _readIds.value
        _readIds.value = ids
        prefs.edit().putStringSet("read_ids", ids).apply()
    }

    private fun loadReadIds() {
        val ids = prefs.getStringSet("read_ids", emptySet()) ?: emptySet()
        _readIds.value = ids
    }

    private fun loadSettings() {
        _fontScale.value = prefs.getFloat("font_scale", 1.0f)
        _darkTheme.value = prefs.getBoolean("dark_theme", true)
        _viewMode.value = prefs.getString("view_mode", "cards") ?: "cards"
        _sortMode.value = prefs.getString("sort_mode", "date") ?: "date"
        _notifEnabled.value = prefs.getBoolean(NewsCheckWorker.KEY_ENABLED, false)
        _notifIntervalHours.value = prefs.getInt(NewsCheckWorker.KEY_INTERVAL_HOURS, 3)
    }

    fun setFontScale(scale: Float) {
        _fontScale.value = scale
        prefs.edit().putFloat("font_scale", scale).apply()
    }

    fun setDarkTheme(enabled: Boolean) {
        _darkTheme.value = enabled
        prefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    // ---------- Режим отображения ленты и сортировка ----------
    private val _viewMode = MutableStateFlow("cards")   // cards / list / grid
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    private val _readingFont = MutableStateFlow("sans")  // sans / serif
    val readingFont: StateFlow<String> = _readingFont.asStateFlow()

    fun setReadingFont(font: String) {
        _readingFont.value = font
        prefs.edit().putString("reading_font", font).apply()
    }

    private val _sortMode = MutableStateFlow("date")    // date / source
    val sortMode: StateFlow<String> = _sortMode.asStateFlow()

    fun setViewMode(mode: String) {
        _viewMode.value = mode
        prefs.edit().putString("view_mode", mode).apply()
    }

    fun setSortMode(mode: String) {
        _sortMode.value = mode
        prefs.edit().putString("sort_mode", mode).apply()
    }

    // ---------- Уведомления ----------
    private val _notifEnabled = MutableStateFlow(false)
    val notifEnabled: StateFlow<Boolean> = _notifEnabled.asStateFlow()

    private val _notifIntervalHours = MutableStateFlow(3)
    val notifIntervalHours: StateFlow<Int> = _notifIntervalHours.asStateFlow()

    /** Включает/выключает фоновые уведомления (пересоздаёт worker). */
    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        _notifEnabled.value = enabled
        prefs.edit().putBoolean("notif_enabled", enabled).apply()
        scheduleNewsCheck(context, enabled)
    }

    fun setNotifIntervalHours(context: Context, hours: Int) {
        _notifIntervalHours.value = hours
        prefs.edit().putInt("notif_interval_hours", hours).apply()
        if (_notifEnabled.value) {
            scheduleNewsCheck(context, true)
        }
    }

    private fun scheduleNewsCheck(context: Context, enabled: Boolean) {
        try {
            val wm = androidx.work.WorkManager.getInstance(context)
            if (enabled) {
                val request = androidx.work.PeriodicWorkRequestBuilder<NewsCheckWorker>(
                    _notifIntervalHours.value.toLong(), java.util.concurrent.TimeUnit.HOURS,
                    15, java.util.concurrent.TimeUnit.MINUTES
                )
                    .setConstraints(
                        androidx.work.Constraints.Builder()
                            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
                wm.enqueueUniquePeriodicWork(
                    NewsCheckWorker.WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
                // первый запуск вручную для immediateness
                wm.enqueue(androidx.work.OneTimeWorkRequestBuilder<NewsCheckWorker>().build())
            } else {
                wm.cancelUniqueWork(NewsCheckWorker.WORK_NAME)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /** Загружает сохранённые источники из SharedPreferences. */
    fun loadSources() {
        val json = prefs.getString("sources_json", null) ?: return
        _sources.value = Source.fromJsonList(json)
    }

    /** Сохраняет источники в SharedPreferences. */
    fun saveSources(sources: List<Source>) {
        val json = Source.toJsonList(sources)
        prefs.edit().putString("sources_json", json).apply()
        _sources.value = sources
    }

    /** Добавляет источник по вставленной ссылке: тип определяется автоматически. */
    fun addSource(type: String, url: String, folder: String = "") {
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
        val newSource = Source(detected.type, trimmedUrl, detected.label, folder.trim())
        val updated = current + newSource
        saveSources(updated)
        clearError()
        loadPosts()
    }

    /** Меняет папку источника. */
    fun setSourceFolder(url: String, folder: String) {
        val updated = _sources.value.map {
            if (it.url == url) it.copy(folder = folder.trim()) else it
        }
        saveSources(updated)
    }

    /** Загружает посты из всех сохранённых источников. */
    fun loadPosts() {
        viewModelScope.launch {
            val allSources = _sources.value
            val folder = _selectedFolder.value
            val sources = if (folder.isBlank()) allSources else allSources.filter { it.folder == folder }
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
                        var parsed = withContext(Dispatchers.Default) {
                            parseFeed(source.type, xml)
                        }
                        // Telegram: догружаем вторую страницу превью (старые посты)
                        if (source.type == "TELEGRAM" && parsed.size >= 10) {
                            try {
                                val lastBefore = parsed.last().sourceUrl
                                    ?.substringAfterLast("/")
                                    ?.takeIf { it.isNotBlank() }
                                if (lastBefore != null) {
                                    val pagedUrl = if (feedUrl.contains("?")) "$feedUrl&before=$lastBefore"
                                    else "$feedUrl?before=$lastBefore"
                                    val xml2 = withContext(Dispatchers.Default) { fetchXml(pagedUrl) }
                                    val parsed2 = withContext(Dispatchers.Default) {
                                        parseFeed(source.type, xml2)
                                    }
                                    // склеиваем без дублей
                                    val seen = parsed.map { it.id }.toSet()
                                    parsed = parsed + parsed2.filter { it.id !in seen }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        allPosts.addAll(parsed)
                        success = true
                        // сбрасываем ошибку этого источника при успехе
                        if (_sourceErrors.value.containsKey(source.url)) {
                            _sourceErrors.value = _sourceErrors.value - source.url
                        }
                        if (parsed.isEmpty()) {
                            _sourceErrors.value = _sourceErrors.value + (source.url to "Источник загрузился, но новостей не найдено")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _sourceErrors.value = _sourceErrors.value + (source.url to (e.message ?: "Ошибка загрузки"))
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
                // Кэшируем загруженное для офлайна
                if (allPosts.isNotEmpty()) {
                    saveCachedPosts(_posts.value)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchXml(url: String): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
                .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                .build()
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
                "GNEWS" -> parseRss(xml)   // Google News отдаёт стандартный RSS
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
            var currentSiteUrl: String? = null
            var currentSiteName: String? = null
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
                            currentSiteUrl = null
                            currentSiteName = null
                        } else if (inItem && tag == "source") {
                            // <source url="https://site.domain">Название</source> (Google News)
                            currentSiteUrl = parser.getAttributeValue(null, "url")
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
                                "source" -> currentSiteName = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tag = parser.name
                        if (tag == "item") {
                            inItem = false
                            if (currentTitle != null) {
                                // Fallback: <img src> в описании
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
                                    imageUrl = finalImageUrl,
                                    siteUrl = currentSiteUrl,
                                    siteName = currentSiteName
                                ))
                            }
                            currentTitle = null
                            currentDesc = null
                            currentDate = null
                            currentLink = null
                            currentImageUrl = null
                            currentSiteUrl = null
                            currentSiteName = null
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

    // ---------- Читать позже ----------
    /** Добавляет или убирает статью из «Читать позже» с сохранением. */
    fun toggleReadLater(post: Post) {
        val current = _readLater.value
        _readLater.value = if (current.any { it.id == post.id }) {
            current.filter { it.id != post.id }
        } else {
            current + post
        }
        prefs.edit().putString("readlater_json", Post.toJsonList(_readLater.value)).apply()
    }

    fun isReadLater(post: Post): Boolean = _readLater.value.any { it.id == post.id }

    private fun loadReadLater() {
        val json = prefs.getString("readlater_json", null) ?: return
        _readLater.value = Post.fromJsonList(json)
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
