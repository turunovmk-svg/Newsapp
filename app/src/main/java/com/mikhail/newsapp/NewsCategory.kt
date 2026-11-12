package com.mikhail.newsapp

/**
 * Категория новости.
 *
 * [id] — стабильный машинный идентификатор (латиница, snake_case).
 *        Он не зависит от языка интерфейса и может использоваться
 *        в фильтрах, сохранённых настройках и аналитике.
 * [title] — подпись, показываемая пользователю (чипы, метка на карточке).
 *
 * Порядок констант задаёт приоритет при равном числе признаков
 * в [NewsCategoryClassifier]: более специфичные темы идут раньше.
 */
enum class NewsCategory(val id: String, val title: String) {

    TECH("tech", "Технологии"),
    SCIENCE("science", "Наука"),
    GAMES("games", "Игры"),
    WORLD("world", "Мир"),
    POLITICS("politics", "Политика"),
    ECONOMY("economy", "Экономика"),
    AUTO("auto", "Авто"),
    SPORT("sport", "Спорт"),
    ENTERTAINMENT("entertainment", "Развлечения"),
    OTHER("other", "Другое");

    /** Подпись для чипа фильтра. */
    val label: String get() = title

    companion object {
        /**
         * Разбор сохранённого идентификатора. Неизвестные значения
         * трактуются как [OTHER], чтобы старые/битые настройки не ломали экран.
         */
        fun fromId(id: String?): NewsCategory =
            entries.firstOrNull { it.id == id } ?: OTHER

        /** Все категории, кроме [OTHER] — для чипов фильтра. */
        val selectable: List<NewsCategory> = entries.filter { it != OTHER }
    }
}
