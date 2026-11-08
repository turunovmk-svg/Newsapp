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
            val imageUrl = doc.select("meta[property=og:image]").attr("content")
                .ifEmpty { doc.select("meta[name=twitter:image]").attr("content") }
                .ifEmpty {
                    doc.select("img")
                        .firstOrNull { img ->
                            val w = img.attr("width").toIntOrNull() ?: 0
                            val src = img.attr("src")
                            src.startsWith("http") && w >= 300
                        }?.attr("src")
                }
                .ifEmpty { null }

            // Заголовок
            val title = doc.select("meta[property=og:title]").attr("content")
                .ifEmpty { doc.selectFirst("h1")?.text() ?: doc.title() }

            // Убираем мусор
            doc.select("script, style, nav, header, footer, aside, iframe, form, .ad, .ads, .advert, .social, .share, .comments, .related, .menu, .sidebar").remove()

            // Основной текст: article → main → [itemprop=articleBody] → самый большой блок с <p>
            var contentEl = doc.selectFirst("article")
                ?: doc.selectFirst("main")
                ?: doc.selectFirst("[itemprop=articleBody]")

            if (contentEl == null || contentEl.select("p").size < 2) {
                // Эвристика: выбираем блок с максимальным суммарным текстом в <p>
                contentEl = doc.select("div, section")
                    .filter { it.select("p").size >= 2 }
                    .maxByOrNull { el -> el.select("p").sumOf { it.text().length } }
            }

            val paragraphs = contentEl?.select("p")
                ?.map { it.text().trim() }
                ?.filter { it.length > 20 }
                ?: emptyList()

            val text = if (paragraphs.isNotEmpty()) {
                paragraphs.joinToString("\n\n")
            } else {
                // fallback — весь видимый текст body
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
