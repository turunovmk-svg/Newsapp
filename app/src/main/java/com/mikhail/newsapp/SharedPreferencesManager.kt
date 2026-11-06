package com.mikhail.newsapp

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("NewsPrefs", Context.MODE_PRIVATE)

    fun isFavorite(articleId: String): Boolean {
        return prefs.getBoolean(articleId, false)
    }

    fun toggleFavorite(articleId: String) {
        val current = isFavorite(articleId)
        prefs.edit().putBoolean(articleId, !current).apply()
    }
}