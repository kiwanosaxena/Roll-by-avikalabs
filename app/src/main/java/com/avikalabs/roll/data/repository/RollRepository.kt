package com.avikalabs.roll.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.avikalabs.roll.data.local.RollDao
import com.avikalabs.roll.data.local.RollDatabase
import com.avikalabs.roll.data.local.RollPreferences
import com.avikalabs.roll.data.local.ThemeMode
import com.avikalabs.roll.data.model.DurationMode
import com.avikalabs.roll.data.model.PlaybackOrder
import com.avikalabs.roll.data.model.RemovedUri
import com.avikalabs.roll.data.model.RotationConfig
import com.avikalabs.roll.data.model.SyncedFolder
import com.avikalabs.roll.data.model.WallpaperItem
import com.avikalabs.roll.data.model.WallpaperTarget
import com.avikalabs.roll.wallpaper.WallpaperApplier
import com.avikalabs.roll.worker.RotationScheduler
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
    val isProFlow: Flow<Boolean> = preferences.isProFlow

    fun isProSync(): Boolean = preferences.isProSync()
    suspend fun setIsPro(isPro: Boolean) = preferences.setIsPro(isPro)

    fun getItems(target: WallpaperTarget): Flow<List<WallpaperItem>> = dao.getItemsByTarget(target)
    suspend fun getItemsByTargetSync(target: WallpaperTarget): List<WallpaperItem> = dao.getItemsByTargetSync(target)
    fun getItemCount(target: WallpaperTarget): Flow<Int> = dao.getItemCountByTarget(target)
    fun getTotalItemCount(): Flow<Int> = dao.getTotalItemCount()

    fun getRotationConfig(target: WallpaperTarget): Flow<RotationConfig?> = dao.getRotationConfig(target)
    suspend fun getRotationConfigSync(target: WallpaperTarget): RotationConfig? = dao.getRotationConfigSync(target)

    fun getSyncedFolders(target: WallpaperTarget): Flow<List<SyncedFolder>> = dao.getSyncedFolders(target)
    suspend fun getAllSyncedFoldersSync(): List<SyncedFolder> = dao.getAllSyncedFoldersSync()

    val finishedTipSetsFlow: Flow<Set<String>> = preferences.finishedTipSetsFlow
    val tutorialsDisabledFlow: Flow<Boolean> = preferences.tutorialsDisabledFlow

    suspend fun markTipSetFinished(tipSetId: String) = preferences.markTipSetFinished(tipSetId)
    suspend fun setTutorialsDisabled(disabled: Boolean) = preferences.setTutorialsDisabled(disabled)
    suspend fun resetTutorials() = preferences.resetTutorials()

    suspend fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)
    suspend fun setHomeDoubleTapEnabled(enabled: Boolean) {
        preferences.setHomeDoubleTapEnabled(enabled)
    }
    suspend fun isHomeDoubleTapEnabledSync(): Boolean = preferences.isHomeDoubleTapEnabledSync()

    suspend fun addItems(items: List<WallpaperItem>) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        val isPro = isProSync()
        val itemsToInsert = if (!isPro) {
            val totalExisting = dao.getItemsByTargetSync(WallpaperTarget.HOME).size +
                    dao.getItemsByTargetSync(WallpaperTarget.LOCK).size
            val allowed = (10 - totalExisting).coerceAtLeast(0)
            if (allowed <= 0) return@withContext
            if (items.size > allowed) items.take(allowed) else items
        } else {
            items
        }
        if (itemsToInsert.isEmpty()) return@withContext

        val target = itemsToInsert.first().target
        val existing = dao.getItemsByTargetSync(target)
        dao.insertItems(itemsToInsert)

        // Ensure rotation config exists
        ensureConfigExists(target)

        // If this was the first item added and no work was scheduled, apply wallpaper and schedule
        if (existing.isEmpty() && itemsToInsert.isNotEmpty()) {
            val config = dao.getRotationConfigSync(target) ?: RotationConfig(target)
            applyCurrentWallpaper(target, 0)
            if (!(target == WallpaperTarget.LOCK && config.autoChangeOnUnlock && isPro)) {
                val duration = getItemDuration(itemsToInsert.first(), config)
                val nextTime = System.currentTimeMillis() + (duration * 1000L)
                dao.updateNextScheduledAt(target, nextTime)
                scheduler.scheduleNextRotation(target, duration)
            }
        }
    }

    suspend fun importFromFolder(target: WallpaperTarget, treeUri: Uri, folderName: String): Int = withContext(Dispatchers.IO) {
        if (!isProSync()) return@withContext 0
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

        val folderDocUris = mutableListOf<String>()
        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val mimeCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (c.moveToNext()) {
                val mime = c.getString(mimeCol)
                if (mime != null && mime.startsWith("image/")) {
                    val docId = c.getString(idCol)
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()
                    folderDocUris.add(docUri)
                }
            }
        }

        // When the user manually re-imports a folder, clear its entries from removed_uris first
        val allRemovedUris = dao.getAllRemovedUris().toSet()
        val toClear = folderDocUris.filter { allRemovedUris.contains(it) }
        if (toClear.isNotEmpty()) {
            toClear.chunked(500).forEach { chunk ->
                dao.deleteRemovedUris(chunk)
            }
        }
        val remainingRemovedUris = allRemovedUris - toClear.toSet()

        val existingItems = dao.getItemsByTargetSync(target)
        val existingUris = existingItems.map { it.uri }.toSet()
        val newItems = mutableListOf<WallpaperItem>()
        var currentSort = (existingItems.maxOfOrNull { it.sortIndex } ?: -1) + 1

        for (docUri in folderDocUris) {
            if (!existingUris.contains(docUri) && !remainingRemovedUris.contains(docUri)) {
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

    suspend fun disableUnlockModeIfLiveWallpaperInactive() = withContext(Dispatchers.IO) {
        val lockConfig = dao.getRotationConfigSync(WallpaperTarget.LOCK) ?: return@withContext
        if (lockConfig.autoChangeOnUnlock && !WallpaperApplier.isLiveWallpaperActive(context)) {
            setAutoChangeOnUnlock(false)
        }
    }

    suspend fun reorderItems(target: WallpaperTarget, reorderedList: List<WallpaperItem>) = withContext(Dispatchers.IO) {
        dao.updateItemOrder(target, reorderedList)
    }

    suspend fun removeItem(itemId: String, target: WallpaperTarget) = withContext(Dispatchers.IO) {
        val currentItems = dao.getItemsByTargetSync(target)
        val removedItem = currentItems.find { it.id == itemId }
        val removedIndex = currentItems.indexOfFirst { it.id == itemId }
        val config = dao.getRotationConfigSync(target) ?: RotationConfig(target)
        val oldShowingIndex = config.currentIndex

        if (removedItem != null && !removedItem.sourceFolderUri.isNullOrEmpty()) {
            dao.insertRemovedUri(RemovedUri(removedItem.uri))
        }

        dao.deleteItemById(itemId)
        val remaining = dao.getItemsByTargetSync(target)
        if (remaining.isEmpty()) {
            scheduler.cancelRotation(target)
            dao.updateNextScheduledAt(target, null)
            dao.updateCurrentIndex(target, 0)
        } else {
            if (removedIndex != -1 && removedIndex < oldShowingIndex) {
                // When removing a photo that comes before the one on screen,
                // shift current position so "Showing now" marker remains on the same photo
                val shiftedIndex = (oldShowingIndex - 1).coerceIn(0, remaining.size - 1)
                dao.updateCurrentIndex(target, shiftedIndex)
            } else if (removedIndex != -1 && removedIndex == oldShowingIndex) {
                // Removing the photo that's currently on screen changes the wallpaper
                val newIndex = oldShowingIndex.coerceIn(0, remaining.size - 1)
                dao.updateCurrentIndex(target, newIndex)

                val newItem = remaining[newIndex]
                WallpaperApplier.applyWallpaper(context, newItem.uri, target)
                if (!isProSync() && target == WallpaperTarget.HOME) {
                    WallpaperApplier.applyWallpaper(context, newItem.uri, WallpaperTarget.LOCK)
                }

                if (target == WallpaperTarget.LOCK && config.autoChangeOnUnlock && isProSync()) {
                    dao.updateNextScheduledAt(target, null)
                } else {
                    val duration = getItemDuration(newItem, config)
                    val nextTime = System.currentTimeMillis() + (duration * 1000L)
                    dao.updateNextScheduledAt(target, nextTime)
                    scheduler.scheduleNextRotation(target, duration)
                }
            } else {
                if (oldShowingIndex >= remaining.size) {
                    val clampedIndex = (remaining.size - 1).coerceAtLeast(0)
                    dao.updateCurrentIndex(target, clampedIndex)
                }
            }
        }
    }

    /**
     * Advances the rotation to the next photo.
     * If isScheduledTrigger is true, calculates next scheduled time and reschedules.
     * If false (e.g. manual skip from Widget or Live Wallpaper double-tap), maintains running schedule per PRD §7.5.
     */
    suspend fun advanceRotation(target: WallpaperTarget, isScheduledTrigger: Boolean): Boolean = withContext(Dispatchers.IO) {
        val isPro = isProSync()
        if (!isPro && target == WallpaperTarget.LOCK) {
            scheduler.cancelRotation(WallpaperTarget.LOCK)
            dao.updateNextScheduledAt(WallpaperTarget.LOCK, null)
            return@withContext false
        }

        val allItems = dao.getItemsByTargetSync(target)
        val items = if (!isPro) allItems.take(10) else allItems
        if (items.isEmpty()) {
            scheduler.cancelRotation(target)
            dao.updateNextScheduledAt(target, null)
            return@withContext false
        }

        val config = dao.getRotationConfigSync(target) ?: RotationConfig(target)
        var currentIndex = config.currentIndex
        var attempts = 0
        var success = false
        var itemToApply: WallpaperItem? = null
        var chosenIndex = currentIndex

        while (attempts < items.size && !success) {
            chosenIndex = computeNextIndex(currentIndex, items.size, config.playbackOrder)
            val candidate = items[chosenIndex]
            success = WallpaperApplier.applyWallpaper(context, candidate.uri, target)
            if (success) {
                itemToApply = candidate
                dao.updateCurrentIndex(target, chosenIndex)
                if (!isPro && target == WallpaperTarget.HOME) {
                    WallpaperApplier.applyWallpaper(context, candidate.uri, WallpaperTarget.LOCK)
                }
                break
            } else {
                // Candidate photo missing or inaccessible, attempt next candidate
                currentIndex = chosenIndex
                attempts++
            }
        }

        if (success && itemToApply != null) {
            // If target is Lock with unlock trigger, don't schedule duration work
            if (target == WallpaperTarget.LOCK && config.autoChangeOnUnlock && isPro) {
                dao.updateNextScheduledAt(target, null)
                return@withContext true
            }

            // Reschedule duration-based work
            if (isScheduledTrigger) {
                val duration = getItemDuration(itemToApply, config)
                val nextTime = System.currentTimeMillis() + (duration * 1000L)
                dao.updateNextScheduledAt(target, nextTime)
                scheduler.scheduleNextRotation(target, duration)
            }
            return@withContext true
        } else {
            // If all items failed, reschedule fallback check if scheduled
            if (isScheduledTrigger && !(target == WallpaperTarget.LOCK && config.autoChangeOnUnlock && isPro)) {
                val duration = 900
                val nextTime = System.currentTimeMillis() + (duration * 1000L)
                dao.updateNextScheduledAt(target, nextTime)
                scheduler.scheduleNextRotation(target, duration)
            }
            return@withContext false
        }
    }

    suspend fun applyCurrentWallpaper(target: WallpaperTarget, index: Int): Boolean = withContext(Dispatchers.IO) {
        val isPro = isProSync()
        val effectiveTarget = if (!isPro) WallpaperTarget.HOME else target
        val allItems = dao.getItemsByTargetSync(effectiveTarget)
        val items = if (!isPro) allItems.take(10) else allItems
        if (items.isEmpty()) return@withContext false
        val validIndex = index.coerceIn(0, items.size - 1)
        val item = items[validIndex]
        val success = WallpaperApplier.applyWallpaper(context, item.uri, effectiveTarget)
        if (success) {
            dao.updateCurrentIndex(effectiveTarget, validIndex)
            if (!isPro) {
                WallpaperApplier.applyWallpaper(context, item.uri, WallpaperTarget.LOCK)
            }
        }
        success
    }

    suspend fun executeRollSetup(target: WallpaperTarget): Boolean = withContext(Dispatchers.IO) {
        val isPro = isProSync()
        val effectiveTarget = if (!isPro) WallpaperTarget.HOME else target
        val allItems = dao.getItemsByTargetSync(effectiveTarget)
        val items = if (!isPro) allItems.take(10) else allItems
        if (items.isEmpty()) return@withContext false

        ensureConfigExists(effectiveTarget)
        val config = dao.getRotationConfigSync(effectiveTarget) ?: RotationConfig(effectiveTarget)
        val validIndex = config.currentIndex.coerceIn(0, items.size - 1)
        val success = applyCurrentWallpaper(effectiveTarget, validIndex)

        if (!isPro) {
            scheduler.cancelRotation(WallpaperTarget.LOCK)
            dao.updateNextScheduledAt(WallpaperTarget.LOCK, null)
        }

        if (!(effectiveTarget == WallpaperTarget.LOCK && config.autoChangeOnUnlock && isPro)) {
            val currentItem = items[validIndex]
            val duration = getItemDuration(currentItem, config)
            val nextTime = System.currentTimeMillis() + (duration * 1000L)
            dao.updateNextScheduledAt(effectiveTarget, nextTime)
            scheduler.scheduleNextRotation(effectiveTarget, duration)
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
        val effectiveOrder = if (isProSync()) playbackOrder else PlaybackOrder.SEQUENTIAL
        return when (effectiveOrder) {
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

    suspend fun getAllRemovedUris(): List<String> = withContext(Dispatchers.IO) {
        dao.getAllRemovedUris()
    }

    suspend fun deleteRemovedUris(uris: List<String>) = withContext(Dispatchers.IO) {
        if (uris.isNotEmpty()) {
            uris.chunked(500).forEach { chunk ->
                dao.deleteRemovedUris(chunk)
            }
        }
    }

    suspend fun clearRemovedUris() = withContext(Dispatchers.IO) {
        dao.clearRemovedUris()
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
