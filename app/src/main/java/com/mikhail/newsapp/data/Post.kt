package com.mikhail.newsapp.data

data class Post(
    val id: String,
    val title: String,
    val description: String?,
    val publishedAt: String?,
    val sourceUrl: String?,
    val imageUrl: String?
)
