package com.mikhail.newsapp

import com.mikhail.newsapp.data.Post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // ---------- Категории ----------

    private fun categorizedPosts() = listOf(
        post("t1", "Новый процессор Intel вышел", "Чип на 3 нм для ноутбуков", siteName = "TechSite"),
        post("t2", "Исследование космоса и планет", "Ученые нашли экзопланету", siteName = "ScienceMag"),
        post("t3", "Финал чемпионата по футболу", "Сборная выиграла турнир", siteName = "SportNews"),
        post("t4", "Президент подписал закон", "Парламент одобрил законопроект", siteName = "PoliticsDaily"),
        post("t5", "Курс доллара и экономика", "Инфляция снизилась на рынке", siteName = "BizReview"),
        post("t6", "Обзор новой игры", "Геймплей и графика проверены", siteName = "GamePortal")
    )

    @Test
    fun `фильтр по категории отдельно`() {
        val posts = categorizedPosts()
        val tech = FeedFilter.categoryOptions(posts).first { it.id == "tech" }
        val found = FeedFilter.filter(posts, "", "Все", null, tech.id)
        assertEquals(1, found.size)
        assertEquals("t1", found.first().id)
    }

    @Test
    fun `категория вместе с поиском`() {
        val posts = categorizedPosts()
        val sport = FeedFilter.categoryOptions(posts).first { it.id == "sport" }
        // В категории Спорт есть слово "турнир" в описании
        val found = FeedFilter.filter(posts, "турнир", "Все", null, sport.id)
        assertEquals(1, found.size)
        assertEquals("t3", found.first().id)
        // Но поиск по слову, которого нет в этой категории — пусто
        val empty = FeedFilter.filter(posts, "процессор", "Все", null, sport.id)
        assertTrue(empty.isEmpty())
    }

    @Test
    fun `категория вместе с фильтром источника`() {
        val posts = categorizedPosts()
        val tech = FeedFilter.categoryOptions(posts).first { it.id == "tech" }
        val sourceOptions = FeedFilter.sourceOptions(posts)
        val techSite = sourceOptions.first { it.label.lowercase().contains("tech") }
        val found = FeedFilter.filter(posts, "", "Все", techSite.key, tech.id)
        assertEquals(1, found.size)
        assertEquals("t1", found.first().id)
    }

    @Test
    fun `все три условия одновременно поиск источник категория`() {
        val posts = categorizedPosts()
        val tech = FeedFilter.categoryOptions(posts).first { it.id == "tech" }
        val sourceOptions = FeedFilter.sourceOptions(posts)
        val techSite = sourceOptions.first { it.label.lowercase().contains("tech") }
        val found = FeedFilter.filter(posts, "процессор", "Все", techSite.key, tech.id)
        assertEquals(1, found.size)
        assertEquals("t1", found.first().id)
    }

    @Test
    fun `возврат к «Все категории» сбрасывает фильтр категории`() {
        val posts = categorizedPosts()
        val tech = FeedFilter.categoryOptions(posts).first { it.id == "tech" }
        val byTech = FeedFilter.filter(posts, "", "Все", null, tech.id)
        assertEquals(1, byTech.size)
        val all = FeedFilter.filter(posts, "", "Все", null, null)
        assertEquals(posts.size, all.size)
    }

    @Test
    fun `пустое состояние при несовпадении категории`() {
        val posts = categorizedPosts()
        val allCategories = FeedFilter.categoryOptions(posts).map { it.id }
        // Фильтруем по несуществующей категории
        val found = FeedFilter.filter(posts, "", "Все", null, "nonexistent-category")
        assertTrue(found.isEmpty())
    }

    @Test
    fun `фильтрация по категории сохраняет порядок сортировки`() {
        val posts = categorizedPosts()
        // Все уже отсортированы по дате (одинаковая, так что порядок сохраняется)
        val tech = FeedFilter.categoryOptions(posts).first { it.id == "tech" }
        val found = FeedFilter.filter(posts, "", "Все", null, tech.id)
        // Один пост — порядок тривиален, но проверяем что не переставляется
        assertEquals(listOf("t1"), found.map { it.id })
    }

    @Test
    fun `опции категорий уникальны и содержат ожидаемые`() {
        val posts = categorizedPosts()
        val options = FeedFilter.categoryOptions(posts)
        val ids = options.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        assertTrue(options.any { it.id == "tech" })
        assertTrue(options.any { it.id == "sport" })
        assertTrue(options.any { it.id == "science" })
        assertTrue(options.any { it.id == "politics" })
        assertTrue(options.any { it.id == "economy" })
        assertTrue(options.any { it.id == "games" })
    }

    // ---- Этап 4: поиск (регистр, частичные совпадения, пустые поля, с фильтрами) ----

    @Test
    fun search_isCaseInsensitive() {
        val p = post("1", "BREAKING News Today", description = "x")
        assertTrue(FeedFilter.matchesQuery(p, "breaking"))
        assertTrue(FeedFilter.matchesQuery(p, "NEWS"))
        assertTrue(FeedFilter.matchesQuery(p, "Breaking"))
    }

    @Test
    fun search_matchesPartialSubstring_inTitleAndDescription() {
        val p = post("1", "Market rally continues", description = "Investors cheer the rally")
        assertTrue(FeedFilter.matchesQuery(p, "rally"))
        assertTrue(FeedFilter.matchesQuery(p, "market"))
        assertTrue(FeedFilter.matchesQuery(p, "cheer"))
        assertFalse(FeedFilter.matchesQuery(p, "crash"))
    }

    @Test
    fun search_emptyQuery_matchesAll_andBlankDescriptionSafe() {
        val p1 = post("1", "A", description = null)
        val p2 = post("2", "B", description = "  ")
        assertTrue(FeedFilter.matchesQuery(p1, ""))
        assertTrue(FeedFilter.matchesQuery(p2, "   "))
        assertFalse(FeedFilter.matchesQuery(p1, "zzz"))
        assertFalse(FeedFilter.matchesQuery(p2, "zzz")) // пустое описание не падает
    }

    @Test
    fun search_combinesWithSourceFilter_withoutDuplicates() {
        val a = post("1", "Tech news", sourceUrl = "https://a.example/1")
        val b = post("2", "Tech news", sourceUrl = "https://b.example/2")
        val c = post("3", "Sports", sourceUrl = "https://a.example/3")
        val all = listOf(a, b, c)
        val res = FeedFilter.filter(all, query = "tech", type = "Все", sourceKey = FeedFilter.sourceKey(a), category = null)
        assertEquals(listOf(a), res) // поиск + фильтр источника вместе, порядок сохранён
    }

}
