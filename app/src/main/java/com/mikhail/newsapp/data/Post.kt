package com.mikhail.newsapp.data

import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser

data class Post(
    val id: String,
    val title: String,
    val description: String?,
    val publishedAt: String?,
    val sourceUrl: String?,
    val imageUrl: String?
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
                            description = obj.optString("description", null),
                            publishedAt = obj.optString("publishedAt", null),
                            sourceUrl = obj.optString("sourceUrl", null),
                            imageUrl = obj.optString("imageUrl", null)
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
            if (text == null || text.isEmpty()) return text
            return try {
                val result = StringBuilder()
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(java.io.StringReader(text))
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.TEXT) {
                        result.append(parser.text)
                    }
                    eventType = parser.next()
                }
                result.toString().trim()
            } catch (e: Exception) {
                text // возвращаем как есть при ошибке
            }
        }
    }
}
