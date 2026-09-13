package com.opxl.sleepslide.data.observer

import android.util.Log
import com.opxl.sleepslide.di.ApplicationScope
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.observer.AudioStateObserver
import com.opxl.sleepslide.domain.repository.PlayHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the play-history clock ([PlayHistoryRepository.setPlaying] /
 * [PlayHistoryRepository.flushPlayedTime]) in step with the audio service.
 *
 * Lives on the application scope rather than in a ViewModel because in a sleep app
 * playback usually outlives every screen: the user locks the phone and the timer
 * fires hours later. The foreground service keeps emitting state the whole time,
 * so the running durationPlayedMs stays honest even if the process is later killed
 * and the session has to be closed as stale on the next cold start.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class PlaySessionTracker @Inject constructor(
    private val audioStateObserver: AudioStateObserver,
    private val playHistoryRepository: PlayHistoryRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    companion object {
        private const val TAG = "PlaySessionTracker"
        /** Bound on time lost to process death; one tiny UPDATE per tick while playing. */
        private const val TICK_MS = 60_000L
    }

    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = audioStateObserver.playbackStatus
            .map { it.isAudible() }
            .distinctUntilChanged()
            .onEach { audible -> safely { playHistoryRepository.setPlaying(audible) } }
            .flatMapLatest { audible -> if (audible) ticker() else emptyFlow() }
            .onEach { safely { playHistoryRepository.flushPlayedTime() } }
            .catch { e -> Log.w(TAG, "Play session clock stopped", e) }
            .launchIn(scope)
    }

    // Fades are audible — the user is hearing sound during them.
    private fun Domain.PlaybackStatus.isAudible() = when (this) {
        Domain.PlaybackStatus.PLAYING,
        Domain.PlaybackStatus.FADING_IN,
        Domain.PlaybackStatus.FADING_OUT -> true
        else -> false
    }

    private fun ticker() = flow {
        while (true) {
            delay(TICK_MS)
            emit(Unit)
        }
    }

    /** History is best-effort: log DB failures but never swallow cancellation. */
    private suspend inline fun safely(block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Play session clock write failed", e)
        }
    }
}
