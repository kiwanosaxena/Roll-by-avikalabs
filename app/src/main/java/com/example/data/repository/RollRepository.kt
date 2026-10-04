package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.example.data.local.RollDao
import com.example.data.local.RollDatabase
import com.example.data.local.RollPreferences
import com.example.data.local.ThemeMode
import com.example.data.model.DurationMode
import com.example.data.model.PlaybackOrder
import com.example.data.model.RotationConfig
import com.example.data.model.SyncedFolder
import com.example.data.model.WallpaperItem
import com.example.data.model.WallpaperTarget
import com.example.wallpaper.WallpaperApplier
import com.example.worker.RotationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.random.Random

class RollRepository private constructor(
    private val context: Context,
    private val dao: RollDao,
    private val preferences: RollPreferences,
    private val scheduler: RotationScheduler
) {

    val themeModeFlow: Flow<ThemeMode> = preferences.themeModeFlow
    val homeDoubleTapEnabledFlow: Flow<Boolean> = preferences.homeDoubleTapEnabledFlow

    fun getItems(target: WallpaperTarget): Flow<List<WallpaperItem>> = dao.getItemsByTarget(target)
    suspend fun getItemsByTargetSync(target: WallpaperTarget): List<WallpaperItem> = dao.getItemsByTargetSync(target)
    fun getItemCount(target: WallpaperTarget): Flow<Int> = dao.getItemCountByTarget(target)
    fun getTotalItemCount(): Flow<Int> = dao.getTotalItemCount()

    fun getRotationConfig(target: WallpaperTarget): Flow<RotationConfig?> = dao.getRotationConfig(target)
    suspend fun getRotationConfigSync(target: WallpaperTarget): RotationConfig? = dao.getRotationConfigSync(target)

    fun getSyncedFolders(target: WallpaperTarget): Flow<List<SyncedFolder>> = dao.getSyncedFolders(target)
    suspend fun getAllSyncedFoldersSync(): List<SyncedFolder> = dao.getAllSyncedFoldersSync()

    suspend fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)
    suspend fun setHomeDoubleTapEnabled(enabled: Boolean) = preferences.setHomeDoubleTapEnabled(enabled)
    suspend fun isHomeDoubleTapEnabledSync(): Boolean = preferences.isHomeDoubleTapEnabledSync()

    suspend fun addItems(items: List<WallpaperItem>) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        val target = items.first().target
        val existing = dao.getItemsByTargetSync(target)
        dao.insertItems(items)

        // Ensure rotation config exists
        ensureConfigExists(target)

        // If this was the first item added and no work was scheduled, apply wallpaper and schedule
        if (existing.isEmpty() && items.isNotEmpty()) {
            val config = dao.getRotationConfigSync(target) ?: RotationConfig(target)
            applyCurrentWallpaper(target, 0)
            if (!(target == WallpaperTarget.LOCK && config.autoChangeOnUnlock)) {
                val duration = getItemDuration(items.first(), config)
                val nextTime = System.currentTimeMillis() + (duration * 1000L)
                dao.updateNextScheduledAt(target, nextTime)
                scheduler.scheduleNextRotation(target, duration)
            }
        }
    }

    suspend fun importFromFolder(target: WallpaperTarget, treeUri: Uri, folderName: String): Int = withContext(Dispatchers.IO) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )

        val cursor = context.contentResolver.query(
            childrenUri,
            projection,
            null,
            null,
            null
        )

        val existingItems = dao.getItemsByTargetSync(target)
        val existingUris = existingItems.map { it.uri }.toSet()
        val newItems = mutableListOf<WallpaperItem>()
        var currentSort = (existingItems.maxOfOrNull { it.sortIndex } ?: -1) + 1

        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val mimeCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (c.moveToNext()) {
                val mime = c.getString(mimeCol)
                if (mime != null && mime.startsWith("image/")) {
                    val docId = c.getString(idCol)
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()
                    if (!existingUris.contains(docUri)) {
                        newItems.add(
                            WallpaperItem(
                                uri = docUri,
                                target = target,
                                durationSeconds = 900,
                                sourceFolderUri = treeUri.toString(),
                                sortIndex = currentSort++
                            )
                        )
                    }
                }
            }
        }

        if (newItems.isNotEmpty()) {
            dao.insertSyncedFolder(
                SyncedFolder(
                    treeUri = treeUri.toString(),
                    target = target,
                    folderName = folderName,
                    lastCheckedAt = System.currentTimeMillis()
                )
            )
            addItems(newItems)
            scheduler.schedulePeriodicFolderSync()
        }
        newItems.size
    }

    suspend fun updateItemDuration(itemId: String, durationSeconds: Int) = withContext(Dispatchers.IO) {
        val items = dao.getItemsByTargetSync(WallpaperTarget.HOME) + dao.getItemsByTargetSync(WallpaperTarget.LOCK)
        val item = items.find { it.id == itemId } ?: return@withContext
        val updated = item.copy(durationSeconds = durationSeconds)
        dao.updateItem(updated)
    }

    suspend fun applyGlobalDuration(target: WallpaperTarget, durationSeconds: Int) = withContext(Dispatchers.IO) {
        val config = dao.getRotationConfigSync(target) ?: RotationConfig(target)
        val updatedConfig = config.copy(
            durationMode = DurationMode.GLOBAL,
            globalDurationSeconds = durationSeconds
        )
        dao.saveRotationConfig(updatedConfig)
        dao.applyGlobalDurationToAll(target, durationSeconds)

        // Reschedule if currently scheduled
        if (!(target == WallpaperTarget.LOCK && updatedConfig.autoChangeOnUnlock)) {
            val items = dao.getItemsByTargetSync(target)
            if (items.isNotEmpty()) {
                val nextTime = System.currentTimeMillis() + (durationSeconds * 1000L)
                dao.updateNextScheduledAt(target, nextTime)
                scheduler.scheduleNextRotation(target, durationSeconds)
            }
        }
    }

    suspend fun setDurationMode(target: WallpaperTarget, mode: DurationMode) = withContext(Dispatchers.IO) {
        val config = dao.getRotationConfigSync(target) ?: RotationConfig(target)
        val updated = config.copy(durationMode = mode)
        dao.saveRotationConfig(updated)
    }

    suspend fun setPlaybackOrder(target: WallpaperTarget, order: PlaybackOrder) = withContext(Dispatchers.IO) {
        val config = dao.getRotationConfigSync(target) ?: RotationConfig(target)
        val updated = config.copy(playbackOrder = order)
        dao.saveRotationConfig(updated)
    }

    suspend fun setAutoChangeOnUnlock(autoChange: Boolean) = withContext(Dispatchers.IO) {
        val target = WallpaperTarget.LOCK
        val config = dao.getRotationConfigSync(target) ?: RotationConfig(target)
        val updated = config.copy(
            autoChangeOnUnlock = autoChange,
            nextScheduledAt = if (autoChange) null else config.nextScheduledAt
        )
        dao.saveRotationConfig(updated)

        if (autoChange) {
            scheduler.cancelRotation(target)
            dao.updateNextScheduledAt(target, null)
        } else {
            val items = dao.getItemsByTargetSync(target)
            if (items.isNotEmpty()) {
                val currentIndex = config.currentIndex.coerceIn(0, items.size - 1)
                val duration = getItemDuration(items[currentIndex], updated)
                val nextTime = System.currentTimeMillis() + (duration * 1000L)
                dao.updateNextScheduledAt(target, nextTime)
                scheduler.scheduleNextRotation(target, duration)
            }
        }
    }

    suspend fun reorderItems(target: WallpaperTarget, reorderedList: List<WallpaperItem>) = withContext(Dispatchers.IO) {
        dao.updateItemOrder(target, reorderedList)
    }

    suspend fun removeItem(itemId: String, target: WallpaperTarget) = withContext(Dispatchers.IO) {
        dao.deleteItemById(itemId)
        val remaining = dao.getItemsByTargetSync(target)
        if (remaining.isEmpty()) {
            scheduler.cancelRotation(target)
            dao.updateNextScheduledAt(target, null)
            dao.updateCurrentIndex(target, 0)
        } else {
            val config = dao.getRotationConfigSync(target) ?: RotationConfig(target)
            if (config.currentIndex >= remaining.size) {
                dao.updateCurrentIndex(target, 0)
            }
        }
    }

    /**
     * Advances the rotation to the next photo.
     * If isScheduledTrigger is true, calculates next scheduled time and reschedules.
     * If false (e.g. manual skip from Widget or Live Wallpaper double-tap), maintains running schedule per PRD §7.5.
     */
    suspend fun advanceRotation(target: WallpaperTarget, isScheduledTrigger: Boolean): Boolean = withContext(Dispatchers.IO) {
        val items = dao.getItemsByTargetSync(target)
        if (items.isEmpty()) {
            scheduler.cancelRotation(target)
            dao.updateNextScheduledAt(target, null)
            return@withContext false
        }

        val config = dao.getRotationConfigSync(target) ?: RotationConfig(target)
        val nextIndex = computeNextIndex(config.currentIndex, items.size, config.playbackOrder)

        // Apply wallpaper
        val itemToApply = items[nextIndex]
        val success = WallpaperApplier.applyWallpaper(context, itemToApply.uri, target)

        if (success) {
            dao.updateCurrentIndex(target, nextIndex)
        }

        // If target is Lock with unlock trigger, don't schedule duration work
        if (target == WallpaperTarget.LOCK && config.autoChangeOnUnlock) {
            dao.updateNextScheduledAt(target, null)
            return@withContext success
        }

        // Reschedule duration-based work
        if (isScheduledTrigger) {
            val duration = getItemDuration(itemToApply, config)
            val nextTime = System.currentTimeMillis() + (duration * 1000L)
            dao.updateNextScheduledAt(target, nextTime)
            scheduler.scheduleNextRotation(target, duration)
        }

        success
    }

    suspend fun applyCurrentWallpaper(target: WallpaperTarget, index: Int): Boolean = withContext(Dispatchers.IO) {
        val items = dao.getItemsByTargetSync(target)
        if (items.isEmpty()) return@withContext false
        val validIndex = index.coerceIn(0, items.size - 1)
        val item = items[validIndex]
        val success = WallpaperApplier.applyWallpaper(context, item.uri, target)
        if (success) {
            dao.updateCurrentIndex(target, validIndex)
        }
        success
    }

    suspend fun executeRollSetup(target: WallpaperTarget): Boolean = withContext(Dispatchers.IO) {
        val items = dao.getItemsByTargetSync(target)
        if (items.isEmpty()) return@withContext false

        ensureConfigExists(target)
        val config = dao.getRotationConfigSync(target) ?: RotationConfig(target)
        val validIndex = config.currentIndex.coerceIn(0, items.size - 1)
        val success = applyCurrentWallpaper(target, validIndex)

        if (!(target == WallpaperTarget.LOCK && config.autoChangeOnUnlock)) {
            val currentItem = items[validIndex]
            val duration = getItemDuration(currentItem, config)
            val nextTime = System.currentTimeMillis() + (duration * 1000L)
            dao.updateNextScheduledAt(target, nextTime)
            scheduler.scheduleNextRotation(target, duration)
        }
        success
    }

    private suspend fun ensureConfigExists(target: WallpaperTarget) {
        val existing = dao.getRotationConfigSync(target)
        if (existing == null) {
            dao.saveRotationConfig(RotationConfig(target = target))
        }
    }

    private fun computeNextIndex(currentIndex: Int, totalSize: Int, playbackOrder: PlaybackOrder): Int {
        if (totalSize <= 1) return 0
        return when (playbackOrder) {
            PlaybackOrder.SEQUENTIAL -> (currentIndex + 1) % totalSize
            PlaybackOrder.SHUFFLE -> {
                var next = Random.nextInt(totalSize)
                if (next == currentIndex) {
                    next = (currentIndex + 1) % totalSize
                }
                next
            }
        }
    }

    private fun getItemDuration(item: WallpaperItem, config: RotationConfig): Int {
        return if (config.durationMode == DurationMode.GLOBAL) {
            config.globalDurationSeconds
        } else {
            item.durationSeconds
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: RollRepository? = null

        fun getInstance(context: Context): RollRepository {
            return INSTANCE ?: synchronized(this) {
                val db = RollDatabase.getInstance(context)
                val preferences = RollPreferences(context)
                val scheduler = RotationScheduler(context)
                val instance = RollRepository(context.applicationContext, db.rollDao(), preferences, scheduler)
                INSTANCE = instance
                instance
            }
        }
    }
}
