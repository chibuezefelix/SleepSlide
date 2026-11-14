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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// Singleton on the class too: SleepSlideApp injects the concrete type, and the
// playback clock below must be one instance app-wide.
@Singleton
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
        // Flush first: PlayerViewModel closes the row *before* it stops the service,
        // so the clock is still running and the final partial tick would be lost.
        clockMutex.withLock { flushLocked(now) }
        playHistoryDao.closeSession(sessionId, now, reason.name)
    }

    override suspend fun closeAnyActiveSessions(reason: Domain.StopReason) = withContext(io) {
        playHistoryDao.closeAllActiveSessions(reason.name)
    }

    // ── Playback clock ───────────────────────────────────────────────────────

    private val clockMutex = Mutex()
    private var playingSince: Long? = null

    override suspend fun setPlaying(isPlaying: Boolean) = withContext(io) {
        val now = System.currentTimeMillis()
        clockMutex.withLock {
            if (isPlaying) {
                if (playingSince == null) playingSince = now
            } else {
                flushLocked(now)
                playingSince = null
            }
        }
    }

    override suspend fun flushPlayedTime() = withContext(io) {
        val now = System.currentTimeMillis()
        clockMutex.withLock { flushLocked(now) }
    }

    /** Caller must hold [clockMutex]. No-op when the clock is not running. */
    private suspend fun flushLocked(now: Long) {
        val since = playingSince ?: return
        val delta = now - since
        if (delta > 0L) playHistoryDao.addPlayedTimeToActiveSessions(delta)
        playingSince = now
    }

    override suspend fun pruneOlderThan(epochMs: Long) = withContext(io) {
        playHistoryDao.deleteOlderThan(epochMs)
    }

    override suspend fun clearAll() = withContext(io) {
        playHistoryDao.deleteAll()
    }
}