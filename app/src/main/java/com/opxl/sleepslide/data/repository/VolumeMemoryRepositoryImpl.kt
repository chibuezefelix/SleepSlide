package com.opxl.sleepslide.data.repository

import com.opxl.sleepslide.data.local.Local
import com.opxl.sleepslide.data.local.SoundVolumeMemoryDao
import com.opxl.sleepslide.di.IoDispatcher
import com.opxl.sleepslide.domain.repository.VolumeMemoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VolumeMemoryRepositoryImpl @Inject constructor(
    private val volumeMemoryDao: SoundVolumeMemoryDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : VolumeMemoryRepository {

    companion object {
        private const val DEFAULT_VOLUME = 1.0f
    }

    override fun observeVolume(soundId: String): Flow<Float> =
        volumeMemoryDao.observeForSound(soundId).map { it?.volume ?: DEFAULT_VOLUME }

    override suspend fun getVolume(soundId: String): Float = withContext(io) {
        volumeMemoryDao.getForSound(soundId)?.volume ?: DEFAULT_VOLUME
    }

    override suspend fun getVolumesForSounds(soundIds: List<String>): Map<String, Float> =
        withContext(io) {
            val stored = volumeMemoryDao.getAllForSounds(soundIds).associate { it.soundId to it.volume }
            soundIds.associate { id -> id to (stored[id] ?: DEFAULT_VOLUME) }
        }

    override suspend fun saveVolume(soundId: String, volume: Float) = withContext(io) {
        volumeMemoryDao.upsert(
            Local.SoundVolumeMemoryEntity(
                soundId = soundId,
                volume = volume,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun saveVolumes(volumes: Map<String, Float>) = withContext(io) {
        val timestamp = System.currentTimeMillis()
        volumeMemoryDao.upsertAll(
            volumes.map { (soundId, volume) ->
                Local.SoundVolumeMemoryEntity(
                    soundId = soundId,
                    volume = volume,
                    updatedAt = timestamp
                )
            }
        )
    }

    override suspend fun clearVolume(soundId: String) = withContext(io) {
        volumeMemoryDao.deleteForSound(soundId)
    }

    override suspend fun clearAll() = withContext(io) {
        volumeMemoryDao.deleteAll()
    }
}