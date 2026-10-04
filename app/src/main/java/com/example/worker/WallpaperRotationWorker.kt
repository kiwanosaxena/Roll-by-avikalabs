package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.RollDatabase
import com.example.data.model.WallpaperTarget
import com.example.data.repository.RollRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WallpaperRotationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val targetName = inputData.getString(KEY_TARGET) ?: return@withContext Result.failure()
        val target = try {
            WallpaperTarget.valueOf(targetName)
        } catch (_: Exception) {
            return@withContext Result.failure()
        }

        val repository = RollRepository.getInstance(applicationContext)
        val advanced = repository.advanceRotation(target, isScheduledTrigger = true)

        if (advanced) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        const val KEY_TARGET = "key_wallpaper_target"
    }
}
