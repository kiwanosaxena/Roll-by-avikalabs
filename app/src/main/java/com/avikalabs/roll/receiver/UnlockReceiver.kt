package com.avikalabs.roll.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.avikalabs.roll.data.model.WallpaperTarget
import com.avikalabs.roll.data.repository.RollRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            val now = System.currentTimeMillis()
            if (now - lastUnlockTime < DEBOUNCE_MS) {
                return
            }
            lastUnlockTime = now

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = RollRepository.getInstance(context)
                    val lockConfig = repository.getRotationConfigSync(WallpaperTarget.LOCK)
                    if (lockConfig?.autoChangeOnUnlock == true) {
                        repository.advanceRotation(WallpaperTarget.LOCK, isScheduledTrigger = false)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        @Volatile
        private var lastUnlockTime: Long = 0L
        private const val DEBOUNCE_MS = 1500L
    }
}
