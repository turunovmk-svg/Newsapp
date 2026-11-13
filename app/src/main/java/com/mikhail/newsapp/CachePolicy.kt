package com.mikhail.newsapp

import com.mikhail.newsapp.data.Post
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Чистая логика кэша ленты (вынесена из MainViewModel для unit-тестов, этап 9B).
 *
 * Семантика полностью совпадает с прежним clearOldCache():
 *  - у поста нет publishedAt → храним;
 *  - дата не распарсилась (исключение) → храним;
 *  - дата спарсилась в null → считаем устаревшей (как было: ?: 0L >= sevenDaysAgo);
 *  - иначе храним, если младше MAX_CACHE_AGE_MS.
 */
object CachePolicy {

    const val MAX_CACHE_AGE_MS = 7L * 24 * 60 * 60 * 1000
    const val MAX_CACHED_POSTS = 200

    fun isFresh(post: Post, now: Long): Boolean {
        val dateStr = post.publishedAt ?: return true
        return try {
            val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
            val parsed = sdf.parse(dateStr)
            (parsed?.time ?: 0L) >= now - MAX_CACHE_AGE_MS
        } catch (e: Exception) {
            true // если не распарсилось — оставляем
        }
    }

    /** Убирает устаревшие записи кэша по прежним правилам. Идемпотентно. */
    fun prune(cached: List<Post>, now: Long): List<Post> = cached.filter { isFresh(it, now) }
}
