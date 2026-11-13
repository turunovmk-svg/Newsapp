package com.mikhail.newsapp

import com.mikhail.newsapp.data.Post
import com.mikhail.newsapp.data.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Чистая (без Android) логика загрузки ленты, вынесена из MainViewModel для unit-тестов:
 *  - FeedParsers: разбор RSS/Atom/YouTube/Telegram в список Post (исключения НЕ глушатся —
 *    валидный пустой фид возвращает пустой список, битый XML пробрасывает исключение вызывающему коду);
 *  - FeedMerge: слияние постов по источникам, дедупликация, решение об общей ошибке;
 *  - shouldRetry: политика повторных попыток;
 *  - loadSourcesParallel/mergeLoadResults: параллельная загрузка источников с лимитом конкурентности.
 */

/** Максимум одновременно загружаемых источников (этап 10). */
const val MAX_CONCURRENT_SOURCE_LOADS = 3

/** Результат загрузки одного источника: либо посты, либо ошибка. */
data class SourceLoadResult(
    val sourceUrl: String,
    val posts: List<Post>?,   // null — источник упал
    val error: Throwable?
)

/**
 * Загружает источники параллельно, но не более [maxConcurrent] одновременно.
 *
 *  - каждый источник грузится своей задачей через [loader], промежуточное состояние не пишется;
 *  - результат возвращается В ПОРЯДКЕ исходного списка источников (awaitAll сохраняет порядок),
 *    несмотря на произвольный порядок завершения сетевых запросов;
 *  - ошибка одного источника не отменяет остальные (превращается в SourceLoadResult.error);
 *  - CancellationException пробрасывается наверх и не глушится.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun loadSourcesParallel(
    sources: List<Source>,
    maxConcurrent: Int = MAX_CONCURRENT_SOURCE_LOADS,
    loader: suspend (Source) -> List<Post>
): List<SourceLoadResult> = coroutineScope {
    val semaphore = Semaphore(maxConcurrent)
    sources.map { source ->
        async {
            semaphore.withPermit {
                try {
                    SourceLoadResult(source.url, loader(source), null)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    SourceLoadResult(source.url, null, e)
                }
            }
        }
    }.awaitAll()
}

/**
 * Применяет результаты загрузки к карте постов по источникам строго в порядке списка результатов:
 * успешные перезаписывают/дополняют свой источник, упавшие не трогают прежние посты.
 * Вызывать один раз после завершения всех задач — без гонок.
 */
internal fun mergeLoadResults(
    bySource: Map<String, List<Post>>,
    results: List<SourceLoadResult>
): Map<String, List<Post>> {
    var acc = bySource
    for (r in results) {
        if (r.posts != null) {
            acc = FeedMerge.mergeSourcePosts(acc, r.sourceUrl, r.posts)
        }
    }
    return acc
}

/** Повторяем только сетевые ошибки (IOException) и HTTP 5xx. 4xx и ошибки парсинга — нет. */
internal fun shouldRetry(e: Throwable): Boolean {
    return e is java.io.IOException || e.message?.let { it.startsWith("HTTP 5") } == true
}

/** Чистая логика слияния/дедупликации ленты. */
object FeedMerge {

    /**
     * Обновляет посты источника после загрузки.
     * Пустой (но валидный) ответ НЕ затирает ранее загруженные посты источника.
     */
    fun mergeSourcePosts(
        bySource: Map<String, List<Post>>,
        sourceUrl: String,
        parsed: List<Post>
    ): Map<String, List<Post>> =
        if (parsed.isNotEmpty()) bySource + (sourceUrl to parsed) else bySource

    fun normalizeTitle(t: String): String = t.lowercase().trim()

    /**
     * Склеивает посты всех источников, убирает дубли:
     *  1) по оригинальной ссылке (originalUrl для GNEWS, иначе sourceUrl, иначе title);
     *  2) по точному совпадению нормализованного заголовка.
     * Сортирует по дате публикации (новые сверху).
     */
    fun flattenDedupSort(bySource: Map<String, List<Post>>): List<Post> {
        val allPosts = bySource.values.flatten()
        val byUrl = allPosts.distinctBy { it.originalUrl ?: it.sourceUrl ?: it.title }
        val seenTitles = mutableSetOf<String>()
        val deduplicated = byUrl.filter { post ->
            val key = normalizeTitle(post.title)
            if (key.isEmpty()) true // без заголовка не считаем дублем
            else if (key in seenTitles) false
            else { seenTitles.add(key); true }
        }
        return deduplicated.sortedByDescending { it.publishedAt ?: "" }
    }

    /**
     * Общая ошибка ленты: показываем только если ВСЕ источники упали и совсем нет постов
     * (ни свежих, ни сохранённых ранее). Валидные пустые ленты ошибкой не считаются.
     */
    fun decideError(anySuccess: Boolean, totalPosts: Int): String? =
        if (!anySuccess && totalPosts == 0) "Не удалось загрузить новости ни из одного источника" else null
}

private fun upgradeImageUrl(url: String): String {
    val u = url.trim()
    return when {
        // BBC: https://ichef.bbci.co.uk/ace/ws/240/... → .../1024/...
        u.contains("ichef.bbci.co.uk/ace/ws/") ->
            Regex("""\/ace\/ws\/\d+\/""").replace(u, "/ace/ws/1024/")
        u.contains("ichef-1.bbci.co.uk/ace/ws/") ->
            Regex("""\/ace\/ws\/\d+\/""").replace(u, "/ace/ws/1024/")
        else -> u
    }
}

private fun categorizePost(title: String?, description: String?): List<String> {
    val text = (title ?: "") + " " + (description ?: "")
    val lower = text.lowercase()
    val tags = mutableListOf<String>()
    if (lower.contains("политик") || lower.contains("выбор") || lower.contains("парламент") || lower.contains("президент") || lower.contains("депутат")) tags.add("Политика")
    if (lower.contains("технолог") || lower.contains("гитхаб") || lower.contains("программ") || lower.contains("айти") || lower.contains("разработ") || lower.contains("сервер") || lower.contains("код")) tags.add("Технологии")
    if (lower.contains("экон") || lower.contains("доллар") || lower.contains("валюта") || lower.contains("рынок") || lower.contains("бизнес") || lower.contains("финанс")) tags.add("Экономика")
    if (lower.contains("спорт") || lower.contains("футбол") || lower.contains("баскетбол") || lower.contains("олимпи")) tags.add("Спорт")
    if (lower.contains("культур") || lower.contains("кино") || lower.contains("музык") || lower.contains("книг") || lower.contains("艺术")) tags.add("Культура")
    if (lower.contains("наука") || lower.contains("исследов") || lower.contains("учен") || lower.contains("космос")) tags.add("Наука")
    if (lower.contains("здоров") || lower.contains("медиц") || lower.contains("врач") || lower.contains("лечен") || lower.contains("эпидеми")) tags.add("Здоровье")
    if (lower.contains("погода") || lower.contains("климат") || lower.contains("эколог") || lower.contains("природ")) tags.add("Природа")
    return tags
}

internal fun parseRss(xml: String): List<Post> {
    val posts = mutableListOf<Post>()
    
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
        var currentImageWidth = 0
        var currentSiteUrl: String? = null
        var currentSiteName: String? = null
        var currentOriginalUrl: String? = null
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
                        currentImageWidth = 0
                        currentSiteUrl = null
                        currentSiteName = null
                        currentOriginalUrl = null
                    } else if (inItem && tag == "source") {
                        // <source url="https://site.domain">Название</source> (Google News)
                        currentSiteUrl = parser.getAttributeValue(null, "url")
                    } else if (inItem && tag == "link" && currentLink == null) {
                        // <link> — для GNEWS это редирект Google, для обычных RSS — прямая ссылка на статью
                        currentOriginalUrl = parser.nextText()
                        currentLink = currentOriginalUrl
                    } else if (inItem && (tag == "enclosure" || tag == "content" || tag == "thumbnail"
                            || tag == "media:content" || tag == "media:thumbnail" || tag == "itunes:image")) {
                        val imgUrl = parser.getAttributeValue(null, "url")
                            ?: parser.getAttributeValue(null, "href")
                            ?: parser.getAttributeValue(null, "image")
                        if (imgUrl != null && imgUrl.isNotEmpty()) {
                            val declaredWidth = parser.getAttributeValue(null, "width")?.toIntOrNull() ?: 0
                            // берём самый большой из доступных
                            if (declaredWidth >= currentImageWidth) {
                                currentImageWidth = declaredWidth
                                currentImageUrl = imgUrl
                            } else if (currentImageUrl == null) {
                                currentImageUrl = imgUrl
                            }
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
                                originalUrl = currentOriginalUrl,
                                imageUrl = finalImageUrl?.let { upgradeImageUrl(it) },
                                siteUrl = currentSiteUrl,
                                siteName = currentSiteName,
                                tags = categorizePost(currentTitle, currentDesc)
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
    
    return posts
}

internal fun parseYouTube(xml: String): List<Post> {
    val posts = mutableListOf<Post>()
    
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
    
    return posts
}

internal fun parseTelegram(html: String): List<Post> {
    val posts = mutableListOf<Post>()
    
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
            // Только текст сообщения: без кнопок ("Поделиться", "Обсудить" и т.п.)
            val rawText = textEl?.let { el ->
                el.select("script, style, a.tgme_widget_message_link_inline, .tgme_widget_message_meta").remove()
                el.wholeText().trim().ifEmpty { el.text().trim() }
            }.orEmpty()
            // Видео-пост: <video src=...> + постер (thumbnail)
            val videoUrl = msg.selectFirst("video.js-message_video, video.tgme_widget_message_video, video")
                ?.attr("src")?.takeIf { it.isNotBlank() }
            val videoThumb = msg.selectFirst(".tgme_widget_message_video_thumb")
                ?.attr("style")
                ?.let { Regex("url\\(['\"]?([^'\")]+)['\"]?\\)").find(it)?.groupValues?.get(1) }
            val imageUrl = videoThumb
                ?: msg.selectFirst("a.tgme_widget_message_photo_wrap")?.attr("style")
                    ?.let { Regex("url\\(['\"]?([^'\")]+)['\"]?\\)").find(it)?.groupValues?.get(1) }
                ?: msg.selectFirst("img.tgme_widget_message_photo_image")?.attr("src")
                ?: msg.selectFirst("img")?.attr("src")
            if (rawText.isEmpty() && permalink == null && videoUrl == null) continue
            val title = rawText.take(120).let { if (rawText.length > 120) "$it…" else it }
            val id = permalink ?: rawText.hashCode().toString()
            posts.add(
                Post(
                    id = id,
                    title = title,
                    description = rawText.ifEmpty { null },
                    publishedAt = date,
                    sourceUrl = permalink ?: "https://t.me/s/$channelTitle",
                    imageUrl = imageUrl,
                    videoUrl = videoUrl,
                    siteUrl = null,
                    siteName = channelTitle
                )
            )
        }
    
    return posts
}
