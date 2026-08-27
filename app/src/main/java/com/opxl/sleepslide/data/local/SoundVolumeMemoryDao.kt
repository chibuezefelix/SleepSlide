package com.opxl.sleepslide.data.local


import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundVolumeMemoryDao {

    @Query("SELECT * FROM sound_volume_memory WHERE soundId = :soundId")
    suspend fun getForSound(soundId: String): Local.SoundVolumeMemoryEntity?

    @Query("SELECT * FROM sound_volume_memory WHERE soundId = :soundId")
    fun observeForSound(soundId: String): Flow<Local.SoundVolumeMemoryEntity?>

    @Query("SELECT * FROM sound_volume_memory")
    suspend fun getAll(): List<Local.SoundVolumeMemoryEntity>

    @Query("SELECT * FROM sound_volume_memory WHERE soundId IN (:soundIds)")
    suspend fun getAllForSounds(soundIds: List<String>): List<Local.SoundVolumeMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: Local.SoundVolumeMemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<Local.SoundVolumeMemoryEntity>)

    @Query("UPDATE sound_volume_memory SET volume = :volume, updatedAt = :timestamp WHERE soundId = :soundId")
    suspend fun updateVolume(soundId: String, volume: Float, timestamp: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM sound_volume_memory WHERE soundId = :soundId")
    suspend fun deleteForSound(soundId: String) : Int

    @Query("DELETE FROM sound_volume_memory")
    suspend fun deleteAll() : Int
}
 