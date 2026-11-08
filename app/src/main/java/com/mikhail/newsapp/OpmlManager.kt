package com.mikhail.newsapp

import android.content.Context
import com.mikhail.newsapp.data.Source
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/**
 * Импорт и экспорт подписок (RSS / Reddit / YouTube) в формате OPML 2.0.
 */
class OpmlManager(private val context: Context) {

    /**
     * Экспортирует список источников в OPML-документ.
     * Возвращает true, если весь документ записан без ошибок.
     */
    fun exportToXml(sources: List<Source>, outputStream: OutputStream): Boolean {
        return try {
            val serializer = XmlPullParserFactory.newInstance().newSerializer()
            val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
            serializer.setOutput(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag(null, "opml")
            serializer.attribute(null, "version", "2.0")

            serializer.startTag(null, "head")
            serializer.startTag(null, "title")
            serializer.text("NewsApp subscriptions")
            serializer.endTag(null, "title")
            serializer.endTag(null, "head")

            serializer.startTag(null, "body")
            for (source in sources) {
                serializer.startTag(null, "outline")
                serializer.attribute(null, "text", source.url)
                serializer.attribute(null, "title", source.url)
                serializer.attribute(null, "type", "rss")
                serializer.attribute(null, "xmlUrl", source.url)
                serializer.endTag(null, "outline")
            }
            serializer.endTag(null, "body")

            serializer.endTag(null, "opml")
            serializer.endDocument()
            writer.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Читает OPML-документ и возвращает найденные источники.
     * Поддерживает атрибут xmlUrl в элементах <outline>.
     */
    fun importFromXml(inputStream: InputStream): List<Source> {
        val sources = mutableListOf<Source>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(InputStreamReader(inputStream, StandardCharsets.UTF_8))

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG &&
                    parser.name.equals("outline", ignoreCase = true)
                ) {
                    val xmlUrl = parser.getAttributeValue(null, "xmlUrl")
                        ?: parser.getAttributeValue(null, "xmlurl")
                        ?: parser.getAttributeValue(null, "url")
                    // Feedly иногда использует title как URL, если xmlUrl отсутствует
                    val feedUrl = if (!xmlUrl.isNullOrBlank()) xmlUrl
                    else {
                        val title = parser.getAttributeValue(null, "title") ?: ""
                        if (title.startsWith("http://") || title.startsWith("https://")) title else null
                    }
                    if (!feedUrl.isNullOrBlank()) {
                        val rawType = parser.getAttributeValue(null, "type")?.uppercase() ?: "RSS"
                        val normalizedType = when {
                            rawType.contains("REDDIT") -> "REDDIT"
                            rawType.contains("YOUTUBE") -> "YOUTUBE"
                            else -> "RSS"
                        }
                        // Feedly category → папка
                        val folder = parser.getAttributeValue(null, "category")?.takeIf { it.isNotBlank() } ?: ""
                        sources.add(Source(normalizedType, feedUrl, feedUrl, folder))
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sources
    }

    /**
     * Сохраняет импортированные источники в те же Preferences, что использует MainViewModel.
     */
    fun saveSources(sources: List<Source>) {
        context.getSharedPreferences("NewsAppPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString("sources_json", Source.toJsonList(sources))
            .apply()
    }
}
