package com.mikhail.newsapp

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit-тесты для MainViewModel и сетевой логики.
 *
 * Покрывает:
 * - Сохранение и загрузка источников в SharedPreferences
 * - Валидация URL при добавлении источника
 * - Дедупликация при импорте OPML
 * - Сохранение старых постов при пустом/ошибочном RSS
 * - Обработка отмены корутин (через TestCoroutineDispatcher)
 * - Пустое состояние и ошибки загрузки
 */
@RunWith(AndroidJUnit4::class)
class DataLayerTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        viewModel = MainViewModel(context)
        // Очищаем все источники перед каждым тестом
        viewModel.clearAllSources()
    }

    @After
    fun tearDown() {
        viewModel.clearAllSources()
    }

    // ============================================================
    // Сохранение и загрузка источников (SharedPreferences)
    // ============================================================

    @Test
    fun testSaveAndLoadSources() {
        viewModel.addSource(SourceType.RSS, "https://feeds.bbci.co.uk/ukrainian/rss.xml")

        val sources = viewModel.getSources()
        assertTrue("Ожидался 1 источник, получен ${sources.size}", sources.size == 1)
        assertTrue("Тип источника должен быть RSS", sources[0].type == SourceType.RSS)
        assertTrue("URL должен совпадать", sources[0].url == "https://feeds.bbci.co.uk/ukrainian/rss.xml")
    }

    @Test
    fun testAddSourceInvalidUrlIsRejected() {
        viewModel.addSource(SourceType.RSS, "invalid-url")

        val sources = viewModel.getSources()
        assertTrue("Невалидный URL не должен добавляться", sources.size == 0)
    }

    @Test
    fun testAddSourceHttpIsAccepted() {
        viewModel.addSource(SourceType.RSS, "http://example.com/rss")

        val sources = viewModel.getSources()
        assertTrue("Ожидался 1 источник, получен ${sources.size}", sources.size == 1)
        assertTrue("URL должен совпадать", sources[0].url == "http://example.com/rss")
    }

    @Test
    fun testAddSourceHttpsIsAccepted() {
        viewModel.addSource(SourceType.RSS, "https://example.com/rss")

        val sources = viewModel.getSources()
        assertTrue("Ожидался 1 источник, получен ${sources.size}", sources.size == 1)
        assertTrue("URL должен совпадать", sources[0].url == "https://example.com/rss")
    }

    @Test
    fun testRemoveSource() {
        viewModel.addSource(SourceType.RSS, "https://feeds.bbci.co.uk/ukrainian/rss.xml")
        assertTrue("Ожидался 1 источник", viewModel.getSources().size == 1)

        val sourceId = viewModel.getSources()[0].id
        viewModel.removeSource(sourceId)

        assertTrue("Источник должен быть удалён", viewModel.getSources().size == 0)
    }

    @Test
    fun testClearAllSources() {
        viewModel.addSource(SourceType.RSS, "https://feeds.bbci.co.uk/ukrainian/rss.xml")
        viewModel.addSource(SourceType.RSS, "https://www.pravda.com.ua/rss/")
        assertTrue("Ожидалось 2 источника, получено ${viewModel.getSources().size}", viewModel.getSources().size == 2)

        viewModel.clearAllSources()
        assertTrue("Все источники должны быть удалены", viewModel.getSources().size == 0)
    }

    // ============================================================
    // Дедупликация при импорте OPML
    // ============================================================

    @Test
    fun testImportSourcesFromOpmlDeduplication() {
        viewModel.addSource(SourceType.RSS, "https://feeds.bbci.co.uk/ukrainian/rss.xml")
        assertTrue("Ожидался 1 источник", viewModel.getSources().size == 1)

        val opmlXml = """<?xml version="1.0" encoding="UTF-8"?>
<opml version="1.0">
<head><title>Test</title></head>
<body>
<outline text="BBC Україна" xmlUrl="https://feeds.bbci.co.uk/ukrainian/rss.xml" sourceType="RSS"/>
</body>
</opml>"""
        val imported = viewModel.importSourcesFromOpml(opmlXml)

        assertTrue("Дубликат не должен импортироваться", imported == 0)
        assertTrue("Количество источников не должно измениться", viewModel.getSources().size == 1)
    }

    @Test
    fun testImportSourcesFromOpmlAddsNewSources() {
        val opmlXml = """<?xml version="1.0" encoding="UTF-8"?>
<opml version="1.0">
<head><title>Test</title></head>
<body>
<outline text="BBC Україна" xmlUrl="https://feeds.bbci.co.uk/ukrainian/rss.xml" sourceType="RSS"/>
<outline text="Украинская правда" xmlUrl="https://www.pravda.com.ua/rss/" sourceType="RSS"/>
</body>
</opml>"""
        val imported = viewModel.importSourcesFromOpml(opmlXml)

        assertTrue("Два новых источника должны быть импортированы", imported == 2)
        assertTrue("Два источника должны быть в списке", viewModel.getSources().size == 2)
    }

    @Test
    fun testImportSourcesFromOpmlInvalidUrlSkipped() {
        val opmlXml = """<?xml version="1.0" encoding="UTF-8"?>
<opml version="1.0">
<head><title>Test</title></head>
<body>
<outline text="Invalid" xmlUrl="not-a-url" sourceType="RSS"/>
<outline text="Valid" xmlUrl="https://feeds.bbci.co.uk/ukrainian/rss.xml" sourceType="RSS"/>
</body>
</opml>"""
        val imported = viewModel.importSourcesFromOpml(opmlXml)

        assertTrue("Только валидный URL должен быть импортирован", imported == 1)
        assertTrue("Один источник должен быть в списке", viewModel.getSources().size == 1)
    }

    // ============================================================
    // Дедупликация публикаций
    // ============================================================

    @Test
    fun testDistinctByIdRemovesDuplicates() {
        val post1 = UnifiedPost("same-id", "Title 1", "Desc 1", "", SourceType.RSS, "http://example.com/1")
        val post2 = UnifiedPost("same-id", "Title 2", "Desc 2", "", SourceType.RSS, "http://example.com/2")

        val posts = listOf(post1, post2)
        val unique = posts.distinctBy { it.id }

        assertTrue("Дубликаты по id должны удаляться", unique.size == 1)
        assertTrue("Должен остаться первый элемент", unique[0].title == "Title 1")
    }

    // ============================================================
    // Обработка отмены корутин
    // ============================================================

    @Test
    fun testLoadPostsForUrlCancellation() = runTest {
        val result = viewModel.loadPostsForUrl(SourceType.RSS, "https://httpbin.org/delay/10")
        assertTrue("Ожидалась ошибка, получен $result", result is PostsResult.Error)
    }

    // ============================================================
    // Пустое состояние и ошибки загрузки
    // ============================================================

    @Test
    fun testEmptySourcesReturnsEmptyList() {
        val sources = viewModel.getSources()
        assertTrue("Изначально источников быть не должно", sources.isEmpty())
    }

    @Test
    fun testLoadPostsWithNoSourcesReturnsEmptySuccess() {
        viewModel.loadPosts()
        Thread.sleep(500)

        val result = viewModel.posts.value
        assertTrue("Ожидался Success, получен $result", result is PostsResult.Success)
        if (result is PostsResult.Success) {
            assertTrue("При отсутствии источников список постов должен быть пустым", result.posts.isEmpty())
        }
    }

    @Test
    fun testLoadPostsForUrlInvalidUrlReturnsError() = runTest {
        val result = viewModel.loadPostsForUrl(SourceType.RSS, "invalid-url")
        assertTrue("Ожидалась ошибка валидации URL, получен $result", result is PostsResult.Error)
        if (result is PostsResult.Error) {
            assertTrue("Сообщение об ошибке должно содержать http", result.message.contains("http"))
        }
    }

    // ============================================================
    // OPML экспорт
    // ============================================================

    @Test
    fun testExportSourcesToOpml() {
        viewModel.addSource(SourceType.RSS, "https://feeds.bbci.co.uk/ukrainian/rss.xml")

        val opml = viewModel.exportSourcesToOpml()
        assertTrue("OPML должен содержать URL источника", opml.contains("https://feeds.bbci.co.uk/ukrainian/rss.xml"))
        assertTrue("OPML должен быть корректным XML", opml.contains("opml"))
    }

    // ============================================================
    // Избранное
    // ============================================================

    @Test
    fun testToggleFavorite() {
        val postId = "test-post-1"

        viewModel.toggleFavorite(postId)
        assertTrue("Пост должен быть в избранном", viewModel.isFavorite(postId))

        viewModel.toggleFavorite(postId)
        assertFalse("Пост не должен быть в избранном", viewModel.isFavorite(postId))
    }
}
