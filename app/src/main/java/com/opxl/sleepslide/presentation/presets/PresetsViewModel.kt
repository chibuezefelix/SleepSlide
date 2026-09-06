package com.opxl.sleepslide.presentation.presets

import com.opxl.sleepslide.domain.model.Domain



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opxl.sleepslide.domain.observer.AudioStateObserver
import com.opxl.sleepslide.domain.observer.EntitlementObserver
import com.opxl.sleepslide.domain.repository.PlayHistoryRepository
import com.opxl.sleepslide.domain.repository.PresetRepository
import com.opxl.sleepslide.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import com.opxl.sleepslide.data.AudioServiceHolder
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

private const val SHARING_STOP_TIMEOUT_MS = 5_000L
private const val SEARCH_DEBOUNCE_MS      = 200L
private const val REORDER_DEBOUNCE_MS     = 600L

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class PresetsViewModel @Inject constructor(
    private val presetRepository: PresetRepository,
    private val playHistoryRepository: PlayHistoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val audioServiceHolder: AudioServiceHolder,
    private val audioStateObserver: AudioStateObserver,
    private val entitlementObserver: EntitlementObserver,
) : ViewModel() {

    //  Events

    private val _events = Channel<PresetsEvent>(Channel.BUFFERED)
    val events: Flow<PresetsEvent> = _events.receiveAsFlow()

    //  Internal state
    private val _searchQuery   = MutableStateFlow("")
    private val _selectionMode = MutableStateFlow<SelectionMode>(SelectionMode.None)
    private val _reorderMode   = MutableStateFlow(false)

    // Pending drag order — null means no active drag.
    // Written optimistically on every drag frame; committed to DB on drag end
    // or after REORDER_DEBOUNCE_MS of inactivity.
    private val _pendingOrder = MutableStateFlow<List<Long>?>(null)

    // Play stats loaded on demand — combined into uiState so Compose reacts
    private val _playStats = MutableStateFlow<Map<Long, Long>>(emptyMap())

    // Mutex guards deletedPreset — confirmDelete (coroutine A) writes it,
    // undoDelete (coroutine B) reads and clears it; without a lock they race.
    private val deleteMutex  = Mutex()
    private var deletedPreset: Domain.Preset? = null

    //  Primary UI state

    val uiState: StateFlow<PresetsUiState> = buildUiState()
        .catch { e ->
            emit(
                PresetsUiState(
                    screen = PresetsScreenState.Error(
                        e.message ?: "Could not load presets"
                    )
                )
            )
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(SHARING_STOP_TIMEOUT_MS),
            initialValue = PresetsUiState(),
        )

    //  Init

    init {
        observeReorderDebounce()
    }

    // PLAYBACK

    fun launchPreset(preset: Domain.Preset) {
        viewModelScope.launch {
            val service = audioServiceHolder.current ?: run {
                _events.trySend(PresetsEvent.ShowServiceUnavailable)
                return@launch
            }
            runCatching {
                val isPlaying = audioStateObserver.audioState.value
                    .playbackStatus == Domain.PlaybackStatus.PLAYING
                if (isPlaying) service.crossfadeTo(preset.mix) else service.play(preset.mix)
                presetRepository.recordUsed(preset.id)
                userPreferencesRepository.setLastPlayedPreset(preset.id)
                _events.trySend(PresetsEvent.NavigateToPlayer(preset.id))
            }.onFailure { e ->
                _events.trySend(PresetsEvent.ShowError(
                    e.message ?: "Could not launch ${preset.name}"
                ))
            }
        }
    }

    // PRESET MUTATIONS

    fun requestRename(preset: Domain.Preset) {
        viewModelScope.launch { _events.send(PresetsEvent.ShowRenameDialog(preset)) }
    }

    fun rename(presetId: Long, newName: String) {
        viewModelScope.launch {
            val trimmed = newName.trim()
            if (trimmed.isBlank()) {
                _events.trySend(PresetsEvent.ShowError("Name cannot be empty"))
                return@launch
            }
            runCatching { presetRepository.rename(presetId, trimmed) }
                .onFailure { e ->
                    _events.trySend(
                        PresetsEvent.ShowError(e.message ?: "Could not rename preset")
                    )
                }
        }
    }

    fun setEmoji(presetId: Long, emoji: String?) {
        viewModelScope.launch {
            runCatching { presetRepository.setEmoji(presetId, emoji) }
                .onFailure { e ->
                    _events.trySend(
                        PresetsEvent.ShowError(e.message ?: "Could not update emoji")
                    )
                }
        }
    }

    fun pin(presetId: Long) {
        viewModelScope.launch {
            runCatching { presetRepository.pin(presetId) }
                .onFailure { _events.trySend(PresetsEvent.ShowError("Could not pin preset")) }
        }
    }

    fun unpin(presetId: Long) {
        viewModelScope.launch {
            runCatching { presetRepository.unpin(presetId) }
                .onFailure { _events.trySend(PresetsEvent.ShowError("Could not unpin preset")) }
        }
    }

    //  Delete — single

    fun requestDelete(preset: Domain.Preset) {
        viewModelScope.launch {
            _events.send(PresetsEvent.ShowDeleteConfirmation(preset.id, preset.name))
        }
    }

    fun confirmDelete(presetId: Long) {
        viewModelScope.launch {
            // Read and stage the preset BEFORE deleting so undo has the full object
            val preset = runCatching { presetRepository.getById(presetId) }.getOrNull()

            runCatching { presetRepository.delete(presetId) }
                .onSuccess {
                    deleteMutex.withLock { deletedPreset = preset }

                    // Clear last-played reference if it pointed to the deleted preset
                    runCatching {
                        if (userPreferencesRepository.get().lastPlayedPresetId == presetId) {
                            userPreferencesRepository.clearLastPlayed()
                        }
                    }

                    _events.trySend(PresetsEvent.PresetDeleted(preset?.name ?: "Preset"))
                }
                .onFailure { e ->
                    _events.trySend(
                        PresetsEvent.ShowError(e.message ?: "Could not delete preset")
                    )
                }
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            val preset = deleteMutex.withLock {
                deletedPreset.also { deletedPreset = null }
            } ?: run {
                _events.trySend(PresetsEvent.ShowError("Nothing to undo"))
                return@launch
            }
            runCatching { presetRepository.save(preset.copy(id = 0L)) }
                .onSuccess { newId ->
                    _events.trySend(PresetsEvent.UndoDelete(preset.copy(id = newId)))
                }
                .onFailure { e ->
                    // Restore the staged preset so the user can retry undo
                    deleteMutex.withLock { deletedPreset = preset }
                    _events.trySend(
                        PresetsEvent.ShowError(e.message ?: "Could not restore preset")
                    )
                }
        }
    }

    // Delete — bulk

    fun requestBulkDelete() {
        val ids = (_selectionMode.value as? SelectionMode.Active)?.selectedIds
            ?: return
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _events.send(PresetsEvent.ShowBulkDeleteConfirmation(ids.size))
        }
    }

    fun confirmBulkDelete() {
        viewModelScope.launch {
            val ids = (_selectionMode.value as? SelectionMode.Active)?.selectedIds
                ?: return@launch
            if (ids.isEmpty()) return@launch

            exitSelectionMode()

            // Delete all concurrently — partial failure is reported but other
            // deletes are not rolled back (user saw checkboxes, chose which to remove)
            val results = ids.map { id ->
                async { id to runCatching { presetRepository.delete(id) } }
            }.awaitAll()

            val succeeded = results.filter { it.second.isSuccess }.map { it.first }.toSet()
            val failCount = results.count { it.second.isFailure }

            // Clear last-played only if that preset was successfully deleted
            runCatching {
                val lastPlayedId = userPreferencesRepository.get().lastPlayedPresetId
                if (lastPlayedId != null && lastPlayedId in succeeded) {
                    userPreferencesRepository.clearLastPlayed()
                }
            }

            if (failCount > 0) {
                _events.trySend(
                    PresetsEvent.ShowError("$failCount preset${if (failCount > 1) "s" else ""} could not be deleted")
                )
            }
        }
    }

    // REORDER

    // Called on every drag frame — optimistic local update only
    fun onReorder(orderedIds: List<Long>) {
        _pendingOrder.value = orderedIds
    }

    // Called on drag end — flush immediately, bypassing the debounce
    fun onReorderCommitted(orderedIds: List<Long>) {
        _pendingOrder.value = orderedIds
        viewModelScope.launch {
            runCatching { presetRepository.reorder(orderedIds) }
                .onSuccess { _pendingOrder.value = null }
                .onFailure {
                    _events.trySend(PresetsEvent.ShowError("Could not save new order"))
                    _pendingOrder.value = null
                }
        }
    }

    fun enableReorderMode() {
        exitSelectionMode()
        _reorderMode.value = true
    }

    fun disableReorderMode() {
        _reorderMode.value = false
        // Flush any uncommitted pending order before clearing
        val pending = _pendingOrder.value
        if (pending != null) {
            viewModelScope.launch {
                runCatching { presetRepository.reorder(pending) }
                _pendingOrder.value = null
            }
        }
    }

    // SELECTION MODE

    fun enterSelectionMode(initialPresetId: Long) {
        _reorderMode.value = false
        _selectionMode.value = SelectionMode.Active(setOf(initialPresetId))
    }

    fun toggleSelection(presetId: Long) {
        val mode = _selectionMode.value as? SelectionMode.Active ?: return
        val updated = if (presetId in mode.selectedIds) {
            mode.selectedIds - presetId
        } else {
            mode.selectedIds + presetId
        }
        _selectionMode.value =
            if (updated.isEmpty()) SelectionMode.None else SelectionMode.Active(updated)
    }

    fun selectAll() {
        val screen = uiState.value.screen as? PresetsScreenState.Ready ?: return
        val allIds = (screen.pinned + screen.unpinned).map { it.preset.id }.toSet()
        _selectionMode.value = SelectionMode.Active(allIds)
    }

    fun exitSelectionMode() {
        _selectionMode.value = SelectionMode.None
    }

    // SEARCH

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }

    fun clearSearch() { _searchQuery.value = "" }

    // PLAY STATS — loaded on demand, combined into uiState

    // Called by the screen once the list is visible.
    // Uses awaitAll for concurrent DB fetches — N presets = 1 parallel batch.
    fun loadPlayStats(presetIds: List<Long>) {
        if (presetIds.isEmpty()) return
        viewModelScope.launch {
            val stats = presetIds
                .map { id -> async { id to runCatching { playHistoryRepository.getTotalPlayedMsForPreset(id) }.getOrDefault(0L) } }
                .awaitAll()
                .toMap()
            // Merge with existing stats so a partial reload doesn't wipe other entries
            _playStats.update { current -> current + stats }
        }
    }

    // NAVIGATION

    fun navigateToLibrary() {
        viewModelScope.launch {
            val state = uiState.value
            if (state.hasReachedLimit && state.entitlementTier == Domain.EntitlementTier.FREE) {
                _events.send(PresetsEvent.ShowPresetLimitReached(FREE_PRESET_LIMIT))
            } else {
                _events.send(PresetsEvent.NavigateToLibrary)
            }
        }
    }

    // STATE CONSTRUCTION

    private fun buildUiState(): Flow<PresetsUiState> {

        val allPresetsFlow = presetRepository.observeAll()
            .catch { emit(emptyList()) }
            .distinctUntilChanged()

        val tierFlow = entitlementObserver.tier
            .catch { emit(Domain.EntitlementTier.FREE) }
            .distinctUntilChanged()

        val activePresetIdFlow = audioServiceHolder.service
            .flatMapLatest { service ->
                service?.audioState?.map { it.activePresetId } ?: flowOf(null)
            }
            .catch { emit(null) }
            .distinctUntilChanged()

        val debouncedSearchFlow = _searchQuery
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()

        return combine(
            allPresetsFlow,
            tierFlow,
            activePresetIdFlow,
            _playStats,
        ) { presets, tier, activeId, stats ->
            DataLayer(presets, tier, activeId, stats)
        }.combine(
            combine(
                debouncedSearchFlow,
                _selectionMode,
                _reorderMode,
                _pendingOrder,
            ) { query, selection, reorder, pending ->
                InteractionLayer(query, selection, reorder, pending)
            }
        ) { data, interaction ->
            buildState(data, interaction)
        }
    }

    private fun buildState(data: DataLayer, interaction: InteractionLayer): PresetsUiState {
        val selectedIds = (interaction.selectionMode as? SelectionMode.Active)?.selectedIds
            ?: emptySet()

        // Separate pinned and unpinned before applying pending order.
        // Pinned presets have their own stable position above all unpinned ones —
        // they are never in the same reorder pool as unpinned presets.
        val (pinnedPresets, unpinnedPresets) = data.presets.partition { it.isPinned }
        val orderedUnpinned = applyPendingOrder(unpinnedPresets, interaction.pendingOrder)

        fun itemState(preset: Domain.Preset): PresetItemState {
            val ms = data.stats[preset.id] ?: 0L
            return PresetItemState(
                preset             = preset,
                isActive           = preset.id == data.activePresetId,
                isSelected         = preset.id in selectedIds,
                totalListenedMs    = ms,
                totalListenedLabel = ms.toListenedLabel(),
                playCount          = 0, // populated via loadPlayStats if needed separately
            )
        }

        val pinnedItems   = pinnedPresets.map { itemState(it) }
        val unpinnedItems = orderedUnpinned.map { itemState(it) }

        val isFiltered = interaction.searchQuery.isNotBlank()
        val filteredItems = if (isFiltered) {
            (pinnedItems + unpinnedItems).filter { item ->
                item.preset.name.contains(interaction.searchQuery, ignoreCase = true) ||
                        item.preset.mix.layers.any { layer ->
                            layer.sound.title.contains(interaction.searchQuery, ignoreCase = true)
                        }
            }
        } else emptyList()

        val totalCount = data.presets.size
        val freeLimit  = data.tier == Domain.EntitlementTier.FREE && totalCount >= FREE_PRESET_LIMIT

        val screenState = when {
            totalCount == 0 && !isFiltered -> PresetsScreenState.Empty
            else -> PresetsScreenState.Ready(
                pinned     = pinnedItems,
                unpinned   = unpinnedItems,
                filtered   = filteredItems,
                isFiltered = isFiltered,
            )
        }

        return PresetsUiState(
            screen          = screenState,
            entitlementTier = data.tier,
            presetCount     = totalCount,
            freePresetLimit = FREE_PRESET_LIMIT,
            hasReachedLimit = freeLimit,
            activePresetId  = data.activePresetId,
            searchQuery     = interaction.searchQuery,
            selectionMode   = interaction.selectionMode,
            reorderMode     = interaction.reorderMode,
        )
    }

    // REORDER DEBOUNCE SAFETY NET

    // Fires REORDER_DEBOUNCE_MS after the last drag event.
    // Guards against the case where onReorderCommitted is never called
    // (gesture interrupted, device rotated, etc.).
    private fun observeReorderDebounce() {
        _pendingOrder
            .debounce(REORDER_DEBOUNCE_MS)
            .onEach { pending ->
                pending ?: return@onEach
                runCatching { presetRepository.reorder(pending) }
                    .onSuccess { _pendingOrder.value = null }
                    .onFailure { _events.trySend(PresetsEvent.ShowError("Could not save order")) }
            }
            .catch { }
            .launchIn(viewModelScope)
    }

    // CLEANUP

    override fun onCleared() {
        // If a pending order exists when the ViewModel is cleared (user
        // navigated away mid-drag), flush it synchronously as a best-effort.
        val pending = _pendingOrder.value
        if (pending != null) {
            viewModelScope.launch {
                runCatching { presetRepository.reorder(pending) }
            }
        }
        deleteMutex.tryLock().let { if (it) deletedPreset = null }
        super.onCleared()
    }

    // HELPERS`

    private fun applyPendingOrder(presets: List<Domain.Preset>, pendingIds: List<Long>?): List<Domain.Preset> {
        if (pendingIds == null) return presets
        val presetIdSet = presets.map { it.id }.toSet()
        val pendingIdSet = pendingIds.toSet()
        // If sets don't match exactly (add/delete during drag) fall back to DB order
        if (presetIdSet != pendingIdSet) return presets
        val rankOf = pendingIds.withIndex().associate { (i, id) -> id to i }
        return presets.sortedBy { rankOf[it.id] ?: Int.MAX_VALUE }
    }

    private fun Long.toListenedLabel(): String {
        if (this <= 0L) return ""
        val h = this / 3_600_000L
        val m = (this % 3_600_000L) / 60_000L
        return when {
            h > 0  -> "${h}h ${m}m"
            m > 0  -> "${m}m"
            else   -> "<1m"
        }
    }


    private data class DataLayer(
        val presets: List<Domain.Preset>,
        val tier: Domain.EntitlementTier,
        val activePresetId: Long?,
        val stats: Map<Long, Long>,
    )

    private data class InteractionLayer(
        val searchQuery: String,
        val selectionMode: SelectionMode,
        val reorderMode: Boolean,
        val pendingOrder: List<Long>?,
    )
}