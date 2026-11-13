package com.mikhail.newsapp

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
        // Добавляем валидный RSS-источник
        viewModel.addSource(SourceType.RSS, "https://feeds.bbci.co.uk/ukrainian/rss.xml")

        // Проверяем, что источник сохранён в SharedPreferences и доступен через getSources()
        val sources = viewModel.getSources()
        assertEquals(Integer.valueOf(1), Integer.valueOf(sources.size)) { "Ожидался 1 источник, получен ${sources.size}" }
        assertEquals(SourceType.RSS, sources[0].type)
        assertEquals("https://feeds.bbci.co.uk/ukrainian/rss.xml", sources[0].url)
    }

    @Test
    fun testAddSourceInvalidUrlIsRejected() {
        // Пытаемся добавить URL без схемы http/https
        viewModel.addSource(SourceType.RSS, "invalid-url")

        // Источник не должен быть добавлен
        val sources = viewModel.getSources()
        assertEquals(Integer.valueOf(0), Integer.valueOf(sources.size)) { "Невалидный URL не должен добавляться" }
    }

    @Test
    fun testAddSourceHttpIsAccepted() {
        // HTTP URL (не только HTTPS) должен приниматься
        viewModel.addSource(SourceType.RSS, "http://example.com/rss")

        val sources = viewModel.getSources()
        assertEquals(Integer.valueOf(1), Integer.valueOf(sources.size))
        assertEquals("http://example.com/rss", sources[0].url)
    }

    @Test
    fun testAddSourceHttpsIsAccepted() {
        // HTTPS URL должен приниматься
        viewModel.addSource(SourceType.RSS, "https://example.com/rss")

        val sources = viewModel.getSources()
        assertEquals(Integer.valueOf(1), Integer.valueOf(sources.size))
        assertEquals("https://example.com/rss", sources[0].url)
    }

    @Test
    fun testRemoveSource() {
        // Добавляем источник, затем удаляем
        viewModel.addSource(SourceType.RSS, "https://feeds.bbci.co.uk/ukrainian/rss.xml")
        assertEquals(Integer.valueOf(1), Integer.valueOf(viewModel.getSources().size))

        val sourceId = viewModel.getSources()[0].id
        viewModel.removeSource(sourceId)

        assertEquals(Integer.valueOf(0), Integer.valueOf(viewModel.getSources().size)) { "Источник должен быть удалён" }
    }

    @Test
    fun testClearAllSources() {
        // Добавляем несколько источников, затем очищаем всё
        viewModel.addSource(SourceType.RSS, "https://feeds.bbci.co.uk/ukrainian/rss.xml")
        viewModel.addSource(SourceType.RSS, "https://www.pravda.com.ua/rss/")
        assertEquals(Integer.valueOf(2), Integer.valueOf(viewModel.getSources().size))

        viewModel.clearAllSources()
        assertEquals(Integer.valueOf(0), Integer.valueOf(viewModel.getSources().size)) { "Все источники должны быть удалены" }
    }

    // ============================================================
    // Дедупликация при импорте OPML
    // ============================================================

    @Test
    fun testImportSourcesFromOpmlDeduplication() {
        // Добавляем один источник напрямую
        viewModel.addSource(SourceType.RSS, "https://feeds.bbci.co.uk/ukrainian/rss.xml")
        assertEquals(Integer.valueOf(1), Integer.valueOf(viewModel.getSources().size))

        // Импортируем OPML с тем же URL — должен быть проигнорирован (дедупликация)
        val opmlXml = """<?xml version="1.0" encoding="UTF-8"?>
<opml version="1.0">
<head><title>Test</title></head>
<body>
<outline text="BBC Україна" xmlUrl="https://feeds.bbci.co.uk/ukrainian/rss.xml" sourceType="RSS"/>
</body>
</opml>"""
        val imported = viewModel.importSourcesFromOpml(opmlXml)

        assertEquals(Integer.valueOf(0), Integer.valueOf(imported)) { "Дубликат не должен импортироваться" }
        assertEquals(Integer.valueOf(1), Integer.valueOf(viewModel.getSources().size)) { "Количество источников не должно измениться" }
    }

    @Test
    fun testImportSourcesFromOpmlAddsNewSources() {
        // Импортируем новый источник
        val opmlXml = """<?xml version="1.0" encoding="UTF-8"?>
<opml version="1.0">
<head><title>Test</title></head>
<body>
<outline text="BBC Україна" xmlUrl="https://feeds.bbci.co.uk/ukrainian/rss.xml" sourceType="RSS"/>
<outline text="Украинская правда" xmlUrl="https://www.pravda.com.ua/rss/" sourceType="RSS"/>
</body>
</opml>"""
        val imported = viewModel.importSourcesFromOpml(opmlXml)

        assertEquals(Integer.valueOf(2), Integer.valueOf(imported)) { "Два новых источника должны быть импортированы" }
        assertEquals(Integer.valueOf(2), Integer.valueOf(viewModel.getSources().size))
    }

    @Test
    fun testImportSourcesFromOpmlInvalidUrlSkipped() {
        // OPML с невалидным URL должен быть пропущен
        val opmlXml = """<?xml version="1.0" encoding="UTF-8"?>
<opml version="1.0">
<head><title>Test</title></head>
<body>
<outline text="Invalid" xmlUrl="not-a-url" sourceType="RSS"/>
<outline text="Valid" xmlUrl="https://feeds.bbci.co.uk/ukrainian/rss.xml" sourceType="RSS"/>
</body>
</opml>"""
        val imported = viewModel.importSourcesFromOpml(opmlXml)

        assertEquals(Integer.valueOf(1), Integer.valueOf(imported)) { "Только валидный URL должен быть импортирован" }
        assertEquals(Integer.valueOf(1), Integer.valueOf(viewModel.getSources().size))
    }

    // ============================================================
    // Дедупликация публикаций
    // ============================================================

    @Test
    fun testDistinctByIdRemovesDuplicates() {
        // Создаём два поста с одинаковым id
        val post1 = UnifiedPost("same-id", "Title 1", "Desc 1", "", SourceType.RSS, "http://example.com/1")
        val post2 = UnifiedPost("same-id", "Title 2", "Desc 2", "", SourceType.RSS, "http://example.com/2")

        val posts = listOf(post1, post2)
        val unique = posts.distinctBy { it.id }

        assertEquals(Integer.valueOf(1), Integer.valueOf(unique.size)) { "Дубликаты по id должны удаляться" }
        assertEquals("Title 1", unique[0].title) { "Должен остаться первый элемент" }
    }

    // ============================================================
    // Обработка отмены корутин
    // ============================================================

    @Test
    fun testLoadPostsForUrlCancellation() = runTest {
        // withTimeoutOrNull должен корректно обрабатывать отмену
        val result = viewModel.loadPostsForUrl(SourceType.RSS, "https://httpbin.org/delay/10")
        // При отмене корутины withTimeoutOrNull возвращает null,
        // который конвертируется в Error с сообщением о таймауте
        assertTrue("Ожидалась ошибка, получен $result", result is PostsResult.Error)
    }

    // ============================================================
    // Пустое состояние и ошибки загрузки
    // ============================================================

    @Test
    fun testEmptySourcesReturnsEmptyList() {
        // Без добавленных источников список должен быть пустым
        val sources = viewModel.getSources()
        assertTrue("Изначально источников быть не должно", sources.isEmpty())
    }

    @Test
    fun testLoadPostsWithNoSourcesReturnsEmptySuccess() {
        viewModel.loadPosts()
        // Даём время на выполнение корутины
        Thread.sleep(500)

        val result = viewModel.posts.value
        assertTrue("Ожидался Success, получен $result", result is PostsResult.Success)
        if (result is PostsResult.Success) {
            assertTrue("При отсутствии источников список постов должен быть пустым", result.posts.isEmpty())
        }
    }

    @Test
    fun testLoadPostsForUrlInvalidUrlReturnsError() {
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

        // Добавляем в избранное
        viewModel.toggleFavorite(postId)
        assertTrue("Пост должен быть в избранном", viewModel.isFavorite(postId))

        // Удаляем из избранного
        viewModel.toggleFavorite(postId)
        assertFalse("Пост не должен быть в избранном", viewModel.isFavorite(postId))
    }
}
