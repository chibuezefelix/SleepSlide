package com.opxl.sleepslide.domain.repository

import com.opxl.sleepslide.domain.model.Domain
import kotlinx.coroutines.flow.Flow

interface PlayHistoryRepository {

    fun observeRecent(limit: Int = 20): Flow<List<Domain.PlaySession>>

    fun observeByPreset(presetId: Long): Flow<List<Domain.PlaySession>>

        fun observeTotalPlayedMs(): Flow<Long>

    fun observeSessionCount(): Flow<Int>

    suspend fun getLatestSession(): Domain.PlaySession?
    suspend fun getActiveSession(): Domain.PlaySession?
    suspend fun getTotalPlayedMsForPreset(presetId: Long): Long
    suspend fun openSession(mix: Domain.SoundMix, presetId: Long?): Long
    suspend fun closeSession(sessionId: Long, reason: Domain.StopReason)
    suspend fun closeAnyActiveSessions(reason: Domain.StopReason)

    // ── Playback clock ───────────────────────────────────────────────────────
    // Drives durationPlayedMs for the open session. Only audible time counts —
    // pauses stop the clock. Driven by an app-scoped observer of the audio
    // service so it keeps running after the player UI is gone.

    /** Start (true) or stop (false) the clock; stopping flushes the pending time. */
    suspend fun setPlaying(isPlaying: Boolean)

    /** Persist time accrued since the last flush and restart the clock. */
    suspend fun flushPlayedTime()
    suspend fun pruneOlderThan(epochMs: Long):Int
    suspend fun clearAll():Int
}
