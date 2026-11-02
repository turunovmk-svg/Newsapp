package com.mikhail.newsapp

import androidx.lifecycle.ViewModel

class FeedViewModel : ViewModel() {
    // Получаем список новостей из нашего репозитория
    val newsList = NewsRepository.getNews()
}
