package com.opxl.sleepslide.domain.repository

import com.opxl.sleepslide.domain.model.Domain
import kotlinx.coroutines.flow.Flow

interface SoundRepository {

    fun observeAll(): Flow<List<Domain.Sound>>

    fun observeByCategory(category: Domain.SoundCategory): Flow<List<Domain.Sound>>

    fun observeFree(): Flow<List<Domain.Sound>>

    fun observePremium(): Flow<List<Domain.Sound>>

    fun observeRecentlyPlayed(limit: Int = 10): Flow<List<Domain.Sound>>

    fun observeMostPlayed(limit: Int = 10): Flow<List<Domain.Sound>>

    fun observeDownloaded(): Flow<List<Domain.Sound>>

    suspend fun getById(id: String): Domain.Sound?
    suspend fun getByIds(ids: List<String>): List<Domain.Sound>

    suspend fun getAll(): List<Domain.Sound>

    suspend fun seedBundledSounds()

    suspend fun recordPlayed(soundId: String)

    suspend fun beginDownload(soundId: String)

    suspend fun updateDownloadProgress(soundId: String, downloadedBytes: Long, totalBytes: Long)

    suspend fun markDownloadComplete(soundId: String, localPath: String)

    suspend fun deleteDownload(soundId: String): Int

    suspend fun deleteOrphanedDownloads():Int
}