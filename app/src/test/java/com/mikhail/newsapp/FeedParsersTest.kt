package com.mikhail.newsapp

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Unit-тесты парсеров ленты (FeedLoading.kt) и политики повторов.
 * kxml2 в testImplementation даёт настоящий XmlPullParser в JVM —
 * без него android.jar-stub вернул бы null и все парсер-тесты упали бы с NPE.
 *
 * Контракт, который защищают эти тесты (этап 7P/8):
 *  - валидный XML без публикаций → пустой список, БЕЗ исключения;
 *  - битый XML → исключение пробрасывается вызывающему коду (а не превращается в "пустой успех");
 *  - shouldRetry: только IOException и HTTP 5xx, никаких 4xx/ошибок парсинга.
 */
class FeedParsersTest {

    // ---------- RSS ----------

    private val validRss = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>Test Feed</title>
            <link>https://example.com/</link>
            <item>
              <title>First Post</title>
              <link>https://example.com/1</link>
              <description>Desc one</description>
              <pubDate>Mon, 10 Nov 2026 10:00:00 +0000</pubDate>
              <enclosure url="https://example.com/img1.jpg" type="image/jpeg" length="100"/>
            </item>
            <item>
              <title>Second Post</title>
              <link>https://example.com/2</link>
              <pubDate>Tue, 11 Nov 2026 10:00:00 +0000</pubDate>
              <source url="https://origin.site">Origin Site</source>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    @Test
    fun parseRss_validFeedWithItems_returnsAllPosts() {
        val posts = parseRss(validRss)
        assertEquals(2, posts.size)
        val first = posts.first { it.title == "First Post" }
        assertEquals("https://example.com/1", first.sourceUrl)
        assertEquals("Desc one", first.description)
        assertEquals("Mon, 10 Nov 2026 10:00:00 +0000", first.publishedAt)
        assertEquals("https://example.com/img1.jpg", first.imageUrl)
    }

    @Test
    fun parseRss_gnewsSourceTag_setsSiteUrlAndSiteName() {
        val posts = parseRss(validRss)
        val second = posts.first { it.title == "Second Post" }
        assertEquals("https://origin.site", second.siteUrl)
        assertEquals("Origin Site", second.siteName)
    }

    @Test
    fun parseRss_validFeedWithoutItems_returnsEmptyListNoException() {
        val xml = """<?xml version="1.0"?><rss version="2.0"><channel><title>Empty</title></channel></rss>"""
        val posts = parseRss(xml)
        assertTrue("Валидный пустой фид должен вернуть пустой список без исключения", posts.isEmpty())
    }

    @Test
    fun parseRss_emptyBody_returnsEmptyListNoException() {
        // Пустое тело ответа (HTTP 200 + 0 байт) — не падаем, возвращаем пустой список.
        assertTrue(parseRss("").isEmpty())
    }

    @Test
    fun parseRss_malformedXml_throwsInsteadOfSilentEmpty() {
        try {
            parseRss("<>")
            fail("Битый XML должен бросать исключение, а не возвращать пустой список")
        } catch (e: Exception) {
            // ожидаемо: исключение дошло до вызывающего кода
            assertTrue(e !is CancellationException)
        }
    }

    @Test
    fun parseRss_mismatchedEndTag_throwsInsteadOfSilentEmpty() {
        // ExpatPullParser на устройстве строгий; kxml2 в JVM бросает на несовпадающем end tag.
        try {
            parseRss("""<?xml version="1.0"?><rss version="2.0"><channel></rss>""")
            fail("XML с несовпадающими тегами должен бросать исключение")
        } catch (e: Exception) {
            assertTrue(e !is CancellationException)
        }
    }

    // ---------- YouTube Atom ----------

    private val validAtom = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom" xmlns:media="http://search.yahoo.com/mrss/">
          <title>Test Channel</title>
          <entry>
            <title>Video One</title>
            <link rel="alternate" href="https://www.youtube.com/watch?v=abc123"/>
            <published>2026-11-10T10:00:00+00:00</published>
            <media:thumbnail url="https://i.ytimg.com/vi/abc123/hqdefault.jpg"/>
          </entry>
        </feed>
    """.trimIndent()

    @Test
    fun parseYouTube_validAtomFeed_returnsEntries() {
        val posts = parseYouTube(validAtom)
        assertEquals(1, posts.size)
        assertEquals("Video One", posts[0].title)
        assertEquals("https://www.youtube.com/watch?v=abc123", posts[0].sourceUrl)
        assertEquals("https://i.ytimg.com/vi/abc123/hqdefault.jpg", posts[0].imageUrl)
        assertEquals("2026-11-10T10:00:00+00:00", posts[0].publishedAt)
    }

    @Test
    fun parseYouTube_validFeedWithoutEntries_returnsEmptyList() {
        val xml = """<?xml version="1.0"?><feed xmlns="http://www.w3.org/2005/Atom"><title>Empty</title></feed>"""
        assertTrue(parseYouTube(xml).isEmpty())
    }

    @Test
    fun parseYouTube_malformedXml_throwsInsteadOfSilentEmpty() {
        try {
            parseYouTube("<>")
            fail("Битый Atom должен бросать исключение")
        } catch (e: Exception) {
            assertTrue(e !is CancellationException)
        }
    }

    // ---------- Telegram (Jsoup, lenient) ----------

    private val validTelegramHtml = """
        <html><body>
        <div class="tgme_channel_header_title">TestChan</div>
        <div class="tgme_widget_message" data-post="testchan/42">
          <div class="tgme_widget_message_text">Hello world post</div>
          <a class="tgme_widget_message_date" href="https://t.me/testchan/42">
            <time datetime="2026-11-11T12:00:00+00:00">12:00</time>
          </a>
        </div>
        </body></html>
    """.trimIndent()

    @Test
    fun parseTelegram_validPreviewHtml_returnsPosts() {
        val posts = parseTelegram(validTelegramHtml)
        assertEquals(1, posts.size)
        assertEquals("https://t.me/testchan/42", posts[0].id)
        assertTrue(posts[0].title.contains("Hello world post"))
        assertEquals("TestChan", posts[0].siteName)
        assertEquals("2026-11-11T12:00:00+00:00", posts[0].publishedAt)
    }

    @Test
    fun parseTelegram_garbageHtml_returnsEmptyListWithoutCrash() {
        // Jsoup ленивый: любой мусор парсится без исключения, просто сообщений нет.
        assertTrue(parseTelegram("this is {{{ not html").isEmpty())
        assertTrue(parseTelegram("").isEmpty())
    }

    // ---------- shouldRetry ----------

    @Test
    fun shouldRetry_ioException_isTrue() {
        assertTrue(shouldRetry(java.io.IOException("timeout")))
    }

    @Test
    fun shouldRetry_http5xx_isTrue() {
        assertTrue(shouldRetry(Exception("HTTP 503")))
        assertTrue(shouldRetry(Exception("HTTP 500")))
    }

    @Test
    fun shouldRetry_http4xx_isFalse() {
        assertTrue(!shouldRetry(Exception("HTTP 404")))
        assertTrue(!shouldRetry(Exception("HTTP 403")))
    }

    @Test
    fun shouldRetry_parseError_isFalse() {
        assertTrue(!shouldRetry(Exception("org.xmlpull.v1.XmlPullParserException: broken")))
    }

    // ---------- Guard отмены (документирует фикс мёртвых elvis-проверок) ----------

    @Test
    fun cancellationGuard_throwsWhenJobCancelled() = runBlocking {
        val job = launch { }
        job.cancel()
        job.join()
        var thrown = false
        try {
            // Тот же guard, что теперь в loadPosts/loadSourceWithRetries:
            // if (!myJob.isActive) throw CancellationException(...) — реально бросает.
            if (!job.isActive) throw CancellationException("Загрузка отменена")
        } catch (e: CancellationException) {
            thrown = true
        }
        assertTrue("Guard должен бросать CancellationException для отменённой задачи", thrown)
    }

    @Test
    fun cancellationGuard_doesNotThrowForActiveJob() = runBlocking {
        val job = launch { kotlinx.coroutines.delay(10_000) }
        try {
            if (!job.isActive) throw CancellationException("Загрузка отменена")
            // активная задача — исключения нет
        } finally {
            job.cancel()
        }
    }
}
