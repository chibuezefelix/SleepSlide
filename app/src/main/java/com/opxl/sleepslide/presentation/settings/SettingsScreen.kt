package com.opxl.sleepslide.presentation.settings

import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.presentation.settings.SettingsVMState.DEFAULT_TIMER_STEPS_MS
import com.opxl.sleepslide.presentation.settings.SettingsVMState.FADE_IN_STEPS_MS
import com.opxl.sleepslide.presentation.settings.SettingsVMState.FADE_OUT_STEPS_MS
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.MutedGray
import com.opxl.sleepslide.ui.theme.PaleBlueText
import com.opxl.sleepslide.ui.theme.PaleGreen
import com.opxl.sleepslide.ui.theme.PaleGreenText
import com.opxl.sleepslide.ui.theme.PaleRed
import com.opxl.sleepslide.ui.theme.PaleRedText
import com.opxl.sleepslide.ui.theme.PaleYellow
import com.opxl.sleepslide.ui.theme.PaleYellowText
import com.opxl.sleepslide.ui.theme.SurfaceMuted
import com.opxl.sleepslide.ui.theme.WarmWhite
import com.opxl.sleepslide.ui.theme.White
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showPaywall by rememberSaveable { mutableStateOf(false) }
    var showResetDialog       by rememberSaveable { mutableStateOf(false) }
    var showOnboardingDialog  by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsVMState.SettingsEvent.NavigateBack             -> onNavigateBack()
                is SettingsVMState.SettingsEvent.NavigateToOnboarding     -> onNavigateToOnboarding()
                is SettingsVMState.SettingsEvent.OpenBatteryOptSettings   -> {
                    onOpenBatterySettings()
                    // Re-check status when returning
                    viewModel.refreshBatteryOptStatus()
                }
                is SettingsVMState.SettingsEvent.ShowDataResetConfirmation        -> showResetDialog = true
                is SettingsVMState.SettingsEvent.ShowRetakeOnboardingConfirmation -> showOnboardingDialog = true
                is SettingsVMState.SettingsEvent.PurchaseSuccess  ->
                    scope.launch { snackbarHostState.showSnackbar("Welcome to premium — all sounds unlocked") }
                is SettingsVMState.SettingsEvent.RestoreSuccess   ->
                    scope.launch { snackbarHostState.showSnackbar("Purchase restored") }
                is SettingsVMState.SettingsEvent.NothingToRestore ->
                    scope.launch { snackbarHostState.showSnackbar("No previous purchase found on this account") }
                is SettingsVMState.SettingsEvent.DataResetComplete ->
                    scope.launch { snackbarHostState.showSnackbar("Preferences reset") }
                is SettingsVMState.SettingsEvent.ShowError        ->
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                is SettingsVMState.SettingsEvent.ShowInfo         ->
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = Charcoal,
                    contentColor   = White,
                    actionColor    = PaleBlueText,
                    shape          = RoundedCornerShape(8.dp),
                )
            }
        },
        containerColor = WarmWhite,
    ) { innerPadding ->
        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { SettingsTopBar(onBack = onNavigateBack) }

            item {
                SectionHeader("Account")
                AccountSection(
                    account  = uiState.account,
                    purchase = uiState.purchase,
                    onBuy    = { viewModel.purchase() },
                    onRestore = { viewModel.restorePurchases() },
                )
            }

            item {
                SectionHeader("Playback")
                PlaybackSection(
                    state              = uiState.playback,
                    onFadeIn           = viewModel::setDefaultFadeIn,
                    onFadeOut          = viewModel::setDefaultFadeOut,
                    onTimerDuration    = viewModel::setDefaultTimerDuration,
                    onNightLockDefault = viewModel::setNightLockDefault,
                )
            }

            item {
                SectionHeader("Appearance")
                AppearanceSection(
                    state          = uiState.appearance,
                    onThemeMode    = viewModel::setThemeMode,
                    onStartHour    = viewModel::setDarkModeStartHour,
                    onEndHour      = viewModel::setDarkModeEndHour,
                )
            }

            item {
                SectionHeader("Wind-down reminder")
                WindDownSection(
                    state        = uiState.windDown,
                    onToggle     = viewModel::setWindDownEnabled,
                    onTimeChange = viewModel::setWindDownTime,
                    onTest       = { viewModel.sendTestWindDownNotification() },
                )
            }

            item {
                SectionHeader("Accessibility")
                AccessibilitySection(
                    state              = uiState.accessibility,
                    onHighContrast     = viewModel::setHighContrast,
                    onBatteryOpt       = { viewModel.openBatteryOptSettings() },
                    onResetBatteryPrompt = { viewModel.resetBatteryOptPrompt() },
                )
            }

            item {
                SectionHeader("About")
                AboutSection(
                    state           = uiState.about,
                    dataReset       = uiState.dataReset,
                    onRetakeOnboarding = { viewModel.requestRetakeOnboarding() },
                    onResetData     = { viewModel.requestDataReset() },
                )
            }

            item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
        }
    }

    if (showResetDialog) {
        DataResetDialog(
            onConfirm = { showResetDialog = false; viewModel.confirmDataReset() },
            onDismiss = { showResetDialog = false },
        )
    }

    if (showOnboardingDialog) {
        RetakeOnboardingDialog(
            onConfirm = { showOnboardingDialog = false; viewModel.confirmRetakeOnboarding() },
            onDismiss = { showOnboardingDialog = false },
        )
    }

    // RevenueCat Paywall — shown when user taps "Unlock All"
    // In testing mode this is bypassed — purchase() simulates success immediately
    if (showPaywall) {
        Paywall(
            options = PaywallOptions.Builder(dismissRequest = { showPaywall = false })
                .setShouldDisplayDismissButton(true)
                .setListener(
                    object : PaywallListener {
                        override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
                            showPaywall = false
                            viewModel.onPaywallPurchaseCompleted()
                        }

                        override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                            showPaywall = false
                            viewModel.onPaywallRestoreCompleted()
                        }
                    }
                )
                .build()
        )
    }
}
@Composable
private fun WindDownSection(
    state: SettingsVMState.WindDownState,
    onToggle: (Boolean) -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    onTest: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Master toggle
        ToggleRow(
            label    = "Daily reminder",
            sublabel = if (state.isEnabled)
                "Fires at ${state.formattedTime} every day"
            else
                "Get a gentle nudge before your wind-down time",
            enabled  = state.isEnabled,
            onToggle = onToggle,
        )

        // Time picker — only shown when enabled
        AnimatedVisibility(
            visible = state.isEnabled,
            enter   = expandVertically(tween(250)) + fadeIn(tween(200)),
            exit    = shrinkVertically(tween(200)) + fadeOut(tween(150)),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Hour selector
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Reminder time",
                            style = MaterialTheme.typography.titleSmall,
                            color = Charcoal,
                        )

                        // Hour chips — evening hours most relevant
                        Text("Hour", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val hours = (18..23).toList()
                            items(hours) { h ->
                                val selected = h == state.hour
                                TimeChip(
                                    label    = "${h}:00",
                                    selected = selected,
                                    onClick  = { onTimeChange(h, state.minute) },
                                )
                            }
                        }

                        // Minute chips
                        Text("Minute", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val minutes = listOf(0, 15, 30, 45)
                            items(minutes) { m ->
                                val selected = m == state.minute
                                TimeChip(
                                    label    = "%02d".format(m),
                                    selected = selected,
                                    onClick  = { onTimeChange(state.hour, m) },
                                )
                            }
                        }

                        HRule()

                        Text(
                            text  = "Set for ${state.formattedTime} daily",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                    }
                }

                // SCHEDULE_EXACT_ALARM permission warning — API 31+
                if (!state.exactAlarmPermissionGranted) {
                    SettingsCard(bg = PaleYellow, border = PaleYellowText) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Precise timing requires permission",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = PaleYellowText,
                                )
                                Text(
                                    "Enable 'Alarms & reminders' for the exact time. Without it, the reminder may arrive a few minutes late.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PaleYellowText.copy(alpha = 0.8f),
                                )
                            }
                        }
                    }
                }

                // Test notification row
                SettingsRow(
                    label    = "Send test notification",
                    sublabel = "Preview what the reminder looks like",
                    trailing = { ChevronRight(MutedGray) },
                    onClick  = onTest,
                )
            }
        }
    }
}

@Composable
private fun TimeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Charcoal else SurfaceMuted)
            .border(1.dp, if (selected) Charcoal else Border, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) White else MutedGray,
        )
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SmallIconButton(onClick = onBack, description = "Back") { ChevronLeft(Charcoal) }
        Text("Settings", style = MaterialTheme.typography.titleMedium, color = Charcoal)
        Spacer(Modifier.size(40.dp))
    }
}


@Composable
private fun AccountSection(
    account: SettingsVMState.AccountState,
    purchase: SettingsVMState.PurchaseOperationState,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Tier badge
        when (account) {
            is SettingsVMState.AccountState.Loading -> SkeletonBox(height = 72.dp)

            is SettingsVMState.AccountState.Premium -> {
                SettingsCard(bg = PaleGreen, border = PaleGreenText) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                "Premium",
                                style = MaterialTheme.typography.titleSmall,
                                color = PaleGreenText,
                            )
                            Text(
                                if (account.isRestored) "Restored purchase"
                                else "One-time unlock — no subscription",
                                style = MaterialTheme.typography.bodySmall,
                                color = PaleGreenText.copy(alpha = 0.7f),
                            )
                        }
                        CheckBadge()
                    }
                }
            }

            is SettingsVMState.AccountState.Free -> {
                // Upgrade CTA
                SettingsCard(bg = Charcoal, border = Charcoal) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Unlock All Sounds",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = White,
                                )
                                Text(
                                    "One-time purchase · No subscription",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = White.copy(alpha = 0.6f),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            PurchaseButton(
                                purchase    = purchase,
                                canPurchase = account.canPurchase,
                                onClick     = onBuy,
                            )
                        }
                    }
                }

                // Restore row
                SettingsRow(
                    label       = "Restore purchases",
                    sublabel    = "Already bought on another device?",
                    trailing    = {
                        when (purchase) {
                            is SettingsVMState.PurchaseOperationState.Restoring ->
                                LoadingDots(MutedGray)
                            is SettingsVMState.PurchaseOperationState.NothingToRestore ->
                                StatusPill("Nothing found", PaleYellow, PaleYellowText)
                            else -> ChevronRight(MutedGray)
                        }
                    },
                    onClick     = if (purchase !is SettingsVMState.PurchaseOperationState.Restoring) onRestore else null,
                )
            }
        }
    }
}

@Composable
private fun PurchaseButton(
    purchase: SettingsVMState.PurchaseOperationState,
    canPurchase: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(White)
            .border(1.dp, Border, RoundedCornerShape(6.dp))
            .clickable(enabled = canPurchase && purchase is SettingsVMState.PurchaseOperationState.Idle, onClick = onClick)
            .alpha(if (canPurchase) 1f else 0.5f)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics { contentDescription = "Unlock all sounds" },
    ) {
        when (purchase) {
            is SettingsVMState.PurchaseOperationState.Purchasing ->
                LoadingDots(Charcoal)
            is SettingsVMState.PurchaseOperationState.PurchaseSuccess ->
                Text("Unlocked ✓", style = MaterialTheme.typography.labelLarge, color = PaleGreenText)
            is SettingsVMState.PurchaseOperationState.Error ->
                Text("Try again", style = MaterialTheme.typography.labelLarge, color = PaleRedText)
            else ->
                Text("Unlock All", style = MaterialTheme.typography.labelLarge, color = Charcoal)
        }
    }
}


@Composable
private fun PlaybackSection(
    state: SettingsVMState.PlaybackSettingsState,
    onFadeIn: (Long) -> Unit,
    onFadeOut: (Long) -> Unit,
    onTimerDuration: (Long) -> Unit,
    onNightLockDefault: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StepSelectorRow(
            label    = "Fade in",
            sublabel = state.fadeInLabel,
            steps    = FADE_IN_STEPS_MS,
            selected = state.defaultFadeInMs,
            onSelect = onFadeIn,
        )

        StepSelectorRow(
            label    = "Fade out",
            sublabel = state.fadeOutLabel,
            steps    = FADE_OUT_STEPS_MS,
            selected = state.defaultFadeOutMs,
            onSelect = onFadeOut,
        )

        StepSelectorRow(
            label    = "Default timer",
            sublabel = state.timerLabel,
            steps    = DEFAULT_TIMER_STEPS_MS,
            selected = state.lastTimerDurationMs,
            onSelect = onTimerDuration,
        )

        ToggleRow(
            label    = "Night lock by default",
            sublabel = "Automatically lock screen during playback",
            enabled  = state.isNightLockDefault,
            onToggle = onNightLockDefault,
        )
    }
}


@Composable
private fun AppearanceSection(
    state: SettingsVMState.AppearanceState,
    onThemeMode: (Domain.ThemeMode) -> Unit,
    onStartHour: (Int) -> Unit,
    onEndHour: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Theme mode chips
        SettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Theme",
                    style = MaterialTheme.typography.titleSmall,
                    color = Charcoal,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Domain.ThemeMode.values().toList()) { mode ->
                        val selected = mode == state.themeMode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) Charcoal else SurfaceMuted)
                                .border(1.dp, if (selected) Charcoal else Border, RoundedCornerShape(6.dp))
                                .clickable { onThemeMode(mode) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text  = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) White else MutedGray,
                            )
                        }
                    }
                }

                // Scheduled time window — only visible when SCHEDULED selected
                AnimatedVisibility(
                    visible = state.showScheduleOptions,
                    enter   = expandVertically(tween(250)) + fadeIn(tween(200)),
                    exit    = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HRule()
                        HourStepRow(
                            label    = "Dark from",
                            hour     = state.darkModeStartHour,
                            onSelect = onStartHour,
                        )
                        HourStepRow(
                            label    = "Light from",
                            hour     = state.darkModeEndHour,
                            onSelect = onEndHour,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HourStepRow(label: String, hour: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MutedGray)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val hours = listOf(20, 21, 22, 23, 0, 6, 7, 8, 9)
            items(hours) { h ->
                val selected = h == hour
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (selected) Charcoal else SurfaceMuted)
                        .border(1.dp, if (selected) Charcoal else Border, RoundedCornerShape(4.dp))
                        .clickable { onSelect(h) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text  = "${h}:00",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) White else MutedGray,
                    )
                }
            }
        }
    }
}

// Accessibility section

@Composable
private fun AccessibilitySection(
    state: SettingsVMState.AccessibilityState,
    onHighContrast: (Boolean) -> Unit,
    onBatteryOpt: () -> Unit,
    onResetBatteryPrompt: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ToggleRow(
            label    = "High contrast",
            sublabel = "Increases colour contrast for readability",
            enabled  = state.isHighContrastEnabled,
            onToggle = onHighContrast,
        )

        // Battery optimisation — shows real status + action
        SettingsCard(
            bg     = when (state.batteryOptStatus) {
                SettingsVMState.BatteryOptStatus.Granted    -> PaleGreen
                SettingsVMState.BatteryOptStatus.NotGranted -> PaleYellow
                SettingsVMState.BatteryOptStatus.Unknown    -> SurfaceMuted
            },
            border = when (state.batteryOptStatus) {
                SettingsVMState.BatteryOptStatus.Granted    -> PaleGreenText
                SettingsVMState.BatteryOptStatus.NotGranted -> PaleYellowText
                SettingsVMState.BatteryOptStatus.Unknown    -> Border
            },
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Background audio",
                        style = MaterialTheme.typography.titleSmall,
                        color = when (state.batteryOptStatus) {
                            SettingsVMState.BatteryOptStatus.Granted    -> PaleGreenText
                            SettingsVMState.BatteryOptStatus.NotGranted -> PaleYellowText
                            SettingsVMState.BatteryOptStatus.Unknown    -> Charcoal
                        },
                    )
                    Text(
                        text = when (state.batteryOptStatus) {
                            SettingsVMState.BatteryOptStatus.Granted    -> "Battery optimisation disabled — audio won't stop"
                            SettingsVMState.BatteryOptStatus.NotGranted -> "Enable to prevent audio stopping at night"
                            SettingsVMState.BatteryOptStatus.Unknown    -> "Tap to check optimisation status"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (state.batteryOptStatus) {
                            SettingsVMState.BatteryOptStatus.Granted    -> PaleGreenText.copy(alpha = 0.7f)
                            SettingsVMState.BatteryOptStatus.NotGranted -> PaleYellowText.copy(alpha = 0.8f)
                            SettingsVMState.BatteryOptStatus.Unknown    -> MutedGray
                        },
                    )
                }
                when (state.batteryOptStatus) {
                    SettingsVMState.BatteryOptStatus.Granted    -> CheckBadge()
                    SettingsVMState.BatteryOptStatus.NotGranted -> Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PaleYellowText)
                            .clickable(onClick = onBatteryOpt)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text("Fix", style = MaterialTheme.typography.labelMedium, color = White)
                    }
                    SettingsVMState.BatteryOptStatus.Unknown    -> ChevronRight(MutedGray)
                }
            }
        }
    }
}


@Composable
private fun AboutSection(
    state: SettingsVMState.AboutState,
    dataReset: SettingsVMState.DataResetState,
    onRetakeOnboarding: () -> Unit,
    onResetData: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsRow(
            label    = "Version",
            sublabel = null,
            trailing = {
                Text(state.appVersion, style = MaterialTheme.typography.bodySmall, color = MutedGray)
            },
        )

        SettingsRow(
            label    = "Retake onboarding",
            sublabel = "Choose your sound profile again",
            trailing = { ChevronRight(MutedGray) },
            onClick  = onRetakeOnboarding,
        )

        // Data reset
        SettingsCard(
            bg     = if (dataReset is SettingsVMState.DataResetState.Done) PaleGreen else SurfaceMuted,
            border = if (dataReset is SettingsVMState.DataResetState.Done) PaleGreenText else Border,
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Reset preferences",
                        style = MaterialTheme.typography.titleSmall,
                        color = when (dataReset) {
                            is SettingsVMState.DataResetState.Done  -> PaleGreenText
                            is SettingsVMState.DataResetState.Error -> PaleRedText
                            else                    -> PaleRedText
                        },
                    )
                    Text(
                        "Clears all settings — does not delete presets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                }
                when (dataReset) {
                    is SettingsVMState.DataResetState.Resetting -> LoadingDots(MutedGray)
                    is SettingsVMState.DataResetState.Done      -> CheckBadge()
                    else                        -> Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PaleRed)
                            .clickable(
                                enabled = dataReset is SettingsVMState.DataResetState.Idle || dataReset is SettingsVMState.DataResetState.Error,
                                onClick = onResetData,
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text("Reset", style = MaterialTheme.typography.labelMedium, color = PaleRedText)
                    }
                }
            }
        }
    }
}


@Composable
private fun DataResetDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = WarmWhite,
        title = {
            Text("Reset preferences?", style = MaterialTheme.typography.headlineSmall, color = Charcoal)
        },
        text = {
            Text(
                "This resets all settings to their defaults. Your presets and play history are not affected.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Reset", style = MaterialTheme.typography.labelLarge, color = PaleRedText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, color = MutedGray)
            }
        },
    )
}

@Composable
private fun RetakeOnboardingDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = WarmWhite,
        title = {
            Text("Change sound profile?", style = MaterialTheme.typography.headlineSmall, color = Charcoal)
        },
        text = {
            Text(
                "This takes you through onboarding again so you can choose tinnitus relief or general relaxation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Continue", style = MaterialTheme.typography.labelLarge, color = Charcoal)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, color = MutedGray)
            }
        },
    )
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text     = label.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = MutedGray,
        modifier = Modifier.padding(start = 20.dp, top = 28.dp, bottom = 10.dp),
    )
}

@Composable
private fun SettingsCard(
    bg: Color = White,
    border: Color = Border,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    label: String,
    sublabel: String?,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(White)
        .border(1.dp, Border, RoundedCornerShape(8.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 16.dp, vertical = 14.dp)

    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = Charcoal)
            if (sublabel != null) {
                Spacer(Modifier.height(2.dp))
                Text(sublabel, style = MaterialTheme.typography.bodySmall, color = MutedGray)
            }
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
private fun ToggleRow(
    label: String,
    sublabel: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(White)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { contentDescription = "$label, ${if (enabled) "on" else "off"}" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = Charcoal)
            Spacer(Modifier.height(2.dp))
            Text(sublabel, style = MaterialTheme.typography.bodySmall, color = MutedGray)
        }
        Spacer(Modifier.width(12.dp))
        TogglePill(enabled)
    }
}

@Composable
private fun StepSelectorRow(
    label: String,
    sublabel: String,
    steps: List<Pair<Long, String>>,
    selected: Long,
    onSelect: (Long) -> Unit,
) {
    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = Charcoal)
                Text(sublabel, style = MaterialTheme.typography.bodySmall, color = MutedGray)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(steps) { (ms, stepLabel) ->
                    val isSelected = ms == selected
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Charcoal else SurfaceMuted)
                            .border(1.dp, if (isSelected) Charcoal else Border, RoundedCornerShape(6.dp))
                            .clickable { onSelect(ms) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text  = stepLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) White else MutedGray,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Micro-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TogglePill(enabled: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) Charcoal else Border),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (enabled) "ON" else "OFF",
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) White else MutedGray,
        )
    }
}

@Composable
private fun StatusPill(label: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

@Composable
private fun CheckBadge() {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(PaleGreen),
        contentAlignment = Alignment.Center,
    ) {
        CheckIcon(PaleGreenText)
    }
}

@Composable
private fun SkeletonBox(height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(Border),
    )
}

@Composable
private fun LoadingDots(tint: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(tint))
        }
    }
}

@Composable
private fun HRule() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
}

@Composable
private fun SmallIconButton(
    onClick: () -> Unit,
    description: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(White)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
        content          = { content() },
    )
}

@Composable
private fun ChevronLeft(tint: Color) {
    Box(Modifier.size(16.dp).drawBehind {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width, 0f)
            lineTo(0f, size.height / 2f)
            lineTo(size.width, size.height)
        }
        drawPath(path, tint, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
    })
}

@Composable
private fun ChevronRight(tint: Color) {
    Box(Modifier.size(16.dp).drawBehind {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(0f, size.height)
        }
        drawPath(path, tint, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
    })
}

@Composable
private fun CheckIcon(tint: Color) {
    Box(Modifier.size(12.dp).drawBehind {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, size.height * 0.5f)
            lineTo(size.width * 0.38f, size.height)
            lineTo(size.width, 0f)
        }
        drawPath(path, tint, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
    })
}