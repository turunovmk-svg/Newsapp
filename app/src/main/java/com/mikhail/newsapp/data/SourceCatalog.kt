package com.mikhail.newsapp.data

/**
 * Каталог популярных источников для поиска и подписки в «Добавить источник».
 * Отображается в виде списка с фавиконом, описанием и кнопкой «Подписаться».
 */
data class CatalogSource(
    val name: String,
    val url: String,
    val description: String,
    val category: String,      // website / youtube / reddit / telegram
    val brandColor: Long,      // цвет фавикона
    val letter: String         // буква/символ в фавиконе
)

object SourceCatalog {

    const val CAT_WEBSITE = "website"
    const val CAT_YOUTUBE = "youtube"
    const val CAT_REDDIT = "reddit"
    const val CAT_TELEGRAM = "telegram"

    val all: List<CatalogSource> = listOf(
        // ---------------- Сайты ----------------
        CatalogSource("BBC News", "https://feeds.bbci.co.uk/news/rss.xml", "Мировые новости и аналитика", CAT_WEBSITE, 0xFFBB1919, "B"),
        CatalogSource("BBC Украина", "https://feeds.bbci.co.uk/ukrainian/rss.xml", "Новости Украины на укр. языке", CAT_WEBSITE, 0xFFBB1919, "B"),
        CatalogSource("Reuters", "https://www.reutersagency.com/feed/", "Финансы, политика, мир", CAT_WEBSITE, 0xFFFF8000, "R"),
        CatalogSource("Meduza", "https://meduza.io/rss/all", "Новости и расследования", CAT_WEBSITE, 0xFF3A74B0, "M"),
        CatalogSource("Lenta.ru", "https://lenta.ru/rss", "Новости России и мира", CAT_WEBSITE, 0xFFE04A3F, "L"),
        CatalogSource("РБК", "https://rssexport.rbc.ru/rbcnews/news/30/full.rss", "Экономика и бизнес", CAT_WEBSITE, 0xFF2A5FD1, "Р"),
        CatalogSource("Хабр", "https://habr.com/ru/rss/all/", "IT, разработка, технологии", CAT_WEBSITE, 0xFF65A3DD, "Х"),
        CatalogSource("TechCrunch", "https://techcrunch.com/feed/", "Стартапы и технологии", CAT_WEBSITE, 0xFF0A9E2F, "T"),
        CatalogSource("The Verge", "https://www.theverge.com/rss/index.xml", "Технологии и медиа", CAT_WEBSITE, 0xFF5B21DC, "V"),
        CatalogSource("Ars Technica", "https://feeds.arstechnica.com/arstechnica/index", "Наука, техника, IT", CAT_WEBSITE, 0xFFFF4E00, "A"),
        CatalogSource("Wired", "https://www.wired.com/feed/rss", "Технологии и культура", CAT_WEBSITE, 0xFF000000, "W"),
        CatalogSource("The Guardian", "https://www.theguardian.com/world/rss", "Великобритания и мир", CAT_WEBSITE, 0xFF052962, "G"),
        CatalogSource("National Geographic", "https://www.nationalgeographic.com/rss/latest", "Природа и наука", CAT_WEBSITE, 0xFFFFCC00, "N"),
        CatalogSource("3D News", "https://3dnews.ru/news/rss/", "Железо и гаджеты", CAT_WEBSITE, 0xFF0F5EB0, "3"),
        CatalogSource("Кот Шрёдингера", "https://kot.sh/feed/", "Наука простым языком", CAT_WEBSITE, 0xFF4B7FC1, "К"),
        CatalogSource("Ferra.ru", "https://www.ferra.ru/rss.xml", "Технологии и обзоры", CAT_WEBSITE, 0xFF1D58BE, "F"),
        CatalogSource("Интерфакс", "https://www.interfax.ru/rss.asp", "Новости и события", CAT_WEBSITE, 0xFF0072BC, "И"),
        CatalogSource("Naked Science", "https://naked-science.ru/rss", "Наука и космос", CAT_WEBSITE, 0xFF0091DC, "N"),
        CatalogSource("Sepia Sepia", "https://www.sepia_sepia.com/feed/", "Дизайн и визуал", CAT_WEBSITE, 0xFF8E6E53, "S"),
        CatalogSource("Ukr.net Новини", "https://www.ukr.net/news/rss/all", "Все новости Украины", CAT_WEBSITE, 0xFF1055A2, "U"),

        // ---------------- YouTube ----------------
        CatalogSource("MKBHD", "https://www.youtube.com/@mkbhd", "Обзоры техники №1 в мире", CAT_YOUTUBE, 0xFFFF0000, "M"),
        CatalogSource("Wylsacom", "https://www.youtube.com/@wylsacom", "Гаджеты и технологии", CAT_YOUTUBE, 0xFFFF0000, "W"),
        CatalogSource("Kurzgesagt", "https://www.youtube.com/@kurzgesagt", "Наука анимацией", CAT_YOUTUBE, 0xFFFF0000, "K"),
        CatalogSource("Vsya Pravda", "https://www.youtube.com/@vssapravda", "Расследования и история", CAT_YOUTUBE, 0xFFFF0000, "V"),
        CatalogSource("Rozenberg", "https://www.youtube.com/@Rozenman", "Гаджеты и русский юмор", CAT_YOUTUBE, 0xFFFF0000, "R"),
        CatalogSource("Popular Science", "https://www.youtube.com/@popsci", "Наука и эксперименты", CAT_YOUTUBE, 0xFFFF0000, "P"),
        CatalogSource("Veritasium", "https://www.youtube.com/@veritasium", "Физика и инженерия", CAT_YOUTUBE, 0xFFFF0000, "V"),
        CatalogSource("Google Developers", "https://www.youtube.com/@GoogleDevelopers", "Разработка и ИИ", CAT_YOUTUBE, 0xFFFF0000, "G"),
        CatalogSource("SciShow", "https://www.youtube.com/@scishow", "Наука каждый день", CAT_YOUTUBE, 0xFFFF0000, "S"),
        CatalogSource("ТОПЛЕС", "https://www.youtube.com/@topleschannel", "Лонгриды и репортажи", CAT_YOUTUBE, 0xFFFF0000, "Т"),

        // ---------------- Reddit ----------------
        CatalogSource("r/worldnews", "https://www.reddit.com/r/worldnews/", "Главные мировые новости", CAT_REDDIT, 0xFFFF4500, "W"),
        CatalogSource("r/technology", "https://www.reddit.com/r/technology/", "Технологии", CAT_REDDIT, 0xFFFF4500, "T"),
        CatalogSource("r/science", "https://www.reddit.com/r/science/", "Научные публикации", CAT_REDDIT, 0xFFFF4500, "S"),
        CatalogSource("r/Futurology", "https://www.reddit.com/r/Futurology/", "Будущее и прогнозы", CAT_REDDIT, 0xFFFF4500, "F"),
        CatalogSource("r/space", "https://www.reddit.com/r/space/", "Космос", CAT_REDDIT, 0xFFFF4500, "S"),
        CatalogSource("r/gaming", "https://www.reddit.com/r/gaming/", "Игры", CAT_REDDIT, 0xFFFF4500, "G"),
        CatalogSource("r/UpliftingNews", "https://www.reddit.com/r/UpliftingNews/", "Хорошие новости", CAT_REDDIT, 0xFFFF4500, "U"),
        CatalogSource("r/AskHistorians", "https://www.reddit.com/r/AskHistorians/", "История и факты", CAT_REDDIT, 0xFFFF4500, "H"),
        CatalogSource("r/gadgets", "https://www.reddit.com/r/gadgets/", "Гаджеты", CAT_REDDIT, 0xFFFF4500, "G"),
        CatalogSource("r/dataisbeautiful", "https://www.reddit.com/r/dataisbeautiful/", "Данные и графики", CAT_REDDIT, 0xFFFF4500, "D"),

        // ---------------- Telegram ----------------
        CatalogSource("Хабр — Telegram", "https://t.me/habr_com", "IT-новости коротко", CAT_TELEGRAM, 0xFF3390EC, "Х"),
        CatalogSource("Медуза — Telegram", "https://t.me/meduzalive", "Главное за день", CAT_TELEGRAM, 0xFF3390EC, "М"),
        CatalogSource("РБК — Telegram", "https://t.me/rbc_news", "Экономика и рынки", CAT_TELEGRAM, 0xFF3390EC, "Р"),
        CatalogSource("Тинькофф Инвестиции", "https://t.me/tinkoff_invest_official", "Финансы и инвестиции", CAT_TELEGRAM, 0xFF3390EC, "Т"),
        CatalogSource("Ъ Новости", "https://t.me/kommersant_news", "Политика и экономика", CAT_TELEGRAM, 0xFF3390EC, "Ъ"),
        CatalogSource("Ведомости", "https://t.me/vedomosti", "Бизнес и финансы", CAT_TELEGRAM, 0xFF3390EC, "В"),
        CatalogSource("Код Дурова", "https://t.me/codurov", "Технологии и медиа", CAT_TELEGRAM, 0xFF3390EC, "К"),
        CatalogSource("Лента.chat", "https://t.me/lentachat", "Новости без фильтров", CAT_TELEGRAM, 0xFF3390EC, "Л"),
    )

    /** Поиск по названию и описанию (регистронезависимый). */
    fun search(query: String, category: String? = null): List<CatalogSource> {
        val q = query.trim().lowercase()
        val base = if (category.isNullOrBlank()) all else all.filter { it.category == category }
        if (q.isEmpty()) return base
        return base.filter {
            it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
    }

    /** Проверяет, добавлен ли уже такой источник (по нормализованному URL). */
    fun isFollowing(source: CatalogSource, sources: List<Source>): Boolean {
        val normalized = Source.detect(source.url)?.url?.trim()
        return normalized != null && sources.any { it.url == normalized }
    }
}
