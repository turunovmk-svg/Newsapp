package com.mikhail.newsapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

/**
 * Бесплатный перевод статей без API-ключа.
 * Основной канал — Google Translate (публичный gtx-endpoint),
 * фолбэк — MyMemory. Текст режется на чанки, чтобы не упереться в лимиты URL.
 */
object ArticleTranslator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(20_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    private const val CHUNK = 3500

    /** Переводит текст на русский. Возвращает null при ошибке всех каналов. */
    suspend fun toRussian(text: String): String? = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext null
        try {
            val chunks = splitChunks(text)
            val translated = StringBuilder()
            for (chunk in chunks) {
                val part = translateGoogle(chunk) ?: translateMyMemory(chunk)
                if (part == null) {
                    return@withContext if (translated.isNotEmpty()) translated.toString() else null
                }
                translated.append(part)
            }
            translated.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Определяет, русский ли текст (грубая эвристика по кириллице). */
    fun looksRussian(text: String): Boolean {
        val total = text.count { it.isLetter() }
        if (total == 0) return false
        val cyr = text.count { it.code in 0x0400..0x04FF }
        return cyr * 100 >= total * 50
    }

    /** Нарезает текст на чанки по границам абзацев/предложений. */
    private fun splitChunks(text: String): List<String> {
        if (text.length <= CHUNK) return listOf(text)
        val parts = text.split("\n")
        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        for (p in parts) {
            if (current.length + p.length + 1 > CHUNK && current.isNotEmpty()) {
                chunks.add(current.toString())
                current.clear()
            }
            if (p.length > CHUNK) {
                // очень длинный абзац — режем по предложениям
                var rest = p
                while (rest.length > CHUNK) {
                    val cut = rest.lastIndexOf(". ", CHUNK)
                    val at = if (cut > CHUNK / 2) cut + 1 else CHUNK
                    current.append(rest.substring(0, at)).append("\n")
                    chunks.add(current.toString())
                    current.clear()
                    rest = rest.substring(at)
                }
                current.append(rest)
            } else {
                current.append(p).append("\n")
            }
        }
        if (current.isNotBlank()) chunks.add(current.toString())
        return chunks
    }

    /** Google Translate: [[["перевод","оригинал",...]],...] */
    private fun translateGoogle(text: String): String? {
        return try {
            val q = java.net.URLEncoder.encode(text, "UTF-8")
            val request = Request.Builder()
                .url("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=ru&dt=t&q=$q")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val arr = JSONArray(body)
                val inner = arr.getJSONArray(0)
                val sb = StringBuilder()
                for (i in 0 until inner.length()) {
                    val seg = inner.getJSONArray(i)
                    sb.append(seg.getString(0))
                }
                sb.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** MyMemory: {"responseData":{"translatedText":"..."}} */
    private fun translateMyMemory(text: String): String? {
        return try {
            val q = java.net.URLEncoder.encode(text, "UTF-8")
            val request = Request.Builder()
                .url("https://api.mymemory.translated.net/get?q=$q&langpair=auto|ru")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val obj = org.json.JSONObject(body)
                val data = obj.optJSONObject("responseData") ?: return null
                data.optString("translatedText").takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
