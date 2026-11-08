package com.mikhail.newsapp.data

import java.io.StringReader
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class Post(
    val id: String,
    val title: String,
    val description: String?,
    val publishedAt: String?,
    val sourceUrl: String?,
    val originalUrl: String? = null,  // оригинальная ссылка на статью (мимо Google News редиректа)
    val imageUrl: String?,
    val videoUrl: String? = null,  // mp4 (Telegram, <video>)
    val siteUrl: String? = null,      // домен первоисточника (для Google News)
    val siteName: String? = null      // название источника
) {
    companion object {
        fun toJsonList(posts: List<Post>): String {
            val array = JSONArray()
            posts.forEach { post ->
                array.put(
                    JSONObject()
                        .put("id", post.id)
                        .put("title", post.title)
                        .put("description", post.description)
                        .put("publishedAt", post.publishedAt)
                        .put("sourceUrl", post.sourceUrl)
                        .put("originalUrl", post.originalUrl ?: JSONObject.NULL)
                        .put("imageUrl", post.imageUrl)
                        .put("videoUrl", post.videoUrl ?: JSONObject.NULL)
                        .put("siteUrl", post.siteUrl ?: JSONObject.NULL)
                        .put("siteName", post.siteName ?: JSONObject.NULL)
                )
            }
            return array.toString()
        }

        fun fromJsonList(json: String): List<Post> {
            val posts = mutableListOf<Post>()
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    posts.add(
                        Post(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            description = if (obj.isNull("description")) null else obj.optString("description", null),
                            publishedAt = if (obj.isNull("publishedAt")) null else obj.optString("publishedAt", null),
                            sourceUrl = if (obj.isNull("sourceUrl")) null else obj.optString("sourceUrl", null),
                            originalUrl = if (obj.isNull("originalUrl")) null else obj.optString("originalUrl", null),
                            imageUrl = if (obj.isNull("imageUrl")) null else obj.optString("imageUrl", null),
                            videoUrl = if (obj.isNull("videoUrl")) null else obj.optString("videoUrl", null),
                            siteUrl = if (obj.isNull("siteUrl")) null else obj.optString("siteUrl", null),
                            siteName = if (obj.isNull("siteName")) null else obj.optString("siteName", null)
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return posts
        }

        /** Удаляет HTML-теги из текста для безопасного отображения. */
        fun stripHtml(text: String?): String? {
            if (text.isNullOrEmpty()) return text
            var result: String = text
            // 1) Парсер: пропускаем содержимое script/style/head
            try {
                val sb = StringBuilder()
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(java.io.StringReader(text))
                var eventType = parser.eventType
                var skipDepth = 0
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            val tag = parser.name?.lowercase() ?: ""
                            if (tag == "script" || tag == "style" || tag == "head" || tag == "noscript") skipDepth++
                        }
                        XmlPullParser.TEXT -> if (skipDepth == 0) sb.append(parser.text)
                        XmlPullParser.END_TAG -> {
                            val tag = parser.name?.lowercase() ?: ""
                            if ((tag == "script" || tag == "style" || tag == "head" || tag == "noscript") && skipDepth > 0) skipDepth--
                        }
                    }
                    eventType = parser.next()
                }
                result = sb.toString()
            } catch (e: Exception) {
                // парсер не справился — работаем с сырым текстом дальше
            }
            // 2) Regex-фолбэк: описание могло прийти экранированным (CDATA/entities),
            //    тогда парсер видел его как обычный текст — чистим регулярками.
            if (result.contains("<", true)) {
                result = Regex("""(?is)<\s*script.*?<\s*/script[^>]*>""").replace(result, "")
                result = Regex("""(?is)<\s*style.*?<\s*/style[^>]*>""").replace(result, "")
                result = Regex("""(?is)<[^>]+>""").replace(result, " ")
            }
            // 3) HTML-сущности
            result = result
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&laquo;", "«")
                .replace("&raquo;", "»")
                .replace("&mdash;", "—")
                .replace("&ndash;", "–")
                .replace("&hellip;", "…")
            // 4) Чистим мусорные строки (кнопки виджетов и пустое)
            result = result
                .lines()
                .filterNot { line ->
                    val t = line.trim()
                    t.isEmpty() || t == "Copy" ||
                        t == "Download Context Embed View In Channel" ||
                        t.contains("telegram.org/js/", true) ||
                        t.contains("data-telegram-post", true) ||
                        t.startsWith("data-width", true)
                }
                .joinToString("\n")
                .replace(Regex("[ \\t]{2,}"), " ")
                .trim()
            return result
        }
    }
}
