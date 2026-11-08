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
    val imageUrl: String?,
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
                        .put("imageUrl", post.imageUrl)
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
                            imageUrl = if (obj.isNull("imageUrl")) null else obj.optString("imageUrl", null),
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
            return try {
                val result = StringBuilder()
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(java.io.StringReader(text))
                var eventType = parser.eventType
                var skipDepth = 0
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            val tag = parser.name?.lowercase() ?: ""
                            if (tag == "script" || tag == "style" || tag == "head" || tag == "noscript") {
                                skipDepth++
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (skipDepth == 0) {
                                result.append(parser.text)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            val tag = parser.name?.lowercase() ?: ""
                            if (tag == "script" || tag == "style" || tag == "head" || tag == "noscript") {
                                if (skipDepth > 0) skipDepth--
                            }
                        }
                    }
                    eventType = parser.next()
                }
                // Декодируем базовые HTML-сущности и чистим мусорные строки
                result.toString()
                    .replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
                    .replace("&#39;", "'")
                    .replace("&laquo;", "«")
                    .replace("&raquo;", "»")
                    .replace("&mdash;", "—")
                    .replace("&ndash;", "–")
                    .lines()
                    .filterNot { line ->
                        val t = line.trim()
                        t.isEmpty() || t == "Copy" || t == "Download Context Embed View In Channel"
                    }
                    .joinToString("\n")
                    .trim()
            } catch (e: Exception) {
                text // возвращаем как есть при ошибке
            }
        }
    }
}
