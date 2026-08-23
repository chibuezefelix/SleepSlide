package com.opxl.sleepslide.data.local
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Local.SoundEntity::class,
        Local.PresetEntity::class,
        Local.PresetLayerEntity::class,
        Local.PlayHistoryEntity::class,
        Local.SoundVolumeMemoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun soundDao(): SoundDao
    abstract fun presetDao(): PresetDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun soundVolumeMemoryDao(): SoundVolumeMemoryDao

    companion object {
        const val NAME = "sleepslide.db"
    }
}