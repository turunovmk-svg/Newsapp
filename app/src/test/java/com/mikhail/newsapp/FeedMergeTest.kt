package com.mikhail.newsapp

import com.mikhail.newsapp.data.Post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-тесты чистой логики слияния ленты (FeedMerge) — регрессионная защита этапа 7P/8:
 *  - пустой (валидный) ответ источника НЕ затирает его ранее загруженные посты;
 *  - дедупликация по originalUrl/sourceUrl и по заголовку;
 *  - общая ошибка показывается только когда все источники упали И постов совсем нет
 *    (сохранённые/кэшированные посты при этом не уничтожаются и не сопровождаются "сетевой ошибкой").
 */
class FeedMergeTest {

    private fun post(
        id: String,
        title: String,
        sourceUrl: String? = "https://a.example/rss",
        originalUrl: String? = null,
        publishedAt: String? = "2026-11-10T10:00:00"
    ) = Post(
        id = id,
        title = title,
        description = null,
        publishedAt = publishedAt,
        sourceUrl = sourceUrl,
        originalUrl = originalUrl,
        imageUrl = null
    )

    // ---------- mergeSourcePosts ----------

    @Test
    fun merge_emptyParsed_keepsPreviouslyLoadedPosts() {
        val old = listOf(post("1", "Old news", sourceUrl = "https://a.example/rss"))
        val bySource = mapOf("https://a.example/rss" to old)
        val result = FeedMerge.mergeSourcePosts(bySource, "https://a.example/rss", emptyList())
        assertEquals("Пустой валидный ответ не должен затирать старые посты источника",
            old, result["https://a.example/rss"])
    }

    @Test
    fun merge_nonEmptyParsed_replacesOldPosts() {
        val old = listOf(post("1", "Old news"))
        val fresh = listOf(post("2", "Fresh news"))
        val result = FeedMerge.mergeSourcePosts(mapOf("u" to old), "u", fresh)
        assertEquals(fresh, result["u"])
    }

    @Test
    fun merge_newSource_addsNewEntry() {
        val result = FeedMerge.mergeSourcePosts(emptyMap(), "https://b.example/rss", listOf(post("9", "B")))
        assertEquals(1, result.size)
        assertEquals("B", result["https://b.example/rss"]!!.first().title)
    }

    @Test
    fun merge_failedSource_keepsItsEntryUntouched() {
        // Источник упал — loadPosts не вызывает merge вообще, но даже если вызовет с пустым — старое останется.
        val old = listOf(post("1", "Cached news"))
        val result = FeedMerge.mergeSourcePosts(mapOf("u" to old), "u", emptyList())
        assertEquals(1, result.size)
        assertEquals(old, result["u"])
    }

    // ---------- flattenDedupSort ----------

    @Test
    fun flatten_removesDuplicatesByOriginalUrl() {
        val p1 = post("1", "Same title", sourceUrl = "https://gnews/1", originalUrl = "https://real/1")
        val p2 = post("2", "Same title", sourceUrl = "https://other/2", originalUrl = "https://real/1")
        val result = FeedMerge.flattenDedupSort(mapOf("a" to listOf(p1), "b" to listOf(p2)))
        assertEquals(1, result.size)
    }

    @Test
    fun flatten_removesDuplicatesBySourceUrlWhenNoOriginalUrl() {
        val p1 = post("1", "Title A", sourceUrl = "https://x/1")
        val p2 = post("2", "Title A", sourceUrl = "https://x/1")
        val result = FeedMerge.flattenDedupSort(mapOf("a" to listOf(p1, p2)))
        assertEquals(1, result.size)
    }

    @Test
    fun flatten_removesDuplicatesByExactTitle() {
        // Один и тот же заголовок с разных источников (без URL-дубля) — дубль.
        val p1 = post("1", "Breaking News", sourceUrl = "https://a/1")
        val p2 = post("2", "breaking news", sourceUrl = "https://b/2") // case-insensitive
        val result = FeedMerge.flattenDedupSort(mapOf("a" to listOf(p1), "b" to listOf(p2)))
        assertEquals(1, result.size)
    }

    @Test
    fun flatten_keepsDifferentTitlesEvenIfSimilar() {
        val p1 = post("1", "Breaking News", sourceUrl = "https://a/1")
        val p2 = post("2", "Breaking News today", sourceUrl = "https://b/2")
        val result = FeedMerge.flattenDedupSort(mapOf("a" to listOf(p1), "b" to listOf(p2)))
        assertEquals(2, result.size)
    }

    @Test
    fun flatten_sortsByPublishedAtDescending() {
        val old = post("1", "Old", sourceUrl = "https://a/1", publishedAt = "2026-11-01T10:00:00")
        val mid = post("2", "Mid", sourceUrl = "https://a/2", publishedAt = "2026-11-10T10:00:00")
        val fresh = post("3", "Fresh", sourceUrl = "https://a/3", publishedAt = "2026-11-12T10:00:00")
        val noDate = post("4", "NoDate", sourceUrl = "https://a/4", publishedAt = null)
        val result = FeedMerge.flattenDedupSort(mapOf("a" to listOf(old, noDate, fresh, mid)))
        assertEquals(listOf("Fresh", "Mid", "Old", "NoDate"), result.map { it.title })
    }

    @Test
    fun flatten_emptyMap_returnsEmptyList() {
        assertTrue(FeedMerge.flattenDedupSort(emptyMap()).isEmpty())
    }

    // ---------- decideError ----------

    @Test
    fun decideError_allSourcesFailedAndNoPosts_returnsErrorMessage() {
        assertEquals(
            "Не удалось загрузить новости ни из одного источника",
            FeedMerge.decideError(anySuccess = false, totalPosts = 0)
        )
    }

    @Test
    fun decideError_allSourcesFailedButOldPostsPreserved_returnsNull() {
        // Все источники упали, но есть сохранённые посты — НЕ показываем сетевую ошибку поверх ленты.
        assertNull(FeedMerge.decideError(anySuccess = false, totalPosts = 25))
    }

    @Test
    fun decideError_allSourcesReturnedValidEmptyFeeds_returnsNull() {
        // Все источники успешно вернули корректные пустые ленты — это НЕ ошибка сети.
        assertNull(FeedMerge.decideError(anySuccess = true, totalPosts = 0))
    }

    @Test
    fun decideError_atLeastOneSourceSucceeded_returnsNull() {
        assertNull(FeedMerge.decideError(anySuccess = true, totalPosts = 5))
    }
}
