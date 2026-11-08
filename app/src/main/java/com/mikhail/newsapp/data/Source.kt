package com.mikhail.newsapp.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Тип источника:
 *  RSS       — обычная RSS/Atom-лента
 *  REDDIT    — сабреддит (реддит сам отдаёт .rss)
 *  YOUTUBE   — канал/плейлист YouTube (Atom-фид)
 *  TELEGRAM  — публичный канал Telegram (превью t.me/s)
 *  TWITTER   — профиль X/Twitter (через RSSHub)
 */
object SourceType {
    const val RSS = "RSS"
    const val REDDIT = "REDDIT"
    const val YOUTUBE = "YOUTUBE"
    const val TELEGRAM = "TELEGRAM"
    const val TWITTER = "TWITTER"
    const val GNEWS = "GNEWS"     // сайт без собственного RSS — через Google News
}

data class Source(
    val type: String,   // RSS / REDDIT / YOUTUBE / TELEGRAM / TWITTER / GNEWS
    val url: String,    // нормализованный URL ленты/фида
    val label: String? = null,  // человекочитаемое имя (домен для GNEWS)
    val folder: String = ""     // папка/категория (пусто = без папки)
) {
    companion object {
        fun toJsonList(sources: List<Source>): String {
            val array = JSONArray()
            sources.forEach { source ->
                array.put(
                    JSONObject()
                        .put("type", source.type)
                        .put("url", source.url)
                        .put("label", source.label ?: JSONObject.NULL)
                        .put("folder", source.folder)
                )
            }
            return array.toString()
        }

        fun fromJsonList(json: String): List<Source> {
            val sources = mutableListOf<Source>()
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val label = if (obj.isNull("label")) null else obj.optString("label", null)
                    sources.add(
                        Source(
                            obj.getString("type"),
                            obj.getString("url"),
                            label,
                            if (obj.isNull("folder")) "" else obj.optString("folder", "")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return sources
        }

        /**
         * Определяет тип источника по вставленной ссылке и нормализует её в URL фида.
         * Возвращает null, если ссылка не похожа ни на один поддерживаемый тип.
         */
        fun detect(input: String, forcedType: String? = null): Source? {
            val originalUrl = input.trim()
            var u = originalUrl
            if (u.isEmpty()) return null
            if (!u.startsWith("http://") && !u.startsWith("https://")) {
                u = "https://$u"
            }
            val host = run {
                val afterScheme = u.substringAfter("://")
                afterScheme.substringBefore('/').lowercase().removePrefix("www.")
            }

            // URL→фид
            val feed = when {
                // ---- Reddit ----
                host == "reddit.com" || host == "old.reddit.com" || host == "np.reddit.com" || host == "new.reddit.com" -> {
                    val path = u.substringAfter(host, "").trimEnd('/')
                    if (path.isEmpty()) return null
                    // r/xxx, user/xxx, domain/xxx,comments — все отдают .rss
                    Source(SourceType.REDDIT, "https://www.reddit.com$path.rss")
                }
                // ---- YouTube ----
                host == "youtube.com" || host == "m.youtube.com" || host == "youtu.be" || host == "music.youtube.com" -> {
                    when {
                        // уже фид канала
                        u.contains("/feeds/videos.xml") -> Source(SourceType.YOUTUBE, u)
                        // канал
                        u.contains("youtube.com/channel/") -> {
                            val id = u.substringAfter("/channel/", "").substringBefore('/').substringBefore('?')
                            Source(SourceType.YOUTUBE, "https://www.youtube.com/feeds/videos.xml?channel_id=$id")
                        }
                        // плейлист
                        u.contains("list=") -> {
                            val id = u.substringAfter("list=", "").substringBefore('&').substringBefore('#')
                            Source(SourceType.YOUTUBE, "https://www.youtube.com/feeds/videos.xml?playlist_id=$id")
                        }
                        // @handle, youtu.be, watch — резолвим позже через HTML
                        else -> Source(SourceType.YOUTUBE, u)
                    }
                }
                // ---- Telegram ----
                host == "t.me" || host == "telegram.me" || host == "telegram.dog" -> {
                    val path = u.substringAfter(host, "").trimStart('/').trimEnd('/')
                    // пользователи/приглашения — не каналы, но t.me/s работает для публичных каналов
                    if (path.isEmpty() || path.startsWith("+") || path.startsWith("joinchat")) return null
                    Source(SourceType.TELEGRAM, "https://t.me/s/$path")
                }
                // ---- Twitter / X ----
                host == "twitter.com" || host == "x.com" || host == "mobile.twitter.com" || host == "mobile.x.com" -> {
                    val handle = u.substringAfter(host, "").trimStart('/').trimEnd('/').substringBefore('/').substringBefore('?')
                        .removePrefix("@")
                    if (handle.isEmpty()) return null
                    Source(SourceType.TWITTER, "https://rsshub.app/twitter/user/$handle")
                }
                // ---- Обычная RSS/Atom лента или сайт без RSS ----
                else -> {
                    val looksLikeFeed = u.endsWith(".xml") || u.endsWith(".rss") ||
                        u.contains("/feed") || u.contains("/rss") ||
                        u.contains("atom") || u.contains("?format=rss")
                    if (looksLikeFeed) {
                        Source(SourceType.RSS, u)
                    } else {
                        // Сайт без собственного RSS → подписка через Google News
                        val domain = host.removePrefix("www.")
                        val feed = "https://news.google.com/rss/search?q=site:$domain&hl=ru&gl=UA&ceid=UA:ru"
                        Source(SourceType.GNEWS, feed, domain)
                    }
                }
            }
            // ручной выбор типа убран — detect всегда возвращает авто-тип
            return feed
        }
    }
}
