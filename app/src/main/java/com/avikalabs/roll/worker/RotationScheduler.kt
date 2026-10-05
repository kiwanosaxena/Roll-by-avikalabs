package com.avikalabs.roll.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.avikalabs.roll.data.model.WallpaperTarget
import java.util.concurrent.TimeUnit

class RotationScheduler(private val context: Context) {

    private val workManager: WorkManager?
        get() = try {
            WorkManager.getInstance(context)
        } catch (_: Exception) {
            null
        }

    fun scheduleNextRotation(target: WallpaperTarget, delaySeconds: Int) {
        val wm = workManager ?: return
        val safeDelay = delaySeconds.coerceAtLeast(5).toLong()
        val data = Data.Builder()
            .putString(WallpaperRotationWorker.KEY_TARGET, target.name)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<WallpaperRotationWorker>()
            .setInitialDelay(safeDelay, TimeUnit.SECONDS)
            .setInputData(data)
            .build()

        val workName = getWorkName(target)
        wm.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelRotation(target: WallpaperTarget) {
        val wm = workManager ?: return
        val workName = getWorkName(target)
        wm.cancelUniqueWork(workName)
    }

    fun schedulePeriodicFolderSync() {
        val wm = workManager ?: return
        val request = PeriodicWorkRequestBuilder<FolderSyncWorker>(
            4, TimeUnit.HOURS,
            30, TimeUnit.MINUTES
        ).build()

        wm.enqueueUniquePeriodicWork(
            "roll_folder_sync_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun getWorkName(target: WallpaperTarget): String {
        return "roll_rotation_${target.name}"
    }
}
