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
) : ViewModel() {


    private val _events = Channel<HomeView.HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeView.HomeEvent> = _events.receiveAsFlow()


    private val _isRefreshing = MutableStateFlow(false)


    val uiState: StateFlow<HomeView.HomeUiState> = buildUiStateFlow()
        .catch { e ->
            emit(
                HomeView.HomeUiState(
                    screen = HomeView.ScreenState.Error(
                        e.message ?: "Something went wrong loading your sounds"
                    )
                )
            )
        }
        .stateIn(
            scope            = viewModelScope,
            started          = SharingStarted.WhileSubscribed(SHARING_STOP_TIMEOUT_MS),
            initialValue     = HomeView.HomeUiState(),
        )


    init {
        observeAudioFocusEvents()
        observeTimerFinished()
        observeServiceAvailability()
    }


    fun resumeLastPlayed() {
        viewModelScope.launch {
            val service = audioServiceHolder.current
            if (service == null) {
                _events.send(HomeView.HomeEvent.ShowServiceUnavailableSnackbar)
                return@launch
            }

            val state = uiState.value.screen as? HomeView.ScreenState.Ready ?: return@launch
            val resume = state.resumeCard ?: run {
                _events.send(HomeView.HomeEvent.NavigateToLibrary)
                return@launch
            }

            runCatching {
                when (resume) {
                    is HomeView.ResumeCardState.FromPreset -> {
                        val preset = presetRepository.getById(resume.preset.id)
                            ?: return@launch
                        service.play(preset.mix)
                        presetRepository.recordUsed(preset.id)
                        userPreferencesRepository.setLastPlayedPreset(preset.id)
                        _events.send(HomeView.HomeEvent.PlaybackStarted(preset.name))
                    }
                    is HomeView.ResumeCardState.FromEphemeralMix -> {
                        service.play(resume.mix)
                        userPreferencesRepository.setLastPlayedEphemeralMix(
                            mixSerializer.serialize(resume.mix)
                        )
                        _events.send(HomeView.HomeEvent.PlaybackStarted(null))
                    }
                }
                _events.send(HomeView.HomeEvent.NavigateToPlayer)
            }.onFailure { e ->
                _events.send(HomeView.HomeEvent.ShowError(
                    e.message ?: "Could not resume playback"
                ))
            }
        }
    }

    fun launchPreset(preset: Domain.Preset) {
        viewModelScope.launch {
            val service = audioServiceHolder.current
            if (service == null) {
                _events.send(HomeView.HomeEvent.ShowServiceUnavailableSnackbar)
                return@launch
            }

            val currentPlayback = uiState.value.playback

            runCatching {
                if (currentPlayback is HomeView.PlaybackUiState.Active &&
                    currentPlayback.status == Domain.PlaybackStatus.PLAYING
                ) {
                    // Already playing — crossfade to the new preset smoothly
                    service.crossfadeTo(preset.mix)
                } else {
                    service.play(preset.mix)
                }
                presetRepository.recordUsed(preset.id)
                userPreferencesRepository.setLastPlayedPreset(preset.id)
                _events.send(HomeView.HomeEvent.PlaybackStarted(preset.name))
                _events.send(HomeView.HomeEvent.NavigateToPlayer)
            }.onFailure { e ->
                _events.send(HomeView.HomeEvent.ShowError(
                    e.message ?: "Could not launch ${preset.name}"
                ))
            }
        }
    }

    fun pausePlayback() {
        viewModelScope.launch {
            runCatching {
                audioServiceHolder.current?.pause()
            }.onFailure {
                _events.send(HomeView.HomeEvent.ShowError("Could not pause playback"))
            }
        }
    }

    fun resumePlayback() {
        viewModelScope.launch {
            val service = audioServiceHolder.current
            if (service == null) {
                _events.send(HomeView.HomeEvent.ShowServiceUnavailableSnackbar)
                return@launch
            }
            runCatching {
                service.resume()
            }.onFailure {
                _events.send(HomeView.HomeEvent.ShowError("Could not resume playback"))
            }
        }
    }

    fun stopPlayback() {
        viewModelScope.launch {
            runCatching {
                audioServiceHolder.current?.stop()
            }.onFailure {
                _events.send(HomeView.HomeEvent.ShowError("Could not stop playback"))
            }
        }
    }

    fun requestNewPreset() {
        viewModelScope.launch {
            val state = uiState.value
            if (state.hasReachedFreePresetLimit) {
                _events.send(HomeView.HomeEvent.ShowPresetLimitReached(FREE_PRESET_LIMIT))
                return@launch
            }
            _events.send(HomeView.HomeEvent.NavigateToLibrary)
        }
    }

    fun navigateToPreset(presetId: Long) {
        viewModelScope.launch {
            _events.send(HomeView.HomeEvent.NavigateToPreset(presetId))
        }
    }

    fun navigateToPlayer() {
        viewModelScope.launch {
            _events.send(HomeView.HomeEvent.NavigateToPlayer)
        }
    }

    fun onSoundClicked(sound: Domain.Sound) {
        viewModelScope.launch {
            val service = audioServiceHolder.current
            if (service == null) {
                _events.send(HomeView.HomeEvent.ShowServiceUnavailableSnackbar)
                return@launch
            }
            // Navigate to library pre-filtered to this sound's category
            _events.send(HomeView.HomeEvent.NavigateToLibrary)
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    private fun buildUiStateFlow(): Flow<HomeView.HomeUiState> {
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



        fun Domain.AudioState.toPlaybackUiState():HomeView.PlaybackUiState {
            return when {
                playbackStatus == Domain.PlaybackStatus.ERROR ->
                    HomeView.  PlaybackUiState.Error

                playbackStatus == Domain.PlaybackStatus.IDLE ->
                    HomeView.   PlaybackUiState.Idle

                audioFocusStatus == Domain.AudioFocusStatus.LOST ->
                    HomeView .  PlaybackUiState.AudioFocusLost(
                        Domain.AudioFocusLostReason.ANOTHER_APP
                    )

                audioFocusStatus == Domain.AudioFocusStatus.LOST_TRANSIENT ->
                    HomeView.  PlaybackUiState.AudioFocusLost(
                        Domain.AudioFocusLostReason.PHONE_CALL
                    )

                else ->
                    HomeView. PlaybackUiState.Active(
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

        val playbackFlow: StateFlow<HomeView.PlaybackUiState> =
            audioServiceHolder.service
                .flatMapLatest { service ->
                    service?.audioState
                        ?.map { audioState ->
                            audioState.toPlaybackUiState()
                        }
                        ?.catch {
                            emit(HomeView.PlaybackUiState.Error)
                        }
                        ?: flowOf(HomeView.PlaybackUiState.ServiceUnavailable)
                }
                .distinctUntilChanged()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = HomeView.PlaybackUiState.ServiceUnavailable
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






        val timerFlow: Flow<HomeView.TimerUiState> = timerStateObserver.timerState
            .map { timerState ->
                when (timerState.status) {
                    Domain.TimerStatus.IDLE     -> HomeView.TimerUiState.Idle
                    Domain.TimerStatus.FINISHED -> HomeView.TimerUiState.Finished
                    Domain.TimerStatus.RUNNING,
                    Domain.TimerStatus.FADING   -> HomeView.TimerUiState.Active(
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
            .catch { emit(HomeView.TimerUiState.Idle) }
            .distinctUntilChanged()

        val bluetoothFlow: Flow<HomeView.BluetoothUiState> = serviceFlow.flatMapLatest { service ->
            if (service == null) {
                flowOf(HomeView.BluetoothUiState.Unknown)
            } else {
                service.audioState
                    .map { state ->
                        if (state.isBluetoothConnected) HomeView.BluetoothUiState.Connected
                        else HomeView.BluetoothUiState.Disconnected
                    }
                    .catch { emit(HomeView.BluetoothUiState.Unknown) }
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
    ): HomeView.HomeUiState {
        val (pinned, recent, sounds, totalMs, prefs) = content
        val (playback, timer, bluetooth, tier, presetCount) = ctx

        val resumeCard = resolveResumeCard(prefs, recent)
        val freeLimit  = tier == Domain.EntitlementTier.FREE && presetCount >= FREE_PRESET_LIMIT

        // Enrich active playback with preset name from recent presets
        val enrichedPlayback = if (playback is HomeView.PlaybackUiState.Active &&
            playback.activePresetId != null
        ) {
            val presetName = recent.find { it.id == playback.activePresetId }?.name
                ?: pinned.find { it.id == playback.activePresetId }?.name
            playback.copy(activePresetName = presetName)
        } else {
            playback
        }

        val isInitialising = pinned.isEmpty() && recent.isEmpty() &&
                sounds.isEmpty() && playback is HomeView.PlaybackUiState.ServiceUnavailable

        val screenState = when {
            isInitialising -> HomeView.ScreenState.Loading
            pinned.isEmpty() && recent.isEmpty() && sounds.isEmpty() -> HomeView.ScreenState.Empty
            else -> HomeView.ScreenState.Ready(
                resumeCard    = resumeCard,
                pinnedPresets = pinned,
                recentPresets = recent.filter { r -> pinned.none { p -> p.id == r.id } },
                recentSounds  = sounds,
                totalListenedMs = totalMs,
            )
        }

        return HomeView.HomeUiState(
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
    ): HomeView.ResumeCardState? {
        val now = System.currentTimeMillis()

        // Priority 1: last used preset within 24 hours
        val lastPresetId = prefs.lastPlayedPresetId
        if (lastPresetId != null) {
            val preset = recentPresets.find { it.id == lastPresetId }
            val lastUsedAt = preset?.lastUsedAt
            if (preset != null && lastUsedAt != null && (now - lastUsedAt) < RESUME_STALENESS_MS) {
                return HomeView.ResumeCardState.FromPreset(preset, lastUsedAt)
            }
        }

        // Priority 2: most recently used preset from the list (even without explicit lastPlayed pref)
        val mostRecent = recentPresets
            .filter { it.lastUsedAt != null && (now - it.lastUsedAt!!) < RESUME_STALENESS_MS }
            .maxByOrNull { it.lastUsedAt!! }
        if (mostRecent != null) {
            return HomeView.ResumeCardState.FromPreset(mostRecent, mostRecent.lastUsedAt!!)
        }

        // Priority 3: last played ephemeral mix (Sandra's un-saved sessions)
        val mixJson = prefs.lastPlayedMixJson
        if (mixJson != null) {
            val mix = runCatching { mixSerializer.deserialize(mixJson) }.getOrNull()
            if (mix != null && !mix.isEmpty) {
                // Approximate timestamp from the mix itself — no dedicated timestamp on ephemeral
                val approxTimestamp = now - (2 * 60 * 60 * 1000L) // assume 2h ago as fallback
                return HomeView.ResumeCardState.FromEphemeralMix(mix, approxTimestamp)
            }
        }

        return null
    }


    private fun buildGreeting(): HomeView.GreetingUiState {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeOfDay = when (hour) {
            in 5..11  -> HomeView.TimeOfDay.MORNING
            in 12..16 -> HomeView.TimeOfDay.AFTERNOON
            in 17..20 -> HomeView.TimeOfDay.EVENING
            else      -> HomeView.TimeOfDay.NIGHT
        }
        val (headline, subtext) = when (timeOfDay) {
            HomeView.TimeOfDay.MORNING   -> "Good morning" to "Start your day with calm"
            HomeView.TimeOfDay.AFTERNOON -> "Good afternoon" to "A moment of quiet"
            HomeView.TimeOfDay.EVENING   -> "Good evening" to "Wind down with sound"
            HomeView.TimeOfDay.NIGHT     -> "Sleep well" to "Let sound carry you"
        }
        return HomeView.GreetingUiState(headline, subtext, timeOfDay)
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
                        if (playback is HomeView.PlaybackUiState.AudioFocusLost &&
                            playback.reason == Domain.AudioFocusLostReason.PHONE_CALL
                        ) {
                            _events.send(HomeView.HomeEvent.AudioFocusRestoredAfterCall)
                        }
                    }
                    else -> { /* Handled via uiState playback sub-state */ }
                }
            }
            .catch { /* Focus observation errors are non-fatal */ }
            .launchIn(viewModelScope)
    }

    /**
     * When the timer finishes, the audio stops automatically via FadeWorker.
     * Emit a one-shot event so the UI can show a gentle "Session ended" message.
     */
    private fun observeTimerFinished() {
        timerStateObserver.timerStatus
            .distinctUntilChanged()
            .onEach { status ->
                if (status == Domain.TimerStatus.FINISHED) {
                    _events.send(HomeView.HomeEvent.ShowError("Your sleep timer has ended"))
                }
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
                    if (playback is HomeView.PlaybackUiState.Active) {
                        _events.send(HomeView.HomeEvent.ShowServiceUnavailableSnackbar)
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
        val playback: HomeView.PlaybackUiState,
        val timer: HomeView.TimerUiState,
        val bluetooth: HomeView.BluetoothUiState,
        val tier: Domain.EntitlementTier,
        val presetCount: Int,
    )

    private operator fun PlaybackContext.component1() = playback
    private operator fun PlaybackContext.component2() = timer
    private operator fun PlaybackContext.component3() = bluetooth
    private operator fun PlaybackContext.component4() = tier
    private operator fun PlaybackContext.component5() = presetCount
}