package com.avikalabs.roll.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import com.avikalabs.roll.MainActivity
import com.avikalabs.roll.R
import com.avikalabs.roll.data.local.RollPreferences
import kotlin.math.min

class RollWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val isPro = RollPreferences(context).isProSync()
        for (appWidgetId in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            updateWidgetView(context, appWidgetManager, appWidgetId, options, isPro)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        val isPro = RollPreferences(context).isProSync()
        updateWidgetView(context, appWidgetManager, appWidgetId, newOptions, isPro)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        fun updateWidgetView(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            options: Bundle?,
            isPro: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_roll)

            val pendingIntent: PendingIntent = if (isPro) {
                // If Pro: keep broadcast PendingIntent to advance wallpaper
                val clickIntent = Intent(context, RollWidgetActionReceiver::class.java).apply {
                    action = RollWidgetActionReceiver.ACTION_WIDGET_ADVANCE
                    setPackage(context.packageName)
                }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                PendingIntent.getBroadcast(context, appWidgetId, clickIntent, flags)
            } else {
                // If not Pro: open MainActivity with EXTRA_NAVIGATE_TO = "pro"
                val activityIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(MainActivity.EXTRA_NAVIGATE_TO, "pro")
                }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                PendingIntent.getActivity(context, appWidgetId, activityIntent, flags)
            }

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

        fun refreshAllWidgets(context: Context, isPro: Boolean = RollPreferences(context).isProSync()) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, RollWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return
            for (appWidgetId in appWidgetIds) {
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                updateWidgetView(context, appWidgetManager, appWidgetId, options, isPro)
            }
        }
    }
}
