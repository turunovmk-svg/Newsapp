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
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Фоновая проверка обновлений лент.
 * Достаёт самый свежий материал каждого источника и сравнивает
 * с сохранённым ранее. При появлении нового — показывает уведомление.
 */
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
    }

    private val client = OkHttpClient()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = applicationContext
                .getSharedPreferences("NewsAppPrefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_ENABLED, false)) return@withContext Result.success()

            val sources = Source.fromJsonList(prefs.getString("sources_json", null) ?: "")
            if (sources.isEmpty()) return@withContext Result.success()

            val lastSeen = prefs.getStringSet("last_seen_set", emptySet()) ?: emptySet()
            val isFirstRun = lastSeen.isEmpty()
            val newTitles = mutableListOf<String>()
            val newCount = mutableListOf<Int>()
            val allMarkers = mutableSetOf<String>()

            for (source in sources) {
                try {
                    val url = source.url
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0")
                        .build()
                    val xml = client.newCall(request).execute().use { resp ->
                        if (!resp.isSuccessful) continue
                        resp.body?.string() ?: continue
                    }
                    // самый свежий материал: first <item>..</item> или <entry>..</entry>
                    val itemRe = Regex("(?s)<(?:item|entry)[ >].*?</(?:item|entry)>")
                    val titleRe = Regex("(?s)<title[^>]*>(.*?)</title>")
                    val linkRe = Regex("(?s)<link[^>]*>(.*?)</link>")
                    val linkRelRe = Regex("""<link[^>]*href=["']([^"']+)["']""")
                    val latest = itemRe.find(xml) ?: continue
                    val title = titleRe.find(latest.value)?.groupValues?.get(1)
                        ?.replace(Regex("<[^>]+>"), "")?.trim().orEmpty()
                    val link = linkRe.find(latest.value)?.groupValues?.get(1)?.trim()
                        ?: linkRelRe.find(latest.value)?.groupValues?.get(1)?.trim()
                        ?: url
                    val marker = "$url|$link"
                    allMarkers.add(marker)
                    // Уведомляем только если источник уже проверялся раньше и материал новый
                    if (!isFirstRun && lastSeen.contains(url) && !lastSeen.contains(marker)) {
                        if (title.isNotEmpty()) newTitles.add(title.take(90))
                        newCount.add(1)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // сохраняем снимок «виденного»
            prefs.edit().putStringSet("last_seen_set", allMarkers).apply()

            val totalNew = newCount.sum()
            if (totalNew > 0) {
                showNotification(newTitles, totalNew)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showNotification(titles: List<String>, count: Int) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Новые статьи",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Уведомления о свежих материалах в лентах" }
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
