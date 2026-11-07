package com.mikhail.newsapp

import android.content.Context
import com.mikhail.newsapp.data.Source
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.OutputStream

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
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()

            parser.startDocument("UTF-8", true)
            parser.startTag(null, "opml")
            parser.attribute(null, "version", "2.0")

            parser.startTag(null, "head")
            parser.startTag(null, "title")
            parser.text("NewsApp subscriptions")
            parser.endTag(null, "title")
            parser.endTag(null, "head")

            parser.startTag(null, "body")
            for (source in sources) {
                parser.startTag(null, "outline")
                parser.attribute(null, "text", source.url)
                parser.attribute(null, "title", source.url)
                parser.attribute(null, "type", "rss")
                parser.attribute(null, "xmlUrl", source.url)
                parser.endTag(null, "outline")
            }
            parser.endTag(null, "body")

            parser.endTag(null, "opml")
            parser.endDocument()
            outputStream.flush()
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
            parser.setInput(inputStream)

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG &&
                    parser.name.equals("outline", ignoreCase = true)
                ) {
                    val xmlUrl = parser.getAttributeValue(null, "xmlUrl")
                        ?: parser.getAttributeValue(null, "xmlurl")
                    if (!xmlUrl.isNullOrBlank()) {
                        val rawType = parser.getAttributeValue(null, "type")?.uppercase() ?: "RSS"
                        val normalizedType = when {
                            rawType.contains("REDDIT") -> "REDDIT"
                            rawType.contains("YOUTUBE") -> "YOUTUBE"
                            else -> "RSS"
                        }
                        sources.add(Source(normalizedType, xmlUrl))
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
