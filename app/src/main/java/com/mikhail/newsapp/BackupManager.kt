package com.mikhail.newsapp

import android.content.Context
import com.mikhail.newsapp.data.Post
import com.mikhail.newsapp.data.Source
import com.mikhail.newsapp.data.SourceType
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

/**
 * Резервная копия пользовательских данных NewsApp (версионируемый JSON, один файл).
 *
 * Формат v1:
 * { "format":"newsapp-backup", "version":1, "exportedAt":<epochMs>,
 *   "sources":[Source…], "favorites":[Post…], "readLater":[Post…], "readIds":[String…],
 *   "readTs":{id:ts…}, "settings":{…}, "notifications":{…} }
 *
 * В временный кэш ленты (posts_json), изображения и runtime-счётчики НЕ входят.
 * Чистая логика (build/parse/validate/merge) отделена от Android I/O и покрыта unit-тестами.
 */
object BackupFormat {

    const val FORMAT = "newsapp-backup"
    const val VERSION = 1
    /** Лимит входного файла: 5 МБ — с запасом покрывает десятки тысяч избранных статей. */
    const val MAX_BACKUP_BYTES = 5L * 1024 * 1024

    /** Результат валидации/парсинга документа. */
    data class Parsed(
        val sources: List<Source>,
        val favorites: List<Post>,
        val readLater: List<Post>,
        val readIds: Set<String>,
        val readTs: Map<String, Long>,
        val settings: Map<String, Any?>,      // ключи prefs → значения (валидированные)
        val notifications: Map<String, Any?>
    )

    /** Исключение валидации — сообщение показываем пользователю. */
    class InvalidBackupException(message: String) : Exception(message)

    // ---------- ключи prefs, попадающие в бэкап ----------
    val SETTINGS_KEYS = listOf(
        "font_scale" to "float", "dark_theme" to "bool", "theme_mode" to "str",
        "view_mode" to "str", "sort_mode" to "str",
        "reading_font" to "str", "reading_theme" to "str"
    )
    val NOTIF_KEYS = listOf(
        NewsCheckWorker.KEY_ENABLED to "bool",
        NewsCheckWorker.KEY_INTERVAL_HOURS to "int",
        "notif_disabled_sources" to "strset",
        "quiet_start" to "int", "quiet_end" to "int",
        "notif_sound" to "bool"
    )
    // допустимые диапазоны/значения (те же, что в UI приложения)
    private val VIEW_MODES = setOf("cards", "list", "grid")
    private val SORT_MODES = setOf("date", "source")
    private val READING_FONTS = setOf("sans", "serif")
    private val READING_THEMES = setOf("light", "dark", "sepia")
    private val THEME_MODES = setOf("obsidian", "neon", "harmony")

    // ---------- экспорт ----------

    fun build(
        sources: List<Source>,
        favorites: List<Post>,
        readLater: List<Post>,
        readIds: Set<String>,
        readTs: Map<String, Long>,
        settings: Map<String, Any?>,
        notifications: Map<String, Any?>,
        exportedAt: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("exportedAt", exportedAt)
        root.put("sources", JSONArray(Source.toJsonList(sources)))
        root.put("favorites", JSONArray(Post.toJsonList(favorites)))
        root.put("readLater", JSONArray(Post.toJsonList(readLater)))
        root.put("readIds", JSONArray(readIds.toList()))
        val ts = JSONObject()
        readTs.forEach { (k, v) -> ts.put(k, v) }
        root.put("readTs", ts)
        root.put("settings", JSONObject(settings.filterValues { it != null }))
        root.put("notifications", JSONObject(notifications.filterValues { it != null }))
        return root.toString()
    }

    // ---------- импорт: валидация ----------

    /**
     * Парсит и полностью валидирует документ. При любой ошибке бросает InvalidBackupException —
     * до этого вызова ничего в хранилище не менялось.
     */
    fun parse(json: String): Parsed {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw InvalidBackupException("Файл повреждён: не является JSON")
        }
        if (root.optString("format") != FORMAT)
            throw InvalidBackupException("Это не резервная копия NewsApp")
        val version = root.optInt("version", -1)
        if (version != VERSION)
            throw InvalidBackupException("Неподдерживаемая версия резервной копии: $version")

        val sources = parseSources(root.optJSONArray("sources"))
        val favorites = parsePosts(root.optJSONArray("favorites"), "favorites")
        val readLater = parsePosts(root.optJSONArray("readLater"), "readLater")
        val readIds = parseStringArray(root.optJSONArray("readIds"), "readIds")
        val readTs = parseTsMap(root.optJSONObject("readTs"))
        val settings = parseTypedMap(root.optJSONObject("settings"), SETTINGS_KEYS, "settings")
        val notifications = parseTypedMap(root.optJSONObject("notifications"), NOTIF_KEYS, "notifications")

        // валидация значений-ограничений
        (settings["view_mode"] as? String)?.let {
            if (it !in VIEW_MODES) throw InvalidBackupException("Недопустимое значение view_mode: $it")
        }
        (settings["sort_mode"] as? String)?.let {
            if (it !in SORT_MODES) throw InvalidBackupException("Недопустимое значение sort_mode: $it")
        }
        (settings["reading_font"] as? String)?.let {
            if (it !in READING_FONTS) throw InvalidBackupException("Недопустимое значение reading_font: $it")
        }
        (settings["reading_theme"] as? String)?.let {
            if (it !in READING_THEMES) throw InvalidBackupException("Недопустимое значение reading_theme: $it")
        }
        (settings["theme_mode"] as? String)?.let {
            if (it !in THEME_MODES) throw InvalidBackupException("Недопустимое значение theme_mode: $it")
        }
        (settings["font_scale"] as? Number)?.let {
            val f = it.toFloat()
            if (f < 0.5f || f > 2.0f) throw InvalidBackupException("font_scale вне диапазона 0.5–2.0: $f")
        }
        (notifications[NewsCheckWorker.KEY_INTERVAL_HOURS] as? Number)?.let {
            val h = it.toInt()
            if (h < 1 || h > 24) throw InvalidBackupException("notif_interval_hours вне диапазона 1–24: $h")
        }
        (notifications["quiet_start"] as? Number)?.let {
            val q = it.toInt()
            if (q != -1 && q !in 0..1439) throw InvalidBackupException("quiet_start вне диапазона: $q")
        }
        (notifications["quiet_end"] as? Number)?.let {
            val q = it.toInt()
            if (q != -1 && q !in 0..1439) throw InvalidBackupException("quiet_end вне диапазона: $q")
        }
        return Parsed(sources, favorites, readLater, readIds, readTs, settings, notifications)
    }

    private fun parseSources(arr: JSONArray?): List<Source> {
        if (arr == null) return emptyList()
        val out = mutableListOf<Source>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: throw InvalidBackupException("Некорректная запись источника #$i")
            val url = o.optString("url", "")
            if (url.isBlank() || !url.startsWith("http")) throw InvalidBackupException("Некорректный URL источника: '$url'")
            val type = o.optString("type", SourceType.RSS).uppercase()
            val label = if (o.isNull("label")) null else o.optString("label")
            val folder = if (o.isNull("folder")) "" else o.optString("folder", "")
            out.add(Source(type, url, label, folder))
        }
        return out
    }

    private fun parsePosts(arr: JSONArray?, section: String): List<Post> {
        if (arr == null) return emptyList()
        val out = mutableListOf<Post>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: throw InvalidBackupException("Некорректная запись в секции $section #$i")
            val id = o.optString("id", "")
            val title = o.optString("title", "")
            if (id.isBlank()) throw InvalidBackupException("У публикации в $section пустой id")
            out.add(
                Post(
                    id = id,
                    title = title,
                    description = o.optString("description").ifEmpty { null },
                    publishedAt = o.optString("publishedAt").ifEmpty { null },
                    sourceUrl = o.optString("sourceUrl").ifEmpty { null },
                    originalUrl = o.optString("originalUrl").ifEmpty { null },
                    imageUrl = o.optString("imageUrl").ifEmpty { null },
                    videoUrl = o.optString("videoUrl").ifEmpty { null },
                    siteUrl = o.optString("siteUrl").ifEmpty { null },
                    siteName = o.optString("siteName").ifEmpty { null },
                    tags = parseStringArray(o.optJSONArray("tags"), "tags").toList()
                )
            )
        }
        return out
    }

    private fun parseStringArray(arr: JSONArray?, name: String): Set<String> {
        if (arr == null) return emptySet()
        val out = linkedSetOf<String>()
        for (i in 0 until arr.length()) {
            val s = arr.optString(i, "")
            if (s.isNotBlank()) out.add(s)
        }
        return out
    }

    private fun parseTsMap(obj: JSONObject?): Map<String, Long> {
        if (obj == null) return emptyMap()
        val out = mutableMapOf<String, Long>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = obj.optLong(k, -1)
            if (k.isNotBlank() && v >= 0) out[k] = v
        }
        return out
    }

    /** Валидирует секцию типа settings/notifications по списку (ключ → тип). */
    private fun parseTypedMap(obj: JSONObject?, spec: List<Pair<String, String>>, section: String): Map<String, Any?> {
        if (obj == null) return emptyMap()
        val out = mutableMapOf<String, Any?>()
        for ((key, type) in spec) {
            if (!obj.has(key) || obj.isNull(key)) continue
            when (type) {
                "str" -> out[key] = obj.getString(key)
                "bool" -> out[key] = obj.getBoolean(key)
                "int" -> out[key] = obj.getInt(key)
                "float" -> out[key] = obj.getDouble(key).toFloat()
                "strset" -> {
                    val arr = obj.optJSONArray(key)
                        ?: throw InvalidBackupException("Секция $section: $key должен быть массивом строк")
                    out[key] = parseStringArray(arr, key)
                }
            }
        }
        return out
    }

    // ---------- merge (объединение без удаления) ----------

    /** Источники: дедуп по URL; существующий label/folder не затирается пустыми значениями. */
    fun mergeSources(current: List<Source>, imported: List<Source>): List<Source> {
        val byUrl = LinkedHashMap<String, Source>()
        current.forEach { byUrl[it.url] = it }
        imported.forEach { imp ->
            val cur = byUrl[imp.url]
            byUrl[imp.url] = if (cur == null) imp else cur.copy(
                label = cur.label ?: imp.label,
                folder = if (cur.folder.isNotEmpty()) cur.folder else imp.folder,
                type = cur.type.ifEmpty { imp.type }
            )
        }
        return byUrl.values.toList()
    }

    /** Посты: дедуп по id, существующие записи сохраняются. */
    fun mergePosts(current: List<Post>, imported: List<Post>): List<Post> {
        val byId = LinkedHashMap<String, Post>()
        current.forEach { byId[it.id] = it }
        imported.forEach { if (!byId.containsKey(it.id)) byId[it.id] = it }
        return byId.values.toList()
    }
}

/**
 * Android I/O: чтение/запись файла резервной копии.
 */
class BackupManager(private val context: Context) {

    fun export(outputStream: OutputStream, snapshot: BackupFormat.Parsed, exportedAt: Long): Boolean {
        return try {
            val json = BackupFormat.build(
                sources = snapshot.sources,
                favorites = snapshot.favorites,
                readLater = snapshot.readLater,
                readIds = snapshot.readIds,
                readTs = snapshot.readTs,
                settings = snapshot.settings,
                notifications = snapshot.notifications,
                exportedAt = exportedAt
            )
            outputStream.write(json.toByteArray(StandardCharsets.UTF_8))
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Читает и валидирует файл. Бросает [BackupFormat.InvalidBackupException] при любой проблеме —
     * вызывающий код не должен ничего записывать.
     */
    fun readAndValidate(inputStream: InputStream): BackupFormat.Parsed {
        val bytes = inputStream.readBytes()
        if (bytes.size > BackupFormat.MAX_BACKUP_BYTES)
            throw BackupFormat.InvalidBackupException("Файл слишком большой (>${BackupFormat.MAX_BACKUP_BYTES / 1024 / 1024} МБ)")
        val text = String(bytes, StandardCharsets.UTF_8)
        if (text.isBlank()) throw BackupFormat.InvalidBackupException("Файл пустой")
        return BackupFormat.parse(text)
    }
}
