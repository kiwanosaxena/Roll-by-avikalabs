package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import com.example.R
import com.example.data.model.WallpaperTarget
import com.example.data.repository.RollRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.min

class RollWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            updateWidgetView(context, appWidgetManager, appWidgetId, options)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        updateWidgetView(context, appWidgetManager, appWidgetId, newOptions)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_ADVANCE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = RollRepository.getInstance(context)
                    // Advance HOME wallpaper only, without resetting running timer schedule per PRD §7.5
                    repository.advanceRotation(WallpaperTarget.HOME, isScheduledTrigger = false)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun updateWidgetView(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        options: Bundle?
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_roll)

        // PendingIntent for tap-to-advance
        val clickIntent = Intent(context, RollWidgetProvider::class.java).apply {
            action = ACTION_WIDGET_ADVANCE
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, clickIntent, flags)
        views.setOnClickPendingIntent(R.id.widget_icon, pendingIntent)

        // Resizing logic per PRD §7.5: 46% of shorter dimension
        val minWidthDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 40
        val minHeightDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 40
        val shorterDimDp = min(minWidthDp, minHeightDp)
        val iconSizeDp = (shorterDimDp * 0.46f).toInt().coerceAtLeast(18)

        val iconSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            iconSizeDp.toFloat(),
            context.resources.displayMetrics
        ).toInt()

        views.setViewLayoutWidth(R.id.widget_icon, iconSizePx.toFloat(), TypedValue.COMPLEX_UNIT_PX)
        views.setViewLayoutHeight(R.id.widget_icon, iconSizePx.toFloat(), TypedValue.COMPLEX_UNIT_PX)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val ACTION_WIDGET_ADVANCE = "com.example.roll.ACTION_WIDGET_ADVANCE"
    }
}
