package com.opxl.sleepslide.data.repository

import com.opxl.sleepslide.data.local.Mapper.toDomain
import com.opxl.sleepslide.data.local.Mapper.toEntity
import com.opxl.sleepslide.data.local.SoundDao
import com.opxl.sleepslide.di.IoDispatcher
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.repository.SoundRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SoundRepositoryImpl @Inject constructor(
    private val soundDao: SoundDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SoundRepository {

    override fun observeAll(): Flow<List<Domain.Sound>> =
        soundDao.observeAll().map { it.map { e -> e.toDomain() } }

    override fun observeByCategory(category: Domain.SoundCategory): Flow<List<Domain.Sound>> =
        soundDao.observeByCategory(category.name).map { it.map { e -> e.toDomain() } }

    override fun observeFree(): Flow<List<Domain.Sound>> =
        soundDao.observeFree().map { it.map { e -> e.toDomain() } }

    override fun observePremium(): Flow<List<Domain.Sound>> =
        soundDao.observePremium().map { it.map { e -> e.toDomain() } }

    override fun observeRecentlyPlayed(limit: Int): Flow<List<Domain.Sound>> =
        soundDao.observeRecentlyPlayed(limit).map { it.map { e -> e.toDomain() } }

    override fun observeMostPlayed(limit: Int): Flow<List<Domain.Sound>> =
        soundDao.observeMostPlayed(limit).map { it.map { e -> e.toDomain() } }

    override fun observeDownloaded(): Flow<List<Domain.Sound>> =
        soundDao.observeDownloaded().map { it.map { e -> e.toDomain() } }

    override suspend fun getById(id: String): Domain.Sound? = withContext(io) {
        soundDao.getById(id)?.toDomain()
    }

    override suspend fun getByIds(ids: List<String>): List<Domain.Sound> = withContext(io) {
        soundDao.getByIds(ids).map { it.toDomain() }
    }

    override suspend fun getAll(): List<Domain.Sound> = withContext(io) {
        soundDao.getAll().map { it.toDomain() }
    }

    override suspend fun seedBundledSounds() = withContext(io) {
        val existing = soundDao.getAll()
        if (existing.isNotEmpty()) return@withContext
        soundDao.insertAll(BundledSoundCatalogue.all.map { it.toEntity() })
    }

    override suspend fun recordPlayed(soundId: String) = withContext(io) {
        soundDao.incrementPlayCount(soundId, System.currentTimeMillis())
    }

    override suspend fun beginDownload(soundId: String) = withContext(io) {
        soundDao.updateDownloadProgress(soundId, isDownloading = true, downloadedBytes = 0L, totalBytes = 0L)
    }

    override suspend fun updateDownloadProgress(
        soundId: String,
        downloadedBytes: Long,
        totalBytes: Long,
    ) = withContext(io) {
        soundDao.updateDownloadProgress(soundId, isDownloading = true, downloadedBytes, totalBytes)
    }

    override suspend fun markDownloadComplete(soundId: String, localPath: String) = withContext(io) {
        soundDao.markDownloadComplete(soundId, localPath)
    }

    override suspend fun deleteDownload(soundId: String) = withContext(io) {
        soundDao.clearDownload(soundId)
    }

    override suspend fun deleteOrphanedDownloads() = withContext(io) {
        soundDao.deleteOrphaned()
    }
}