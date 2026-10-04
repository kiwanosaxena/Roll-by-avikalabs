package com.example.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.model.WallpaperTarget
import java.util.concurrent.TimeUnit

class RotationScheduler(private val context: Context) {

    private val workManager by lazy { WorkManager.getInstance(context) }

    fun scheduleNextRotation(target: WallpaperTarget, delaySeconds: Int) {
        val safeDelay = delaySeconds.coerceAtLeast(5).toLong()
        val data = Data.Builder()
            .putString(WallpaperRotationWorker.KEY_TARGET, target.name)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<WallpaperRotationWorker>()
            .setInitialDelay(safeDelay, TimeUnit.SECONDS)
            .setInputData(data)
            .build()

        val workName = getWorkName(target)
        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelRotation(target: WallpaperTarget) {
        val workName = getWorkName(target)
        workManager.cancelUniqueWork(workName)
    }

    fun schedulePeriodicFolderSync() {
        val request = PeriodicWorkRequestBuilder<FolderSyncWorker>(
            4, TimeUnit.HOURS,
            30, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "roll_folder_sync_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun getWorkName(target: WallpaperTarget): String {
        return "roll_rotation_${target.name}"
    }
}
