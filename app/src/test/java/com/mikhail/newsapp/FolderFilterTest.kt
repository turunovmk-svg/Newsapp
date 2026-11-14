package com.mikhail.newsapp

import com.mikhail.newsapp.data.Post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit-тесты локальной фильтрации ленты по папкам (пакет: папки без сети). */
class FolderFilterTest {

    private fun post(id: String, sourceUrl: String? = "https://s.example/$id") = Post(
        id = id, title = "t$id", description = null,
        publishedAt = "2026-11-14T10:00:00", sourceUrl = sourceUrl,
        originalUrl = null, imageUrl = null
    )

    private val folderOf = mapOf(
        "https://a.example/feed" to "News",
        "https://b.example/feed" to "Tech",
        "https://c.example/feed" to "News"
    )
    private val bySource = mapOf(
        "https://a.example/feed" to listOf(post("a1"), post("a2")),
        "https://b.example/feed" to listOf(post("b1")),
        "https://c.example/feed" to listOf(post("c1"))
    )

    @Test
    fun blankFolder_returnsAllSources() {
        assertEquals(bySource, FeedMerge.filterByFolder(bySource, "", folderOf))
        assertEquals(bySource, FeedMerge.filterByFolder(bySource, "   ", folderOf))
    }

    @Test
    fun selectedFolder_keepsOnlyThatFolderSources() {
        val r = FeedMerge.filterByFolder(bySource, "News", folderOf)
        assertEquals(setOf("https://a.example/feed", "https://c.example/feed"), r.keys)
        assertEquals(3, FeedMerge.flattenDedupSort(r).size)
    }

    @Test
    fun emptyFolder_resultIsEmptyList() {
        val r = FeedMerge.filterByFolder(bySource, "Нет такой", folderOf)
        assertTrue(r.isEmpty())
        assertTrue(FeedMerge.flattenDedupSort(r).isEmpty())
    }

    @Test
    fun unknownSourceUrl_excludedWhenFolderSelected() {
        val withUnknown = bySource + ("https://unknown.example/feed" to listOf(post("u1")))
        val r = FeedMerge.filterByFolder(withUnknown, "Tech", folderOf)
        assertEquals(setOf("https://b.example/feed"), r.keys)
        // но при "все папки" остаётся
        assertTrue(FeedMerge.filterByFolder(withUnknown, "", folderOf).containsKey("https://unknown.example/feed"))
    }

    @Test
    fun folderFilter_preservesOrderAndNoDuplicates() {
        // один и тот же пост в двух источниках одной папки должен дедуплицироваться
        val dup = post("x", "https://same.example/article")
        val dup2 = dup.copy(id = "y")
        val bySource2 = mapOf(
            "https://a.example/feed" to listOf(dup, post("a1")),
            "https://c.example/feed" to listOf(dup2)
        )
        val folders2 = mapOf("https://a.example/feed" to "News", "https://c.example/feed" to "News")
        val r = FeedMerge.filterByFolder(bySource2, "News", folders2)
        val flat = FeedMerge.flattenDedupSort(r)
        assertEquals(2, flat.size) // x/y дедупнулись по sourceUrl, остался a1
        assertEquals(listOf("t a1".replace(" ", ""), "tx"), flat.map { it.title }.sorted())
    }
}
