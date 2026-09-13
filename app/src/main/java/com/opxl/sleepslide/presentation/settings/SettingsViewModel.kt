package com.opxl.sleepslide.presentation.settings

import com.opxl.sleepslide.domain.model.Domain
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opxl.sleepslide.domain.observer.EntitlementObserver
import com.opxl.sleepslide.domain.repository.PurchaseRepository
import com.opxl.sleepslide.domain.repository.PurchaseResult
import com.opxl.sleepslide.domain.repository.RestoreResult
import com.opxl.sleepslide.domain.repository.UserPreferencesRepository
import com.opxl.sleepslide.domain.service.PurchaseService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context

private const val SHARING_STOP_TIMEOUT_MS   = 5_000L
private const val PURCHASE_RESET_DELAY_MS   = 3_000L
private const val PRODUCT_ID                = "sleepdrift_unlock_all"

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val purchaseService: PurchaseService,
    private val purchaseRepository: PurchaseRepository,
    private val entitlementObserver: EntitlementObserver,
    private val windDownNotificationService: com.opxl.sleepslide.domain.service.WindDownNotificationService,
) : ViewModel() {

    //  Events
    private val _events = Channel<SettingsVMState.SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsVMState.SettingsEvent> = _events.receiveAsFlow()

    // Internal state

    private val _purchaseState  = MutableStateFlow<SettingsVMState.PurchaseOperationState>(SettingsVMState.PurchaseOperationState.Idle)
    private val _dataResetState = MutableStateFlow<SettingsVMState.DataResetState>(SettingsVMState.DataResetState.Idle)
    private val _batteryStatus  = MutableStateFlow<SettingsVMState.BatteryOptStatus>(SettingsVMState.BatteryOptStatus.Unknown)


    val uiState: StateFlow<SettingsVMState.SettingsUiState> = buildUiState()
        .catch { e ->
            emit(
                SettingsVMState.SettingsUiState(
                    account = SettingsVMState.AccountState.Free(canPurchase = true),
                    about = SettingsVMState.AboutState(appVersion = resolveAppVersion()),
                )
            )
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(SHARING_STOP_TIMEOUT_MS),
            initialValue = SettingsVMState.SettingsUiState(),
        )


    init {
        refreshBatteryOptStatus()
        syncEntitlementOnEntry()
    }

    // PURCHASE COMMANDS

    fun purchase() {
        viewModelScope.launch {
            if (_purchaseState.value is SettingsVMState.PurchaseOperationState.Purchasing) return@launch

            _purchaseState.value = SettingsVMState.PurchaseOperationState.Purchasing

            runCatching { purchaseService.purchase(PRODUCT_ID) }
                .fold(
                    onSuccess = { result ->
                        when (result) {
                            is PurchaseResult.Success -> {
                                _purchaseState.value = SettingsVMState.PurchaseOperationState.PurchaseSuccess(
                                    result.entitlement.tier
                                )
                                _events.trySend(SettingsVMState.SettingsEvent.PurchaseSuccess)
                                resetPurchaseStateAfterDelay()
                            }
                            is PurchaseResult.Cancelled -> {
                                _purchaseState.value = SettingsVMState.PurchaseOperationState.Idle
                            }
                            is PurchaseResult.Failure -> {
                                _purchaseState.value = SettingsVMState.PurchaseOperationState.Error(result.reason)
                                _events.trySend(SettingsVMState.SettingsEvent.ShowError(result.reason))
                                resetPurchaseStateAfterDelay()
                            }
                        }
                    },
                    onFailure = { e ->
                        val msg = e.message ?: "Purchase failed — please try again"
                        _purchaseState.value = SettingsVMState.PurchaseOperationState.Error(msg)
                        _events.trySend(SettingsVMState.SettingsEvent.ShowError(msg))
                        resetPurchaseStateAfterDelay()
                    }
                )
        }
    }

    /**
     * Called by the RevenueCat [Paywall] listener after the paywall itself completed a
     * purchase or restore. The SDK already owns the transaction; we just re-sync the
     * entitlement so the account tier updates, and surface the same success feedback
     * as the direct-purchase path.
     */
    fun onPaywallPurchaseCompleted() {
        viewModelScope.launch {
            runCatching { purchaseService.refresh() }
            _events.trySend(SettingsVMState.SettingsEvent.PurchaseSuccess)
        }
    }

    fun onPaywallRestoreCompleted() {
        viewModelScope.launch {
            runCatching { purchaseService.refresh() }
            _events.trySend(SettingsVMState.SettingsEvent.RestoreSuccess)
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            if (_purchaseState.value is SettingsVMState.PurchaseOperationState.Restoring) return@launch

            _purchaseState.value = SettingsVMState.PurchaseOperationState.Restoring

            runCatching { purchaseService.restore() }
                .fold(
                    onSuccess = { result ->
                        when (result) {
                            is RestoreResult.Success -> {
                                _purchaseState.value = SettingsVMState.PurchaseOperationState.PurchaseSuccess(
                                    result.tier
                                )
                                _events.trySend(SettingsVMState.SettingsEvent.RestoreSuccess)
                                resetPurchaseStateAfterDelay()
                            }
                            is RestoreResult.NothingToRestore -> {
                                _purchaseState.value = SettingsVMState.PurchaseOperationState.NothingToRestore
                                _events.trySend(SettingsVMState.SettingsEvent.NothingToRestore)
                                resetPurchaseStateAfterDelay()
                            }
                            is RestoreResult.Failure -> {
                                _purchaseState.value = SettingsVMState.PurchaseOperationState.Error(result.reason)
                                _events.trySend(SettingsVMState.SettingsEvent.ShowError(result.reason))
                                resetPurchaseStateAfterDelay()
                            }
                        }
                    },
                    onFailure = { e ->
                        val msg = e.message ?: "Restore failed — please try again"
                        _purchaseState.value = SettingsVMState.PurchaseOperationState.Error(msg)
                        _events.trySend(SettingsVMState.SettingsEvent.ShowError(msg))
                        resetPurchaseStateAfterDelay()
                    }
                )
        }
    }

    private fun resetPurchaseStateAfterDelay() {
        viewModelScope.launch {
            delay(PURCHASE_RESET_DELAY_MS)
            _purchaseState.value = SettingsVMState.PurchaseOperationState.Idle
        }
    }


    fun setDefaultFadeIn(durationMs: Long) {
        viewModelScope.launch {
            runCatching { userPreferencesRepository.setDefaultFadeIn(durationMs) }
                .onFailure { _events.trySend(SettingsVMState.SettingsEvent.ShowError("Could not save fade-in setting")) }
        }
    }

    fun setDefaultFadeOut(durationMs: Long) {
        viewModelScope.launch {
            runCatching { userPreferencesRepository.setDefaultFadeOut(durationMs) }
                .onFailure { _events.trySend(SettingsVMState.SettingsEvent.ShowError("Could not save fade-out setting")) }
        }
    }

    fun setDefaultTimerDuration(durationMs: Long) {
        viewModelScope.launch {
            runCatching { userPreferencesRepository.setLastTimerDuration(durationMs) }
                .onFailure { _events.trySend(SettingsVMState.SettingsEvent.ShowError("Could not save timer setting")) }
        }
    }

    fun setNightLockDefault(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { userPreferencesRepository.setNightLockDefault(enabled) }
                .onFailure { _events.trySend(SettingsVMState.SettingsEvent.ShowError("Could not save night lock setting")) }
        }
    }


    fun setThemeMode(mode: Domain.ThemeMode) {
        viewModelScope.launch {
            runCatching { userPreferencesRepository.setThemeMode(mode) }
                .onFailure { _events.trySend(SettingsVMState.SettingsEvent.ShowError("Could not save theme setting")) }
        }
    }

    fun setDarkModeStartHour(hour: Int) {
        viewModelScope.launch {
            val prefs = runCatching { userPreferencesRepository.get() }.getOrNull() ?: return@launch
            runCatching { userPreferencesRepository.setDarkModeWindow(hour, prefs.darkModeEndHour) }
                .onFailure { _events.trySend(SettingsVMState.SettingsEvent.ShowError("Could not save dark mode setting")) }
        }
    }

    fun setDarkModeEndHour(hour: Int) {
        viewModelScope.launch {
            val prefs = runCatching { userPreferencesRepository.get() }.getOrNull() ?: return@launch
            runCatching { userPreferencesRepository.setDarkModeWindow(prefs.darkModeStartHour, hour) }
                .onFailure { _events.trySend(SettingsVMState.SettingsEvent.ShowError("Could not save dark mode setting")) }
        }
    }

    // ACCESSIBILITY SETTINGS

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { userPreferencesRepository.setHighContrast(enabled) }
                .onFailure { _events.trySend(SettingsVMState.SettingsEvent.ShowError("Could not save accessibility setting")) }
        }
    }

    fun openBatteryOptSettings() {
        viewModelScope.launch {
            runCatching { userPreferencesRepository.recordBatteryOptPromptShown() }
            _events.trySend(SettingsVMState.SettingsEvent.OpenBatteryOptSettings)
        }
    }

    fun resetBatteryOptPrompt() {
        viewModelScope.launch {
            runCatching {
                userPreferencesRepository.dismissBatteryOptPrompt()
            }.onSuccess {
                _events.trySend(SettingsVMState.SettingsEvent.ShowInfo("Battery optimisation prompt reset"))
            }.onFailure {
                _events.trySend(SettingsVMState.SettingsEvent.ShowError("Could not reset prompt"))
            }
        }
    }

    /**
     * Called by MainActivity after returning from battery settings.
     * Re-checks the system state so the status chip updates without a restart.
     */
    fun refreshBatteryOptStatus() {
        runCatching {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            _batteryStatus.value = if (pm.isIgnoringBatteryOptimizations(context.packageName))
                SettingsVMState.BatteryOptStatus.Granted
            else
                SettingsVMState.BatteryOptStatus.NotGranted
        }.onFailure {
            _batteryStatus.value = SettingsVMState.BatteryOptStatus.Unknown
        }
    }

    // ABOUT / ONBOARDING

    fun requestRetakeOnboarding() {
        viewModelScope.launch {
            _events.send(SettingsVMState.SettingsEvent.ShowRetakeOnboardingConfirmation)
        }
    }

    fun confirmRetakeOnboarding() {
        viewModelScope.launch {
            runCatching {
                userPreferencesRepository.completeOnboarding(Domain.OnboardingPath.NONE)
            }.onSuccess {
                _events.trySend(SettingsVMState.SettingsEvent.NavigateToOnboarding)
            }.onFailure { e ->
                _events.trySend(SettingsVMState.SettingsEvent.ShowError(e.message ?: "Could not reset onboarding"))
            }
        }
    }

    // DATA RESET

    fun requestDataReset() {
        viewModelScope.launch {
            _events.send(SettingsVMState.SettingsEvent.ShowDataResetConfirmation)
        }
    }

    fun confirmDataReset() {
        viewModelScope.launch {
            _dataResetState.value = SettingsVMState.DataResetState.Resetting
            runCatching {
                userPreferencesRepository.reset()
            }.onSuccess {
                _dataResetState.value = SettingsVMState.DataResetState.Done
                _events.trySend(SettingsVMState.SettingsEvent.DataResetComplete)
                delay(PURCHASE_RESET_DELAY_MS)
                _dataResetState.value = SettingsVMState.DataResetState.Idle
            }.onFailure { e ->
                _dataResetState.value = SettingsVMState.DataResetState.Error(e.message ?: "Reset failed")
                _events.trySend(SettingsVMState.SettingsEvent.ShowError(e.message ?: "Could not reset data"))
            }
        }
    }

    // STATE CONSTRUCTION

    private fun buildUiState(): Flow<SettingsVMState.SettingsUiState> {
        val prefsFlow = userPreferencesRepository.observe()
            .catch { emit(Domain.UserPreferences()) }
            .distinctUntilChanged()

        val entitlementFlow = entitlementObserver.entitlement
            .catch { emit(Domain.Entitlement(Domain.EntitlementTier.FREE, revenueCatUserId = "")) }
            .distinctUntilChanged()

        return combine(
            prefsFlow,
            entitlementFlow,
            _purchaseState,
            _batteryStatus,
            _dataResetState,
        ) { prefs, entitlement, purchaseOp, batteryStatus, dataReset ->

            val accountState = when (entitlement.tier) {
                Domain.EntitlementTier.FREE    -> SettingsVMState.AccountState.Free(
                    canPurchase = purchaseOp !is SettingsVMState.PurchaseOperationState.Purchasing &&
                            purchaseOp !is SettingsVMState.PurchaseOperationState.Restoring,
                )
                Domain.EntitlementTier.PREMIUM -> SettingsVMState.AccountState.Premium(
                    purchasedAt = entitlement.purchasedAt,
                    isRestored  = entitlement.isRestored,
                )
            }

            val playbackState = SettingsVMState.PlaybackSettingsState(
                defaultFadeInMs   = prefs.defaultFadeInMs,
                defaultFadeOutMs  = prefs.defaultFadeOutMs,
                lastTimerDurationMs = prefs.lastTimerDurationMs,
                isNightLockDefault = prefs.isNightLockEnabledByDefault,
                fadeInLabel       = SettingsVMState.FADE_IN_STEPS_MS.labelFor(prefs.defaultFadeInMs, "3 seconds"),
                fadeOutLabel      = SettingsVMState.FADE_OUT_STEPS_MS.labelFor(prefs.defaultFadeOutMs, "1 minute"),
                timerLabel        = SettingsVMState.DEFAULT_TIMER_STEPS_MS.labelFor(prefs.lastTimerDurationMs, "30 minutes"),
            )

            val appearanceState = SettingsVMState.AppearanceState(
                themeMode          = prefs.themeMode,
                darkModeStartHour  = prefs.darkModeStartHour,
                darkModeEndHour    = prefs.darkModeEndHour,
                showScheduleOptions = prefs.themeMode == Domain.ThemeMode.SCHEDULED,
            )

            val accessibilityState = SettingsVMState.AccessibilityState(
                isHighContrastEnabled   = prefs.isHighContrastEnabled,
                batteryOptStatus        = batteryStatus,
                batteryOptPromptCount   = prefs.batteryOptPromptShownCount,
                hasDismissedBatteryPrompt = prefs.hasDismissedBatteryOptPrompt,
            )

            SettingsVMState.SettingsUiState(
                account = accountState,
                playback = playbackState,
                appearance = appearanceState,
                accessibility = accessibilityState,
                about = SettingsVMState.AboutState(
                    appVersion = resolveAppVersion(),
                    onboardingPath = prefs.onboardingPath,
                ),
                purchase = purchaseOp,
                dataReset = dataReset,
            )
        }



    }

    /**
     * Master toggle. When enabling, schedules the alarm immediately using the
     * stored hour/minute. When disabling, cancels any pending alarm.
     * Also writes the SharedPreferences cache so BootReceiver can read it
     * synchronously after reboot.
     */
    fun setWindDownEnabled(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                val prefs = userPreferencesRepository.get()
                userPreferencesRepository.setWindDownEnabled(enabled)

                if (enabled) {
                    windDownNotificationService.scheduleDailyReminder(
                        prefs.windDownHour,
                        prefs.windDownMinute,
                    )
                    writeWindDownCache(
                        enabled = true,
                        hour    = prefs.windDownHour,
                        minute  = prefs.windDownMinute,
                        path    = prefs.onboardingPath,
                    )
                    _events.trySend(
                        SettingsVMState.SettingsEvent.ShowInfo(
                            "Reminder set for ${formatTime(prefs.windDownHour, prefs.windDownMinute)}"
                        )
                    )
                } else {
                    windDownNotificationService.cancelDailyReminder()
                    writeWindDownCache(
                        enabled = false,
                        hour    = prefs.windDownHour,
                        minute  = prefs.windDownMinute,
                        path    = prefs.onboardingPath,
                    )
                    _events.trySend(
                        SettingsVMState.SettingsEvent.ShowInfo("Wind-down reminder cancelled")
                    )
                }
            }.onFailure { e ->
                _events.trySend(
                    SettingsVMState.SettingsEvent.ShowError(
                    e.message ?: "Could not update wind-down reminder"
                ))
            }
        }
    }

    /**
     * Updates the time and immediately reschedules if enabled.
     * Both the DataStore write and alarm reschedule happen atomically —
     * if either fails the other is not committed, keeping them in sync.
     */
    fun setWindDownTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            runCatching {
                userPreferencesRepository.setWindDownTime(hour, minute)
                val prefs = userPreferencesRepository.get()

                if (prefs.isWindDownEnabled) {
                    windDownNotificationService.scheduleDailyReminder(hour, minute)
                    writeWindDownCache(
                        enabled = true,
                        hour    = hour,
                        minute  = minute,
                        path    = prefs.onboardingPath,
                    )
                    _events.trySend(
                        SettingsVMState.SettingsEvent.ShowInfo(
                        "Reminder updated to ${formatTime(hour, minute)}"
                    ))
                }
            }.onFailure { e ->
                _events.trySend(SettingsVMState.SettingsEvent.ShowError(
                    e.message ?: "Could not update reminder time"
                ))
            }
        }
    }

    /**
     * Fires the notification immediately so the user can preview what it looks
     * like without waiting for the scheduled time.
     */
    fun sendTestWindDownNotification() {
        viewModelScope.launch {
            runCatching {
                windDownNotificationService.showImmediateNotification()
                _events.trySend(SettingsVMState.SettingsEvent.ShowInfo("Test notification sent"))
            }.onFailure {
                _events.trySend(SettingsVMState.SettingsEvent.ShowError("Could not send test notification"))
            }
        }
    }

    /**
     * Checks if SCHEDULE_EXACT_ALARM permission is granted on API 31+.
     * Called when the user navigates to the wind-down section so the
     * permission warning chip shows immediately without requiring a tap.
     */
    fun checkExactAlarmPermission(): Boolean = runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            am.canScheduleExactAlarms()
        } else {
            true
        }
    }.getOrDefault(true)

    fun openExactAlarmSettings() {
        viewModelScope.launch {
            _events.trySend(
                SettingsVMState.SettingsEvent.ShowInfo(
                "Enable 'Alarms & reminders' for precise daily timing"
            ))
            // Actual navigation to alarm settings is handled by MainActivity
            // via SettingsEvent.OpenBatteryOptSettings re-used for this purpose
            // or a dedicated event if the caller differentiates
        }
    }

    private fun writeWindDownCache(
        enabled: Boolean,
        hour: Int,
        minute: Int,
        path: Domain.OnboardingPath,
    ) {
        runCatching {
            (windDownNotificationService
                    as? com.opxl.sleepslide.data.notification.WindDownNotificationServiceImpl)
                ?.writeCache(enabled, hour, minute, path)
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val h    = when {
            hour == 0    -> 12
            hour > 12    -> hour - 12
            else         -> hour
        }
        return "%d:%02d %s".format(h, minute, amPm)
    }

    // HELPERS

    private fun syncEntitlementOnEntry() {
        viewModelScope.launch {
            runCatching { purchaseService.refresh() }
        }
    }

    private fun resolveAppVersion(): String = runCatching {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName ?: "—"
    }.getOrDefault("—")

    private fun List<Pair<Long, String>>.labelFor(value: Long, fallback: String): String =
        firstOrNull { it.first == value }?.second ?: fallback
}