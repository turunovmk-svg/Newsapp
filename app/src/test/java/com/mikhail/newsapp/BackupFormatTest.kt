package com.mikhail.newsapp

import com.mikhail.newsapp.data.Post
import com.mikhail.newsapp.data.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupFormatTest {

    private fun post(id: String, title: String = "t$id") = Post(
        id = id, title = title, description = "d", publishedAt = "Fri, 13 Nov 2026 10:00:00 GMT",
        sourceUrl = "https://a/$id", originalUrl = "https://orig/$id", imageUrl = null,
        videoUrl = null, siteUrl = "https://a", siteName = "A", tags = listOf("tech")
    )

    private fun src(url: String, label: String? = null, folder: String = "") =
        Source("RSS", url, label, folder)

    private fun sampleBackupJson(): String = BackupFormat.build(
        sources = listOf(src("https://a/feed", "A", "News"), src("https://b/feed")),
        favorites = listOf(post("f1"), post("f2")),
        readLater = listOf(post("r1")),
        readIds = setOf("f1", "x9"),
        readTs = mapOf("f1" to 123456L),
        settings = mapOf("font_scale" to 1.2f, "view_mode" to "grid", "sort_mode" to "source", "theme_mode" to "neon"),
        notifications = mapOf("notif_enabled" to true, "notif_interval_hours" to 6, "quiet_start" to 1380)
    )

    @Test
    fun roundTrip_allFieldsPreserved() {
        val parsed = BackupFormat.parse(sampleBackupJson())
        assertEquals(2, parsed.sources.size)
        assertEquals("A", parsed.sources[0].label)
        assertEquals("News", parsed.sources[0].folder)
        assertEquals(2, parsed.favorites.size)
        assertEquals(1, parsed.readLater.size)
        assertEquals(setOf("f1", "x9"), parsed.readIds)
        assertEquals(123456L, parsed.readTs["f1"])
        assertEquals(1.2f, parsed.settings["font_scale"])
        assertEquals("grid", parsed.settings["view_mode"])
        assertEquals(true, parsed.notifications["notif_enabled"])
        assertEquals(6, parsed.notifications["notif_interval_hours"])
        // post round-trip: все поля
        val p = parsed.favorites[0]
        assertEquals("f1", p.id)
        assertEquals("https://orig/f1", p.originalUrl)
        assertEquals(listOf("tech"), p.tags)
    }

    @Test
    fun parse_corruptJson_throws() {
        try { BackupFormat.parse("{not json[["); fail("должно бросить") }
        catch (e: BackupFormat.InvalidBackupException) { assertTrue(e.message!!.contains("JSON")) }
    }

    @Test
    fun parse_wrongFormat_throws() {
        val bad = sampleBackupJson().replace("newsapp-backup", "other-app")
        try { BackupFormat.parse(bad); fail() } catch (e: BackupFormat.InvalidBackupException) {
            assertTrue(e.message!!.contains("не резервная копия"))
        }
    }

    @Test
    fun parse_unsupportedVersion_throws() {
        val bad = sampleBackupJson().replace("\"version\":1", "\"version\":99")
        try { BackupFormat.parse(bad); fail() } catch (e: BackupFormat.InvalidBackupException) {
            assertTrue(e.message!!.contains("версия"))
        }
    }

    @Test
    fun parse_missingOptionalSections_ok() {
        val minimal = """{"format":"newsapp-backup","version":1,"sources":[]}"""
        val p = BackupFormat.parse(minimal)
        assertTrue(p.favorites.isEmpty())
        assertTrue(p.settings.isEmpty())
        assertTrue(p.notifications.isEmpty())
    }

    @Test
    fun parse_badSourceUrl_throws() {
        val bad = """{"format":"newsapp-backup","version":1,"sources":[{"type":"RSS","url":"notaurl"}]}"""
        try { BackupFormat.parse(bad); fail() } catch (e: BackupFormat.InvalidBackupException) {
            assertTrue(e.message!!.contains("URL"))
        }
    }

    @Test
    fun parse_postWithoutId_throws() {
        val bad = """{"format":"newsapp-backup","version":1,"favorites":[{"id":"","title":"x"}]}"""
        try { BackupFormat.parse(bad); fail() } catch (e: BackupFormat.InvalidBackupException) {
            assertTrue(e.message!!.contains("id"))
        }
    }

    @Test
    fun parse_outOfRangeValues_throws() {
        val bad = """{"format":"newsapp-backup","version":1,"settings":{"view_mode":"weird"}}"""
        try { BackupFormat.parse(bad); fail() } catch (e: BackupFormat.InvalidBackupException) {
            assertTrue(e.message!!.contains("view_mode"))
        }
        val bad2 = """{"format":"newsapp-backup","version":1,"notifications":{"notif_interval_hours":99}}"""
        try { BackupFormat.parse(bad2); fail() } catch (e: BackupFormat.InvalidBackupException) {
            assertTrue(e.message!!.contains("notif_interval_hours"))
        }
    }

    @Test
    fun mergeSources_dedupsByUrl_keepsExistingLabelAndFolder() {
        val current = listOf(src("https://a/feed", "Мой A", "Мои"), src("https://c/feed", "C"))
        val imported = listOf(src("https://a/feed", null, ""), src("https://b/feed", "B", "News"))
        val merged = BackupFormat.mergeSources(current, imported)
        assertEquals(3, merged.size)
        val a = merged.first { it.url == "https://a/feed" }
        assertEquals("Мой A", a.label)      // существующий label не затёрт null
        assertEquals("Мои", a.folder)       // существующая папка не затёрта пустой
        assertTrue(merged.any { it.url == "https://b/feed" && it.label == "B" })
    }

    @Test
    fun mergePosts_dedupsById_keepsExisting() {
        val current = listOf(post("f1", "старое название"))
        val imported = listOf(post("f1", "новое"), post("f2"))
        val merged = BackupFormat.mergePosts(current, imported)
        assertEquals(2, merged.size)
        assertEquals("старое название", merged.first { it.id == "f1" }.title) // существующее не перезаписано
    }

    @Test
    fun merge_readIds_unionWithoutLoss() {
        val current = setOf("a", "b")
        val imported = setOf("b", "c")
        val merged = current + imported
        assertEquals(setOf("a", "b", "c"), merged)
    }

    @Test
    fun sourceSerializer_compatibleWithExistingPrefsFormat() {
        // формат sources_json в prefs должен совпадать с форматом секции backup.sources
        val list = listOf(src("https://a/feed", "A", "F"))
        val prefsJson = Source.toJsonList(list)
        val backup = BackupFormat.build(list, emptyList(), emptyList(), emptySet(), emptyMap(), emptyMap(), emptyMap())
        val parsed = BackupFormat.parse(backup)
        assertEquals(Source.toJsonList(parsed.sources), prefsJson)
    }

    @Test
    fun build_doesNotIncludePostsCache() {
        val json = sampleBackupJson()
        assertTrue(!json.contains("posts_json"))
        assertNull(try { org.json.JSONObject(json).get("posts") } catch (e: Exception) { null })
    }
}
