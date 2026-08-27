package com.opxl.sleepslide.data.local


import androidx.room.*

import kotlinx.coroutines.flow.Flow

@Dao
interface SoundDao {

    @Query("SELECT * FROM sounds ORDER BY category, title")
    fun observeAll(): Flow<List<Local.SoundEntity>>

    @Query("SELECT * FROM sounds WHERE category = :category ORDER BY title")
    fun observeByCategory(category: String): Flow<List<Local.SoundEntity>>

    @Query("SELECT * FROM sounds WHERE id = :id")
    suspend fun getById(id: String): Local.SoundEntity?

    @Query("SELECT * FROM sounds WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<Local.SoundEntity>

    @Query("SELECT * FROM sounds")
    suspend fun getAll(): List<Local.SoundEntity>

    @Query("SELECT * FROM sounds WHERE isPremium = 0 ORDER BY category, title")
    fun observeFree(): Flow<List<Local.SoundEntity>>

    @Query("SELECT * FROM sounds WHERE isPremium = 1 ORDER BY category, title")
    fun observePremium(): Flow<List<Local.SoundEntity>>

    @Query("""
        SELECT * FROM sounds
        WHERE lastPlayedAt IS NOT NULL
        ORDER BY lastPlayedAt DESC
        LIMIT :limit
    """)
    fun observeRecentlyPlayed(limit: Int = 10): Flow<List<Local.SoundEntity>>

    @Query("SELECT * FROM sounds ORDER BY playCount DESC LIMIT :limit")
    fun observeMostPlayed(limit: Int = 10): Flow<List<Local.SoundEntity>>

    @Query("SELECT * FROM sounds WHERE isBundled = 0 AND downloadedPath IS NOT NULL")
    fun observeDownloaded(): Flow<List<Local.SoundEntity>>

    @Query("SELECT * FROM sounds WHERE isDownloading = 1")
    fun observeDownloading(): Flow<List<Local.SoundEntity>>

    @Query("SELECT * FROM sounds WHERE tags LIKE '%' || :tag || '%'")
    fun observeByTag(tag: String): Flow<List<Local.SoundEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(sounds: List<Local.SoundEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(sound: Local.SoundEntity)

    @Update
    suspend fun update(sound: Local.SoundEntity)

    @Query("UPDATE sounds SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: String, timestamp: Long)

    @Query("""
        UPDATE sounds
        SET isDownloading = :isDownloading,
            downloadedBytes = :downloadedBytes,
            totalBytes = :totalBytes
        WHERE id = :id
    """)
    suspend fun updateDownloadProgress(
        id: String,
        isDownloading: Boolean,
        downloadedBytes: Long,
        totalBytes: Long,
    )

    @Query("""
        UPDATE sounds
        SET downloadedPath = :path,
            isDownloading = 0
        WHERE id = :id
    """)
    suspend fun markDownloadComplete(id: String, path: String)

    @Query("UPDATE sounds SET downloadedPath = NULL, isDownloading = 0 WHERE id = :id")
    suspend fun clearDownload(id: String) : Int

    @Query("DELETE FROM sounds WHERE isBundled = 0 AND downloadedPath IS NULL AND isDownloading = 0")
    suspend fun deleteOrphaned() : Int

    @Delete
    suspend fun delete(sound: Local.SoundEntity)
}
