package com.mikhail.newsapp

import com.mikhail.newsapp.data.Post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Unit-тесты кэша ленты (этап 9B): сериализация Post <-> JSON и правило prune из CachePolicy.
 * org.json:json в testImplementation даёт настоящий org.json в JVM (иначе android.jar-stub).
 */
class CachePolicyTest {

    private val fmt = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)

    private fun dateStr(offsetMs: Long, now: Long): String {
        val c = Calendar.getInstance()
        c.timeInMillis = now + offsetMs
        return fmt.format(c.time)
    }

    // фиксированный "сейчас": Fri, 13 Nov 2026 12:00:00 +0000
    private val now: Long = fmt.parse("Fri, 13 Nov 2026 12:00:00 +0000").time

    private fun post(
        id: String,
        title: String,
        publishedAt: String? = dateStr(-3600_000L, now),
        description: String? = "desc $id",
        sourceUrl: String? = "https://a.example/$id",
        originalUrl: String? = "https://orig.example/$id",
        imageUrl: String? = "https://img.example/$id.jpg",
        videoUrl: String? = null,
        siteUrl: String? = "https://site.example",
        siteName: String? = "Site",
        tags: List<String> = listOf("Технологии")
    ) = Post(
        id = id, title = title, description = description, publishedAt = publishedAt,
        sourceUrl = sourceUrl, originalUrl = originalUrl, imageUrl = imageUrl,
        videoUrl = videoUrl, siteUrl = siteUrl, siteName = siteName, tags = tags
    )

    // ---------- 1. валидный кэш: round-trip сериализации ----------

    @Test
    fun jsonRoundTrip_preservesAllPostFields() {
        val posts = listOf(
            post("1", "First", videoUrl = "https://v.example/1.mp4", tags = listOf("Спорт", "Наука")),
            post("2", "Second", description = null, originalUrl = null, imageUrl = null, publishedAt = null)
        )
        val json = Post.toJsonList(posts)
        val restored = Post.fromJsonList(json)
        assertEquals(posts, restored)
    }

    @Test
    fun fromJsonList_validJson_returnsSameContentOnRepeatedParse() {
        // повторный разбор не меняет содержимое (idempotent read)
        val posts = listOf(post("a", "A"), post("b", "B"))
        val json = Post.toJsonList(posts)
        assertEquals(Post.fromJsonList(json), Post.fromJsonList(json))
        assertEquals(2, Post.fromJsonList(json).size)
    }

    // ---------- 2/3/8. отсутствующий, пустой, повреждённый кэш ----------

    @Test
    fun fromJsonList_emptyString_returnsEmptyListWithoutThrowing() {
        val r = try { Post.fromJsonList("") } catch (e: Exception) { throw AssertionError("не должно бросать", e) }
        assertTrue(r.isEmpty())
    }

    @Test
    fun fromJsonList_corruptJson_returnsEmptyListWithoutThrowing() {
        // повреждённый JSON не должен ронять приложение: парсер возвращает пустой список
        for (bad in listOf("{not valid json[[[", "<rss>not json</rss>", "12345")) {
            val r = try { Post.fromJsonList(bad) } catch (e: Exception) { throw AssertionError("не должно бросать на: $bad", e) }
            assertTrue("ожидался пустой список на: $bad", r.isEmpty())
        }
    }

    @Test
    fun prune_emptyList_returnsEmpty() {
        assertTrue(CachePolicy.prune(emptyList(), now).isEmpty())
    }

    // ---------- 4/5. правила устаревания (прежняя семантика clearOldCache) ----------

    @Test
    fun prune_dropsPostsOlderThan7Days() {
        val old = post("old", "Old", publishedAt = dateStr(-8L * 24 * 3600_000, now))
        val fresh = post("fresh", "Fresh", publishedAt = dateStr(-1L * 3600_000, now))
        val result = CachePolicy.prune(listOf(old, fresh), now)
        assertEquals(listOf(fresh), result)
    }

    @Test
    fun prune_keepsExactly7DaysOldBoundary() {
        // правило >= : ровно 7 дней назад запись ещё «свежая»
        val boundary = post("b", "Boundary", publishedAt = dateStr(-CachePolicy.MAX_CACHE_AGE_MS, now))
        assertEquals(listOf(boundary), CachePolicy.prune(listOf(boundary), now))
    }

    @Test
    fun prune_keepsPostsWithoutDateOrWithUnparseableDate() {
        val noDate = post("nd", "NoDate", publishedAt = null)
        val garbageDate = post("gd", "GarbageDate", publishedAt = "not a date at all")
        val result = CachePolicy.prune(listOf(noDate, garbageDate), now)
        assertEquals(2, result.size)
        assertTrue(result.contains(noDate) && result.contains(garbageDate))
    }

    @Test
    fun prune_isIdempotent() {
        val posts = listOf(
            post("old", "Old", publishedAt = dateStr(-30L * 24 * 3600_000, now)),
            post("fresh", "Fresh")
        )
        val once = CachePolicy.prune(posts, now)
        assertEquals(once, CachePolicy.prune(once, now))
    }

    @Test
    fun prune_freshPostsPreservedAfterCleanup() {
        // после очистки свежие записи остаются в кэше (и записываются обратно)
        val fresh = listOf(post("1", "One"), post("2", "Two"))
        val mixed = fresh + post("old", "Old", publishedAt = dateStr(-10L * 24 * 3600_000, now))
        val pruned = CachePolicy.prune(mixed, now)
        assertEquals(fresh, pruned)
        // сериализация pruned пригодна для записи в кэш
        assertEquals(fresh, Post.fromJsonList(Post.toJsonList(pruned)))
    }
}
