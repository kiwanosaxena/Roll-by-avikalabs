package com.avikalabs.roll.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rotation_configs")
data class RotationConfig(
    @PrimaryKey
    val target: WallpaperTarget,
    val durationMode: DurationMode = DurationMode.PER_ITEM,
    val globalDurationSeconds: Int = 900,
    val playbackOrder: PlaybackOrder = PlaybackOrder.SEQUENTIAL,
    val currentIndex: Int = 0,
    val autoChangeOnUnlock: Boolean = false,
    val nextScheduledAt: Long? = null
)
