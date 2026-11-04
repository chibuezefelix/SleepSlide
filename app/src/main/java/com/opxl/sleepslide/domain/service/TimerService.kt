package com.opxl.sleepslide.domain.service

import com.opxl.sleepslide.domain.model.Domain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TimerService {

    val timerState: StateFlow<Domain.TimerState>

    /** One-shot per timer that runs out. Use this, not the FINISHED status, to react once. */
    val finished: Flow<Unit>

    suspend fun start(durationMs: Long, fadeBeforeEndMs: Long = 60_000L)

    suspend fun cancel()

    suspend fun addTime(extraMs: Long)

    suspend fun reduceTime(reduceMs: Long)
}