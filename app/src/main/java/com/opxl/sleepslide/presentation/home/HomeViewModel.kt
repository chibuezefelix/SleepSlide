package com.opxl.sleepslide.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opxl.sleepslide.data.AudioServiceHolder
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.data.repository.MixSerializer
import com.opxl.sleepslide.domain.observer.AudioStateObserver
import com.opxl.sleepslide.domain.observer.EntitlementObserver
import com.opxl.sleepslide.domain.observer.TimerStateObserver
import com.opxl.sleepslide.domain.repository.PlayHistoryRepository
import com.opxl.sleepslide.domain.repository.PresetRepository
import com.opxl.sleepslide.domain.repository.SoundRepository
import com.opxl.sleepslide.domain.repository.UserPreferencesRepository
import com.opxl.sleepslide.domain.service.PlaybackGate
import com.opxl.sleepslide.domain.service.QuickStartService
import com.opxl.sleepslide.domain.service.ResumePicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

private const val FREE_PRESET_LIMIT        = 10
private const val RECENT_PRESETS_LIMIT     = 5
private const val RECENT_SOUNDS_LIMIT      = 8
private const val RESUME_STALENESS_MS      = 24 * 60 * 60 * 1000L   // 24 hours
private const val SHARING_STOP_TIMEOUT_MS  = 5_000L

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val presetRepository: PresetRepository,
    private val playHistoryRepository: PlayHistoryRepository,
    private val soundRepository: SoundRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val audioServiceHolder: AudioServiceHolder,
    private val audioStateObserver: AudioStateObserver,
    private val timerStateObserver: TimerStateObserver,
    private val entitlementObserver: EntitlementObserver,
    private val mixSerializer: MixSerializer,
    private val playbackGate: PlaybackGate,
    private val quickStartService: QuickStartService,
) : ViewModel() {


    private val _events = Channel<HomeViewState.HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeViewState.HomeEvent> = _events.receiveAsFlow()


    private val _isRefreshing = MutableStateFlow(false)


    val uiState: StateFlow<HomeViewState.HomeUiState> = buildUiStateFlow()
        .catch { e ->
            emit(
                HomeViewState.HomeUiState(
                    screen = HomeViewState.ScreenState.Error(
                        e.message ?: "Something went wrong loading your sounds"
                    )
                )
            )
        }
        .stateIn(
            scope            = viewModelScope,
            started          = SharingStarted.WhileSubscribed(SHARING_STOP_TIMEOUT_MS),
            initialValue     = HomeViewState.HomeUiState(),
        )


    init {
        observeAudioFocusEvents()
        observeTimerFinished()
        observeServiceAvailability()
    }


    fun resumeLastPlayed() {
        viewModelScope.launch {
            if (audioServiceHolder.current == null) {
                _events.send(HomeViewState.HomeEvent.ShowServiceUnavailableSnackbar)
                return@launch
            }

            // Same code path as the widget, tile and reminder button; Home keeps its 24 h limit
            // and does not start a timer (the user is looking at the app and can pick one).
            val outcome = quickStartService.start(
                QuickStartService.Options(maxAgeMs = RESUME_STALENESS_MS, applyTimer = false)
            )
            when (outcome) {
                is QuickStartService.Outcome.Started -> {
                    _events.send(HomeViewState.HomeEvent.PlaybackStarted(outcome.presetName))
                    _events.send(HomeViewState.HomeEvent.NavigateToPlayer)
                }
                is QuickStartService.Outcome.AlreadyPlaying ->
                    _events.send(HomeViewState.HomeEvent.NavigateToPlayer)
                // The gate has already told the user which layers are locked.
                is QuickStartService.Outcome.NeedsUpgrade -> Unit
                is QuickStartService.Outcome.NothingToPlay ->
                    _events.send(HomeViewState.HomeEvent.NavigateToLibrary)
                is QuickStartService.Outcome.Failed ->
                    _events.send(HomeViewState.HomeEvent.ShowError(outcome.reason))
            }
        }
    }

    fun launchPreset(preset: Domain.Preset) {
        viewModelScope.launch {
            val service = audioServiceHolder.current
            if (service == null) {
                _events.send(HomeViewState.HomeEvent.ShowServiceUnavailableSnackbar)
                return@launch
            }

            val currentPlayback = uiState.value.playback

            runCatching {
                val mix = gatedMix(preset.mix) ?: return@launch
                if (currentPlayback is HomeViewState.PlaybackUiState.Active &&
                    currentPlayback.status == Domain.PlaybackStatus.PLAYING
                ) {
                    // Already playing — crossfade to the new preset smoothly
                    service.crossfadeTo(mix)
                } else {
                    service.play(mix)
                }
                presetRepository.recordUsed(preset.id)
                userPreferencesRepository.setLastPlayedPreset(preset.id)
                _events.send(HomeViewState.HomeEvent.PlaybackStarted(preset.name))
                _events.send(HomeViewState.HomeEvent.NavigateToPlayer)
            }.onFailure { e ->
                _events.send(HomeViewState.HomeEvent.ShowError(
                    e.message ?: "Could not launch ${preset.name}"
                ))
            }
        }
    }

    /** Free layers of [mix], or null when there is nothing to play (the user has been told). */
    private suspend fun gatedMix(mix: Domain.SoundMix): Domain.SoundMix? =
        when (val decision = playbackGate.prepare(mix)) {
            is PlaybackGate.Decision.Play          -> decision.mix
            is PlaybackGate.Decision.AllLocked     -> null
            is PlaybackGate.Decision.NothingToPlay -> {
                _events.send(HomeViewState.HomeEvent.ShowError("No sounds in this mix"))
                null
            }
        }

    fun pausePlayback() {
        viewModelScope.launch {
            runCatching {
                audioServiceHolder.current?.pause()
            }.onFailure {
                _events.send(HomeViewState.HomeEvent.ShowError("Could not pause playback"))
            }
        }
    }

    fun resumePlayback() {
        viewModelScope.launch {
            val service = audioServiceHolder.current
            if (service == null) {
                _events.send(HomeViewState.HomeEvent.ShowServiceUnavailableSnackbar)
                return@launch
            }
            runCatching {
                service.resume()
            }.onFailure {
                _events.send(HomeViewState.HomeEvent.ShowError("Could not resume playback"))
            }
        }
    }

    fun stopPlayback() {
        viewModelScope.launch {
            runCatching {
                audioServiceHolder.current?.stop()
            }.onFailure {
                _events.send(HomeViewState.HomeEvent.ShowError("Could not stop playback"))
            }
        }
    }

    fun requestNewPreset() {
        viewModelScope.launch {
            val state = uiState.value
            if (state.hasReachedFreePresetLimit) {
                _events.send(HomeViewState.HomeEvent.ShowPresetLimitReached(FREE_PRESET_LIMIT))
                return@launch
            }
            _events.send(HomeViewState.HomeEvent.NavigateToLibrary)
        }
    }

    fun navigateToPreset(presetId: Long) {
        viewModelScope.launch {
            _events.send(HomeViewState.HomeEvent.NavigateToPreset(presetId))
        }
    }

    fun navigateToPlayer() {
        viewModelScope.launch {
            _events.send(HomeViewState.HomeEvent.NavigateToPlayer)
        }
    }

    fun onSoundClicked(sound: Domain.Sound) {
        viewModelScope.launch {
            val service = audioServiceHolder.current
            if (service == null) {
                _events.send(HomeViewState.HomeEvent.ShowServiceUnavailableSnackbar)
                return@launch
            }
            // Navigate to library pre-filtered to this sound's category
            _events.send(HomeViewState.HomeEvent.NavigateToLibrary)
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    private fun buildUiStateFlow(): Flow<HomeViewState.HomeUiState> {
        val pinnedPresetsFlow = presetRepository.observePinned()
            .catch { emit(emptyList()) }
            .distinctUntilChanged()

        val recentPresetsFlow = presetRepository.observeRecentlyUsed(RECENT_PRESETS_LIMIT)
            .catch { emit(emptyList()) }
            .distinctUntilChanged()

       val presetCountFlow = presetRepository.observeCount()
            .catch { emit(0) }
            .distinctUntilChanged()

        val recentSoundsFlow = soundRepository.observeRecentlyPlayed(RECENT_SOUNDS_LIMIT)
            .catch { emit(emptyList()) }
            .distinctUntilChanged()

        val totalListenedFlow = playHistoryRepository.observeTotalPlayedMs()
            .catch { emit(0L) }
            .distinctUntilChanged()

        val preferencesFlow = userPreferencesRepository.observe()
            .catch { emit(Domain.UserPreferences()) }
            .distinctUntilChanged()



        fun Domain.AudioState.toPlaybackUiState():HomeViewState.PlaybackUiState {
            return when {
                playbackStatus == Domain.PlaybackStatus.ERROR ->
                    HomeViewState.  PlaybackUiState.Error

                playbackStatus == Domain.PlaybackStatus.IDLE ->
                    HomeViewState.   PlaybackUiState.Idle

                audioFocusStatus == Domain.AudioFocusStatus.LOST ->
                    HomeViewState .  PlaybackUiState.AudioFocusLost(
                        Domain.AudioFocusLostReason.ANOTHER_APP
                    )

                audioFocusStatus == Domain.AudioFocusStatus.LOST_TRANSIENT ->
                    HomeViewState.  PlaybackUiState.AudioFocusLost(
                        Domain.AudioFocusLostReason.PHONE_CALL
                    )

                else ->
                    HomeViewState. PlaybackUiState.Active(
                        status = playbackStatus,
                        activePresetId = activePresetId,
                        activePresetName = null,
                        activeMix = activeMix,
                        audioFocusStatus = audioFocusStatus,
                        fadeInProgress = fadeInProgress,
                        isNightLockEnabled = isNightLockEnabled,
                        isPlayingInBackground = isPlayingInBackground
                    )
            }
        }

        val playbackFlow: StateFlow<HomeViewState.PlaybackUiState> =
            audioServiceHolder.service
                .flatMapLatest { service ->
                    service?.audioState
                        ?.map { audioState ->
                            audioState.toPlaybackUiState()
                        }
                        ?.catch {
                            emit(HomeViewState.PlaybackUiState.Error)
                        }
                        ?: flowOf(HomeViewState.PlaybackUiState.ServiceUnavailable)
                }
                .distinctUntilChanged()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = HomeViewState.PlaybackUiState.ServiceUnavailable
                )

//        // AudioState flows — flat-map the service holder so we handle null service gracefully
        val serviceFlow = audioServiceHolder.service
//        val playbackFlow: Flow<PlaybackUiState> = serviceFlow.flatMapLatest { service ->
//            service?.audioState?.map { audioState ->
//                when {
//                    audioState.playbackStatus == Domain.PlaybackStatus.ERROR ->
//                        PlaybackUiState.Error
//
//                    audioState.playbackStatus == Domain.PlaybackStatus.IDLE ->
//                        PlaybackUiState.Idle
//
//                    audioState.audioFocusStatus == Domain.AudioFocusStatus.LOST ->
//                        PlaybackUiState.AudioFocusLost(Domain.AudioFocusLostReason.ANOTHER_APP)
//
//                    audioState.audioFocusStatus == Domain.AudioFocusStatus.LOST_TRANSIENT ->
//                        PlaybackUiState.AudioFocusLost(Domain.AudioFocusLostReason.PHONE_CALL)
//
//                    else -> PlaybackUiState.Active(
//                        status                = audioState.playbackStatus,
//                        activePresetId        = audioState.activePresetId,
//                        activePresetName      = null, // enriched below
//                        activeMix             = audioState.activeMix,
//                        audioFocusStatus      = audioState.audioFocusStatus,
//                        fadeInProgress        = audioState.fadeInProgress,
//                        isNightLockEnabled    = audioState.isNightLockEnabled,
//                        isPlayingInBackground = audioState.isPlayingInBackground,
//                    )
//                }
//            }?.catch { emit(PlaybackUiState.Error) }?.distinctUntilChanged()
//                ?: flowOf(PlaybackUiState.ServiceUnavailable)
//        }






        val timerFlow: Flow<HomeViewState.TimerUiState> = timerStateObserver.timerState
            .map { timerState ->
                when (timerState.status) {
                    Domain.TimerStatus.IDLE     -> HomeViewState.TimerUiState.Idle
                    Domain.TimerStatus.FINISHED -> HomeViewState.TimerUiState.Finished
                    Domain.TimerStatus.RUNNING,
                    Domain.TimerStatus.FADING   -> HomeViewState.TimerUiState.Active(
                        status          = timerState.status,
                        remainingMs     = timerState.remainingMs,
                        remainingLabel  = timerState.remainingMs.toTimerLabel(),
                        totalDurationMs = timerState.durationMs,
                        progressFraction = if (timerState.durationMs > 0L) {
                            1f - (timerState.remainingMs.toFloat() / timerState.durationMs)
                        } else 0f,
                        isFading        = timerState.status == Domain.TimerStatus.FADING,
                    )
                }
            }
            .catch { emit(HomeViewState.TimerUiState.Idle) }
            .distinctUntilChanged()

        val bluetoothFlow: Flow<HomeViewState.BluetoothUiState> = serviceFlow.flatMapLatest { service ->
            if (service == null) {
                flowOf(HomeViewState.BluetoothUiState.Unknown)
            } else {
                service.audioState
                    .map { state ->
                        if (state.isBluetoothConnected) HomeViewState.BluetoothUiState.Connected
                        else HomeViewState.BluetoothUiState.Disconnected
                    }
                    .catch { emit(HomeViewState.BluetoothUiState.Unknown) }
                    .distinctUntilChanged()
            }
        }

        val entitlementFlow = entitlementObserver.tier
            .catch { emit(Domain.EntitlementTier.FREE) }
            .distinctUntilChanged()

        return combine(
            pinnedPresetsFlow,
            recentPresetsFlow,
            recentSoundsFlow,
            totalListenedFlow,
            preferencesFlow,
        ) { pinned, recent, sounds, totalMs, prefs ->
            Quintuple(pinned, recent, sounds, totalMs, prefs)
        }.combine(
            combine(playbackFlow, timerFlow, bluetoothFlow, entitlementFlow, presetCountFlow) {
                    pb, timer, bt, tier, count -> PlaybackContext(pb, timer, bt, tier, count)
            }
        ) { content, ctx ->
            buildState(content, ctx)
        }
    }

    private fun buildState(
        content: Quintuple<
                List<Domain.Preset>,
                List<Domain.Preset>,
                List<Domain.Sound>,
                Long,
                Domain.UserPreferences>,
        ctx: PlaybackContext,
    ): HomeViewState.HomeUiState {
        val (pinned, recent, sounds, totalMs, prefs) = content
        val (playback, timer, bluetooth, tier, presetCount) = ctx

        val resumeCard = resolveResumeCard(prefs, recent)
        val freeLimit  = tier == Domain.EntitlementTier.FREE && presetCount >= FREE_PRESET_LIMIT

        // Enrich active playback with preset name from recent presets
        val enrichedPlayback = if (playback is HomeViewState.PlaybackUiState.Active &&
            playback.activePresetId != null
        ) {
            val presetName = recent.find { it.id == playback.activePresetId }?.name
                ?: pinned.find { it.id == playback.activePresetId }?.name
            playback.copy(activePresetName = presetName)
        } else {
            playback
        }

        val isInitialising = pinned.isEmpty() && recent.isEmpty() &&
                sounds.isEmpty() && playback is HomeViewState.PlaybackUiState.ServiceUnavailable

        val screenState = when {
            isInitialising -> HomeViewState.ScreenState.Loading
            pinned.isEmpty() && recent.isEmpty() && sounds.isEmpty() -> HomeViewState.ScreenState.Empty
            else -> HomeViewState.ScreenState.Ready(
                resumeCard    = resumeCard,
                pinnedPresets = pinned,
                recentPresets = recent.filter { r -> pinned.none { p -> p.id == r.id } },
                recentSounds  = sounds,
                totalListenedMs = totalMs,
            )
        }

        return HomeViewState.HomeUiState(
            screen                  = screenState,
            playback                = enrichedPlayback,
            timer                   = timer,
            greeting                = buildGreeting(),
            bluetooth               = bluetooth,
            entitlementTier         = tier,
            presetCount             = presetCount,
            hasReachedFreePresetLimit = freeLimit,
        )
    }


    private fun resolveResumeCard(
        prefs: Domain.UserPreferences,
        recentPresets: List<Domain.Preset>,
    ): HomeViewState.ResumeCardState? {
        val now = System.currentTimeMillis()
        return when (val pick = ResumePicker.pick(
            prefs          = prefs,
            presets        = recentPresets,
            now            = now,
            maxAgeMs       = RESUME_STALENESS_MS,
            deserializeMix = { mixSerializer.deserialize(it) },
        )) {
            is ResumePicker.Pick.FromPreset ->
                HomeViewState.ResumeCardState.FromPreset(pick.preset, pick.lastUsedAt)
            // No timestamp is stored for unsaved mixes; assume 2 h ago for display.
            is ResumePicker.Pick.FromMix ->
                HomeViewState.ResumeCardState.FromEphemeralMix(pick.mix, now - 2 * 60 * 60 * 1000L)
            null -> null
        }
    }


    private fun buildGreeting(): HomeViewState.GreetingUiState {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeOfDay = when (hour) {
            in 5..11  -> HomeViewState.TimeOfDay.MORNING
            in 12..16 -> HomeViewState.TimeOfDay.AFTERNOON
            in 17..20 -> HomeViewState.TimeOfDay.EVENING
            else      -> HomeViewState.TimeOfDay.NIGHT
        }
        val (headline, subtext) = when (timeOfDay) {
            HomeViewState.TimeOfDay.MORNING   -> "Good morning" to "Start your day with calm"
            HomeViewState.TimeOfDay.AFTERNOON -> "Good afternoon" to "A moment of quiet"
            HomeViewState.TimeOfDay.EVENING   -> "Good evening" to "Wind down with sound"
            HomeViewState.TimeOfDay.NIGHT     -> "Sleep well" to "Let sound carry you"
        }
        return HomeViewState.GreetingUiState(headline, subtext, timeOfDay)
    }


    /**
     * Watches AudioFocus status for transient changes and emits one-shot events.
     * The UI shows a non-intrusive snackbar when a call interrupts — not a modal.
     * On focus regain after a call, a quiet confirmation is sent.
     */
    private fun observeAudioFocusEvents() {
        audioStateObserver.audioFocusStatus
            .distinctUntilChanged()
            .onEach { status ->
                when (status) {
                    Domain.AudioFocusStatus.GAINED -> {
                        // Only send the "restored" event if we were previously interrupted
                        val playback = uiState.value.playback
                        if (playback is HomeViewState.PlaybackUiState.AudioFocusLost &&
                            playback.reason == Domain.AudioFocusLostReason.PHONE_CALL
                        ) {
                            _events.send(HomeViewState.HomeEvent.AudioFocusRestoredAfterCall)
                        }
                    }
                    else -> { /* Handled via uiState playback sub-state */ }
                }
            }
            .catch { /* Focus observation errors are non-fatal */ }
            .launchIn(viewModelScope)
    }

    /**
     * When the timer finishes, the audio service stops the sound itself
     * (SleepTimerController). Emit a one-shot event so the UI can show a gentle
     * "Session ended" message — once per timer, not on every new collector.
     */
    private fun observeTimerFinished() {
        timerStateObserver.finished
            .onEach {
                _events.send(HomeViewState.HomeEvent.ShowError("Your sleep timer has ended"))
            }
            .catch { }
            .launchIn(viewModelScope)
    }

    /**
     * When the service becomes unavailable mid-session (process death, OOM),
     * notify the UI so it can show the correct idle state rather than stale controls.
     */
    private fun observeServiceAvailability() {
        audioServiceHolder.service
            .map { it == null }
            .distinctUntilChanged()
            .onEach { unavailable ->
                if (unavailable) {
                    val playback = uiState.value.playback
                    if (playback is HomeViewState.PlaybackUiState.Active) {
                        _events.send(HomeViewState.HomeEvent.ShowServiceUnavailableSnackbar)
                    }
                }
            }
            .catch { }
            .launchIn(viewModelScope)
    }


    private fun Long.toTimerLabel(): String {
        val totalSeconds = this / 1_000L
        val hours   = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }


    private data class Quintuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

    private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component1() = a
    private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component2() = b
    private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component3() = c
    private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component4() = d
    private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component5() = e

    private data class PlaybackContext(
        val playback: HomeViewState.PlaybackUiState,
        val timer: HomeViewState.TimerUiState,
        val bluetooth: HomeViewState.BluetoothUiState,
        val tier: Domain.EntitlementTier,
        val presetCount: Int,
    )

    private operator fun PlaybackContext.component1() = playback
    private operator fun PlaybackContext.component2() = timer
    private operator fun PlaybackContext.component3() = bluetooth
    private operator fun PlaybackContext.component4() = tier
    private operator fun PlaybackContext.component5() = presetCount
}