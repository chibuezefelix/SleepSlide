package com.opxl.sleepslide.data.local

import androidx.room.*

import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {

    @Transaction
    @Query("SELECT * FROM presets ORDER BY isPinned DESC, sortOrder ASC, createdAt DESC")
    fun observeAll(): Flow<List<Local.PresetWithLayers>>

    @Transaction
    @Query("SELECT * FROM presets WHERE isPinned = 1 ORDER BY sortOrder ASC")
    fun observePinned(): Flow<List<Local.PresetWithLayers>>

    @Transaction
    @Query("SELECT * FROM presets WHERE id = :id")
    fun observeById(id: Long): Flow<Local.PresetWithLayers?>

    @Transaction
    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getById(id: Long): Local.PresetWithLayers?

    @Transaction
    @Query("""
        SELECT * FROM presets 
        WHERE lastUsedAt IS NOT NULL 
        ORDER BY lastUsedAt DESC 
        LIMIT 1
    """)
    suspend fun getLastUsed(): Local.PresetWithLayers?

    @Transaction
    @Query("""
        SELECT * FROM presets 
        WHERE lastUsedAt IS NOT NULL 
        ORDER BY lastUsedAt DESC 
        LIMIT :limit
    """)
    fun observeRecentlyUsed(limit: Int = 5): Flow<List<Local.PresetWithLayers>>

    @Query("SELECT COUNT(*) FROM presets")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: Local.PresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayers(layers: List<Local.PresetLayerEntity>)

    @Transaction
    suspend fun upsertFull(preset: Local.PresetEntity, layers: List<Local.PresetLayerEntity>): Long {
        val id = insertPreset(preset)
        deleteLayersByPresetId(id)
        insertLayers(layers.map { it.copy(presetId = id) })
        return id
    }

    @Update
    suspend fun updatePreset(preset: Local.PresetEntity)

    @Query("UPDATE presets SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long)

    @Query("UPDATE presets SET isPinned = :pinned WHERE id = :id")
    suspend fun updatePinned(id: Long, pinned: Boolean)

    @Query("UPDATE presets SET sortOrder = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Long, order: Int)

    @Query("UPDATE presets SET name = :name WHERE id = :id")
    suspend fun updateName(id: Long, name: String)

    @Query("UPDATE presets SET emoji = :emoji WHERE id = :id")
    suspend fun updateEmoji(id: Long, emoji: String?)

    @Transaction
    suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            updateSortOrder(id, index)
        }
    }

    @Query("DELETE FROM preset_layers WHERE presetId = :presetId")
    suspend fun deleteLayersByPresetId(presetId: Long)

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun delete(preset: Local.PresetEntity)

    @Query("DELETE FROM presets")
    suspend fun deleteAll()
}