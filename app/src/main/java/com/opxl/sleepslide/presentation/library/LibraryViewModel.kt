package com.opxl.sleepslide.presentation.library


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opxl.sleepslide.data.AudioServiceHolder
import com.opxl.sleepslide.data.repository.MixSerializer
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.observer.AudioStateObserver
import com.opxl.sleepslide.domain.observer.EntitlementObserver
import com.opxl.sleepslide.domain.repository.PresetRepository
import com.opxl.sleepslide.domain.repository.SoundRepository
import com.opxl.sleepslide.domain.repository.UserPreferencesRepository
import com.opxl.sleepslide.domain.repository.VolumeMemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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

private const val SHARING_STOP_TIMEOUT_MS   = 5_000L
private const val SEARCH_DEBOUNCE_MS        = 200L
private const val VOLUME_DEBOUNCE_MS        = 400L
private const val MAX_LAYERS                = 3
private const val PREVIEW_AUTO_STOP_MS      = 10_000L

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val soundRepository: SoundRepository,
    private val volumeMemoryRepository: VolumeMemoryRepository,
    private val presetRepository: PresetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val audioServiceHolder: AudioServiceHolder,
    private val audioStateObserver: AudioStateObserver,
    private val entitlementObserver: EntitlementObserver,
    private val mixSerializer: MixSerializer,
) : ViewModel() {


    private val _events = Channel<LibraryVMState.LibraryEvent>(Channel.BUFFERED)
    val events: Flow<LibraryVMState.LibraryEvent> = _events.receiveAsFlow()


    // Selected category tab — null means "All"
    private val _selectedCategory = MutableStateFlow<Domain.SoundCategory?>(null)

    // Search
    private val _searchQuery = MutableStateFlow("")

    // Local mix builder state — source of truth for the bottom sheet
    // Seeded from AudioService on first composition, then owned by this ViewModel
    private val _layers      = MutableStateFlow<List<LibraryVMState.MixLayerState>>(emptyList())
    private val _masterVolume = MutableStateFlow(1.0f)
    private val _isDirty     = MutableStateFlow(false)

    // Tracks which position is being dragged — suppresses remote echoes
    private val _dragging    = MutableStateFlow<Set<Int>>(emptySet())
    private val volumeMutex  = Mutex()

    // Preview state
    private val _preview     = MutableStateFlow<LibraryVMState.PreviewState>(LibraryVMState.PreviewState.None)

    // Save state
    private val _saveState   = MutableStateFlow<LibraryVMState.SavePresetState>(LibraryVMState.SavePresetState.Idle)

    // Volume debounce jobs
    private val volumeJobs   = mutableMapOf<String, kotlinx.coroutines.Job>()


    val uiState: StateFlow<LibraryVMState.LibraryUiState> = buildUiState()
        .catch { e ->
            emit(
                LibraryVMState.LibraryUiState(
                    catalogue = LibraryVMState.CatalogueState.Error(
                        e.message ?: "Could not load sounds"
                    )
                )
            )
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(SHARING_STOP_TIMEOUT_MS),
            initialValue = LibraryVMState.LibraryUiState(),
        )


    init {
        syncMixFromService()
        observeServiceRebind()
    }

    // CATALOGUE COMMANDS

    fun selectCategory(category: Domain.SoundCategory?) {
        _selectedCategory.value = category
        // Clear search when switching tabs — avoids confusing filtered + tab state
        if (_searchQuery.value.isNotEmpty()) _searchQuery.value = ""
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        // Reset to "All" tab when searching — search crosses all categories
        if (query.isNotEmpty()) _selectedCategory.value = null
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    // MIX BUILDER COMMANDS

    /**
     * Primary action when user taps a sound card.
     *
     * Decision tree:
     * 1. Premium locked → emit upgrade prompt
     * 2. Already in mix → remove it
     * 3. Mix is full → emit max layers event
     * 4. Otherwise → add it, applying volume memory
     */
    fun onSoundTapped(sound: Domain.Sound) {
        viewModelScope.launch {
            val tier = entitlementObserver.entitlement.value.tier
            if (sound.isPremium && tier == Domain.EntitlementTier.FREE) {
                _events.trySend(LibraryVMState.LibraryEvent.ShowUpgradePrompt(sound.title))
                return@launch
            }

            val current = _layers.value
            val existingPosition = current.indexOfFirst { it.sound.id == sound.id }
                .takeIf { it >= 0 }

            if (existingPosition != null) {
                // Sound already in mix — remove it
                removeFromMix(existingPosition)
                return@launch
            }

            if (current.size >= MAX_LAYERS) {
                _events.trySend(LibraryVMState.LibraryEvent.ShowMaxLayersReached)
                return@launch
            }

            addToMix(sound)
        }
    }

    fun onSoundLongPressed(sound: Domain.Sound) {
        viewModelScope.launch {
            val tier = entitlementObserver.entitlement.value.tier
            if (sound.isPremium && tier == Domain.EntitlementTier.FREE) {
                _events.trySend(LibraryVMState.LibraryEvent.ShowUpgradePrompt(sound.title))
                return@launch
            }
            // Long press previews the sound solo — does not add to mix
            startPreview(sound)
        }
    }

    private suspend fun addToMix(sound: Domain.Sound) {
        val current = _layers.value
        val position = (0 until MAX_LAYERS).first { pos ->
            current.none { it.position == pos }
        }

        val volume = runCatching {
            volumeMemoryRepository.getVolume(sound.id)
        }.getOrDefault(1.0f)

        val newLayer = LibraryVMState.MixLayerState(
            position  = position,
            sound     = sound,
            volume    = volume,
            isMuted   = false,
            isDragging = false,
        )

        _layers.update { it + newLayer }
        _isDirty.value = true

        val service = audioServiceHolder.current
        if (service != null) {
            runCatching {
                service.addLayer(
                    Domain.SoundLayer(sound = sound, volume = volume, position = position)
                )
            }.onFailure { e ->
                // Rollback optimistic update
                _layers.update { layers -> layers.filter { it.position != position } }
                _events.trySend(LibraryVMState.LibraryEvent.ShowError(e.message ?: "Could not add ${sound.title}"))
            }
        } else {
            // Service not yet bound — build ephemeral mix locally, play when service arrives
            _events.trySend(LibraryVMState.LibraryEvent.ShowServiceUnavailable)
        }
    }

    private suspend fun removeFromMix(position: Int) {
        _layers.update { layers -> layers.filter { it.position != position } }
        _isDirty.value = _layers.value.isNotEmpty()

        val service = audioServiceHolder.current ?: return
        runCatching { service.removeLayer(position) }
            .onFailure { e ->
                _events.trySend(LibraryVMState.LibraryEvent.ShowError(e.message ?: "Could not remove sound"))
            }
    }

    fun onLayerVolumeChanged(position: Int, volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        viewModelScope.launch {
            volumeMutex.withLock {
                _layers.update { layers ->
                    layers.map { if (it.position == position) it.copy(volume = clamped) else it }
                }
                _dragging.update { it + position }
            }
        }
        scheduleVolumeWrite("layer_$position") {
            runCatching {
                audioServiceHolder.current?.setLayerVolume(position, clamped)
                _layers.value.find { it.position == position }?.let { layer ->
                    volumeMemoryRepository.saveVolume(layer.sound.id, clamped)
                }
            }.onFailure {
                _events.trySend(LibraryVMState.LibraryEvent.ShowError("Volume update failed"))
            }
            volumeMutex.withLock { _dragging.update { it - position } }
        }
    }

    fun onLayerVolumeDragEnded(position: Int) {
        volumeJobs["layer_$position"]?.cancel()
        viewModelScope.launch {
            val layer = volumeMutex.withLock {
                _dragging.update { it - position }
                _layers.value.find { it.position == position }
            } ?: return@launch
            runCatching {
                audioServiceHolder.current?.setLayerVolume(position, layer.volume)
                volumeMemoryRepository.saveVolume(layer.sound.id, layer.volume)
            }.onFailure {
                _events.trySend(LibraryVMState.LibraryEvent.ShowError("Could not save volume"))
            }
        }
    }

    fun onMasterVolumeChanged(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _masterVolume.value = clamped
        scheduleVolumeWrite("master") {
            runCatching { audioServiceHolder.current?.setMasterVolume(clamped) }
                .onFailure { _events.trySend(LibraryVMState.LibraryEvent.ShowError("Master volume update failed")) }
        }
    }

    fun onMasterVolumeDragEnded() {
        volumeJobs["master"]?.cancel()
        viewModelScope.launch {
            runCatching { audioServiceHolder.current?.setMasterVolume(_masterVolume.value) }
                .onFailure { _events.trySend(LibraryVMState.LibraryEvent.ShowError("Could not save master volume")) }
        }
    }

    fun muteLayer(position: Int) {
        viewModelScope.launch {
            _layers.update { layers ->
                layers.map { if (it.position == position) it.copy(isMuted = true) else it }
            }
            runCatching { audioServiceHolder.current?.muteLayer(position) }
                .onFailure { _events.trySend(LibraryVMState.LibraryEvent.ShowError("Could not mute")) }
        }
    }

    fun unmuteLayer(position: Int) {
        viewModelScope.launch {
            _layers.update { layers ->
                layers.map { if (it.position == position) it.copy(isMuted = false) else it }
            }
            runCatching { audioServiceHolder.current?.unmuteLayer(position) }
                .onFailure { _events.trySend(LibraryVMState.LibraryEvent.ShowError("Could not unmute")) }
        }
    }

    // PLAYBACK COMMANDS

    fun playCurrentMix() {
        viewModelScope.launch {
            val service = audioServiceHolder.current ?: run {
                _events.trySend(LibraryVMState.LibraryEvent.ShowServiceUnavailable)
                return@launch
            }
            val layers = _layers.value
            if (layers.isEmpty()) {
                _events.trySend(LibraryVMState.LibraryEvent.ShowInfo("Add at least one sound to play"))
                return@launch
            }
            val mix = buildMix(layers, _masterVolume.value)
            runCatching { service.play(mix) }
                .onSuccess {
                    soundRepository.recordPlayed(layers.map { it.sound.id }.first())
                    userPreferencesRepository.setLastPlayedEphemeralMix(
                        mixSerializer.serialize(mix)
                    )
                    _events.trySend(LibraryVMState.LibraryEvent.NavigateToPlayer)
                }
                .onFailure { e ->
                    _events.trySend(LibraryVMState.LibraryEvent.ShowError(e.message ?: "Could not start playback"))
                }
        }
    }

    fun pausePlayback() {
        viewModelScope.launch {
            runCatching { audioServiceHolder.current?.pause() }
                .onFailure { _events.trySend(LibraryVMState.LibraryEvent.ShowError("Could not pause")) }
        }
    }

    fun resumePlayback() {
        viewModelScope.launch {
            val service = audioServiceHolder.current ?: run {
                _events.trySend(LibraryVMState.LibraryEvent.ShowServiceUnavailable)
                return@launch
            }
            runCatching { service.resume() }
                .onFailure { _events.trySend(LibraryVMState.LibraryEvent.ShowError("Could not resume")) }
        }
    }

    // PREVIEW COMMANDS

    /**
     * Previews a sound solo for [PREVIEW_AUTO_STOP_MS] ms.
     * Preview does not disturb the active mix — it uses a temporary single-layer
     * play that auto-stops. If a mix is active, it pauses during preview and
     * resumes after.
     */
    private fun startPreview(sound: Domain.Sound) {
        viewModelScope.launch {
            val service = audioServiceHolder.current ?: return@launch
            val wasPlaying = audioStateObserver.audioState.value
                .playbackStatus == Domain.PlaybackStatus.PLAYING

            _preview.value = LibraryVMState.PreviewState.Previewing(sound.id)

            runCatching {
                val previewMix =Domain.SoundMix(
                    layers = listOf(Domain.SoundLayer(sound = sound, volume = 0.8f, position = 0)),
                    fadeInDurationMs = 500L,
                )
                if (wasPlaying) service.pause()
                service.play(previewMix)
                delay(PREVIEW_AUTO_STOP_MS)
            }

            stopPreview()
            if (wasPlaying) {
                runCatching { service.resume() }
            }
        }
    }

    fun stopPreview() {
        _preview.value = LibraryVMState.PreviewState.None
    }

    // SAVE PRESET COMMANDS

    fun requestSaveAsPreset() {
        viewModelScope.launch {
            if (_layers.value.isEmpty()) {
                _events.trySend(LibraryVMState.LibraryEvent.ShowInfo("Add sounds before saving a preset"))
                return@launch
            }
            _events.send(LibraryVMState.LibraryEvent.ShowSavePresetDialog)
        }
    }

    fun saveAsPreset(name: String, emoji: String?) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isBlank()) {
                _events.trySend(LibraryVMState.LibraryEvent.ShowError("Preset name cannot be empty"))
                return@launch
            }
            val layers = _layers.value
            if (layers.isEmpty()) {
                _events.trySend(LibraryVMState.LibraryEvent.ShowError("No sounds to save"))
                return@launch
            }

            _saveState.value = LibraryVMState.SavePresetState.Saving

            val mix = buildMix(layers, _masterVolume.value)
            val preset = Domain.Preset(name = trimmed, mix = mix, emoji = emoji)

            runCatching { presetRepository.save(preset) }
                .onSuccess { savedId ->
                    _saveState.value = LibraryVMState.SavePresetState.Success(trimmed)
                    _isDirty.value   = false
                    runCatching { userPreferencesRepository.setLastPlayedPreset(savedId) }
                    _events.trySend(LibraryVMState.LibraryEvent.PresetSaved(trimmed))
                }
                .onFailure { e ->
                    _saveState.value = LibraryVMState.SavePresetState.Error(e.message ?: "Could not save preset")
                    _events.trySend(LibraryVMState.LibraryEvent.ShowError(e.message ?: "Could not save preset"))
                }
        }
    }

    // STATE CONSTRUCTION

    private fun buildUiState(): Flow<LibraryVMState.LibraryUiState> {

        val allSoundsFlow = soundRepository.observeAll()
            .catch { emit(emptyList()) }
            .distinctUntilChanged()

        val recentFlow = soundRepository.observeRecentlyPlayed(8)
            .catch { emit(emptyList()) }
            .distinctUntilChanged()

        val mostPlayedFlow = soundRepository.observeMostPlayed(8)
            .catch { emit(emptyList()) }
            .distinctUntilChanged()

        val tierFlow = entitlementObserver.tier
            .catch { emit(Domain.EntitlementTier.FREE) }
            .distinctUntilChanged()

        val debouncedSearch = _searchQuery
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()

        val playbackFlow = audioServiceHolder.service
            .flatMapLatest { service ->
                if (service == null) return@flatMapLatest flowOf(LibraryVMState.LibraryPlaybackState.ServiceUnavailable)
                service.audioState.map { state ->
                    when (state.playbackStatus) {
                        Domain.PlaybackStatus.PLAYING,
                        Domain.PlaybackStatus.FADING_IN  ->LibraryVMState. LibraryPlaybackState.Playing(
                            isBackground    = state.isPlayingInBackground,
                            activeSoundIds  = state.activeMix?.layers
                                ?.map { it.sound.id }?.toSet() ?: emptySet(),
                        )
                        Domain.PlaybackStatus.PAUSED,
                        Domain.PlaybackStatus.FADING_OUT ->LibraryVMState. LibraryPlaybackState.Paused
                        else                      ->LibraryVMState. LibraryPlaybackState.Idle
                    }
                }
                    .catch { emit(LibraryVMState.LibraryPlaybackState.Idle) }
                    .distinctUntilChanged()
            }

        return combine(
            allSoundsFlow,
            recentFlow,
            mostPlayedFlow,
            tierFlow,
            debouncedSearch,
        ) { all, recent, mostPlayed, tier, query ->
            SoundData(all, recent, mostPlayed, tier, query)
        }.combine(
            combine(
                _selectedCategory,
                _layers,
                _dragging,
                _masterVolume,
                _isDirty,
            ) { cat, layers, dragging, master, dirty ->
                MixData(cat, layers, dragging, master, dirty)
            }
        ) { soundData, mixData ->
            Pair(soundData, mixData)
        }.combine(
            combine(playbackFlow, _preview, _saveState) { pb, preview, save ->
                AuxData(pb, preview, save)
            }
        ) { (soundData, mixData), aux ->
            buildState(soundData, mixData, aux)
        }
    }

    private fun buildState(
        sound: SoundData,
        mix: MixData,
        aux: AuxData,
    ): LibraryVMState.LibraryUiState {

        val activeSoundIds = mix.layers.map { it.sound.id }.toSet()

        fun soundItem(s: Domain.Sound) = LibraryVMState.SoundItemState(
            sound = s,
            isInActiveMix = s.id in activeSoundIds,
            isPreviewPlaying = aux.preview is LibraryVMState.PreviewState.Previewing &&
                    (aux.preview as LibraryVMState.PreviewState.Previewing).soundId == s.id,
            mixPosition = mix.layers.find { it.sound.id == s.id }?.position,
            downloadState = when {
                s.isBundled -> LibraryVMState.DownloadState.NotRequired
                s.downloadedPath != null -> LibraryVMState.DownloadState.Downloaded
                else -> LibraryVMState.DownloadState.NotDownloaded
            },
            isPremiumLocked = s.isPremium && sound.tier == Domain.EntitlementTier.FREE,
        )

        // Category tabs — Tinnitus first (Jeff's primary need), then alpha
        val categoryCounts = sound.all.groupBy { it.category }
        val tabs = buildList {
            add(LibraryVMState.CategoryTab(null, "All", sound.all.size))
            // Tinnitus pinned first regardless of sort
           Domain. SoundCategory.values()
                .sortedWith(compareBy { if (it == Domain.SoundCategory.TINNITUS) 0 else 1 })
                .forEach { cat ->
                    val count = categoryCounts[cat]?.size ?: 0
                    if (count > 0) add(LibraryVMState.CategoryTab(cat, cat.name.lowercase().replaceFirstChar { it.uppercase() }, count))
                }
        }

        val selectedTab = tabs.find { it.category == mix.selectedCategory } ?: tabs.first()

        // Displayed sounds — filter by search or category
        val displayed = when {
            sound.query.isNotBlank() -> sound.all.filter { s ->
                s.title.contains(sound.query, ignoreCase = true) ||
                        s.category.name.contains(sound.query, ignoreCase = true) ||
                        s.tags.any { tag -> tag.contains(sound.query, ignoreCase = true) }
            }
            mix.selectedCategory != null -> sound.all.filter { it.category == mix.selectedCategory }
            else -> sound.all
        }.map { soundItem(it) }

        val catalogue = LibraryVMState.CatalogueState.Ready(
            tabs           = tabs,
            selectedTab    = selectedTab,
            displayedSounds = displayed,
            recentSounds   = sound.recent.map { soundItem(it) },
            mostPlayed     = sound.mostPlayed.map { soundItem(it) },
            showShelves    = sound.query.isBlank() && mix.selectedCategory == null,
        )

        // Mix builder
        val mixBuilder = if (mix.layers.isEmpty()) {
            LibraryVMState.MixBuilderState.Empty
        } else {
            LibraryVMState.MixBuilderState.Active(
                layers      = mix.layers.map { layer ->
                    layer.copy(isDragging = layer.position in mix.dragging)
                },
                masterVolume = mix.masterVolume,
                canAddMore   = mix.layers.size < MAX_LAYERS,
                isDirty      = mix.isDirty,
            )
        }

        return LibraryVMState.LibraryUiState(
            catalogue      = catalogue,
            activeMix      = mixBuilder,
            playback       = aux.playback,
            filter         = LibraryVMState.FilterState(sound.query, sound.query.isNotBlank()),
            preview        = aux.preview,
            entitlementTier = sound.tier,
            savePreset     = aux.saveState,
        )
    }


    // SERVICE SYNC

    /**
     * On first entry to the library, seeds the local mix builder from the
     * AudioService's active mix so the builder matches what is playing.
     * Only seeds if the local builder is empty — user changes are preserved.
     */
    private fun syncMixFromService() {
        viewModelScope.launch {
            if (_layers.value.isNotEmpty()) return@launch
            val activeMix = audioServiceHolder.current?.audioState?.value?.activeMix
                ?: return@launch
            val soundIds = activeMix.layers.map { it.sound.id }
            val volumes  = runCatching {
                volumeMemoryRepository.getVolumesForSounds(soundIds)
            }.getOrElse { emptyMap() }

            _layers.value = activeMix.layers.map { layer ->
                LibraryVMState.MixLayerState(
                    position  = layer.position,
                    sound     = layer.sound,
                    volume    = volumes[layer.sound.id] ?: layer.volume,
                    isMuted   = layer.isMuted,
                    isDragging = false,
                )
            }
            _masterVolume.value = activeMix.masterVolume
            _isDirty.value = false
        }
    }

    /**
     * When the service rebinds after process death, re-sync the local builder
     * so sliders reflect the actual playing state.
     */
    private fun observeServiceRebind() {
        audioServiceHolder.service
            .distinctUntilChanged { old, new -> (old == null) == (new == null) }
            .onEach { service ->
                if (service != null && _layers.value.isEmpty()) {
                    syncMixFromService()
                }
            }
            .catch { }
            .launchIn(viewModelScope)
    }


    // CLEANUP


    override fun onCleared() {
        volumeJobs.values.forEach { it.cancel() }
        volumeJobs.clear()

        // Flush final volumes before ViewModel dies
        val layers = _layers.value
        if (layers.isNotEmpty()) {
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val volMap = layers.associate { it.sound.id to it.volume }
                    volumeMemoryRepository.saveVolumes(volMap)
                }
            }
        }
        super.onCleared()
    }


    // HELPERS


    private fun buildMix(layers: List<LibraryVMState.MixLayerState>, masterVolume: Float): Domain.SoundMix =
        Domain.SoundMix(
            layers       = layers.map { l ->
                Domain.SoundLayer(sound = l.sound, volume = l.volume, isMuted = l.isMuted, position = l.position)
            },
            masterVolume = masterVolume,
        )

    private fun scheduleVolumeWrite(key: String, block: suspend () -> Unit) {
        volumeJobs[key]?.cancel()
        volumeJobs[key] = viewModelScope.launch {
            delay(VOLUME_DEBOUNCE_MS)
            block()
        }
    }



    private data class SoundData(
        val all: List<Domain.Sound>,
        val recent: List<Domain.Sound>,
        val mostPlayed: List<Domain.Sound>,
        val tier: Domain.EntitlementTier,
        val query: String,
    )

    private data class MixData(
        val selectedCategory: Domain.SoundCategory?,
        val layers: List<LibraryVMState.MixLayerState>,
        val dragging: Set<Int>,
        val masterVolume: Float,
        val isDirty: Boolean,
    )
    private val MixData.selectedCategory get() = this.selectedCategory


    private data class AuxData(
        val playback:LibraryVMState.LibraryPlaybackState,
        val preview: LibraryVMState.PreviewState,
        val saveState: LibraryVMState.SavePresetState,
    )
}

// Extension on MixData for selectedCategory access in buildState
