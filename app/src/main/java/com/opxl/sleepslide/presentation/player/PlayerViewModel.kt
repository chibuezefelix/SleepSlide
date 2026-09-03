package com.opxl.sleepslide.presentation.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opxl.sleepslide.data.AudioServiceHolder
import com.opxl.sleepslide.data.repository.MixSerializer
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.observer.AudioStateObserver
import com.opxl.sleepslide.domain.observer.EntitlementObserver
import com.opxl.sleepslide.domain.observer.TimerStateObserver
import com.opxl.sleepslide.domain.repository.PlayHistoryRepository
import com.opxl.sleepslide.domain.repository.PresetRepository
import com.opxl.sleepslide.domain.repository.UserPreferencesRepository
import com.opxl.sleepslide.domain.repository.VolumeMemoryRepository
import com.opxl.sleepslide.domain.service.TimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

// Constants

private const val VOLUME_DEBOUNCE_MS        = 400L
private const val SHARING_STOP_TIMEOUT_MS   = 5_000L
private const val MAX_LAYERS                = 3
private const val FIVE_MINUTES_MS           = 5 * 60 * 1_000L
private const val ADD_TIME_MAX_MS           = 4 * 60 * 60 * 1_000L  // 4 hours cap
private const val PRESET_NAME_KEY           = "pending_preset_name"
private const val PRESET_EMOJI_KEY          = "pending_preset_emoji"
private const val NIGHT_LOCK_USER_KEY       = "night_lock_user_set"  // user explicitly toggled


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val audioServiceHolder: AudioServiceHolder,
    private val audioStateObserver: AudioStateObserver,
    private val timerStateObserver: TimerStateObserver,
    private val entitlementObserver: EntitlementObserver,
    private val timerService: TimerService,
    private val presetRepository: PresetRepository,
    private val playHistoryRepository: PlayHistoryRepository,
    private val volumeMemoryRepository: VolumeMemoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val mixSerializer: MixSerializer,
) : ViewModel() {


    private val _events = Channel<PlayerEvent>(Channel.BUFFERED)
    val events: Flow<PlayerEvent> = _events.receiveAsFlow()


    /**
     * Mutex guards all writes to [_layerVolumes] and [_masterVolume].
     * Slider drags arrive on Main, DB writes go to IO — the Mutex ensures
     * the in-memory map and the debounce flush are never interleaved.
     */
    private val volumeMutex = Mutex()

    // Source-of-truth volume per layer position — optimistic, ahead of service
    private val _layerVolumes   = MutableStateFlow<Map<Int, Float>>(emptyMap())
    private val _masterVolume   = MutableStateFlow(1.0f)

    // Positions currently being dragged — suppresses incoming service echoes
    private val _dragging       = MutableStateFlow<Set<Int>>(emptySet())

    // Active session tracking
    private val _sessionId      = MutableStateFlow<Long?>(null)
    private val _sessionStart   = MutableStateFlow<Long?>(null)

    // Preset / save state
    private val _activePreset   = MutableStateFlow< Domain.Preset?>(null)
    private val _isMixDirty     = MutableStateFlow(false)
    private val _saveState      = MutableStateFlow<ActivePresetState>(ActivePresetState.None)

    // Night lock — separate flag tracks if user explicitly toggled this session
    private val _nightLock      = MutableStateFlow<NightLockState>(NightLockState.Disabled)
    private val _nightLockUserSet = MutableStateFlow(
        savedStateHandle.get<Boolean>(NIGHT_LOCK_USER_KEY) ?: false
    )

    // Notification permission
    private val _notification   = MutableStateFlow<NotificationState>(NotificationState.Idle)

    // Debounce jobs — one per keyed target (layer position or "master")
    private val volumeJobs = mutableMapOf<String, Job>()

    // Tracks previous focus status for transition-based one-shot events
    private var prevFocusStatus: Domain.AudioFocusStatus? = null


    val uiState: StateFlow<PlayerUiState> = buildUiState()
        .catch { e ->
            emit(
                PlayerUiState(
                    playback = PlayerPlaybackState.Error(
                        e.message ?: "An unexpected error occurred"
                    )
                )
            )
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(SHARING_STOP_TIMEOUT_MS),
            initialValue = PlayerUiState(),
        )


    init {
        observeFocusTransitions()
        observeTimerCompletion()
        observeBluetoothLoss()
        observeServiceRebind()
        observeNightLockDefault()
        checkPendingPresetSave()
    }


    /**
     * Starts playback of [mix], optionally from a saved [preset].
     *
     * Order of operations:
     * 1. Gate on notification permission — first play triggers the request
     * 2. Gate on service availability
     * 3. Close any stale session atomically before opening a new one
     * 4. Apply volume memory to the mix layers
     * 5. Call service.play — rollback session open if it fails
     * 6. Persist last played reference
     */
    fun play(mix: Domain.SoundMix, preset: Domain.Preset? = null) {
        viewModelScope.launch {
            if (!gateNotificationPermission()) return@launch
            val service = gateService() ?: return@launch
            if (mix.isEmpty) {
                _events.trySend(PlayerEvent.ShowError("No sounds in this mix"))
                return@launch
            }

            // Close stale session before opening new — non-fatal if it fails
            safeCloseSession(Domain.StopReason.USER)

            // Apply volume memory — fetch all in one query
            val resolvedMix = applyVolumeMemory(mix)

            // Seed local volume state optimistically before the service echoes back
            volumeMutex.withLock {
                _layerVolumes.value = resolvedMix.layers.associate { it.position to it.volume }
                _masterVolume.value = resolvedMix.masterVolume
            }

            _activePreset.value  = preset
            _isMixDirty.value    = false
            _saveState.value     = if (preset != null)
                ActivePresetState.Loaded(preset, isDirty = false)
            else ActivePresetState.None

            // Open history session — get ID before service.play so we can rollback
            val sessionId = runCatching {
                playHistoryRepository.openSession(resolvedMix, preset?.id)
            }.getOrNull()

            // Start audio
            val playResult = runCatching { service.play(resolvedMix) }

            if (playResult.isFailure) {
                // Rollback: close session that never had real audio
                sessionId?.let { safeCloseSessionById(it, Domain.StopReason.ERROR) }
                _sessionId.value   = null
                _sessionStart.value = null
                _events.trySend(
                    PlayerEvent.ShowError(
                        playResult.exceptionOrNull()?.message ?: "Could not start playback"
                    )
                )
                return@launch
            }

            _sessionId.value    = sessionId
            _sessionStart.value = System.currentTimeMillis()

            // Persist last-played reference — non-fatal
            runCatching {
                if (preset != null) {
                    presetRepository.recordUsed(preset.id)
                    userPreferencesRepository.setLastPlayedPreset(preset.id)
                } else {
                    userPreferencesRepository.setLastPlayedEphemeralMix(
                        mixSerializer.serialize(resolvedMix)
                    )
                }
            }
        }
    }

    fun pause() {
        viewModelScope.launch {
            val service = gateService() ?: return@launch
            runCatching { service.pause() }
                .onFailure { _events.trySend(PlayerEvent.ShowError("Could not pause")) }
        }
    }

    fun resume() {
        viewModelScope.launch {
            val service = gateService() ?: return@launch
            runCatching { service.resume() }
                .onFailure { _events.trySend(PlayerEvent.ShowError("Could not resume")) }
        }
    }

    fun stop() {
        viewModelScope.launch {
            safeCloseSession(Domain.StopReason.USER)
            runCatching {
                audioServiceHolder.current?.stop()
                timerService.cancel()
            }.onFailure { _events.trySend(PlayerEvent.ShowError("Could not stop playback")) }
            clearMixState()
        }
    }


    /**
     * Called on every drag event — potentially hundreds per second.
     * Thread-safe: Mutex guards the map write.
     * Debounced: service + DB writes fire only after [VOLUME_DEBOUNCE_MS] idle.
     */
    fun onLayerVolumeChanged(position: Int, volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        viewModelScope.launch {
            volumeMutex.withLock {
                _layerVolumes.update { it + (position to clamped) }
                _dragging.update { it + position }
            }
            _isMixDirty.value = true
        }
        scheduleVolumeWrite("layer_$position") {
            val soundId = uiState.value.mixer.layers
                .find { it.position == position }?.sound?.id
            runCatching {
                audioServiceHolder.current?.setLayerVolume(position, clamped)
                if (soundId != null) {
                    volumeMemoryRepository.saveVolume(soundId, clamped)
                }
            }.onFailure { _events.trySend(PlayerEvent.ShowError("Volume update failed")) }
            volumeMutex.withLock { _dragging.update { it - position } }
        }
    }

    /**
     * Called when the user lifts their finger — flush the debounce immediately.
     * Cancels any pending debounce job and writes now.
     */
    fun onLayerVolumeDragEnded(position: Int) {
        volumeJobs["layer_$position"]?.cancel()
        viewModelScope.launch {
            val volume = volumeMutex.withLock {
                _dragging.update { it - position }
                _layerVolumes.value[position] ?: return@launch
            }
            val soundId = uiState.value.mixer.layers
                .find { it.position == position }?.sound?.id
            runCatching {
                audioServiceHolder.current?.setLayerVolume(position, volume)
                if (soundId != null) {
                    volumeMemoryRepository.saveVolume(soundId, volume)
                }
            }.onFailure { _events.trySend(PlayerEvent.ShowError("Could not save volume")) }
        }
    }

    fun onMasterVolumeChanged(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        viewModelScope.launch {
            volumeMutex.withLock { _masterVolume.value = clamped }
            _isMixDirty.value = true
        }
        scheduleVolumeWrite("master") {
            runCatching {
                audioServiceHolder.current?.setMasterVolume(clamped)
            }.onFailure { _events.trySend(PlayerEvent.ShowError("Master volume update failed")) }
        }
    }

    fun onMasterVolumeDragEnded() {
        volumeJobs["master"]?.cancel()
        viewModelScope.launch {
            val volume = volumeMutex.withLock { _masterVolume.value }
            runCatching {
                audioServiceHolder.current?.setMasterVolume(volume)
            }.onFailure { _events.trySend(PlayerEvent.ShowError("Could not save master volume")) }
        }
    }

    fun muteLayer(position: Int) {
        viewModelScope.launch {
            val service = gateService() ?: return@launch
            runCatching { service.muteLayer(position) }
                .onSuccess { _isMixDirty.value = true }
                .onFailure { _events.trySend(PlayerEvent.ShowError("Could not mute layer")) }
        }
    }

    fun unmuteLayer(position: Int) {
        viewModelScope.launch {
            val service = gateService() ?: return@launch
            runCatching { service.unmuteLayer(position) }
                .onSuccess { _isMixDirty.value = true }
                .onFailure { _events.trySend(PlayerEvent.ShowError("Could not unmute layer")) }
        }
    }

    fun addLayer(sound: Domain.Sound) {
        viewModelScope.launch {
            val service = gateService() ?: return@launch
            val currentLayers = uiState.value.mixer.layers

            if (currentLayers.size >= MAX_LAYERS) {
                _events.trySend(PlayerEvent.ShowInfo("Maximum $MAX_LAYERS sounds at once"))
                return@launch
            }

            if (currentLayers.any { it.sound.id == sound.id }) {
                _events.trySend(PlayerEvent.ShowInfo("${sound.title} is already in this mix"))
                return@launch
            }

            val position = (0 until MAX_LAYERS)
                .firstOrNull { pos -> currentLayers.none { it.position == pos } }
                ?: run {
                    _events.trySend(PlayerEvent.ShowError("No available layer slot"))
                    return@launch
                }

            val volume = runCatching {
                volumeMemoryRepository.getVolume(sound.id)
            }.getOrDefault(1.0f)

            val layer = Domain.SoundLayer(sound = sound, volume = volume, position = position)

            runCatching { service.addLayer(layer) }
                .onSuccess {
                    volumeMutex.withLock { _layerVolumes.update { it + (position to volume) } }
                    _isMixDirty.value = true
                }
                .onFailure {
                    _events.trySend(PlayerEvent.ShowError("Could not add ${sound.title}"))
                }
        }
    }

    fun removeLayer(position: Int) {
        viewModelScope.launch {
            val service = gateService() ?: return@launch

            // Prevent removing the last layer — stop instead
            if (uiState.value.mixer.layers.size <= 1) {
                _events.trySend(
                    PlayerEvent.ShowInfo("Removing the last sound will stop playback")
                )
                stop()
                return@launch
            }

            runCatching { service.removeLayer(position) }
                .onSuccess {
                    volumeMutex.withLock { _layerVolumes.update { it - position } }
                    _isMixDirty.value = true
                }
                .onFailure {
                    _events.trySend(PlayerEvent.ShowError("Could not remove layer"))
                }
        }
    }

    fun swapLayer(position: Int, sound: Domain.Sound) {
        viewModelScope.launch {
            val service = gateService() ?: return@launch

            // Prevent swapping to a sound already in the mix
            if (uiState.value.mixer.layers.any { it.sound.id == sound.id }) {
                _events.trySend(PlayerEvent.ShowInfo("${sound.title} is already in this mix"))
                return@launch
            }

            val volume = runCatching {
                volumeMemoryRepository.getVolume(sound.id)
            }.getOrDefault(1.0f)

            val layer = Domain.SoundLayer(sound = sound, volume = volume, position = position)

            runCatching { service.swapLayer(position, layer) }
                .onSuccess {
                    volumeMutex.withLock { _layerVolumes.update { it + (position to volume) } }
                    _isMixDirty.value = true
                }
                .onFailure {
                    _events.trySend(PlayerEvent.ShowError("Could not swap to ${sound.title}"))
                }
        }
    }


    fun requestTimer() {
        viewModelScope.launch { _events.send(PlayerEvent.ShowTimerPicker) }
    }

    fun startTimer(durationMs: Long) {
        viewModelScope.launch {
            if (durationMs <= 0L) {
                _events.trySend(PlayerEvent.ShowError("Timer duration must be greater than zero"))
                return@launch
            }
            if (durationMs > ADD_TIME_MAX_MS) {
                _events.trySend(PlayerEvent.ShowError("Maximum timer duration is 4 hours"))
                return@launch
            }
            val prefs = runCatching { userPreferencesRepository.get() }.getOrNull()
            val fadeMs = prefs?.defaultFadeOutMs ?: 60_000L

            runCatching { timerService.start(durationMs, fadeMs) }
                .onSuccess {
                    runCatching { userPreferencesRepository.setLastTimerDuration(durationMs) }
                    _events.trySend(PlayerEvent.TimerStarted)
                }
                .onFailure { _events.trySend(PlayerEvent.ShowError("Could not start timer")) }
        }
    }

    fun cancelTimer() {
        viewModelScope.launch {
            val timer = uiState.value.timer
            if (timer is PlayerTimerState.Idle) {
                _events.trySend(PlayerEvent.ShowInfo("No timer is running"))
                return@launch
            }
            runCatching { timerService.cancel() }
                .onSuccess { _events.trySend(PlayerEvent.TimerCancelled) }
                .onFailure { _events.trySend(PlayerEvent.ShowError("Could not cancel timer")) }
        }
    }

    fun addFiveMinutes() {
        viewModelScope.launch {
            val timer = uiState.value.timer
            if (timer !is PlayerTimerState.Active) {
                _events.trySend(PlayerEvent.ShowInfo("Start a timer first"))
                return@launch
            }
            val newTotal = timer.totalDurationMs + FIVE_MINUTES_MS
            if (newTotal > ADD_TIME_MAX_MS) {
                _events.trySend(PlayerEvent.ShowInfo("Timer is already at maximum"))
                return@launch
            }
            runCatching { timerService.addTime(FIVE_MINUTES_MS) }
                .onFailure { _events.trySend(PlayerEvent.ShowError("Could not extend timer")) }
        }
    }

    fun reduceFiveMinutes() {
        viewModelScope.launch {
            val timer = uiState.value.timer
            if (timer !is PlayerTimerState.Active) {
                _events.trySend(PlayerEvent.ShowInfo("Start a timer first"))
                return@launch
            }
            if (timer.remainingMs <= FIVE_MINUTES_MS) {
                _events.trySend(
                    PlayerEvent.ShowInfo("Less than 5 minutes remain — cancel timer instead")
                )
                return@launch
            }
            runCatching { timerService.reduceTime(FIVE_MINUTES_MS) }
                .onFailure { _events.trySend(PlayerEvent.ShowError("Could not reduce timer")) }
        }
    }

    // PRESET COMMANDS

    fun requestSaveAsPreset() {
        viewModelScope.launch {
            val mix = audioServiceHolder.current?.audioState?.value?.activeMix
            if (mix == null || mix.isEmpty) {
                _events.trySend(PlayerEvent.ShowError("Nothing is playing to save"))
                return@launch
            }
            _events.send(PlayerEvent.ShowSavePresetDialog)
        }
    }

    fun saveAsNewPreset(name: String, emoji: String?) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                _events.trySend(PlayerEvent.ShowError("Preset name cannot be empty"))
                return@launch
            }

            val mix = audioServiceHolder.current?.audioState?.value?.activeMix ?: run {
                _events.trySend(PlayerEvent.ShowError("No active mix to save"))
                return@launch
            }

            // Persist name to handle process death mid-save dialog
            savedStateHandle[PRESET_NAME_KEY]  = trimmedName
            savedStateHandle[PRESET_EMOJI_KEY] = emoji
            _saveState.value = ActivePresetState.Saving(trimmedName)

            val preset = Domain.Preset(name = trimmedName, mix = mix, emoji = emoji)

            runCatching { presetRepository.save(preset) }
                .onSuccess { savedId ->
                    val saved = preset.copy(id = savedId)
                    _activePreset.value  = saved
                    _isMixDirty.value    = false
                    _saveState.value     = ActivePresetState.Loaded(saved, isDirty = false)
                    savedStateHandle.remove<String>(PRESET_NAME_KEY)
                    savedStateHandle.remove<String>(PRESET_EMOJI_KEY)
                    runCatching { userPreferencesRepository.setLastPlayedPreset(savedId) }
                    _events.trySend(PlayerEvent.PresetSaved(saved))
                }
                .onFailure { e ->
                    _saveState.value = ActivePresetState.SaveError(
                        e.message ?: "Could not save preset"
                    )
                    _events.trySend(PlayerEvent.ShowError(
                        e.message ?: "Could not save preset"
                    ))
                }
        }
    }

    /**
     * Overwrites the active preset's mix with the current live mix.
     * Only available when [_activePreset] is non-null and [_isMixDirty] is true.
     */
    fun updateActivePreset() {
        viewModelScope.launch {
            val preset = _activePreset.value ?: run {
                _events.trySend(PlayerEvent.ShowError("No preset loaded to update"))
                return@launch
            }
            if (!_isMixDirty.value) {
                _events.trySend(PlayerEvent.ShowInfo("No changes to save"))
                return@launch
            }
            val mix = audioServiceHolder.current?.audioState?.value?.activeMix ?: run {
                _events.trySend(PlayerEvent.ShowError("No active mix"))
                return@launch
            }

            _saveState.value = ActivePresetState.Saving(preset.name)

            runCatching { presetRepository.updateMix(preset.id, mix) }
                .onSuccess {
                    val updated = preset.copy(mix = mix)
                    _activePreset.value = updated
                    _isMixDirty.value   = false
                    _saveState.value    = ActivePresetState.Loaded(updated, isDirty = false)
                    _events.trySend(PlayerEvent.PresetUpdated(updated))
                }
                .onFailure { e ->
                    _saveState.value = ActivePresetState.SaveError(
                        e.message ?: "Could not update preset"
                    )
                    _events.trySend(PlayerEvent.ShowError(
                        e.message ?: "Could not update preset"
                    ))
                }
        }
    }

    fun discardPresetChanges() {
        viewModelScope.launch {
            _isMixDirty.value = false
            val preset = _activePreset.value
            _saveState.value = if (preset != null)
                ActivePresetState.Loaded(preset, isDirty = false)
            else ActivePresetState.None
        }
    }

    // NIGHT LOCK COMMANDS

    fun enableNightLock() {
        viewModelScope.launch {
            val service = gateService() ?: return@launch
            runCatching { service.enableNightLock() }
                .onSuccess {
                    _nightLock.value = NightLockState.Enabled
                    // Record user explicitly set this — prevents default preference re-toggling
                    _nightLockUserSet.value = true
                    savedStateHandle[NIGHT_LOCK_USER_KEY] = true
                    _events.trySend(PlayerEvent.NightLockEnabled)
                }
                .onFailure { _events.trySend(PlayerEvent.ShowError("Could not enable night lock")) }
        }
    }

    fun disableNightLock() {
        viewModelScope.launch {
            val service = gateService() ?: return@launch
            runCatching { service.disableNightLock() }
                .onSuccess {
                    _nightLock.value = NightLockState.Disabled
                    _nightLockUserSet.value = true
                    savedStateHandle[NIGHT_LOCK_USER_KEY] = true
                    _events.trySend(PlayerEvent.NightLockDisabled)
                }
                .onFailure { _events.trySend(PlayerEvent.ShowError("Could not disable night lock")) }
        }
    }

    /** Long-press begins — transition to the Unlocking state for progress UI */
    fun onNightLockLongPressStarted() {
        if (_nightLock.value == NightLockState.Enabled) {
            _nightLock.value = NightLockState.Unlocking
        }
    }

    /** Long-press completed — actually unlock */
    fun onNightLockLongPressCompleted() {
        if (_nightLock.value == NightLockState.Unlocking) {
            disableNightLock()
        }
    }

    /** Long-press cancelled (finger lifted too early) — revert to locked */
    fun onNightLockLongPressCancelled() {
        if (_nightLock.value == NightLockState.Unlocking) {
            _nightLock.value = NightLockState.Enabled
        }
    }

    // NAVIGATION COMMANDS

    /**
     * Back navigation with unsaved-changes guard.
     * Only prompts when a named preset was loaded AND the mix was modified.
     * Ephemeral mixes never prompt — there is nothing to lose.
     */
    fun onBackRequested() {
        viewModelScope.launch {
            val isNamedPresetDirty = _activePreset.value != null && _isMixDirty.value
            if (isNamedPresetDirty) {
                _events.send(PlayerEvent.ShowUnsavedChangesDialog)
            } else {
                _events.send(PlayerEvent.NavigateBack)
            }
        }
    }

    fun discardChangesAndNavigateBack() {
        viewModelScope.launch {
            _isMixDirty.value = false
            _events.send(PlayerEvent.NavigateBack)
        }
    }

    fun onAddSoundRequested() {
        viewModelScope.launch { _events.send(PlayerEvent.NavigateToLibrary) }
    }

    // NOTIFICATION PERMISSION

    /**
     * Called by MainActivity after the permission result callback.
     * [isGranted] — system result. [hasRequestedBefore] — from preferences.
     */
    fun onNotificationPermissionResult(isGranted: Boolean) {
        _notification.value = if (isGranted) NotificationState.Granted else NotificationState.Idle
    }

    fun checkNotificationPermission(isGranted: Boolean) {
        viewModelScope.launch {
            val prefs = runCatching { userPreferencesRepository.get() }.getOrNull()
            _notification.value = when {
                isGranted -> NotificationState.Granted
                prefs?.hasRequestedNotificationPermission == true -> NotificationState.Idle
                else -> NotificationState.PermissionRequired
            }
        }
    }

    // STATE CONSTRUCTION

    private fun buildUiState(): Flow<PlayerUiState> {
        //  Playback
        val playbackFlow = audioServiceHolder.service
            .flatMapLatest { service ->
                if (service == null) return@flatMapLatest flowOf(PlayerPlaybackState.ServiceUnavailable)
                service.audioState.map { state ->
                    when (state.playbackStatus) {
                        Domain.PlaybackStatus.IDLE      -> PlayerPlaybackState.Idle
                        Domain.PlaybackStatus.LOADING   -> PlayerPlaybackState.Loading
                        Domain.PlaybackStatus.FADING_IN -> PlayerPlaybackState.FadingIn
                        Domain.PlaybackStatus.FADING_OUT -> {
                            val timerFading = timerStateObserver.timerState.value.status == Domain.TimerStatus.FADING
                            PlayerPlaybackState.FadingOut(
                                if (timerFading) FadeOutTrigger.TIMER else FadeOutTrigger.USER
                            )
                        }
                        Domain.PlaybackStatus.PAUSED -> PlayerPlaybackState.Paused
                        Domain.PlaybackStatus.ERROR  -> PlayerPlaybackState.Error("Audio engine error — try restarting")
                        Domain.PlaybackStatus.PLAYING -> when (state.audioFocusStatus) {
                            Domain.AudioFocusStatus.LOST -> PlayerPlaybackState.Interrupted(
                                InterruptionReason.ANOTHER_APP
                            )
                            Domain.AudioFocusStatus.LOST_TRANSIENT -> PlayerPlaybackState.Interrupted(
                                InterruptionReason.PHONE_CALL
                            )
                            else -> PlayerPlaybackState.Playing(
                                fadeInProgress        = state.fadeInProgress,
                                isPlayingInBackground = state.isPlayingInBackground,
                                audioFocusStatus      = state.audioFocusStatus,
                            )
                        }
                    }
                }
                    .catch { emit(PlayerPlaybackState.Error("Unexpected playback error")) }
                    .distinctUntilChanged()
            }

        val mixerFlow = combine(
            audioServiceHolder.service.flatMapLatest { service ->
                service?.audioState?.map { it.activeMix } ?: flowOf(null)
            }.distinctUntilChanged(),
            _layerVolumes,
            _dragging,
            _masterVolume,
            _isMixDirty,
        ) { activeMix, volumes, dragging, master, dirty ->
            val layers = activeMix?.layers.orEmpty()
                .sortedBy { it.position }
                .map { layer ->
                    LayerUiState(
                        position             = layer.position,
                        sound                = layer.sound,
                        // Use local optimistic volume while dragging; remote otherwise
                        volume               = if (layer.position in dragging)
                            volumes[layer.position] ?: layer.volume
                        else layer.volume,
                        isMuted              = layer.isMuted,
                        isVolumeBeingDragged = layer.position in dragging,
                    )
                }

            val occupied = layers.map { it.position }.toSet()
            MixerState(
                layers            = layers,
                masterVolume      = master,
                hasUnsavedChanges = dirty,
                canAddLayer       = layers.size < MAX_LAYERS,
                availableSlots    = (0 until MAX_LAYERS).filter { it !in occupied },
            )
        }
            .catch { emit(MixerState()) }
            .distinctUntilChanged()

        //  Timer
        val timerFlow = timerStateObserver.timerState
            .map { state ->
                when (state.status) {
                    Domain.TimerStatus.IDLE     -> PlayerTimerState.Idle
                    Domain.TimerStatus.FINISHED -> PlayerTimerState.Finished
                    Domain.TimerStatus.RUNNING,
                    Domain.TimerStatus.FADING   -> PlayerTimerState.Active(
                        status           = state.status,
                        remainingMs      = state.remainingMs,
                        remainingLabel   = state.remainingMs.toTimerLabel(),
                        totalDurationMs  = state.durationMs,
                        progressFraction = if (state.durationMs > 0L)
                            1f - (state.remainingMs.toFloat() / state.durationMs)
                        else 0f,
                        fadeBeforeEndMs  = state.fadeBeforeEndMs,
                        isFading         = state.status == Domain.TimerStatus.FADING,
                    )
                }
            }
            .catch { emit(PlayerTimerState.Idle) }
            .distinctUntilChanged()

        //  Preset
        val presetFlow = combine(_activePreset, _isMixDirty, _saveState) { preset, dirty, save ->
            when {
                save is ActivePresetState.Saving || save is ActivePresetState.SaveError -> save
                preset != null -> ActivePresetState.Loaded(preset, isDirty = dirty)
                else           -> ActivePresetState.None
            }
        }
            .catch { emit(ActivePresetState.None) }
            .distinctUntilChanged()

        //  Bluetooth
        val bluetoothFlow = audioServiceHolder.service
            .flatMapLatest { service ->
                service?.audioState?.map { it.isBluetoothConnected } ?: flowOf(false)
            }
            .catch { emit(false) }
            .distinctUntilChanged()

        //  Entitlement
        val tierFlow = entitlementObserver.tier
            .catch { emit(Domain.EntitlementTier.FREE) }
            .distinctUntilChanged()

        //  Session
        val sessionFlow = combine(_sessionId, _sessionStart) { id, start ->
            when {
                id != null && start != null -> SessionState.Active(id, start)
                else                        -> SessionState.Idle
            }
        }.distinctUntilChanged()

        //  Combine all streams
        return combine(
            playbackFlow,
            mixerFlow,
            timerFlow,
            presetFlow,
            _nightLock,
        ) { pb, mixer, timer, preset, lock ->
            PlayerPartialA(pb, mixer, timer, preset, lock)
        }.combine(
            combine(sessionFlow, bluetoothFlow, tierFlow, _notification) { s, bt, t, n ->
                PlayerPartialB(s, bt, t, n)
            }
        ) { a, b ->
            PlayerUiState(
                playback             = a.playback,
                mixer                = a.mixer,
                timer                = a.timer,
                preset               = a.preset,
                nightLock            = a.nightLock,
                session              = b.session,
                isBluetoothConnected = b.bluetooth,
                entitlementTier      = b.tier,
                notification         = b.notification,
            )
        }
    }

    // SIDE-EFFECT OBSERVERS

    /**
     * Detects AudioFocus transitions and emits one-shot events for contextual UI.
     * Uses a remembered previous status to detect the LOST_TRANSIENT → GAINED
     * transition (call ended) vs a plain GAINED on startup.
     * Runs on Main — reading [prevFocusStatus] is safe because this is the only
     * coroutine that writes it.
     */
    private fun observeFocusTransitions() {
        audioStateObserver.audioFocusStatus
            .distinctUntilChanged()
            .onEach { status ->
                val previous = prevFocusStatus
                prevFocusStatus = status
                when {
                    status == Domain.AudioFocusStatus.LOST_TRANSIENT ->
                        _events.trySend(PlayerEvent.AudioInterrupted(InterruptionReason.PHONE_CALL))
                    status == Domain.AudioFocusStatus.LOST ->
                        _events.trySend(PlayerEvent.AudioInterrupted(InterruptionReason.ANOTHER_APP))
                    status == Domain.AudioFocusStatus.GAINED && previous == Domain.AudioFocusStatus.LOST_TRANSIENT ->
                        _events.trySend(PlayerEvent.AudioRestoredAfterCall)
                }
            }
            .catch { /* Focus observation errors are non-fatal */ }
            .launchIn(viewModelScope)
    }

    /**
     * When the timer finishes, close the history session with [StopReason.TIMER]
     * and emit a one-shot event.  Audio has already stopped via FadeWorker.
     */
    private fun observeTimerCompletion() {
        timerStateObserver.timerStatus
            .filter { it == Domain.TimerStatus.FINISHED }
            .distinctUntilChanged()
            .onEach {
                safeCloseSession(Domain.StopReason.TIMER)
                clearMixState()
                _events.trySend(PlayerEvent.TimerFinished)
            }
            .catch { }
            .launchIn(viewModelScope)
    }

    /**
     * Detects Bluetooth headset disconnect mid-session.
     * Only fires the event when audio was actively playing — not on app start.
     */
    private fun observeBluetoothLoss() {
        audioStateObserver.isBluetoothConnected
            .distinctUntilChanged()
            .onEach { connected ->
                if (!connected) {
                    val pb = uiState.value.playback
                    if (pb is PlayerPlaybackState.Playing || pb is PlayerPlaybackState.FadingIn) {
                        _events.trySend(
                            PlayerEvent.AudioInterrupted(InterruptionReason.BLUETOOTH_LOST)
                        )
                    }
                }
            }
            .catch { }
            .launchIn(viewModelScope)
    }

    /**
     * When the service rebinds after process death, re-sync local volume state
     * from the service's authoritative AudioState.
     * Without this, sliders show stale pre-death values after rebind.
     */
    private fun observeServiceRebind() {
        audioServiceHolder.service
            .distinctUntilChanged { old, new -> (old == null) == (new == null) }
            .onEach { service ->
                if (service != null) {
                    val activeMix = service.audioState.value.activeMix ?: return@onEach
                    volumeMutex.withLock {
                        _layerVolumes.value = activeMix.layers.associate { it.position to it.volume }
                        _masterVolume.value = activeMix.masterVolume
                    }
                }
            }
            .catch { }
            .launchIn(viewModelScope)
    }

    /**
     * Applies the user's night-lock default preference on the FIRST transition
     * to PLAYING in a session.
     *
     * Guards:
     * - Only fires if the user has NOT explicitly toggled night lock this session
     *   ([_nightLockUserSet] is false)
     * - Only fires once per play — the filter on PLAYING with [distinctUntilChanged]
     *   means PAUSE → RESUME does NOT re-trigger this
     */
    private fun observeNightLockDefault() {
        audioStateObserver.playbackStatus
            .filter { it == Domain.PlaybackStatus.PLAYING }
            .distinctUntilChanged()
            .onEach {
                if (_nightLockUserSet.value) return@onEach
                if (_nightLock.value != NightLockState.Disabled) return@onEach
                val prefs = runCatching { userPreferencesRepository.get() }.getOrNull()
                if (prefs?.isNightLockEnabledByDefault == true) {
                    enableNightLock()
                }
            }
            .catch { }
            .launchIn(viewModelScope)
    }

    /**
     * If the process was killed while the save dialog was open, the name was
     * persisted in [SavedStateHandle]. Re-emit [PlayerEvent.ShowSavePresetDialog]
     * only if audio is still active — there is no point re-prompting on an idle screen.
     */
    private fun checkPendingPresetSave() {
        val pendingName = savedStateHandle.get<String>(PRESET_NAME_KEY)
        if (!pendingName.isNullOrBlank()) {
            // Defer until the service has had a chance to report its state
            viewModelScope.launch {
                delay(300)
                val mix = audioServiceHolder.current?.audioState?.value?.activeMix
                if (mix != null && !mix.isEmpty) {
                    _events.trySend(PlayerEvent.ShowSavePresetDialog)
                } else {
                    // Clear stale keys — mix is gone, save dialog would be empty
                    savedStateHandle.remove<String>(PRESET_NAME_KEY)
                    savedStateHandle.remove<String>(PRESET_EMOJI_KEY)
                }
            }
        }
    }

    // CLEANUP

    override fun onCleared() {
        // Cancel all pending debounce jobs
        volumeJobs.values.forEach { it.cancel() }
        volumeJobs.clear()

        // Flush final volumes to DB using a fresh coroutine on the application scope
        // viewModelScope is cancelled at this point — use GlobalScope as last resort
        // but scope is limited to this single write only
        val layers     = uiState.value.mixer.layers
        val volumes    = _layerVolumes.value
        val masterVol  = _masterVolume.value

        if (layers.isNotEmpty() && volumes.isNotEmpty()) {
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val volMap = layers.associate { l ->
                        l.sound.id to (volumes[l.position] ?: l.volume)
                    }
                    volumeMemoryRepository.saveVolumes(volMap)
                }
            }
        }

        super.onCleared()
    }

    // PRIVATE HELPERS

    /**
     * Returns the current [AudioService] or emits [PlayerEvent.ShowServiceUnavailable]
     * and returns null. Call-site can use `?: return@launch` idiom.
     */
    private suspend fun gateService(): com.opxl.sleepslide.domain.service.AudioService? {
        val service = audioServiceHolder.current
        if (service == null) {
            _events.trySend(PlayerEvent.ShowServiceUnavailable)
        }
        return service
    }

    /**
     * Returns true if playback can proceed.
     * On first play, emits [PlayerEvent.RequestNotificationPermission] and returns false
     * so the caller defers until the permission result arrives.
     */
    private suspend fun gateNotificationPermission(): Boolean {
        val state = _notification.value
        return if (state == NotificationState.PermissionRequired) {
            _events.trySend(PlayerEvent.RequestNotificationPermission)
            false
        } else {
            true
        }
    }

    /**
     * Applies remembered per-sound volumes to the mix layers.
     * Fetches all volumes in one query (no N+1).
     */
    private suspend fun applyVolumeMemory(mix: Domain.SoundMix): Domain.SoundMix {
        val soundIds = mix.layers.map { it.sound.id }
        val remembered = runCatching {
            volumeMemoryRepository.getVolumesForSounds(soundIds)
        }.getOrElse { emptyMap() }

        return mix.copy(
            layers = mix.layers.map { layer ->
                layer.copy(volume = remembered[layer.sound.id] ?: layer.volume)
            }
        )
    }

    private suspend fun safeCloseSession(reason: Domain.StopReason) {
        val id = _sessionId.value ?: return
        safeCloseSessionById(id, reason)
        _sessionId.value    = null
        _sessionStart.value = null
    }

    private suspend fun safeCloseSessionById(id: Long, reason: Domain.StopReason) {
        runCatching { playHistoryRepository.closeSession(id, reason) }
        // Non-fatal — history is best-effort
    }

    private fun clearMixState() {
        _activePreset.value = null
        _isMixDirty.value   = false
        _saveState.value    = ActivePresetState.None
        viewModelScope.launch {
            volumeMutex.withLock {
                _layerVolumes.value = emptyMap()
            }
        }
        _nightLock.value      = NightLockState.Disabled
        _nightLockUserSet.value = false
        savedStateHandle.remove<Boolean>(NIGHT_LOCK_USER_KEY)
    }

    private fun scheduleVolumeWrite(key: String, block: suspend () -> Unit) {
        volumeJobs[key]?.cancel()
        volumeJobs[key] = viewModelScope.launch {
            delay(VOLUME_DEBOUNCE_MS)
            block()
        }
    }

    private fun Long.toTimerLabel(): String {
        val totalSeconds = this / 1_000L
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s)
        else "%02d:%02d".format(m, s)
    }

    //  Combine carriers

    private data class PlayerPartialA(
        val playback  : PlayerPlaybackState,
        val mixer     : MixerState,
        val timer     : PlayerTimerState,
        val preset    : ActivePresetState,
        val nightLock : NightLockState,
    )

    private data class PlayerPartialB(
        val session     : SessionState,
        val bluetooth   : Boolean,
        val tier        : Domain.EntitlementTier,
        val notification: NotificationState,
    )
}