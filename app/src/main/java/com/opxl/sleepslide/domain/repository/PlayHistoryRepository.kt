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

    suspend fun pruneOlderThan(epochMs: Long)

    suspend fun clearAll()
}
