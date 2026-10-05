package com.avikalabs.roll.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "wallpaper_items")
data class WallpaperItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val target: WallpaperTarget,
    val durationSeconds: Int = 900, // default 15 minutes
    val sourceFolderUri: String? = null,
    val sortIndex: Int = 0
)
