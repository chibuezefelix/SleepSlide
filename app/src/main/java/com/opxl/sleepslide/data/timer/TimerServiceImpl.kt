package com.opxl.sleepslide.data.timer


import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.opxl.sleepslide.di.ApplicationScope
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.model.Domain.TimerState
import com.opxl.sleepslide.domain.service.TimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerServiceImpl @Inject constructor(
    private val workManager: WorkManager,
    @ApplicationScope private val scope: CoroutineScope,
) : TimerService {

    companion object {
        private const val TICK_INTERVAL_MS = 1_000L
    }

    private val _timerState = MutableStateFlow(idleState())
    override val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var tickJob: Job? = null

    override suspend fun start(durationMs: Long, fadeBeforeEndMs: Long) {
        cancel()

        val startedAt = System.currentTimeMillis()
        _timerState.value = TimerState(
            status = Domain.TimerStatus.RUNNING,
            durationMs = durationMs,
            remainingMs = durationMs,
            fadeBeforeEndMs = fadeBeforeEndMs,
            startedAt = startedAt,
        )

        scheduleFadeWorker(durationMs, fadeBeforeEndMs)
        startTicking()
    }

    override suspend fun cancel() {
        tickJob?.cancel()
        tickJob = null
        workManager.cancelUniqueWork(FadeWorker.WORK_NAME)
        _timerState.value = idleState()
    }

    override suspend fun addTime(extraMs: Long) {
        val current = _timerState.value
        if (current.status == Domain.TimerStatus.IDLE || current.status == Domain.TimerStatus.FINISHED) return

        val newRemaining = current.remainingMs + extraMs

        val newStartTime =System.currentTimeMillis() - (current.durationMs - newRemaining)
        _timerState.update { it.copy(remainingMs = newRemaining, startedAt =  newStartTime) }


        _timerState.update { it.copy(remainingMs = newRemaining, durationMs = current.durationMs+extraMs, startedAt = newStartTime) }

        workManager.cancelUniqueWork(FadeWorker.WORK_NAME)
        scheduleFadeWorker(newRemaining, current.fadeBeforeEndMs)
    }

    override suspend fun reduceTime(reduceMs: Long) {
        val current = _timerState.value
        if (current.status == Domain.TimerStatus.IDLE || current.status == Domain.TimerStatus.FINISHED) return

        val newRemaining = (current.remainingMs - reduceMs).coerceAtLeast(0L)

        if(newRemaining <= 0L){
            finish()
            return
        }
        val newStartTime =System.currentTimeMillis() - (current.durationMs - newRemaining)
        _timerState.update { it.copy(remainingMs = newRemaining, startedAt =  newStartTime) }


        workManager.cancelUniqueWork(FadeWorker.WORK_NAME)
        scheduleFadeWorker(newRemaining, current.fadeBeforeEndMs)
    }

    // Internal

    private fun startTicking() {
        tickJob = scope.launch {
            while (true) {
                delay(TICK_INTERVAL_MS)
                val state = _timerState.value
                if (state.status == Domain.TimerStatus.IDLE || state.status == Domain.TimerStatus.FINISHED) break

                val elapsed     = System.currentTimeMillis() - (state.startedAt ?: break)
                val remaining   = (state.durationMs - elapsed).coerceAtLeast(0L)
                val isFading    = remaining <= state.fadeBeforeEndMs
                val newStatus   = when {
                    remaining <= 0L -> Domain.TimerStatus.FINISHED
                    isFading        -> Domain.TimerStatus.FADING
                    else            -> Domain.TimerStatus.RUNNING
                }

                _timerState.update { it.copy(remainingMs = remaining, status = newStatus) }

                if (newStatus == Domain.TimerStatus.FINISHED) {
                    finish()
                    break
                }
            }
        }
    }

    private fun scheduleFadeWorker(delayFromNowMs: Long, fadeBeforeEndMs: Long) {
        val workerDelay = (delayFromNowMs - fadeBeforeEndMs).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<FadeWorker>()
            .setInitialDelay(workerDelay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(FadeWorker.KEY_FADE_DURATION_MS to fadeBeforeEndMs))
            .build()

        workManager.enqueueUniqueWork(
            FadeWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun finish() {
        tickJob?.cancel()
        tickJob = null
        _timerState.update { it.copy(status = Domain.TimerStatus.FINISHED, remainingMs = 0L) }
    }

    private fun idleState() = TimerState(
        status      = Domain.TimerStatus.IDLE,
        durationMs  = 0L,
        remainingMs = 0L,
    )
}