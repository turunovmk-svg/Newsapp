package com.mikhail.newsapp

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Виджет с последними новостями на домашний экран. */
class NewsWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.mikhail.newsapp.action.REFRESH_WIDGET"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Виджет добавлен на экран
    }

    override fun onDisabled(context: Context) {
        // Виджет удалён
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, NewsWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.news_widget)

        // Заголовок
        views.setTextViewText(R.id.widget_title, "NewsApp")

        // Устанавливаем адаптер для списка новостей
        val intent = Intent(context, NewsWidgetRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        views.setRemoteAdapter(R.id.widget_list, intent)

        // Пустое состояние
        views.setEmptyView(R.id.widget_list, android.R.id.empty)

        // Кнопка обновления
        val refreshIntent = Intent(context, NewsWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context, appWidgetId, refreshIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh, pendingIntent)

        // Клик на элемент списка — открывает приложение
        val clickIntent = Intent(context, NewsWidgetProvider::class.java).apply {
            action = "com.mikhail.newsapp.action.OPEN_APP"
        }
        val clickPending = android.app.PendingIntent.getBroadcast(
            context, appWidgetId, clickIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_list, clickPending)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
