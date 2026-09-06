package com.opxl.sleepslide.presentation.presets

import com.opxl.sleepslide.domain.model.Domain


data class PresetsUiState(
    val screen: PresetsScreenState          = PresetsScreenState.Loading,
    val entitlementTier: Domain.EntitlementTier = Domain.EntitlementTier.FREE,
    val presetCount: Int                    = 0,
    val freePresetLimit: Int                = FREE_PRESET_LIMIT,
    val hasReachedLimit: Boolean            = false,
    val activePresetId: Long?               = null,
    val searchQuery: String                 = "",
    val selectionMode: SelectionMode        = SelectionMode.None,
    val reorderMode: Boolean                = false,
)

const val FREE_PRESET_LIMIT = 10

sealed interface PresetsScreenState {
    data object Loading : PresetsScreenState
    data object Empty : PresetsScreenState
    data class Ready(
        val pinned: List<PresetItemState>,
        val unpinned: List<PresetItemState>,
        val filtered: List<PresetItemState>,
        val isFiltered: Boolean,
    ) : PresetsScreenState
    data class Error(val message: String) : PresetsScreenState
}

data class PresetItemState(
    val preset: Domain.Preset,
    val isActive: Boolean,
    val isSelected: Boolean,
    val totalListenedMs: Long,
    val totalListenedLabel: String,
    val playCount: Int,
)

sealed interface SelectionMode {
    data object None : SelectionMode
    data class Active(val selectedIds: Set<Long>) : SelectionMode
}

sealed interface PresetsEvent {
    data class NavigateToPlayer(val presetId: Long) : PresetsEvent
    data object NavigateToLibrary : PresetsEvent

    data class ShowRenameDialog(val preset: Domain.Preset) : PresetsEvent
    data class ShowDeleteConfirmation(val presetId: Long, val presetName: String) : PresetsEvent
    data class ShowBulkDeleteConfirmation(val count: Int) : PresetsEvent

    data class PresetDeleted(val presetName: String) : PresetsEvent
    data class UndoDelete(val preset: Domain.Preset) : PresetsEvent
    data class ShowPresetLimitReached(val limit: Int) : PresetsEvent

    data class ShowError(val message: String) : PresetsEvent
    data class ShowInfo(val message: String) : PresetsEvent
    data object ShowServiceUnavailable : PresetsEvent
}