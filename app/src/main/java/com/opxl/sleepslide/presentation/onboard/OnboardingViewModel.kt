package com.opxl.sleepslide.presentation.onboard


import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    //  Events

    private val _events = Channel<OnboardingVMState.OnboardingEvent>(Channel.BUFFERED)
    val events: Flow<OnboardingVMState.OnboardingEvent> = _events.receiveAsFlow()

    //  Internal state
    private val _step        = MutableStateFlow<OnboardingVMState.OnboardingStep>(OnboardingVMState.OnboardingStep.Welcome)
    private val _selectedPath = MutableStateFlow<Domain.OnboardingPath?>(null)
    private val _isSaving    = MutableStateFlow(false)
    private val _permissions = MutableStateFlow(resolvePermissionsState())

    // UI state

    val uiState: StateFlow<OnboardingVMState.OnboardingUiState> = combine(
        _step,
        _selectedPath,
        _isSaving,
        _permissions,
    ) { step, path, saving, perms ->
        OnboardingVMState.OnboardingUiState(
            step = step,
            selectedPath = path,
            isSaving = saving,
            permissionsState = perms,
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = OnboardingVMState.OnboardingUiState(),
    )

    // STEP NAVIGATION

    fun onWelcomeContinue() {
        _step.value = OnboardingVMState.OnboardingStep.PathSelection
    }

    fun onPathSelected(path: Domain.OnboardingPath) {
        _selectedPath.value = path
    }

    fun onPathSelectionContinue() {
        val perms = _permissions.value
        if (perms.notificationRequired || perms.batteryOptRequired) {
            _step.value = OnboardingVMState.OnboardingStep.Permissions
        } else {
            completeOnboarding()
        }
    }

    /**
     * Skips path selection — uses GENERAL as default and proceeds.
     * Sandra scenario: user just wants to try the app immediately.
     */
    fun onSkip() {
        if (_selectedPath.value == null) {
            _selectedPath.value = Domain.OnboardingPath.GENERAL
        }
        completeOnboarding()
    }

    fun onBack() {
        when (_step.value) {
            is OnboardingVMState.OnboardingStep.PathSelection -> _step.value = OnboardingVMState.OnboardingStep.Welcome
            is OnboardingVMState.OnboardingStep.Permissions   -> _step.value = OnboardingVMState.OnboardingStep.PathSelection
            is OnboardingVMState.OnboardingStep.Welcome       -> Unit // no-op — cannot go back from welcome
        }
    }

    fun onPermissionsContinue() {
        completeOnboarding()
    }

    // PERMISSION HANDLERS

    fun onNotificationPermissionResult(granted: Boolean) {
        _permissions.update { it.copy(notificationGranted = granted) }
        // Record the ask regardless of outcome so PlayerScreen doesn't prompt again on first play
        viewModelScope.launch {
            runCatching { userPreferencesRepository.markNotificationPermissionRequested() }
        }
    }

    fun requestNotificationPermission() {
        viewModelScope.launch {
            _events.send(OnboardingVMState.OnboardingEvent.RequestNotificationPermission)
            runCatching { userPreferencesRepository.recordBatteryOptPromptShown() }
        }
    }

    fun requestBatteryOptimisation() {
        viewModelScope.launch {
            _events.send(OnboardingVMState.OnboardingEvent.OpenBatterySettings)
        }
    }

    /**
     * Called by the host when returning from battery settings.
     * Re-reads system state — no observer possible for this.
     */
    fun onReturnFromBatterySettings() {
        _permissions.update { current ->
            current.copy(batteryOptGranted = checkBatteryOptGranted())
        }
    }

    fun refreshPermissions() {
        _permissions.value = resolvePermissionsState()
    }

    // COMPLETION

    private fun completeOnboarding() {
        viewModelScope.launch {
            _isSaving.value = true
            val path = _selectedPath.value ?: Domain.OnboardingPath.GENERAL
            runCatching {
                userPreferencesRepository.completeOnboarding(path)
            }.onSuccess {
                _events.trySend(OnboardingVMState.OnboardingEvent.NavigateToHome)
            }.onFailure { e ->
                _isSaving.value = false
                _events.trySend(
                    OnboardingVMState.OnboardingEvent.ShowError(
                        e.message ?: "Could not save your preference — please try again"
                    )
                )
            }
        }
    }

    // HELPERS

    private fun resolvePermissionsState(): OnboardingVMState.PermissionsState {
        val notifGranted = checkNotificationGranted()
        val batteryGranted = checkBatteryOptGranted()
        return OnboardingVMState.PermissionsState(
            notificationGranted  = notifGranted,
            batteryOptGranted    = batteryGranted,
            notificationRequired = !notifGranted,
            batteryOptRequired   = !batteryGranted,
        )
    }

    private fun checkNotificationGranted(): Boolean = runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true // Pre-13 — permission is implicit
        }
    }.getOrDefault(false)

    private fun checkBatteryOptGranted(): Boolean = runCatching {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(context.packageName)
    }.getOrDefault(false)
}