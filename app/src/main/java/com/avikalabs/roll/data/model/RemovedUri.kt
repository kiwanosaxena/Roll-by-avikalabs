package com.avikalabs.roll.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "removed_uris")
data class RemovedUri(
    @PrimaryKey
    val uri: String
)
