package com.opxl.sleepslide.presentation.player

import com.opxl.sleepslide.domain.model.Domain


data class PlayerUiState(
    val playback: PlayerPlaybackState       = PlayerPlaybackState.ServiceUnavailable,
    val mixer: MixerState                   = MixerState(),
    val timer: PlayerTimerState             = PlayerTimerState.Idle,
    val preset: ActivePresetState           = ActivePresetState.None,
    val nightLock: NightLockState           = NightLockState.Disabled,
    val session: SessionState               = SessionState.Idle,
    val notification: NotificationState     = NotificationState.Idle,
    val entitlementTier: Domain.EntitlementTier = Domain.EntitlementTier.FREE,
    val isBluetoothConnected: Boolean       = false,
)


sealed interface PlayerPlaybackState {
    data object ServiceUnavailable : PlayerPlaybackState
    data object Idle : PlayerPlaybackState
    data object Loading : PlayerPlaybackState
    data class Playing(
        val fadeInProgress: Float?,
        val isPlayingInBackground: Boolean,
        val audioFocusStatus: Domain.AudioFocusStatus,
    ) : PlayerPlaybackState
    data object Paused : PlayerPlaybackState
    data object FadingIn : PlayerPlaybackState
    data class FadingOut(val triggeredBy: FadeOutTrigger) : PlayerPlaybackState
    data class Interrupted(val reason: InterruptionReason) : PlayerPlaybackState
    data class Error(val message: String) : PlayerPlaybackState
}

enum class FadeOutTrigger { TIMER, USER }

enum class InterruptionReason {
    PHONE_CALL,
    ANOTHER_APP,
    BLUETOOTH_LOST,
}


data class MixerState(
    val layers: List<LayerUiState>      = emptyList(),
    val masterVolume: Float             = 1.0f,
    val hasUnsavedChanges: Boolean      = false,
    val canAddLayer: Boolean            = false,
    val availableSlots: List<Int>       = listOf(0, 1, 2),
)

data class LayerUiState(
    val position: Int,
    val sound: Domain.Sound,
    val volume: Float,
    val isMuted: Boolean,
    val isVolumeBeingDragged: Boolean   = false,
)


sealed interface PlayerTimerState {
    data object Idle : PlayerTimerState
    data class Active(
        val status: Domain.TimerStatus,
        val remainingMs: Long,
        val remainingLabel: String,
        val totalDurationMs: Long,
        val progressFraction: Float,
        val fadeBeforeEndMs: Long,
        val isFading: Boolean,
    ) : PlayerTimerState
    data object Finished : PlayerTimerState
}

val TIMER_PRESETS_MS = listOf(
    15 * 60 * 1000L,
    30 * 60 * 1000L,
    45 * 60 * 1000L,
    60 * 60 * 1000L,
    90 * 60 * 1000L,
    120 * 60 * 1000L,
)


sealed interface ActivePresetState {
    data object None : ActivePresetState
    data class Loaded(
        val preset: Domain.Preset,
        val isDirty: Boolean,
    ) : ActivePresetState
    data class Saving(val name: String) : ActivePresetState
    data class SaveError(val message: String) : ActivePresetState
}


sealed interface NightLockState {
    data object Disabled : NightLockState
    data object Enabled : NightLockState
    data object Unlocking : NightLockState
}


sealed interface SessionState {
    data object Idle : SessionState
    data class Active(val sessionId: Long, val startedAt: Long) : SessionState
    data class Closed(val durationMs: Long) : SessionState
}


sealed interface NotificationState {
    data object Idle : NotificationState
    data object PermissionRequired : NotificationState
    data object Granted : NotificationState
}


sealed interface PlayerEvent {
    data object NavigateBack : PlayerEvent
    data object NavigateToLibrary : PlayerEvent

    data object ShowSavePresetDialog : PlayerEvent
    data class PresetSaved(val preset: Domain.Preset) : PlayerEvent
    data class PresetUpdated(val preset:  Domain.Preset) : PlayerEvent

    data object ShowUnsavedChangesDialog : PlayerEvent

    data object ShowTimerPicker : PlayerEvent
    data object TimerStarted : PlayerEvent
    data object TimerCancelled : PlayerEvent
    data object TimerFinished : PlayerEvent

    data object NightLockEnabled : PlayerEvent
    data object NightLockDisabled : PlayerEvent

    data class ShowError(val message: String) : PlayerEvent
    data class ShowInfo(val message: String) : PlayerEvent
    data object ShowServiceUnavailable : PlayerEvent
    data object RequestNotificationPermission : PlayerEvent
    data class AudioInterrupted(val reason: InterruptionReason) : PlayerEvent
    data object AudioRestoredAfterCall : PlayerEvent
}