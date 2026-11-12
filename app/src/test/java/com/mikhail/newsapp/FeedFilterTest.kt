package com.mikhail.newsapp

import com.mikhail.newsapp.data.Post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit-тесты чистой логики поиска/фильтрации ленты (без Android/Compose). */
class FeedFilterTest {

    private fun post(
        id: String,
        title: String,
        description: String? = null,
        sourceUrl: String? = "https://example.com/rss",
        siteName: String? = null,
        publishedAt: String = "2026-11-11T10:00:00"
    ) = Post(
        id = id,
        title = title,
        description = description,
        publishedAt = publishedAt,
        sourceUrl = sourceUrl,
        originalUrl = null,
        imageUrl = null,
        siteUrl = null,
        siteName = siteName
    )

    private val posts = listOf(
        post("1", "Рынок акций вырос", "Индексы обновили максимум", siteName = "Хабр"),
        post("2", "Новый процессор AMD", "Чип на 5 нм", sourceUrl = "https://youtube.com/watch?v=abc"),
        post("3", "Обзор языка Kotlin", "Сравнение с Java", sourceUrl = "https://t.me/s/channel/12"),
        post("4", "Reddit discussion", "Comments about Kotlin", sourceUrl = "https://www.reddit.com/r/kotlin/.rss")
    )

    @Test
    fun `поиск по части заголовка без учёта регистра`() {
        val found = FeedFilter.filter(posts, "AMD", "Все", null)
        assertEquals(1, found.size)
        assertEquals("2", found.first().id)
    }

    @Test
    fun `поиск по описанию`() {
        val found = FeedFilter.filter(posts, "максимум", "Все", null)
        assertEquals(1, found.size)
        assertEquals("1", found.first().id)
    }

    @Test
    fun `поиск игнорирует регистр`() {
        val lower = FeedFilter.filter(posts, "kotlin", "Все", null)
        val upper = FeedFilter.filter(posts, "KOTLIN", "Все", null)
        assertEquals(2, lower.size) // заголовок поста 3 + описание поста 4
        assertEquals(lower.map { it.id }, upper.map { it.id })
    }

    @Test
    fun `пустой запрос возвращает все новости`() {
        assertEquals(posts.size, FeedFilter.filter(posts, "", "Все", null).size)
        assertEquals(posts.size, FeedFilter.filter(posts, "   ", "Все", null).size)
    }

    @Test
    fun `без совпадений возвращается пустой список`() {
        assertTrue(FeedFilter.filter(posts, "zzzzz-нетакого", "Все", null).isEmpty())
    }

    @Test
    fun `фильтр по типу источника`() {
        assertEquals(1, FeedFilter.filter(posts, "", "YouTube", null).size)
        assertEquals(1, FeedFilter.filter(posts, "", "Telegram", null).size)
        assertEquals(1, FeedFilter.filter(posts, "", "Reddit", null).size)
        assertEquals(1, FeedFilter.filter(posts, "", "Статьи", null).size)
    }

    @Test
    fun `фильтр по конкретному источнику`() {
        val options = FeedFilter.sourceOptions(posts)
        val habr = options.first { it.label == "Хабр" }
        val byHabr = FeedFilter.filter(posts, "", "Все", habr.key)
        assertEquals(1, byHabr.size)
        assertEquals("1", byHabr.first().id)
    }

    @Test
    fun `возврат к всем источникам сбрасывает фильтр`() {
        val options = FeedFilter.sourceOptions(posts)
        val habr = options.first { it.label == "Хабр" }
        assertEquals(1, FeedFilter.filter(posts, "", "Все", habr.key).size)
        assertEquals(posts.size, FeedFilter.filter(posts, "", "Все", null).size)
    }

    @Test
    fun `поиск и фильтр применяются одновременно`() {
        val options = FeedFilter.sourceOptions(posts)
        val habr = options.first { it.label == "Хабр" }
        // В источнике Хабр нет слова "процессор"
        assertTrue(FeedFilter.filter(posts, "процессор", "Все", habr.key).isEmpty())
        // Тип Telegram + запрос "Kotlin" — найдётся только пост 3
        val found = FeedFilter.filter(posts, "Kotlin", "Telegram", null)
        assertEquals(1, found.size)
        assertEquals("3", found.first().id)
    }

    @Test
    fun `фильтрация сохраняет порядок сортировки по дате`() {
        // Порядок как у ViewModel: от свежих к старым
        val dated = listOf(
            post("b", "Новая", publishedAt = "2026-11-01T00:00:00"),
            post("c", "Средняя", publishedAt = "2026-06-01T00:00:00"),
            post("a", "Старая", publishedAt = "2026-01-01T00:00:00")
        )
        val found = FeedFilter.filter(dated, "", "Все", null)
        assertEquals(listOf("b", "c", "a"), found.map { it.id })
        // Фильтр не переставляет элементы
        val filtered = FeedFilter.filter(dated, "ая", "Все", null)
        assertEquals(listOf("b", "a"), filtered.map { it.id })
    }

    @Test
    fun `опции источников уникальны и отсортированы`() {
        val options = FeedFilter.sourceOptions(posts + posts)
        val keys = options.map { it.key }
        assertEquals(keys.size, keys.distinct().size)
        assertEquals(options.map { it.label.lowercase() }.sorted(), options.map { it.label.lowercase() })
        assertTrue(options.any { it.label == "Хабр" })
        assertTrue(options.any { it.label.contains("youtube") })
    }

    @Test
    fun `пустые заголовки и описания не ломают фильтр`() {
        val edge = listOf(
            post("x", "", description = null),
            post("y", "Заголовок", description = "")
        )
        assertEquals(2, FeedFilter.filter(edge, "", "Все", null).size)
        assertTrue(FeedFilter.filter(edge, "что-то", "Все", null).isEmpty())
    }
}
