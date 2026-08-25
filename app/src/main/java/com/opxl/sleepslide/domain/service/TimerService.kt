package com.opxl.sleepslide.domain.service

import com.opxl.sleepslide.domain.model.Domain
import kotlinx.coroutines.flow.StateFlow

interface TimerService {

    val timerState: StateFlow<Domain.TimerState>

    suspend fun start(durationMs: Long, fadeBeforeEndMs: Long = 60_000L)

    suspend fun cancel()

    suspend fun addTime(extraMs: Long)

    suspend fun reduceTime(reduceMs: Long)
}