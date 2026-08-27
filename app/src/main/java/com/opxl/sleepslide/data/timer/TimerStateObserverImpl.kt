package com.opxl.sleepslide.data.timer


import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.observer.TimerStateObserver
import com.opxl.sleepslide.domain.service.TimerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerStateObserverImpl @Inject constructor(
    private val timerService: TimerService,
) : TimerStateObserver {

    override val timerState: StateFlow<Domain.TimerState> = timerService.timerState

    override val timerStatus: Flow<Domain.TimerStatus> =
        timerService.timerState.map { it.status }.distinctUntilChanged()

    override val remainingMs: Flow<Long> =
        timerService.timerState.map { it.remainingMs }.distinctUntilChanged()

    override val isActive: Flow<Boolean> =
        timerService.timerState.map {
            it.status == Domain.TimerStatus.RUNNING || it.status == Domain.TimerStatus.FADING
        }.distinctUntilChanged()

    override val isFading: Flow<Boolean> =
        timerService.timerState.map { it.status == Domain.TimerStatus.FADING }.distinctUntilChanged()
}
