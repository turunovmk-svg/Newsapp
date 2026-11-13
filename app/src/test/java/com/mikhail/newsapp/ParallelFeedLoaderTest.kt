package com.mikhail.newsapp

import com.mikhail.newsapp.data.Post
import com.mikhail.newsapp.data.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit-тесты параллельной загрузки источников (этап 10): лимит конкурентности,
 * частичный отказ, стабильный порядок, отмена, слияние результатов.
 * Источники — заглушки с задержками и счётчиком активных «запросов», без реальной сети.
 */
class ParallelFeedLoaderTest {

    private fun src(name: String) = Source("RSS", "https://$name.example/feed", name)

    private fun post(id: String, sourceUrl: String? = "https://$id.example/a") = Post(
        id = id, title = "t$id", description = null,
        publishedAt = "2026-11-13T10:00:00", sourceUrl = sourceUrl,
        originalUrl = null, imageUrl = null
    )

    @Test
    fun allSourcesSucceed_andParallelIsFasterThanSequential() = runBlocking {
        val sources = (1..5).map { src("s$it") }
        val start = System.currentTimeMillis()
        val results = loadSourcesParallel(sources) { s ->
            delay(100)
            listOf(post(s.url))
        }
        val elapsed = System.currentTimeMillis() - start
        assertEquals(5, results.size)
        assertTrue("все должны загрузиться", results.all { it.error == null && it.posts?.size == 1 })
        // последовательно было бы 5*100=500мс; с лимитом 3 — две волны ≈ 200мс
        assertTrue("ожидалась параллельность: elapsed=${elapsed}мс при 500мс последовательных", elapsed < 400)
    }

    @Test
    fun concurrency_neverExceedsLimit() = runBlocking {
        val sources = (1..10).map { src("s$it") }
        val active = AtomicInteger(0)
        val maxActive = AtomicInteger(0)
        val start = System.currentTimeMillis()
        val results = loadSourcesParallel(sources, maxConcurrent = 3) { s ->
            val a = active.incrementAndGet()
            maxActive.updateAndGet { m -> maxOf(m, a) }
            try {
                delay(30)
                listOf(post(s.url))
            } finally {
                active.decrementAndGet()
            }
        }
        val elapsed = System.currentTimeMillis() - start
        assertEquals(10, results.size)
        assertTrue("лимит нарушен: maxActive=${maxActive.get()}", maxActive.get() <= 3)
        assertTrue("ожидалась реальная параллельность (maxActive>=2), max=${maxActive.get()}", maxActive.get() >= 2)
        // 10 задач по 30мс с лимитом 3: минимум 4 волны ≈ 120мс; последовательно было бы 300мс
        assertTrue("слишком медленно, похоже на последовательность: ${elapsed}мс", elapsed < 300)
    }

    @Test
    fun oneSourceFails_othersStillLoad() = runBlocking {
        val sources = listOf(src("ok1"), src("bad"), src("ok2"))
        val results = loadSourcesParallel(sources) { s ->
            if (s.url.contains("bad")) throw java.io.IOException("нет сети")
            delay(20)
            listOf(post(s.url))
        }
        assertEquals(3, results.size)
        assertNull(results[0].error)
        assertEquals(1, results[0].posts?.size)
        assertTrue("bad должен иметь ошибку", results[1].error is java.io.IOException)
        assertNull(results[1].posts)
        assertNull(results[2].error)
    }

    @Test
    fun resultsStayInSourceOrder_evenWhenLaterSourcesFinishFirst() = runBlocking {
        val sources = (1..5).map { src("s$it") }
        val results = loadSourcesParallel(sources) { s ->
            val n = s.url.filter { it.isDigit() }.toInt()
            delay((6 - n) * 40L) // s5 завершается первым, s1 — последним
            listOf(post(s.url))
        }
        assertEquals("порядок результатов должен совпадать с порядком источников",
            sources.map { it.url }, results.map { it.sourceUrl })
    }

    @Test
    fun cancellation_propagatesAsCancellationException() = runBlocking {
        val sources = (1..4).map { src("s$it") }
        val deferred = async {
            loadSourcesParallel(sources) { delay(1000); emptyList() }
        }
        delay(100)
        deferred.cancel()
        var thrown = false
        try {
            deferred.await()
        } catch (e: CancellationException) {
            thrown = true
        }
        assertTrue("отмена должна пробрасывать CancellationException, а не глушиться", thrown)
    }

    @Test
    fun emptySourceList_returnsEmptyWithoutError() = runBlocking {
        val results = loadSourcesParallel(emptyList()) { throw AssertionError("не должно вызываться") }
        assertTrue(results.isEmpty())
    }

    @Test
    fun mergeResults_dedupsSamePostAcrossSources() {
        val p1 = post("1", "https://dup.example/a")
        val p1dup = post("2", "https://dup.example/a") // тот же originalUrl? нет — sourceUrl; сделаем originalUrl
        val dup = p1.copy(id = "2", originalUrl = "https://real.example/a")
        val base = p1.copy(originalUrl = "https://real.example/a")
        val results = listOf(
            SourceLoadResult("https://a.example/feed", listOf(base), null),
            SourceLoadResult("https://b.example/feed", listOf(dup), null)
        )
        val merged = mergeLoadResults(emptyMap(), results)
        val flat = FeedMerge.flattenDedupSort(merged)
        assertEquals("дубль по originalUrl должен исчезнуть", 1, flat.size)
    }

    @Test
    fun mergeResults_failedSource_keepsItsPreviousPosts() {
        val old = listOf(post("old", "https://old.example/a"))
        val bySource = mapOf("https://bad.example/feed" to old)
        val results = listOf(SourceLoadResult("https://bad.example/feed", null, Exception("fail")))
        val merged = mergeLoadResults(bySource, results)
        assertEquals("прежние посты упавшего источника должны остаться", old, merged["https://bad.example/feed"])
        assertEquals(old, FeedMerge.flattenDedupSort(merged))
    }

    @Test
    fun mergeResults_successReplacesOnlyThatSource() {
        val oldA = listOf(post("a1", "https://a1.example"))
        val oldB = listOf(post("b1", "https://b1.example"))
        val bySource = mapOf("https://a.example/feed" to oldA, "https://b.example/feed" to oldB)
        val freshA = listOf(post("a2", "https://a2.example"))
        val results = listOf(
            SourceLoadResult("https://a.example/feed", freshA, null),
            SourceLoadResult("https://b.example/feed", null, Exception("x"))
        )
        val merged = mergeLoadResults(bySource, results)
        assertEquals(freshA, merged["https://a.example/feed"])
        assertEquals(oldB, merged["https://b.example/feed"])
    }

    @Test
    fun mergeResults_emptyValidFeed_keepsOldPosts() {
        // валидный пустой ответ (posts = emptyList, error = null) не должен затирать старые посты
        val old = listOf(post("old", "https://old.example"))
        val bySource = mapOf("https://a.example/feed" to old)
        val results = listOf(SourceLoadResult("https://a.example/feed", emptyList(), null))
        val merged = mergeLoadResults(bySource, results)
        assertEquals(old, merged["https://a.example/feed"])
    }
}
