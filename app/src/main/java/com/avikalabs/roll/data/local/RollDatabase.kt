package com.avikalabs.roll.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.avikalabs.roll.data.model.DurationMode
import com.avikalabs.roll.data.model.PlaybackOrder
import com.avikalabs.roll.data.model.RemovedUri
import com.avikalabs.roll.data.model.RotationConfig
import com.avikalabs.roll.data.model.SyncedFolder
import com.avikalabs.roll.data.model.WallpaperItem
import com.avikalabs.roll.data.model.WallpaperTarget

class RollTypeConverters {
    @TypeConverter
    fun fromTarget(value: WallpaperTarget): String = value.name

    @TypeConverter
    fun toTarget(value: String): WallpaperTarget = WallpaperTarget.valueOf(value)

    @TypeConverter
    fun fromDurationMode(value: DurationMode): String = value.name

    @TypeConverter
    fun toDurationMode(value: String): DurationMode = DurationMode.valueOf(value)

    @TypeConverter
    fun fromPlaybackOrder(value: PlaybackOrder): String = value.name

    @TypeConverter
    fun toPlaybackOrder(value: String): PlaybackOrder = PlaybackOrder.valueOf(value)
}

@Database(
    entities = [
        WallpaperItem::class,
        RotationConfig::class,
        SyncedFolder::class,
        RemovedUri::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(RollTypeConverters::class)
abstract class RollDatabase : RoomDatabase() {

    abstract fun rollDao(): RollDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `removed_uris` (`uri` TEXT NOT NULL, PRIMARY KEY(`uri`))")
            }
        }

        @Volatile
        private var INSTANCE: RollDatabase? = null

        fun getInstance(context: Context): RollDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RollDatabase::class.java,
                    "roll_database.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
