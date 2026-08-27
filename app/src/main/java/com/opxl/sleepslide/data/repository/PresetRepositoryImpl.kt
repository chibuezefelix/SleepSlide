package com.opxl.sleepslide.data.repository

import com.opxl.sleepslide.data.local.Mapper.toDomain
import com.opxl.sleepslide.data.local.Mapper.toEntity
import com.opxl.sleepslide.data.local.Mapper.toLayerEntities
import com.opxl.sleepslide.data.local.PresetDao
import com.opxl.sleepslide.data.local.SoundDao
import com.opxl.sleepslide.di.IoDispatcher
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.repository.PresetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PresetRepositoryImpl @Inject constructor(
    private val presetDao: PresetDao,
    private val soundDao: SoundDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PresetRepository {

    override fun observeAll(): Flow<List<Domain.Preset>> =
        presetDao.observeAll().map { list ->
            val soundMap = soundDao.getAll().associate { it.id to it.toDomain() }
            list.map { it.toDomain(soundMap) }
        }

    override fun observePinned(): Flow<List<Domain.Preset>> =
        presetDao.observePinned().map { list ->
            val soundMap = soundDao.getAll().associate { it.id to it.toDomain() }
            list.map { it.toDomain(soundMap) }
        }

    override fun observeRecentlyUsed(limit: Int): Flow<List<Domain.Preset>> =
        presetDao.observeRecentlyUsed(limit).map { list ->
            val soundMap = soundDao.getAll().associate { it.id to it.toDomain() }
            list.map { it.toDomain(soundMap) }
        }

    override fun observeById(id: Long): Flow<Domain.Preset?> =
        presetDao.observeById(id).map { entity ->
            entity?.let {
                val soundMap = soundDao.getAll().associate { s -> s.id to s.toDomain() }
                it.toDomain(soundMap)
            }
        }

    override fun observeCount(): Flow<Int> = presetDao.observeCount()

    override suspend fun getById(id: Long): Domain.Preset? = withContext(io) {
        presetDao.getById(id)?.let { entity ->
            val soundMap = soundDao.getAll().associate { it.id to it.toDomain() }
            entity.toDomain(soundMap)
        }
    }

    override suspend fun getLastUsed(): Domain.Preset? = withContext(io) {
        presetDao.getLastUsed()?.let { entity ->
            val soundMap = soundDao.getAll().associate { it.id to it.toDomain() }
            entity.toDomain(soundMap)
        }
    }

    override suspend fun getCount(): Int = withContext(io) {
        presetDao.getCount()
    }

    override suspend fun save(preset: Domain.Preset): Long = withContext(io) {
        presetDao.upsertFull(preset.toEntity(), preset.toLayerEntities())
    }

    override suspend fun update(preset: Domain.Preset) = withContext(io) {
        presetDao.upsertFull(preset.toEntity(), preset.toLayerEntities())
    }

    override suspend fun updateMix(presetId: Long, mix: Domain.SoundMix) = withContext(io) {
        val existing = presetDao.getById(presetId) ?: return@withContext
        val soundMap = soundDao.getAll().associate { it.id to it.toDomain() }
        val updated = existing.toDomain(soundMap).copy(mix = mix)
        presetDao.upsertFull(updated.toEntity(), updated.toLayerEntities())
    }

    override suspend fun rename(presetId: Long, name: String) = withContext(io) {
        presetDao.updateName(presetId, name)
    }

    override suspend fun setEmoji(presetId: Long, emoji: String?) = withContext(io) {
        presetDao.updateEmoji(presetId, emoji)
    }

    override suspend fun pin(presetId: Long) = withContext(io) {
        presetDao.updatePinned(presetId, true)
    }

    override suspend fun unpin(presetId: Long) = withContext(io) {
        presetDao.updatePinned(presetId, false)
    }

    override suspend fun recordUsed(presetId: Long) = withContext(io) {
        presetDao.updateLastUsed(presetId, System.currentTimeMillis())
    }

    override suspend fun reorder(orderedIds: List<Long>) = withContext(io) {
        presetDao.reorder(orderedIds)
    }

    override suspend fun delete(presetId: Long) = withContext(io) {
        presetDao.deleteById(presetId)
    }

    override suspend fun deleteAll() = withContext(io) {
        presetDao.deleteAll()
    }
}