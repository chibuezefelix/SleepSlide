package com.opxl.sleepslide.data.repository

import com.opxl.sleepslide.data.local.Local
import com.opxl.sleepslide.data.local.Mapper.toDomain
import com.opxl.sleepslide.data.local.PlayHistoryDao
import com.opxl.sleepslide.di.IoDispatcher
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.repository.PlayHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PlayHistoryRepositoryImpl @Inject constructor(
    private val playHistoryDao: PlayHistoryDao,
    private val mixSerializer: MixSerializer,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PlayHistoryRepository {

    override fun observeRecent(limit: Int): Flow<List<Domain.PlaySession>> =
        playHistoryDao.observeRecent(limit).map { it.map { e -> e.toDomain() } }

    override fun observeByPreset(presetId: Long): Flow<List<Domain.PlaySession>> =
        playHistoryDao.observeByPreset(presetId).map { it.map { e -> e.toDomain() } }

    override fun observeTotalPlayedMs(): Flow<Long> =
        playHistoryDao.observeTotalPlayedMs().map { it ?: 0L }

    override fun observeSessionCount(): Flow<Int> =
        playHistoryDao.observeSessionCount()

    override suspend fun getLatestSession(): Domain.PlaySession? = withContext(io) {
        playHistoryDao.getLatest()?.toDomain()
    }

    override suspend fun getActiveSession(): Domain.PlaySession? = withContext(io) {
        playHistoryDao.getActiveSession()?.toDomain()
    }

    override suspend fun getTotalPlayedMsForPreset(presetId: Long): Long = withContext(io) {
        playHistoryDao.getTotalPlayedMsForPreset(presetId) ?: 0L
    }

    override suspend fun openSession(mix: Domain.SoundMix, presetId: Long?): Long = withContext(io) {
        val entity = Local.PlayHistoryEntity(
            presetId = presetId,
            mixSnapshot = mixSerializer.serialize(mix),
            startedAt = System.currentTimeMillis(),
            endedAt = null,
            durationPlayedMs = 0L,
            stoppedBy = Local.StopReason.USER,

        )
        playHistoryDao.insert(entity)
    }

    override suspend fun closeSession(sessionId: Long, reason: Domain.StopReason) = withContext(io) {
        val now = System.currentTimeMillis()
        val session = playHistoryDao.getById(sessionId) ?: return@withContext
        val duration = now - session.startedAt
        playHistoryDao.closeSession(sessionId, now, duration, reason.name)
    }
    override suspend fun closeAnyActiveSessions(reason: Domain.StopReason) = withContext(io) {
        val now = System.currentTimeMillis()
        val active = playHistoryDao.getActiveSession()
        val duration = active?.let { now - it.startedAt } ?: 0L
        playHistoryDao.closeAllActiveSessions(now, duration, reason.name)
    }

    override suspend fun pruneOlderThan(epochMs: Long) = withContext(io) {
        playHistoryDao.deleteOlderThan(epochMs)
    }

    override suspend fun clearAll() = withContext(io) {
        playHistoryDao.deleteAll()
    }
}