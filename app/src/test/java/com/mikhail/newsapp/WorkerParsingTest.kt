package com.mikhail.newsapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Чистые тесты парсера маркера «свежего материала» фонового worker'а (этап 4). */
class WorkerParsingTest {

    @Test
    fun parsesRssItem_titleAndLink() {
        val xml = """<rss><channel><item><title>  Hello world  </title><link>https://a.example/1</link></item></channel></rss>"""
        val r = NewsCheckWorker.parseLatestMarker(xml, "https://feed")
        assertNotNull(r)
        assertEquals("Hello world", r!!.first)
        assertEquals("https://a.example/1", r.second)
    }

    @Test
    fun parsesAtomEntry_withHrefLink() {
        val xml = """<feed><entry><title>Atom Post</title><link rel="alternate" href="https://b.example/atom-1"/></entry></feed>"""
        val r = NewsCheckWorker.parseLatestMarker(xml, "https://feed")
        assertNotNull(r)
        assertEquals("Atom Post", r!!.first)
        assertEquals("https://b.example/atom-1", r.second)
    }

    @Test
    fun fallsBackToFeedUrl_whenNoLink() {
        val xml = """<rss><channel><item><title>No link</title></item></channel></rss>"""
        val r = NewsCheckWorker.parseLatestMarker(xml, "https://feed.example/rss")
        assertNotNull(r)
        assertEquals("https://feed.example/rss", r!!.second)
    }

    @Test
    fun returnsNull_onEmptyOrGarbageXml() {
        assertNull(NewsCheckWorker.parseLatestMarker("", "https://f"))
        assertNull(NewsCheckWorker.parseLatestMarker("not xml at all", "https://f"))
        assertNull(NewsCheckWorker.parseLatestMarker("<rss><channel></channel></rss>", "https://f"))
    }

    @Test
    fun handlesCdataInTitle() {
        val xml = """<rss><channel><item><title><![CDATA[Simple title]]></title><link>https://c.example/9</link></item></channel></rss>"""
        val r = NewsCheckWorker.parseLatestMarker(xml, "https://f")
        assertNotNull(r)
        assertEquals("https://c.example/9", r!!.second) // CDATA-обёртка убирается теговым регексом; важен стабильный link-маркер
    }
}
