package com.avikalabs.roll.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "synced_folders", primaryKeys = ["treeUri", "target"])
data class SyncedFolder(
    val treeUri: String,
    val target: WallpaperTarget,
    val folderName: String = "",
    val lastCheckedAt: Long = System.currentTimeMillis()
)
