package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.model.WallpaperTarget
import com.example.data.repository.RollRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
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
}
