package com.opxl.sleepslide.presentation.library

import com.opxl.sleepslide.domain.model.Domain


object LibraryVMState{

data class LibraryUiState(
    val catalogue: CatalogueState           = CatalogueState.Loading,
    val activeMix: MixBuilderState          = MixBuilderState.Empty,
    val playback: LibraryPlaybackState      = LibraryPlaybackState.Idle,
    val filter: FilterState                 = FilterState(),
    val preview: PreviewState               = PreviewState.None,
    val entitlementTier: Domain.EntitlementTier = Domain.EntitlementTier.FREE,
    val savePreset: SavePresetState         = SavePresetState.Idle,
)


sealed interface CatalogueState {
    data object Loading : CatalogueState
    data class Ready(
        val tabs: List<CategoryTab>,
        val selectedTab: CategoryTab,
        val displayedSounds: List<SoundItemState>,
        val recentSounds: List<SoundItemState>,
        val mostPlayed: List<SoundItemState>,
        val showShelves: Boolean,          // false when search is active
    ) : CatalogueState
    data class Error(val message: String) : CatalogueState
}

data class CategoryTab(
    val category: Domain.SoundCategory?,          // null = "All"
    val label: String,
    val count: Int,
)

data class SoundItemState(
    val sound: Domain.Sound,
    val isInActiveMix: Boolean,
    val isPreviewPlaying: Boolean,
    val mixPosition: Int?,                 // which layer slot this sound occupies, or null
    val downloadState: DownloadState,
    val isPremiumLocked: Boolean,          // premium sound + free tier
)

sealed interface DownloadState {
    data object NotRequired : DownloadState       // bundled in APK
    data object NotDownloaded : DownloadState
    data class Downloading(val progress: Float) : DownloadState
    data object Downloaded : DownloadState
}


sealed interface MixBuilderState {
    data object Empty : MixBuilderState
    data class Active(
        val layers: List<MixLayerState>,
        val masterVolume: Float,
        val canAddMore: Boolean,
        val isDirty: Boolean,              // modified since last save / since arriving from player
    ) : MixBuilderState
}

data class MixLayerState(
    val position: Int,
    val sound: Domain.Sound,
    val volume: Float,
    val isMuted: Boolean,
    val isDragging: Boolean,
)


sealed interface LibraryPlaybackState {
    data object Idle : LibraryPlaybackState
    data object ServiceUnavailable : LibraryPlaybackState
    data class Playing(
        val isBackground: Boolean,
        val activeSoundIds: Set<String>,
    ) : LibraryPlaybackState
    data object Paused : LibraryPlaybackState
}


data class FilterState(
    val searchQuery: String  = "",
    val isSearchActive: Boolean = false,
)


sealed interface PreviewState {
    data object None : PreviewState
    data class Previewing(val soundId: String) : PreviewState
}


sealed interface SavePresetState {
    data object Idle : SavePresetState
    data object Saving : SavePresetState
    data class Success(val presetName: String) : SavePresetState
    data class Error(val message: String) : SavePresetState
}


sealed interface LibraryEvent {
    data object NavigateToPlayer : LibraryEvent
    data object ShowSavePresetDialog : LibraryEvent
    data class ShowUpgradePrompt(val soundTitle: String) : LibraryEvent
    data object ShowMaxLayersReached : LibraryEvent
    data class ShowError(val message: String) : LibraryEvent
    data class ShowInfo(val message: String) : LibraryEvent
    data object ShowServiceUnavailable : LibraryEvent
    data class PresetSaved(val name: String) : LibraryEvent
    data class SoundAlreadyInMix(val soundTitle: String) : LibraryEvent
}}