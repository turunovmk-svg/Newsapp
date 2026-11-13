package com.mikhail.newsapp

import android.app.Application
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import java.net.URI
import androidx.lifecycle.viewModelScope
import com.mikhail.newsapp.data.Source
import com.mikhail.newsapp.data.SourceType
import com.mikhail.newsapp.data.Post
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"
        const val MAX_POSTS_PER_SOURCE = 20
    }

    private val prefs: SharedPreferences = getApplication<Application>().getSharedPreferences("NewsAppPrefs", Context.MODE_PRIVATE)
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(15_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .writeTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .callTimeout(30_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(request)
        }
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

    // ---------- Счётчик неудач источника (url → количество неудач подряд) ----------
    private val _sourceErrorCount = MutableStateFlow<Map<String, Int>>(emptyMap())
    val sourceErrorCount: StateFlow<Map<String, Int>> = _sourceErrorCount.asStateFlow()

    // ---------- Посты по источникам (url → список постов этого источника) ----------
    // Позволяет при сбое одного источника оставить его предыдущие новости,
    // не теряя их при перезаписи общей ленты.
    private val _postsBySource = MutableStateFlow<Map<String, List<Post>>>(emptyMap())
    val postsBySource: StateFlow<Map<String, List<Post>>> = _postsBySource.asStateFlow()

    // ---------- Текущая задача загрузки (для отмены при повторном нажатии "Обновить") ----------
    private var currentLoadJob: Job? = null

    // ---------- Временно отключённые источники (url → время истечения в мс) ----------
    private val _disabledSources = MutableStateFlow<Map<String, Long>>(emptyMap())
    val disabledSources: StateFlow<Map<String, Long>> = _disabledSources.asStateFlow()

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

    // ---------- Режим оформления: obsidian / neon / harmony ----------
    private val _themeMode = MutableStateFlow("obsidian")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()
    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        _darkTheme.value = mode != "harmony"
        prefs.edit().putString("theme_mode", mode).apply()
    }



    // ---------- Кэш постов (офлайн) ----------
    /**
     * Читает и разбирает posts_json ОДИН раз (на Dispatchers.IO), показывает кэш в ленту
     * и подрезает устаревшие записи по прежним правилам (CachePolicy, 7 дней).
     *
     * Безопасность:
     *  - кэш в _posts ставится только если лента ещё пуста — не затираем свежий сетевой результат;
     *  - prune-запись делается только если в prefs до сих пор тот же JSON (CAS) —
     *    не перезаписываем более свежий кэш, записанный saveCachedPosts() из loadPosts();
     *  - отсутствующий/пустой/битый JSON → Post.fromJsonList вернёт пустой список, ничего не пишем;
     *  - отмена корутины пробрасывается через withContext как CancellationException.
     */
    private suspend fun initPostsCache() {
        val json = withContext(Dispatchers.IO) { prefs.getString("posts_json", null) } ?: return
        val cached = withContext(Dispatchers.IO) { Post.fromJsonList(json) }
        if (cached.isEmpty()) return
        if (_posts.value.isEmpty()) {
            _posts.value = cached.sortedByDescending { it.publishedAt ?: "" }
        }
        val fresh = CachePolicy.prune(cached, System.currentTimeMillis())
        if (fresh.size != cached.size) {
            val freshJson = Post.toJsonList(fresh)
            withContext(Dispatchers.IO) {
                if (prefs.getString("posts_json", null) == json) {
                    prefs.edit().putString("posts_json", freshJson).apply()
                }
            }
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
        _themeMode.value = prefs.getString("theme_mode", null)
            ?: if (_darkTheme.value) "obsidian" else "harmony"
        _viewMode.value = prefs.getString("view_mode", "cards") ?: "cards"
        _sortMode.value = prefs.getString("sort_mode", "date") ?: "date"
        _notifEnabled.value = prefs.getBoolean(NewsCheckWorker.KEY_ENABLED, false)
        _notifIntervalHours.value = prefs.getInt(NewsCheckWorker.KEY_INTERVAL_HOURS, 3)
        _notifDisabledSources.value = prefs.getStringSet("notif_disabled_sources", emptySet()) ?: emptySet()
        _quietStart.value = prefs.getInt("quiet_start", -1)
        _quietEnd.value = prefs.getInt("quiet_end", -1)
        _notifSound.value = prefs.getBoolean("notif_sound", true)
    }

    fun setFontScale(scale: Float) {
        _fontScale.value = scale
        prefs.edit().putFloat("font_scale", scale).apply()
    }

    fun setDarkTheme(enabled: Boolean) {
        _darkTheme.value = enabled
        // Старый переключатель сохраняем для совместимости: светлый → harmony, тёмный → obsidian
        _themeMode.value = if (enabled) "obsidian" else "harmony"
        prefs.edit().putBoolean("dark_theme", enabled).apply()
        prefs.edit().putString("theme_mode", _themeMode.value).apply()
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

    // ---------- Тема чтения (светлая / тёмная / сепия) ----------
    private val _readingTheme = MutableStateFlow("light")  // light / dark / sepia
    val readingTheme: StateFlow<String> = _readingTheme.asStateFlow()
    fun setReadingTheme(theme: String) {
        _readingTheme.value = theme
        prefs.edit().putString("reading_theme", theme).apply()
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

    // ---------- Продвинутые настройки уведомлений ----------
    /** Источники, для которых уведомления ОТКЛЮЧЕНЫ (url). */
    private val _notifDisabledSources = MutableStateFlow<Set<String>>(emptySet())
    val notifDisabledSources: StateFlow<Set<String>> = _notifDisabledSources.asStateFlow()

    /** Тихие часы: минуты от начала суток. -1 = выключено. */
    private val _quietStart = MutableStateFlow(-1)
    val quietStart: StateFlow<Int> = _quietStart.asStateFlow()

    private val _quietEnd = MutableStateFlow(-1)
    val quietEnd: StateFlow<Int> = _quietEnd.asStateFlow()

    /** Звук уведомлений. */
    private val _notifSound = MutableStateFlow(true)
    val notifSound: StateFlow<Boolean> = _notifSound.asStateFlow()

    fun toggleSourceNotify(url: String) {
        val cur = _notifDisabledSources.value
        _notifDisabledSources.value = if (url in cur) cur - url else cur + url
        prefs.edit().putStringSet("notif_disabled_sources", _notifDisabledSources.value).apply()
    }

    fun setQuietHours(startMinutes: Int, endMinutes: Int) {
        _quietStart.value = startMinutes
        _quietEnd.value = endMinutes
        prefs.edit()
            .putInt("quiet_start", startMinutes)
            .putInt("quiet_end", endMinutes)
            .apply()
    }

    fun setNotifSound(enabled: Boolean) {
        _notifSound.value = enabled
        prefs.edit().putBoolean("notif_sound", enabled).apply()
        // пересоздаём канал с новой важностью
        val ctx = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(android.app.NotificationManager::class.java)
            nm?.deleteNotificationChannel(NewsCheckWorker.CHANNEL_ID)
        }
        if (_notifEnabled.value) {
            scheduleNewsCheck(ctx, true)
        }
    }

        init {
        loadSources()
        loadFavorites()
        loadReadLater()
        loadReadIds()
        loadSettings()
        // Кэш постов читаем/парсим один раз на IO (этап 9B), чтобы не блокировать старт
        viewModelScope.launch { initPostsCache() }
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
        val loaded = Source.fromJsonList(json)
        // Миграция: старые Reddit-ссылки вида /r/xxx.rss → /r/xxx/.rss (Reddit это сменил)
        var changed = false
        val migrated = loaded.map { source ->
            if (source.type == com.mikhail.newsapp.data.SourceType.REDDIT &&
                source.url.contains("reddit.com") &&
                source.url.endsWith(".rss") &&
                !source.url.endsWith("/.rss")
            ) {
                changed = true
                val newUrl = source.url.removeSuffix(".rss") + "/.rss"
                com.mikhail.newsapp.data.Source(source.type, newUrl, source.label, source.folder)
            } else {
                source
            }
        }
        if (changed) {
            saveSources(migrated)
        } else {
            _sources.value = loaded
        }
    }

    /** Сохраняет источники в SharedPreferences. */
    fun saveSources(sources: List<Source>) {
        val json = Source.toJsonList(sources)
        prefs.edit().putString("sources_json", json).apply()
        _sources.value = sources
    }

    /** Устанавливает источники без записи в prefs (для тестов). */
    fun setSourcesForTest(sources: List<Source>) {
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
        // Апгрейд: для GNEWS пробуем найти скрытый RSS у самого сайта
        if (detected.type == SourceType.GNEWS) {
            viewModelScope.launch { probeHiddenRss(trimmedUrl, folder.trim()) }
        }
    }

    /**
     * Для сайта без своего RSS ищем скрытый фид: /feed/, /rss.xml, /atom.xml и т.п.
     * Если нашли — заменяем Google-News-подписку на прямой RSS (статьи короче и надёжнее).
     */
    private suspend fun probeHiddenRss(gnewsUrl: String, folder: String) {
        val domain = gnewsUrl
            .substringAfter("site:")
            .substringBefore('&')
            .removePrefix("www.")
        if (domain.isEmpty()) return
        val paths = listOf("/feed/", "/feed", "/rss.xml", "/rss", "/atom.xml", "/index.rss", "/feeds/posts/default")
        for (path in paths) {
            try {
                val feedUrl = "https://$domain$path"
                val xml = fetchXml(feedUrl)
                if (xml.contains("<rss", true) || xml.contains("<feed", true)) {
                    val parsed = parseFeed(SourceType.RSS, xml)
                    if (parsed.size >= 3) {
                        val upgraded = Source(SourceType.RSS, feedUrl, domain, folder)
                        val updated = _sources.value.map { if (it.url == gnewsUrl) upgraded else it }
                        saveSources(updated)
                        loadPosts()
                        return
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // путь не подошёл — пробуем следующий
            }
        }
    }

    /** Меняет папку источника. */
    fun setSourceFolder(url: String, folder: String) {
        val updated = _sources.value.map {
            if (it.url == url) it.copy(folder = folder.trim()) else it
        }
        saveSources(updated)
    }

    /** Загружает посты из всех сохранённых источников. */
    fun loadPosts(): Job {
        // Отменяем предыдущую задачу загрузки, если она ещё активна
        currentLoadJob?.cancel()
        currentLoadJob = viewModelScope.launch {
            val myJob = coroutineContext[Job]
            val allSources = _sources.value
            val folder = _selectedFolder.value
            val sources = if (folder.isBlank()) allSources else allSources.filter { it.folder == folder }
            _error.value = null
            if (sources.isEmpty()) {
                _posts.value = emptyList()
                _postsBySource.value = emptyMap()
                _isLoading.value = false
                return@launch
            }
            _isLoading.value = true
            val loadStart = SystemClock.elapsedRealtime()
            var anySuccess = false
            try {
                val now = System.currentTimeMillis()
                val disabled = _disabledSources.value
                // Истёкшие блокировки сбрасываем, временно отключённые источники пропускаем.
                // Делаем это до запуска задач — на главном потоке, без гонок.
                val expired = disabled.filterValues { it <= now }.keys
                if (expired.isNotEmpty()) {
                    _disabledSources.value = _disabledSources.value - expired
                    _sourceErrorCount.value = _sourceErrorCount.value - expired
                }
                val activeSources = sources.filter { source ->
                    val disableUntil = disabled[source.url]
                    disableUntil == null || disableUntil <= now
                }
                // Этап 10: загружаем источники параллельно, но не более MAX_CONCURRENT_SOURCE_LOADS
                // одновременно. Каждый источник возвращает свой результат, общее состояние не пишется
                // до завершения всех задач. Порядок результатов = порядку источников.
                val results = loadSourcesParallel(activeSources) { source ->
                    loadSourceWithRetries(source)
                }
                // Применяем результаты ОДНИМ проходом в порядке источников и только если этот цикл
                // всё ещё текущий — отменённый старый цикл не может перезаписать состояние нового.
                if (currentLoadJob === myJob) {
                    for (r in results) {
                        val url = r.sourceUrl
                        if (r.error != null) {
                            val e = r.error
                            e.printStackTrace()
                            // Увеличиваем счётчик неудач
                            val currentCount = _sourceErrorCount.value[url] ?: 0
                            val newCount = currentCount + 1
                            _sourceErrorCount.value = _sourceErrorCount.value + (url to newCount)
                            _sourceErrors.value = _sourceErrors.value + (url to (e.message ?: "Ошибка загрузки"))
                            // Если неудач 3 подряд — отключаем источник на 24 часа
                            if (newCount >= 3) {
                                val disableUntil = now + 24 * 60 * 60 * 1000L
                                _disabledSources.value = _disabledSources.value + (url to disableUntil)
                            }
                            // При ошибке источника оставляем его предыдущие посты в _postsBySource (не трогаем)
                        } else {
                            val parsed = r.posts.orEmpty()
                            anySuccess = true
                            // Сбрасываем счётчик неудач при успехе
                            if (_sourceErrorCount.value.containsKey(url)) {
                                _sourceErrorCount.value = _sourceErrorCount.value - url
                            }
                            // сбрасываем ошибку этого источника при успехе
                            if (_sourceErrors.value.containsKey(url)) {
                                _sourceErrors.value = _sourceErrors.value - url
                            }
                            if (parsed.isEmpty()) {
                                _sourceErrors.value = _sourceErrors.value + (url to "Источник загрузился, но новостей не найдено")
                            }
                        }
                    }
                    // Объединяем посты всех источников (включая старые посты из ошибочных источников)
                    _postsBySource.value = mergeLoadResults(_postsBySource.value, results)
                    _posts.value = FeedMerge.flattenDedupSort(_postsBySource.value)
                    _error.value = FeedMerge.decideError(anySuccess, _posts.value.size)
                    // Кэшируем загруженное для офлайна (один раз, после всех задач — без частичного кэша)
                    if (_posts.value.isNotEmpty()) {
                        saveCachedPosts(_posts.value)
                    }
                }
            } catch (e: CancellationException) {
                // Задача отменена пользователем — не считаем это ошибкой
                throw e
            } finally {
                val totalDuration = SystemClock.elapsedRealtime() - loadStart
                if (BuildConfig.DEBUG) {
                    Log.d("NewsApp.FeedLoader", "loadPosts: total=${totalDuration}ms sources=${sources.size} anySuccess=$anySuccess")
                }
                // Сбрасываем флаг загрузки только если текущая корутина всё ещё активна
                // и не была отменена. Если старая задача отменена новой, не сбрасываем флаг,
                // чтобы не скрыть активную загрузку от пользователя.
                if (currentLoadJob === myJob) {
                    _isLoading.value = false
                }
            }
        }
        return currentLoadJob!!
    }

    /**
     * Загружает посты одного источника с ограниченными повторными попытками.
     *
     * Повторяет только сетевые ошибки (IOException) и HTTP 5xx.
     * Не повторяет HTTP 4xx, ошибки парсинга XML и отменённые операции.
     * Максимум 3 попытки: первая + 2 повтора с задержками 1с и 2с.
     */
    private suspend fun loadSourceWithRetries(source: Source): List<Post> {
        val maxAttempts = 3
        val delays = listOf(1000L, 2000L) // задержки перед 2-й и 3-й попыткой
        var lastException: Exception? = null
        val myJob = currentCoroutineContext()[Job]!!  // Текущая корутина для проверки отмены
        val sourceStart = SystemClock.elapsedRealtime()
        val sourceId = safeSourceId(source)
        var attemptsUsed = 0
        var sourceResult = "failure"
        var sourcePosts = 0
        for (attempt in 0 until maxAttempts) {
            // Проверяем отмену перед каждой попыткой
            if (!myJob.isActive) throw CancellationException("Загрузка отменена")
            val attemptStart = SystemClock.elapsedRealtime()
            try {
                val feedUrl = if (source.type == "YOUTUBE") resolveYoutubeFeedUrl(source.url) else source.url
                val xml = withContext(Dispatchers.Default) {
                    fetchXml(feedUrl)
                }
                // Проверяем отмену корутины после сетевого запроса
                if (!myJob.isActive) throw CancellationException("Загрузка отменена")
                var parsed = withContext(Dispatchers.Default) {
                    parseFeed(source.type, xml)
                }
                // Ограничиваем количество постов из одного источника
                if (parsed.size > MAX_POSTS_PER_SOURCE) {
                    parsed = parsed.take(MAX_POSTS_PER_SOURCE)
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
                            // Проверяем отмену корутины после второго сетевого запроса
                            if (!myJob.isActive) throw CancellationException("Загрузка отменена")
                            val parsed2 = withContext(Dispatchers.Default) {
                                parseFeed(source.type, xml2)
                            }
                            // склеиваем без дублей
                            val seen = parsed.map { it.id }.toSet()
                            parsed = parsed + parsed2.filter { it.id !in seen }
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        e.printStackTrace()
                    }
                }
                // Итоговый лимит постов источника (после возможной склейки 2-й страницы Telegram)
                if (parsed.size > MAX_POSTS_PER_SOURCE) {
                    parsed = parsed.take(MAX_POSTS_PER_SOURCE)
                }
                attemptsUsed = attempt + 1
                sourceResult = "success"
                sourcePosts = parsed.size
                if (BuildConfig.DEBUG) {
                    val attemptDuration = SystemClock.elapsedRealtime() - attemptStart
                    Log.d("NewsApp.FeedLoader", "source=$sourceId attempt=$attemptsUsed/$maxAttempts duration=${attemptDuration}ms result=success posts=${parsed.size}")
                }
                return parsed
            } catch (e: CancellationException) {
                attemptsUsed = attempt + 1
                sourceResult = "cancelled"
                if (BuildConfig.DEBUG) {
                    val attemptDuration = SystemClock.elapsedRealtime() - attemptStart
                    Log.d("NewsApp.FeedLoader", "source=$sourceId attempt=$attemptsUsed/$maxAttempts duration=${attemptDuration}ms result=cancelled")
                }
                throw e
            } catch (e: Exception) {
                lastException = e
                attemptsUsed = attempt + 1
                sourceResult = "failure"
                val errorType = if (e is java.io.IOException) "IOException" else if (e.message?.startsWith("HTTP 5") == true) "HTTP5xx" else "Exception"
                if (BuildConfig.DEBUG) {
                    val attemptDuration = SystemClock.elapsedRealtime() - attemptStart
                    Log.d("NewsApp.FeedLoader", "source=$sourceId attempt=$attemptsUsed/$maxAttempts duration=${attemptDuration}ms result=failure error=$errorType")
                }
                if (!shouldRetry(e)) {
                    throw e
                }
                // Задержка перед следующей попыткой (если не последняя)
                if (attempt < maxAttempts - 1) {
                    val delayMs = delays[attempt]
                    withContext(Dispatchers.Default) {
                        delay(delayMs)
                    }
                    // Проверяем отмену после задержки
                    if (!myJob.isActive) throw CancellationException("Загрузка отменена")
                }
            }
        }
        val sourceDuration = SystemClock.elapsedRealtime() - sourceStart
        if (BuildConfig.DEBUG) {
            Log.d("NewsApp.FeedLoader", "source=$sourceId total=${sourceDuration}ms attempts=$attemptsUsed result=$sourceResult posts=$sourcePosts")
        }
        throw lastException ?: Exception("Неизвестная ошибка загрузки источника")
    }
    /**
     * Безопасный идентификатор источника для диагностических логов: хост из URL,
     * иначе siteName, иначе "unknown". Полный URL не логируется.
     */
    private fun safeSourceId(source: Source): String {
        return try {
            URI(source.url).host ?: source.label ?: "unknown"
        } catch (e: Exception) {
            source.label ?: "unknown"
        }
    }

    /** Дайджест «Утро»: посты за последние 12 часов (кроме уже прочитанных). */
    fun getMorningDigest(): List<Post> {
        val now = System.currentTimeMillis()
        val twelveHoursAgo = now - 12 * 60 * 60 * 1000L
        return _posts.value.filter { post ->
            post.publishedAt?.let { dateStr ->
                try {
                    val sdf = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.US)
                    val parsed: java.util.Date? = sdf.parse(dateStr)
                    val postTime = parsed?.time ?: 0L
                    postTime >= twelveHoursAgo && !isRead(post)
                } catch (e: Exception) {
                    false
                }
            } ?: false
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

    /**
     * Апскейл картинок для CDN, которые отдают миниатюры по размеру в пути (BBC ichef и т.п.).
     * 240px → 1024px: резче на полном экране.
     */
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
                if (e is CancellationException) throw e
                e.printStackTrace()
                url
            }
        }
    }

    /** Категоризация статьи по темам на основе заголовка и описания. */
    /**
     * Telegram: парсим публичное превью канала t.me/s/<channel> (без API-ключа).
     * Каждый пост — .tgme_widget_message внутри message_bubble.
     */
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

    // ---------- Кэш полных текстов статей (ночной офлайн) ----------
    /** Сохраняет полный текст статьи для офлайн-чтения. */
    fun cacheFullText(postId: String, text: String) {
        prefs.edit().putString("fulltext_$postId", text).apply()
    }

    /** Возвращает закешированный полный текст статьи или null. */
    fun getCachedFullText(postId: String): String? {
        return prefs.getString("fulltext_$postId", null)
    }

    /** Удаляет закешированный текст статьи. */
    fun clearCachedFullText(postId: String) {
        prefs.edit().remove("fulltext_$postId").apply()
    }

    /** Очищает кэш постов (офлайн-копии). */
    fun clearCache() {
        prefs.edit().remove("posts_json").apply()
        _posts.value = emptyList()
    }

}
