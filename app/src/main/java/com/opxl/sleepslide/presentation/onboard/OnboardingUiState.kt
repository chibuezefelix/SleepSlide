package com.opxl.sleepslide.presentation.onboard

import com.opxl.sleepslide.domain.model.Domain

object OnboardingVMState{

data class OnboardingUiState(
    val step: OnboardingStep            = OnboardingStep.Welcome,
    val selectedPath: Domain.OnboardingPath?   = null,
    val isSaving: Boolean               = false,
    val permissionsState: PermissionsState = PermissionsState(),
)

sealed interface OnboardingStep {
    data object Welcome : OnboardingStep
    data object PathSelection : OnboardingStep
    data object Permissions : OnboardingStep
}

data class PermissionsState(
    val notificationGranted: Boolean    = false,
    val batteryOptGranted: Boolean      = false,
    val notificationRequired: Boolean   = false,
    val batteryOptRequired: Boolean     = false,
)

data class PathOption(
    val path: Domain.OnboardingPath,
    val title: String,
    val description: String,
    val whoIsThisFor: String,
    val sounds: List<String>,
)

val ONBOARDING_PATHS = listOf(
    PathOption(
        path        = Domain.OnboardingPath.TINNITUS,
        title       = "Tinnitus relief",
        description = "White, pink, brown and grey noise to mask ear ringing and help you sleep.",
        whoIsThisFor = "For Jeff — veterans, hearing loss, tinnitus sufferers",
        sounds      = listOf("White noise", "Brown noise", "Pink noise", "Grey noise"),
    ),
    PathOption(
        path        = Domain.OnboardingPath.GENERAL,
        title       = "Wind down",
        description = "Rain, ocean, forest and ambient sounds to help you relax after a long day.",
        whoIsThisFor = "For Sandra — stress relief, daily wind-down",
        sounds      = listOf("Rain", "Ocean", "Forest", "Fireplace"),
    ),
)

sealed interface OnboardingEvent {
    data object NavigateToHome : OnboardingEvent
    data object RequestNotificationPermission : OnboardingEvent
    data object OpenBatterySettings : OnboardingEvent
    data class ShowError(val message: String) : OnboardingEvent
}}