package com.opxl.sleepslide.presentation.settings

import com.opxl.sleepslide.domain.model.Domain


object SettingsVMState {

data class SettingsUiState(
    val account: AccountState               = AccountState.Loading,
    val playback: PlaybackSettingsState     = PlaybackSettingsState(),
    val appearance: AppearanceState         = AppearanceState(),
    val accessibility: AccessibilityState   = AccessibilityState(),
    val about: AboutState                   = AboutState(),
    val purchase: PurchaseOperationState    = PurchaseOperationState.Idle,
    val dataReset: DataResetState           = DataResetState.Idle,
)


sealed interface AccountState {
    data object Loading : AccountState
    data class Free(
        val canPurchase: Boolean,
    ) : AccountState
    data class Premium(
        val purchasedAt: Long?,
        val isRestored: Boolean,
    ) : AccountState
}

sealed interface PurchaseOperationState {
    data object Idle : PurchaseOperationState
    data object Purchasing : PurchaseOperationState
    data object Restoring : PurchaseOperationState
    data class PurchaseSuccess(val tier: Domain.EntitlementTier) : PurchaseOperationState
    data object NothingToRestore : PurchaseOperationState
    data class Error(val message: String) : PurchaseOperationState
}


data class PlaybackSettingsState(
    val defaultFadeInMs: Long               = 3_000L,
    val defaultFadeOutMs: Long              = 60_000L,
    val lastTimerDurationMs: Long           = 30 * 60 * 1_000L,
    val isNightLockDefault: Boolean         = false,
    val fadeInLabel: String                 = "3 seconds",
    val fadeOutLabel: String                = "1 minute",
    val timerLabel: String                  = "30 minutes",
)

// Slider steps for fade-in — human-readable
val FADE_IN_STEPS_MS = listOf(
    1_000L  to "1 second",
    2_000L  to "2 seconds",
    3_000L  to "3 seconds",
    5_000L  to "5 seconds",
    8_000L  to "8 seconds",
    10_000L to "10 seconds",
)

// Steps for fade-out / timer
val FADE_OUT_STEPS_MS = listOf(
    30_000L       to "30 seconds",
    60_000L       to "1 minute",
    90_000L       to "90 seconds",
    120_000L      to "2 minutes",
    300_000L      to "5 minutes",
)

val DEFAULT_TIMER_STEPS_MS = listOf(
    15 * 60_000L  to "15 minutes",
    30 * 60_000L  to "30 minutes",
    45 * 60_000L  to "45 minutes",
    60 * 60_000L  to "1 hour",
    90 * 60_000L  to "1.5 hours",
    120 * 60_000L to "2 hours",
)


data class AppearanceState(
    val themeMode: Domain.ThemeMode = Domain.ThemeMode.SYSTEM,
    val darkModeStartHour: Int      = 21,
    val darkModeEndHour: Int        = 7,
    val showScheduleOptions: Boolean = false,
)

data class AccessibilityState(
    val isHighContrastEnabled: Boolean          = false,
    val batteryOptStatus: BatteryOptStatus      = BatteryOptStatus.Unknown,
    val batteryOptPromptCount: Int              = 0,
    val hasDismissedBatteryPrompt: Boolean      = false,
)

sealed interface BatteryOptStatus {
    data object Unknown : BatteryOptStatus
    data object Granted : BatteryOptStatus
    data object NotGranted : BatteryOptStatus
}


data class AboutState(
    val appVersion: String          = "",
    val onboardingPath: Domain.OnboardingPath = Domain.OnboardingPath.NONE,
)


sealed interface DataResetState {
    data object Idle : DataResetState
    data object ConfirmationRequired : DataResetState
    data object Resetting : DataResetState
    data object Done : DataResetState
    data class Error(val message: String) : DataResetState
}


sealed interface SettingsEvent {
    data object NavigateBack : SettingsEvent
    data object NavigateToOnboarding : SettingsEvent
    data object OpenBatteryOptSettings : SettingsEvent
    data class ShowError(val message: String) : SettingsEvent
    data class ShowInfo(val message: String) : SettingsEvent
    data object PurchaseSuccess : SettingsEvent
    data object RestoreSuccess : SettingsEvent
    data object NothingToRestore : SettingsEvent
    data object DataResetComplete : SettingsEvent
    data object ShowDataResetConfirmation : SettingsEvent
    data object ShowRetakeOnboardingConfirmation : SettingsEvent
}
}