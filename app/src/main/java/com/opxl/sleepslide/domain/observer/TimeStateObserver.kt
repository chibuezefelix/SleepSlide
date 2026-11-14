package com.opxl.sleepslide.domain.observer;

import com.opxl.sleepslide.domain.model.Domain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


interface TimerStateObserver {

    val timerState: StateFlow<Domain.TimerState>

    val timerStatus: Flow<Domain.TimerStatus>

    val remainingMs: Flow<Long>

    val isActive: Flow<Boolean>

    val isFading: Flow<Boolean>

    /** One-shot per timer that runs out — see [com.opxl.sleepslide.domain.service.TimerService.finished]. */
    val finished: Flow<Unit>
}
