package com.opxl.sleepslide.data.timer

import android.util.Log
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.service.AudioService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Applies the sleep timer to playback. Runs inside the audio service, so it works
 * whether or not any screen is alive — the service is running whenever there is
 * sound to stop.
 *
 * - FADING: fade out over what is left of the timer
 * - FADING → RUNNING (time added during the fade): cancel the fade, restore volume
 * - finished: stop playback, then disarm the backstop. A failed stop leaves the
 *   backstop armed so it can try again.
 */
class SleepTimerController(
    private val timerService: TimerServiceImpl,
    private val audio: AudioService,
    private val scope: CoroutineScope,
) {
    companion object {
        private const val TAG = "SleepTimerController"
    }

    private var fadingForTimer = false

    fun start() {
        timerService.timerState
            .map { it.status }
            .distinctUntilChanged()
            .onEach(::onStatus)
            .launchIn(scope)

        timerService.finished
            .onEach { stopForTimer() }
            .launchIn(scope)
    }

    private fun onStatus(status: Domain.TimerStatus) {
        when (status) {
            Domain.TimerStatus.FADING -> {
                if (fadingForTimer || !audio.audioState.value.playbackStatus.isAudible()) return
                fadingForTimer = true
                val remaining = timerService.timerState.value.remainingMs
                scope.launch { audio.fadeOut(remaining) }
            }
            Domain.TimerStatus.RUNNING -> {
                if (!fadingForTimer) return
                fadingForTimer = false
                scope.launch { audio.cancelFadeOut() }
            }
            else -> fadingForTimer = false
        }
    }

    private suspend fun stopForTimer() {
        fadingForTimer = false
        try {
            audio.stop()
            timerService.acknowledgeFinished()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Timer stop failed — leaving the backstop armed", e)
        }
    }

    private fun Domain.PlaybackStatus.isAudible() =
        this == Domain.PlaybackStatus.PLAYING || this == Domain.PlaybackStatus.FADING_IN
}
