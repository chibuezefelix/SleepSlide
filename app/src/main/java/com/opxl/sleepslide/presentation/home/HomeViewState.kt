package com.opxl.sleepslide.presentation.home

import com.opxl.sleepslide.domain.model.Domain

object  HomeView {
    data class HomeUiState(
        val screen:HomeView.ScreenState                 = HomeView.ScreenState.Loading,
        val playback:HomeView. PlaybackUiState           =HomeView. PlaybackUiState.Idle,
        val timer:HomeView.TimerUiState                 =HomeView. TimerUiState.Idle,
        val greeting: GreetingUiState           = GreetingUiState(),
        val bluetooth:HomeView. BluetoothUiState         =HomeView. BluetoothUiState.Unknown,
        val entitlementTier: Domain.EntitlementTier = Domain.EntitlementTier.FREE,
        val presetCount: Int                    = 0,
        val hasReachedFreePresetLimit: Boolean  = false,
    )

    sealed interface ScreenState {
        data object Loading : ScreenState
        data object Empty : ScreenState               // no presets, no history — first launch
        data class Ready(
            val resumeCard: ResumeCardState?,
            val pinnedPresets: List<Domain.Preset>,
            val recentPresets: List<Domain.Preset>,
            val recentSounds: List<Domain.Sound>,
            val totalListenedMs: Long,
        ) : ScreenState

        data class Error(val message: String) : ScreenState
    }


    sealed interface ResumeCardState {
        data class FromPreset(
            val preset: Domain.Preset,
            val lastUsedAt: Long,
        ) : ResumeCardState

        data class FromEphemeralMix(
            val mix: Domain.SoundMix,
            val lastPlayedAt: Long,
        ) : ResumeCardState
    }


    sealed interface PlaybackUiState {
        data object Idle : PlaybackUiState
        data object ServiceUnavailable : PlaybackUiState    // AudioServiceHolder.current == null
        data class Active(
            val status: Domain.PlaybackStatus,
            val activePresetId: Long?,
            val activePresetName: String?,
            val activeMix: Domain.SoundMix?,
            val audioFocusStatus: Domain.AudioFocusStatus,
            val fadeInProgress: Float?,
            val isNightLockEnabled: Boolean,
            val isPlayingInBackground: Boolean,
        ) : PlaybackUiState

        data class AudioFocusLost(val reason: Domain.AudioFocusLostReason) : PlaybackUiState
        data object Error : PlaybackUiState
    }


    sealed interface BluetoothUiState {
        data object Unknown : BluetoothUiState
        data object Connected : BluetoothUiState
        data object Disconnected : BluetoothUiState
    }


    sealed interface HomeEvent {
        data object NavigateToLibrary : HomeEvent
        data object NavigateToPlayer : HomeEvent
        data class NavigateToPreset(val presetId: Long) : HomeEvent
        data class ShowPresetLimitReached(val limit: Int) : HomeEvent
        data class ShowError(val message: String) : HomeEvent
        data object ShowServiceUnavailableSnackbar : HomeEvent
        data class PlaybackStarted(val presetName: String?) : HomeEvent
        data object AudioFocusRestoredAfterCall : HomeEvent
    }


    sealed interface TimerUiState {
        data object Idle : TimerUiState
        data class Active(
            val status: Domain.TimerStatus,
            val remainingMs: Long,
            val remainingLabel: String,     // pre-formatted e.g. "28:45"
            val totalDurationMs: Long,
            val progressFraction: Float,    // 0.0 → 1.0 (for progress ring)
            val isFading: Boolean,
        ) : TimerUiState

        data object Finished : TimerUiState
    }

    data class GreetingUiState(
        val headline: String    = "",
        val subtext: String     = "",
        val timeOfDay: TimeOfDay = TimeOfDay.EVENING,
    )

    enum class TimeOfDay { MORNING, AFTERNOON, EVENING, NIGHT }
}


