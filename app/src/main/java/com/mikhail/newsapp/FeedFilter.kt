package com.mikhail.newsapp

import com.mikhail.newsapp.data.Post

/**
 * Чистая логика поиска и фильтрации ленты новостей.
 *
 * Вынесена из HomeScreen отдельно, чтобы:
 *  - не зависеть от Android/Compose (обычные JVM-тесты);
 *  - гарантировать, что ввод текста в поиск НЕ вызывает сетевых запросов
 *    (данные фильтруются локально, по уже загруженному списку posts);
 *  - сохранять исходный порядок списка ( сортировка по дате делается во ViewModel ).
 */
object FeedFilter {

    /** Одна опция фильтра по источнику: ключ для сравнения + подпись для чипа. */
    data class SourceOption(val key: String, val label: String)

    /** Одна опция фильтра по категории: id категории + подпись для чипа. */
    data class CategoryOption(val id: String, val label: String)

    /** Тип ленты по ссылке на статью (как раньше в HomeScreen). */
    fun typeOf(post: Post): String {
        val u = post.sourceUrl ?: ""
        return when {
            u.contains("youtube.com") || u.contains("youtu.be") -> "YouTube"
            u.contains("t.me") -> "Telegram"
            u.contains("reddit.com") -> "Reddit"
            else -> "Статьи"
        }
    }

    /** Категория поста, вычисленная локально (не сохраняется в модели). */
    fun categoryOf(post: Post): NewsCategory {
        return NewsCategoryClassifier.classify(
            post.title,
            post.description,
            post.siteName
        )
    }

    /** Идентификатор категории поста (для сравнения в фильтре). */
    fun categoryKey(post: Post): String = categoryOf(post).id

    /** Подпись категории поста для отображения на карточке. */
    fun categoryLabel(post: Post): String = categoryOf(post).title

    /** Все категории, присутствующие в ленте (кроме OTHER, если она единственная). */
    fun categoryOptions(posts: List<Post>): List<CategoryOption> {
        val ids = posts.map { categoryKey(it) }.distinct().sorted()
        return ids.map { id ->
            CategoryOption(id, NewsCategory.fromId(id).title)
        }
    }

    /** Нормализованный ключ источника: имя издателя (GNEWS) или домен статьи. */
    fun sourceKey(post: Post): String {
        post.siteName?.trim()?.takeIf { it.isNotEmpty() }?.let { return "name:" + it.lowercase() }
        val domain = domainOf(post.siteUrl ?: post.sourceUrl)
        return if (domain.isEmpty()) "" else "domain:" + domain
    }

    /** Человекочитаемая подпись источника для чипа. */
    fun sourceLabel(post: Post): String {
        post.siteName?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val domain = domainOf(post.siteUrl ?: post.sourceUrl)
        return domain.ifEmpty { "Источник" }
    }

    private fun domainOf(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return url
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore('/')
            .substringBefore('?')
            .trim()
            .lowercase()
    }

    /**
     * Уникальные источники текущей ленты, отсортированные по подписи.
     * Для ключа берётся самая частая подпись (чтобы не дублировать «Habr»/«habr»).
     */
    fun sourceOptions(posts: List<Post>): List<SourceOption> {
        val labels = LinkedHashMap<String, MutableList<String>>()
        posts.forEach { post ->
            val key = sourceKey(post)
            if (key.isEmpty()) return@forEach
            labels.getOrPut(key) { mutableListOf() }.add(sourceLabel(post))
        }
        return labels.map { (key, variants) ->
            val label = variants
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
                ?: key.removePrefix("name:").removePrefix("domain:")
            SourceOption(key, label)
        }.sortedBy { it.label.lowercase() }
    }

    /** Поиск без учёта регистра по заголовку и описанию. Пустой запрос — пропускает всё. */
    fun matchesQuery(post: Post, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        if (post.title.contains(q, ignoreCase = true)) return true
        val desc = post.description
        return !desc.isNullOrBlank() && desc.contains(q, ignoreCase = true)
    }

    /**
     * Полная фильтрация: поиск + тип ленты + источник + категория.
     * Порядок входного списка сохраняется (сортировка уже применена во ViewModel).
     */
    fun filter(
        posts: List<Post>,
        query: String,
        type: String,
        sourceKey: String?,
        category: String? = null
    ): List<Post> {
        var result = posts.filter { matchesQuery(it, query) }
        if (type != "Все") {
            result = result.filter { typeOf(it) == type }
        }
        if (!sourceKey.isNullOrBlank()) {
            result = result.filter { sourceKey(it) == sourceKey }
        }
        if (!category.isNullOrBlank() && category != "Все") {
            result = result.filter { categoryKey(it) == category }
        }
        return result
    }
}