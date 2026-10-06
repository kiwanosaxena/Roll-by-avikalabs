package com.avikalabs.roll.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.avikalabs.roll.data.model.RemovedUri
import com.avikalabs.roll.data.model.RotationConfig
import com.avikalabs.roll.data.model.SyncedFolder
import com.avikalabs.roll.data.model.WallpaperItem
import com.avikalabs.roll.data.model.WallpaperTarget
import kotlinx.coroutines.flow.Flow

@Dao
interface RollDao {

    @Query("SELECT * FROM wallpaper_items WHERE target = :target ORDER BY sortIndex ASC")
    fun getItemsByTarget(target: WallpaperTarget): Flow<List<WallpaperItem>>

    @Query("SELECT * FROM wallpaper_items WHERE target = :target ORDER BY sortIndex ASC")
    suspend fun getItemsByTargetSync(target: WallpaperTarget): List<WallpaperItem>

    @Query("SELECT COUNT(*) FROM wallpaper_items WHERE target = :target")
    fun getItemCountByTarget(target: WallpaperTarget): Flow<Int>

    @Query("SELECT COUNT(*) FROM wallpaper_items")
    fun getTotalItemCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<WallpaperItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: WallpaperItem)

    @Update
    suspend fun updateItem(item: WallpaperItem)

    @Update
    suspend fun updateItems(items: List<WallpaperItem>)

    @Query("DELETE FROM wallpaper_items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("DELETE FROM wallpaper_items WHERE target = :target")
    suspend fun clearItemsByTarget(target: WallpaperTarget)

    @Query("SELECT * FROM rotation_configs WHERE target = :target")
    fun getRotationConfig(target: WallpaperTarget): Flow<RotationConfig?>

    @Query("SELECT * FROM rotation_configs WHERE target = :target")
    suspend fun getRotationConfigSync(target: WallpaperTarget): RotationConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRotationConfig(config: RotationConfig)

    @Query("UPDATE rotation_configs SET currentIndex = :newIndex WHERE target = :target")
    suspend fun updateCurrentIndex(target: WallpaperTarget, newIndex: Int)

    @Query("UPDATE rotation_configs SET nextScheduledAt = :nextTime WHERE target = :target")
    suspend fun updateNextScheduledAt(target: WallpaperTarget, nextTime: Long?)

    @Query("UPDATE wallpaper_items SET durationSeconds = :duration WHERE target = :target")
    suspend fun applyGlobalDurationToAll(target: WallpaperTarget, duration: Int)

    @Query("SELECT * FROM synced_folders WHERE target = :target")
    fun getSyncedFolders(target: WallpaperTarget): Flow<List<SyncedFolder>>

    @Query("SELECT * FROM synced_folders")
    suspend fun getAllSyncedFoldersSync(): List<SyncedFolder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncedFolder(folder: SyncedFolder)

    @Query("DELETE FROM synced_folders WHERE treeUri = :treeUri AND target = :target")
    suspend fun deleteSyncedFolder(treeUri: String, target: WallpaperTarget)

    @Transaction
    suspend fun updateItemOrder(target: WallpaperTarget, orderedItems: List<WallpaperItem>) {
        val updated = orderedItems.mapIndexed { index, item ->
            item.copy(sortIndex = index)
        }
        updateItems(updated)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemovedUri(removedUri: RemovedUri)

    @Query("SELECT uri FROM removed_uris")
    suspend fun getAllRemovedUris(): List<String>

    @Query("DELETE FROM removed_uris WHERE uri = :uri")
    suspend fun deleteRemovedUri(uri: String)

    @Query("DELETE FROM removed_uris WHERE uri IN (:uris)")
    suspend fun deleteRemovedUris(uris: List<String>)

    @Query("DELETE FROM removed_uris")
    suspend fun clearRemovedUris()
}
