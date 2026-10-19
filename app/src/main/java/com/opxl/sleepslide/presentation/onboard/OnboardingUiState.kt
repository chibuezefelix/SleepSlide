package com.opxl.sleepslide.presentation.onboard

import com.opxl.sleepslide.domain.model.Domain
import androidx.annotation.StringRes
import com.opxl.sleepslide.R
import com.opxl.sleepslide.presentation.components.UiText

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
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val whoIsThisFor: Int,
    val sounds: List<Int>,
)

val ONBOARDING_PATHS = listOf(
    PathOption(
        path        = Domain.OnboardingPath.TINNITUS,
        title       = R.string.onboarding_path_tinnitus_title,
        description = R.string.onboarding_path_tinnitus_description,
        whoIsThisFor = R.string.onboarding_path_tinnitus_who,
        sounds      = listOf(
            R.string.onboarding_pill_white_noise, R.string.onboarding_pill_brown_noise,
            R.string.onboarding_pill_pink_noise, R.string.onboarding_pill_grey_noise,
        ),
    ),
    PathOption(
        path        = Domain.OnboardingPath.GENERAL,
        title       = R.string.onboarding_path_general_title,
        description = R.string.onboarding_path_general_description,
        whoIsThisFor = R.string.onboarding_path_general_who,
        sounds      = listOf(
            R.string.onboarding_pill_rain, R.string.onboarding_pill_ocean,
            R.string.onboarding_pill_forest, R.string.onboarding_pill_fireplace,
        ),
    ),
)

sealed interface OnboardingEvent {
    data object NavigateToHome : OnboardingEvent
    data object RequestNotificationPermission : OnboardingEvent
    data object OpenBatterySettings : OnboardingEvent
    data class ShowError(val message: UiText) : OnboardingEvent
}}