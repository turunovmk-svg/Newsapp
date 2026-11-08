package com.mikhail.newsapp

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.mikhail.newsapp.data.Post

/** Сервис提供 данных для списка новостей в виджете. */
class NewsWidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return NewsWidgetRemoteViewsFactory(applicationContext)
    }
}

/** Фабрика элементов списка новостей для виджета. */
class NewsWidgetRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var posts: List<Post> = emptyList()

    override fun onCreate() {
        loadPosts()
    }

    override fun onDataSetChanged() {
        loadPosts()
    }

    override fun onDestroy() {
        posts = emptyList()
    }

    override fun getCount(): Int = posts.size

    override fun getViewAt(position: Int): RemoteViews {
        val post = posts[position] ?: return RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        val rv = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        rv.setTextViewText(android.R.id.text1, post.title)
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun loadPosts() {
        val json = context.getSharedPreferences("NewsAppPrefs", Context.MODE_PRIVATE)
            .getString("posts_json", null)
        posts = if (json != null) {
            com.mikhail.newsapp.data.Post.fromJsonList(json).take(8)
        } else {
            emptyList()
        }
    }
}
