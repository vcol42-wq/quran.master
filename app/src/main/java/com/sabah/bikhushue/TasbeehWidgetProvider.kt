package com.sabah.bikhushue

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class TasbeehWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TASBEEH_INCREMENT = "com.sabah.bikhushue.ACTION_TASBEEH_INCREMENT"
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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TASBEEH_INCREMENT) {
            val prefs = context.getSharedPreferences("SalawatProgress", Context.MODE_PRIVATE)
            var currentCount = prefs.getInt("salawat_count", 0)
            val currentGoal = prefs.getInt("salawat_goal", 1000)
            
            if (currentCount < currentGoal) {
                currentCount++
            } else {
                currentCount = 1
            }
            prefs.edit().putInt("salawat_count", currentCount).apply()

            // Update all widgets
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, TasbeehWidgetProvider::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences("SalawatProgress", Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("salawat_count", 0)

        val views = RemoteViews(context.packageName, R.layout.tasbeeh_widget)
        views.setTextViewText(R.id.tv_tasbeeh_count, currentCount.toString())

        val intent = Intent(context, TasbeehWidgetProvider::class.java).apply {
            action = ACTION_TASBEEH_INCREMENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(R.id.widget_tasbeeh_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
