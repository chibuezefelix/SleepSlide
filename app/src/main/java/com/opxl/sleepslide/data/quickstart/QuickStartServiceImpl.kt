package com.opxl.sleepslide.data.quickstart

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.opxl.sleepslide.data.AudioServiceHolder
import com.opxl.sleepslide.data.audio.AudioServiceImpl
import com.opxl.sleepslide.data.repository.MixSerializer
import com.opxl.sleepslide.di.MainDispatcher
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.repository.PresetRepository
import com.opxl.sleepslide.domain.repository.UserPreferencesRepository
import com.opxl.sleepslide.domain.service.AudioService
import com.opxl.sleepslide.domain.service.PlaybackGate
import com.opxl.sleepslide.domain.service.QuickStartService
import com.opxl.sleepslide.domain.service.QuickStartService.Outcome
import com.opxl.sleepslide.domain.service.ResumePicker
import com.opxl.sleepslide.domain.service.TimerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "QuickStart"
private const val SERVICE_WAIT_MS = 3_000L

@Singleton
class QuickStartServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val presetRepository: PresetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val mixSerializer: MixSerializer,
    private val playbackGate: PlaybackGate,
    private val audioServiceHolder: AudioServiceHolder,
    private val timerService: TimerService,
    @MainDispatcher private val main: CoroutineDispatcher,
) : QuickStartService {

    // ExoPlayer (behind AudioService) may only be touched on the main thread. Callers such as
    // the notification receiver run on a background scope, so hop here rather than trust them.
    override suspend fun start(options: QuickStartService.Options): Outcome =
        withContext(main) { startOnMain(options) }

    private suspend fun startOnMain(options: QuickStartService.Options): Outcome {
        val prefs = userPreferencesRepository.get()

        // Never restart a running night. Only add the timer if there is none.
        val running = audioServiceHolder.current
        if (running != null && running.audioState.value.playbackStatus.isSounding()) {
            return Outcome.AlreadyPlaying(timerStarted = startTimerIfIdle(options, prefs))
        }

        // Decide what to play BEFORE touching the service. Starting it in the foreground puts up
        // a "Playing" media notification, which must never appear when there is nothing to play.
        val presets = presetRepository.observeAll().firstOrNull().orEmpty()
        val now = System.currentTimeMillis()
        val pick = if (options.presetId != null) {
            presets.find { it.id == options.presetId }
                ?.let { ResumePicker.Pick.FromPreset(it, now) }
        } else {
            ResumePicker.pick(
                prefs          = prefs,
                presets        = presets,
                now            = now,
                maxAgeMs       = options.maxAgeMs,
                deserializeMix = { mixSerializer.deserialize(it) },
            )
        } ?: return Outcome.NothingToPlay

        val stored = when (pick) {
            is ResumePicker.Pick.FromPreset -> pick.preset.mix
            is ResumePicker.Pick.FromMix    -> pick.mix
        }
        val mix = when (val decision = playbackGate.prepare(stored)) {
            is PlaybackGate.Decision.Play          -> decision.mix
            is PlaybackGate.Decision.AllLocked     -> return Outcome.NeedsUpgrade
            is PlaybackGate.Decision.NothingToPlay -> return Outcome.NothingToPlay
        }

        val startedByUs = audioServiceHolder.current == null
        val service = awaitAudioService()
        if (service == null) {
            if (startedByUs) stopServiceWeStarted()
            return Outcome.Failed("Audio service did not start")
        }
        // A service we just started is not yet foreground; Android 15 denies it audio focus until it
        // is. Playing too early failed on a cold start from the tile.
        if (startedByUs && !awaitForeground()) {
            Log.w(TAG, "Audio service never became foreground")
            return Outcome.Failed("Audio service did not start")
        }

        val ok = runCatching { service.play(mix, proceedWithoutAudioFocus = true) }
            .onFailure { Log.e(TAG, "play() threw", it) }
            .isSuccess
        if (!ok || service.audioState.value.playbackStatus == Domain.PlaybackStatus.ERROR) {
            if (startedByUs) stopServiceWeStarted()
            return Outcome.Failed("Couldn't start sound")
        }

        // Bookkeeping exactly as Home does, so its resume card and history stay correct.
        val presetName: String?
        when (pick) {
            is ResumePicker.Pick.FromPreset -> {
                presetRepository.recordUsed(pick.preset.id)
                userPreferencesRepository.setLastPlayedPreset(pick.preset.id)
                presetName = pick.preset.name
            }
            is ResumePicker.Pick.FromMix -> {
                // The full mix, locked layers included: they come back once premium is unlocked.
                userPreferencesRepository.setLastPlayedEphemeralMix(mixSerializer.serialize(pick.mix))
                presetName = null
            }
        }

        val timerMs = if (startTimerIfIdle(options, prefs)) prefs.lastTimerDurationMs else null
        return Outcome.Started(presetName, timerMs)
    }

    override suspend fun stop() {
        withContext(main) {
            runCatching {
                audioServiceHolder.current?.stop()
                timerService.cancel()
            }.onFailure { Log.e(TAG, "stop() failed", it) }
        }
    }

    private suspend fun awaitForeground(): Boolean =
        withTimeoutOrNull(SERVICE_WAIT_MS) { audioServiceHolder.foreground.first { it } } != null

    /**
     * Takes down a service this class started when it then could not play, so no idle "Playing"
     * notice lingers. Only once it is foreground: stopping a service that was started with
     * startForegroundService() before it calls startForeground() crashes the app.
     */
    private suspend fun stopServiceWeStarted() {
        if (!audioServiceHolder.foreground.value && !awaitForeground()) return
        runCatching { context.stopService(Intent(context, AudioServiceImpl::class.java)) }
    }

    /** The service when it is up; otherwise start it as a foreground service and wait briefly. */
    private suspend fun awaitAudioService(): AudioService? {
        audioServiceHolder.current?.let { return it }
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, AudioServiceImpl::class.java))
        }.onFailure {
            // Android 12+ refuses background foreground-service starts outside the allowed cases.
            Log.w(TAG, "Could not start audio service", it)
            return null
        }
        return withTimeoutOrNull(SERVICE_WAIT_MS) {
            audioServiceHolder.service.filterNotNull().first()
        }
    }

    private suspend fun startTimerIfIdle(
        options: QuickStartService.Options,
        prefs: Domain.UserPreferences,
    ): Boolean {
        if (!options.applyTimer || !prefs.isQuickStartTimerEnabled) return false
        val status = timerService.timerState.value.status
        if (status == Domain.TimerStatus.RUNNING || status == Domain.TimerStatus.FADING) return false
        timerService.start(prefs.lastTimerDurationMs, prefs.defaultFadeOutMs)
        return true
    }

    private fun Domain.PlaybackStatus.isSounding() =
        this == Domain.PlaybackStatus.PLAYING ||
            this == Domain.PlaybackStatus.FADING_IN ||
            this == Domain.PlaybackStatus.LOADING
}
