package com.avikalabs.roll.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.avikalabs.roll.MainActivity
import com.avikalabs.roll.data.model.WallpaperTarget
import com.avikalabs.roll.data.repository.RollRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RollWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_WIDGET_ADVANCE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = RollRepository.getInstance(context)
                    if (!repository.isProSync()) {
                        val proIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra(MainActivity.EXTRA_NAVIGATE_TO, "pro")
                        }
                        context.startActivity(proIntent)
                    } else {
                        // Advance HOME wallpaper only, without resetting running timer schedule per PRD §7.5
                        repository.advanceRotation(WallpaperTarget.HOME, isScheduledTrigger = false)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_ADVANCE = "com.avikalabs.roll.ACTION_WIDGET_ADVANCE"
    }
}
