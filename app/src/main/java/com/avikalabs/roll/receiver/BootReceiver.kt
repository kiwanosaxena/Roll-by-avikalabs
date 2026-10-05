package com.avikalabs.roll.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.avikalabs.roll.data.model.WallpaperTarget
import com.avikalabs.roll.data.repository.RollRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = RollRepository.getInstance(context)

                    // Reschedule HOME rotation if it has items
                    val homeItems = repository.getItemsByTargetSync(WallpaperTarget.HOME)
                    val homeConfig = repository.getRotationConfigSync(WallpaperTarget.HOME)
                    if (homeItems.isNotEmpty() && homeConfig != null) {
                        val duration = if (homeConfig.durationMode == com.avikalabs.roll.data.model.DurationMode.GLOBAL) {
                            homeConfig.globalDurationSeconds
                        } else {
                            val idx = homeConfig.currentIndex.coerceIn(0, homeItems.size - 1)
                            homeItems[idx].durationSeconds
                        }
                        val scheduler = com.avikalabs.roll.worker.RotationScheduler(context)
                        scheduler.scheduleNextRotation(WallpaperTarget.HOME, duration)
                    }

                    // Reschedule LOCK rotation if duration-based
                    repository.disableUnlockModeIfLiveWallpaperInactive()
                    val lockItems = repository.getItemsByTargetSync(WallpaperTarget.LOCK)
                    val lockConfig = repository.getRotationConfigSync(WallpaperTarget.LOCK)
                    if (lockItems.isNotEmpty() && lockConfig != null) {
                        if (!lockConfig.autoChangeOnUnlock) {
                            val duration = if (lockConfig.durationMode == com.avikalabs.roll.data.model.DurationMode.GLOBAL) {
                                lockConfig.globalDurationSeconds
                            } else {
                                val idx = lockConfig.currentIndex.coerceIn(0, lockItems.size - 1)
                                lockItems[idx].durationSeconds
                            }
                            val scheduler = com.avikalabs.roll.worker.RotationScheduler(context)
                            scheduler.scheduleNextRotation(WallpaperTarget.LOCK, duration)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
