package com.mikhail.newsapp

// Так выглядит одна новость
data class NewsItem(
    val id: Int,
    val title: String,
    val description: String,
    val imageUrl: String,
    val source: String
)

// Пока используем тестовые данные, чтобы UI сразу заработал красиво
object NewsRepository {
    fun getNews(): List<NewsItem> {
        return listOf(
            NewsItem(1, "Илон Маск запустил новую ракету Starship", "SpaceX успешно провела очередное испытание...", "https://images.unsplash.com/photo-1517976487492-5750f3195933?w=800", "SpaceX"),
            NewsItem(2, "Apple представила революционный чип", "Новый процессор M4 обещает изменить...", "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800", "Tech"),
            NewsItem(3, "Ученые нашли воду на Марсе", "Марсоход Perseverance передал сенсационные...", "https://images.unsplash.com/photo-1614728853913-1e22ba0e982b?w=800", "Science"),
            NewsItem(4, "Биткоин пробил новый исторический максимум", "Криптовалютный рынок снова в эпицентре...", "https://images.unsplash.com/photo-1518546305927-5a555bb7020d?w=800", "Finance")
        )
    }
}
