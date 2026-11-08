package com.mikhail.newsapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

/**
 * Полнотекстовое извлечение статьи (readability-lite).
 * Когда RSS отдаёт только анонс — открываем страницу сайта
 * и вытаскиваем основной текст и картинку.
 */
data class FullArticle(
    val text: String,
    val imageUrl: String?,
    val title: String?
)

object ArticleReader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(15_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    /** True, если в описании мало текста и стоит попробовать достать полную статью. */
    fun needsFullText(description: String?): Boolean {
        val clean = description?.let { com.mikhail.newsapp.data.Post.stripHtml(it) } ?: ""
        return clean.length in 1 until 500
    }

    suspend fun fetch(url: String): FullArticle? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"
                )
                .header("Accept", "text/html,application/xhtml+xml")
                .build()
            val html = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.string() ?: return@withContext null
            }
            extract(html)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Извлекает контент из HTML: og:image + самый текстoвый блок. */
    fun extract(html: String): FullArticle? {
        return try {
            val doc = Jsoup.parse(html)

            // OG-картинка или первая крупная
            val ogImage: String = doc.select("meta[property=og:image]").attr("content").orEmpty()
            val twImage: String = doc.select("meta[name=twitter:image]").attr("content").orEmpty()
            val bigImg: String = doc.select("img")
                .firstOrNull { img: org.jsoup.nodes.Element ->
                    val w = img.attr("width").toIntOrNull() ?: 0
                    val src = img.attr("src").orEmpty()
                    src.startsWith("http") && w >= 300
                }?.attr("src").orEmpty()
            val imageUrl: String? = ogImage.ifEmpty { twImage }.ifEmpty { bigImg }.ifEmpty { null }

            // Заголовок
            val ogTitle: String = doc.select("meta[property=og:title]").attr("content").orEmpty()
            val h1Text: String = doc.selectFirst("h1")?.text().orEmpty()
            val title: String = ogTitle.ifEmpty { h1Text }.ifEmpty { doc.title().orEmpty() }

            // Убираем мусор
            doc.select("script, style, nav, header, footer, aside, iframe, form, .ad, .ads, .advert, .social, .share, .comments, .related, .menu, .sidebar").remove()

            // Основной текст: article → main → [itemprop=articleBody] → эвристика
            val articleEl: org.jsoup.nodes.Element? = doc.selectFirst("article")
                ?: doc.selectFirst("main")
                ?: doc.selectFirst("[itemprop=articleBody]")

            var contentEl: org.jsoup.nodes.Element? = articleEl
            val articleP = articleEl?.select("p")?.size ?: 0
            if (articleP < 2) {
                // Эвристика: блок с максимальным суммарным текстом в <p>
                val candidates = doc.select("div, section")
                var best: org.jsoup.nodes.Element? = null
                var bestLen = 0
                for (el: org.jsoup.nodes.Element in candidates) {
                    val ps = el.select("p")
                    if (ps.size >= 2) {
                        val len = ps.sumOf { it.text().length }
                        if (len > bestLen) {
                            bestLen = len
                            best = el
                        }
                    }
                }
                contentEl = best
            }

            val paragraphs = contentEl?.select("p")
                ?.map { it.text().trim() }
                ?.filter { it.length > 20 }
                ?: emptyList()

            val text: String = if (paragraphs.isNotEmpty()) {
                paragraphs.joinToString("\n\n")
            } else {
                doc.body()?.text()?.take(6000) ?: ""
            }

            if (text.length < 100) return null
            FullArticle(text = text, imageUrl = imageUrl, title = title)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
