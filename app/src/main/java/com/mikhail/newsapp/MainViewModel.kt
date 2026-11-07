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
import org.xmlpull.v1.XmlPullParserFactory

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        getApplication<Application>().getSharedPreferences("NewsAppPrefs", Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _sources = MutableStateFlow<List<Source>>(emptyList())
    val sources: StateFlow<List<Source>> = _sources.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadSources()
        loadPosts()
    }

    /** Загружает сохранённые источники из SharedPreferences. */
    fun loadSources() {
        val json = prefs.getString("sources_json", null) ?: return
        _sources.value = Source.fromJsonList(json)
    }

    /** Сохраняет источники в SharedPreferences. */
    private fun saveSources(sources: List<Source>) {
        val json = Source.toJsonList(sources)
        prefs.edit().putString("sources_json", json).apply()
        _sources.value = sources
    }

    /** Добавляет источник, сохраняет в SharedPreferences и запускает загрузку постов. */
    fun addSource(type: String, url: String) {
        val current = _sources.value
        if (current.any { it.url == url }) return // дубликат
        val newSource = Source(type, url)
        val updated = current + newSource
        saveSources(updated)
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
            try {
                val allPosts = mutableListOf<Post>()
                for (source in sources) {
                    try {
                        val xml = withContext(Dispatchers.Default) {
                            fetchXml(source.url)
                        }
                        val parsed = withContext(Dispatchers.Default) {
                            parseFeed(source.type, xml)
                        }
                        allPosts.addAll(parsed)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                allPosts.sortByDescending { it.publishedAt }
                _posts.value = allPosts
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchXml(url: String): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful || response.body == null) {
                    throw Exception("HTTP ${response.code}")
                }
                response.body.string()
            }
        }
    }

    private suspend fun parseFeed(type: String, xml: String): List<Post> {
        return withContext(Dispatchers.Default) {
            when (type) {
                "RSS" -> parseRss(xml)
                "REDDIT" -> parseRss(xml) // Reddit .rss тоже валидный RSS
                "YOUTUBE" -> parseYouTube(xml)
                else -> emptyList()
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
                                posts.add(Post(
                                    id = currentLink ?: currentTitle.hashCode().toString(),
                                    title = currentTitle ?: "",
                                    description = currentDesc,
                                    publishedAt = currentDate,
                                    sourceUrl = currentLink,
                                    imageUrl = null
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
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty() && inEntry) {
                            when (currentTag) {
                                "title" -> currentTitle = text
                                "summary" -> currentDesc = text
                                "published" -> currentDate = text
                                "link" -> { /* href в атрибуте */ }
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
                                    imageUrl = null
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
}
