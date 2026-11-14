package com.mikhail.newsapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mikhail.newsapp.data.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Фоновая проверка обновлений лент.
 * Достаёт самый свежий материал каждого источника и сравнивает
 * с сохранённым ранее. При появлении нового — показывает уведомление.
 */
@OptIn(ExperimentalStdlibApi::class)
class NewsCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "news_updates"
        const val WORK_NAME = "news_updates_check"
        const val PREFS_LAST_SEEN = "last_seen_links"  // url -> link новейшего материала
        const val KEY_ENABLED = "notif_enabled"
        const val KEY_INTERVAL_HOURS = "notif_interval_hours"

        /** Чистая проверка тихих часов: nowMinutes — минуты суток, start/end — в минутах, -1 = выключено. */
        internal fun isInQuietHours(nowMinutes: Int, start: Int, end: Int): Boolean {
            if (start < 0 || end < 0) return false
            return if (start <= end) nowMinutes in start until end
            else nowMinutes >= start || nowMinutes < end
        }

        private val ITEM_RE = Regex("(?s)<(?:item|entry)[ >].*?</(?:item|entry)>")
        private val TITLE_RE = Regex("(?s)<title[^>]*>(.*?)</title>")
        private val LINK_RE = Regex("(?s)<link[^>]*>(.*?)</link>")
        private val LINK_HREF_RE = Regex("""<link[^>]*href=["']([^"']+)["']""")

        /** Возвращает (title, link) самого свежего item/entry или null. */
        fun parseLatestMarker(xml: String, fallbackUrl: String): Pair<String, String>? {
            val latest = ITEM_RE.find(xml) ?: return null
            val title = TITLE_RE.find(latest.value)?.groupValues?.get(1)
                ?.replace(Regex("<[^>]+>"), "")?.trim().orEmpty()
            val link = LINK_RE.find(latest.value)?.groupValues?.get(1)?.trim()
                ?: LINK_HREF_RE.find(latest.value)?.groupValues?.get(1)?.trim()
                ?: fallbackUrl
            return title to link
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(15_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .callTimeout(30_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = applicationContext
                .getSharedPreferences("NewsAppPrefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_ENABLED, false)) return@withContext Result.success()

            val sources = Source.fromJsonList(prefs.getString("sources_json", null) ?: "")
            if (sources.isEmpty()) return@withContext Result.success()

            // Источники с отключёнными уведомлениями
            val disabledSources = prefs.getStringSet("notif_disabled_sources", emptySet()) ?: emptySet()
            val activeSources = sources.filter { it.url !in disabledSources }
            if (activeSources.isEmpty()) return@withContext Result.success()

            // Тихие часы
            val quietStart = prefs.getInt("quiet_start", -1)
            val quietEnd = prefs.getInt("quiet_end", -1)
            val notifSound = prefs.getBoolean("notif_sound", true)
            val now = java.util.Calendar.getInstance()
            val nowMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
            val inQuietHours = isInQuietHours(nowMinutes, quietStart, quietEnd)

            val lastSeen = prefs.getStringSet("last_seen_set", emptySet()) ?: emptySet()
            val isFirstRun = lastSeen.isEmpty()
            // параллельная проверка, тот же лимит, что и в ленте (3)
            val sem = Semaphore(3)
            val results = coroutineScope {
                activeSources.map { source ->
                    async {
                        sem.withPermit {
                            try {
                                val xml = client.newCall(
                                    Request.Builder().url(source.url)
                                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0")
                                        .build()
                                ).execute().use { resp -> if (resp.isSuccessful) resp.body?.string() else null }
                                if (xml == null) null
                                else parseLatestMarker(xml, source.url)?.let { (title, link) ->
                                    val marker = source.url + "|" + link
                                    val isNew = !isFirstRun && lastSeen.contains(source.url) && !lastSeen.contains(marker)
                                    Triple(marker, title, isNew)
                                }
                            } catch (e: Exception) {
                                null // один упавший источник не роняет всю проверку
                            }
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            val allMarkers: MutableSet<String> = (lastSeen + results.map { it.first }).toMutableSet()
            val newTitles = results.filter { it.third }.map { it.second.take(90) }.filter { it.isNotEmpty() }.toMutableList()
            val totalNew = results.count { it.third }

            // сохраняем снимок «виденного»
            prefs.edit().putStringSet("last_seen_set", allMarkers).apply()

            if (totalNew > 0 && !inQuietHours) {
                showNotification(newTitles, totalNew, notifSound)
            }
            Result.success()
        } catch (e: Exception) {
            // ожидаемые сетевые ошибки не должны порождать лавину retry — следующий запуск по расписанию
            Result.success()
        }
    }

    private fun showNotification(titles: List<String>, count: Int, withSound: Boolean = true) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = if (withSound) NotificationManager.IMPORTANCE_DEFAULT else NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, "Новые статьи", importance).apply {
                description = "Уведомления о свежих материалах в лентах"
                if (!withSound) setSound(null, null)
            }
            nm.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = when {
            titles.isEmpty() -> "Проверьте ленту"
            titles.size == 1 -> titles.first()
            else -> titles.take(3).joinToString(" · ")
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_home)
            .setContentTitle(if (count > 1) "$count новых статей" else "Новая статья")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        nm.notify(1001, notification)
    }
}
