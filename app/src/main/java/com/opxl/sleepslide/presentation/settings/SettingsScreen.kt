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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opxl.sleepslide.R
import com.opxl.sleepslide.domain.repository.CoachMarkScreens
import com.opxl.sleepslide.presentation.components.CircleIconButton
import com.opxl.sleepslide.presentation.components.Glyph
import com.opxl.sleepslide.presentation.components.GlyphIcon
import com.opxl.sleepslide.presentation.components.GroupCard
import com.opxl.sleepslide.presentation.components.IconCircle
import com.opxl.sleepslide.presentation.components.ListRow
import com.opxl.sleepslide.presentation.components.LoadingDots
import com.opxl.sleepslide.presentation.components.RowDivider
import com.opxl.sleepslide.presentation.components.ScreenTitle
import com.opxl.sleepslide.presentation.components.SectionLabel
import com.opxl.sleepslide.presentation.components.SelectChip
import com.opxl.sleepslide.presentation.components.SleepSwitch
import com.opxl.sleepslide.presentation.components.SurfaceCard
import com.opxl.sleepslide.presentation.components.Tag
import com.opxl.sleepslide.presentation.tutorial.CoachMarkHost
import com.opxl.sleepslide.presentation.tutorial.TutorialViewModel
import com.opxl.sleepslide.presentation.tutorial.coachMarkAnchor
import com.opxl.sleepslide.presentation.tutorial.tutorialViewModel
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.Dimens
import com.opxl.sleepslide.ui.theme.MutedGray
import com.opxl.sleepslide.ui.theme.OnPanel
import com.opxl.sleepslide.ui.theme.PaleBlueText
import com.opxl.sleepslide.ui.theme.PaleGreen
import com.opxl.sleepslide.ui.theme.PaleGreenText
import com.opxl.sleepslide.ui.theme.PaleRed
import com.opxl.sleepslide.ui.theme.PaleRedText
import com.opxl.sleepslide.ui.theme.PaleYellow
import com.opxl.sleepslide.ui.theme.PaleYellowText
import com.opxl.sleepslide.ui.theme.Panel
import com.opxl.sleepslide.ui.theme.SurfaceMuted
import com.opxl.sleepslide.ui.theme.WarmWhite
import com.opxl.sleepslide.ui.theme.White
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onNavigateToTutorial: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    tutorial: TutorialViewModel = tutorialViewModel(CoachMarkScreens.SETTINGS),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showUpgradePrompt by rememberSaveable { mutableStateOf(false) }
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
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.purchase_success)) }
                is SettingsVMState.SettingsEvent.RestoreSuccess   ->
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.purchase_restore_success)) }
                is SettingsVMState.SettingsEvent.NothingToRestore ->
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.purchase_nothing_to_restore)) }
                is SettingsVMState.SettingsEvent.ShowUpgradePrompt -> showUpgradePrompt = true
                is SettingsVMState.SettingsEvent.DataResetComplete ->
                    scope.launch { snackbarHostState.showSnackbar("Preferences reset") }
                is SettingsVMState.SettingsEvent.ShowError        ->
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                is SettingsVMState.SettingsEvent.ShowInfo         ->
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
            }
        }
    }

    CoachMarkHost(viewModel = tutorial, labels = SETTINGS_COACH_MARKS) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = Charcoal,
                    contentColor   = White,
                    actionColor    = PaleBlueText,
                    shape          = RoundedCornerShape(Dimens.controlRadius),
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
            contentPadding = PaddingValues(start = Dimens.gutter, end = Dimens.gutter, bottom = 32.dp),
        ) {
            item { SettingsTopBar(onBack = onNavigateBack) }

            item {
                AccountSection(
                    account  = uiState.account,
                    purchase = uiState.purchase,
                    onBuy    = { viewModel.purchase() },
                    onRestore = { viewModel.restorePurchases() },
                )
            }

            item {
                SectionLabel("Learn")
                LearnSection(
                    onTour      = onNavigateToTutorial,
                    onResetTips = { viewModel.resetCoachMarks() },
                )
            }

            item {
                SectionLabel("Playback")
                PlaybackSection(
                    state              = uiState.playback,
                    onFadeIn           = viewModel::setDefaultFadeIn,
                    onFadeOut          = viewModel::setDefaultFadeOut,
                    onTimerDuration    = viewModel::setDefaultTimerDuration,
                    onNightLockDefault = viewModel::setNightLockDefault,
                )
            }

            item {
                SectionLabel("Appearance")
                AppearanceSection(
                    state          = uiState.appearance,
                    onThemeMode    = viewModel::setThemeMode,
                    onStartHour    = viewModel::setDarkModeStartHour,
                    onEndHour      = viewModel::setDarkModeEndHour,
                )
            }

            item {
                SectionLabel("Wind-down reminder")
                WindDownSection(
                    state        = uiState.windDown,
                    onToggle     = viewModel::setWindDownEnabled,
                    onTimeChange = viewModel::setWindDownTime,
                    onTest       = { viewModel.sendTestWindDownNotification() },
                )
            }

            item {
                SectionLabel("Accessibility")
                AccessibilitySection(
                    state              = uiState.accessibility,
                    onHighContrast     = viewModel::setHighContrast,
                    onBatteryOpt       = { viewModel.openBatteryOptSettings() },
                    onResetBatteryPrompt = { viewModel.resetBatteryOptPrompt() },
                )
            }

            item {
                SectionLabel("About")
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
    } // CoachMarkHost

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

    // Only offered while FREE — the request may race a purchase that just completed.
    // A prompt requested during the Loading skeleton waits for the tier, and is dropped if it's PREMIUM.
    val freeAccount = uiState.account as? SettingsVMState.AccountState.Free
    LaunchedEffect(uiState.account is SettingsVMState.AccountState.Premium) {
        if (uiState.account is SettingsVMState.AccountState.Premium) showUpgradePrompt = false
    }
    if (showUpgradePrompt && freeAccount != null) {
        UpgradePromptDialog(
            price     = freeAccount.price,
            onConfirm = { showUpgradePrompt = false; viewModel.purchase() },
            onDismiss = { showUpgradePrompt = false },
        )
    }
}
/** Anchor key → label for the first-visit coach marks; only laid-out targets are shown. */
private val SETTINGS_COACH_MARKS = listOf(
    "tour"      to R.string.coach_settings_tour,
    "fades"     to R.string.coach_settings_fades,
    "wind_down" to R.string.coach_settings_wind_down,
)

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        CircleIconButton(onClick = onBack, contentDescription = stringResource(R.string.cd_back)) {
            GlyphIcon(Glyph.ChevronLeft, Charcoal, size = 16.dp)
        }
        Spacer(Modifier.height(18.dp))
        ScreenTitle(title = stringResource(R.string.tab_settings))
        Spacer(Modifier.height(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Account
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccountSection(
    account: SettingsVMState.AccountState,
    purchase: SettingsVMState.PurchaseOperationState,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (account) {
            is SettingsVMState.AccountState.Loading -> SkeletonBox(height = 148.dp)

            is SettingsVMState.AccountState.Premium -> {
                GroupCard {
                    ListRow(
                        title    = stringResource(R.string.purchase_premium_title),
                        subtitle = if (account.isRestored) stringResource(R.string.purchase_premium_restored)
                                   else stringResource(R.string.purchase_premium_lifetime),
                        leading  = {
                            IconCircle(PaleGreen) { GlyphIcon(Glyph.Sparkle, PaleGreenText, size = 18.dp) }
                        },
                        trailing = { CheckBadge() },
                        contentPadding = PaddingValues(16.dp),
                    )
                }
            }

            is SettingsVMState.AccountState.Free -> {
                UnlockHero(account = account, purchase = purchase, onBuy = onBuy)

                GroupCard {
                    ListRow(
                        title    = stringResource(R.string.purchase_restore_label),
                        subtitle = stringResource(R.string.purchase_restore_sublabel),
                        leading  = { SettingIcon(Glyph.Refresh) },
                        trailing = {
                            when (purchase) {
                                is SettingsVMState.PurchaseOperationState.Restoring ->
                                    LoadingDots(MutedGray)
                                is SettingsVMState.PurchaseOperationState.NothingToRestore ->
                                    Tag("Nothing found", PaleYellow, PaleYellowText)
                                else -> Chevron()
                            }
                        },
                        onClick  = if (purchase !is SettingsVMState.PurchaseOperationState.Restoring) onRestore else null,
                    )
                }
            }
        }
    }
}

/** The one inverted card on the page — the only thing here that asks for money. */
@Composable
private fun UnlockHero(
    account: SettingsVMState.AccountState.Free,
    purchase: SettingsVMState.PurchaseOperationState,
    onBuy: () -> Unit,
) {
    SurfaceCard(
        modifier       = Modifier.fillMaxWidth(),
        shape          = RoundedCornerShape(Dimens.panelRadius),
        background     = Panel,
        border         = Panel,
        contentPadding = PaddingValues(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconCircle(OnPanel.copy(alpha = 0.12f)) { GlyphIcon(Glyph.Sparkle, OnPanel, size = 18.dp) }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.purchase_unlock_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = OnPanel,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        account.price
                            ?.let { stringResource(R.string.purchase_unlock_subtitle_priced, it) }
                            ?: stringResource(R.string.purchase_unlock_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnPanel.copy(alpha = 0.65f),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            PurchaseButton(
                purchase    = purchase,
                canPurchase = account.canPurchase,
                onClick     = onBuy,
            )
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
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(OnPanel)
            .clickable(
                enabled = canPurchase && purchase is SettingsVMState.PurchaseOperationState.Idle,
                role    = Role.Button,
                onClick = onClick,
            )
            .alpha(if (canPurchase) 1f else 0.5f)
            .semantics { contentDescription = "Unlock all sounds" },
        contentAlignment = Alignment.Center,
    ) {
        when (purchase) {
            is SettingsVMState.PurchaseOperationState.Purchasing ->
                LoadingDots(Panel)
            is SettingsVMState.PurchaseOperationState.PurchaseSuccess ->
                Text("Unlocked ✓", style = MaterialTheme.typography.labelLarge, color = PaleGreenText)
            is SettingsVMState.PurchaseOperationState.Error ->
                Text("Try again", style = MaterialTheme.typography.labelLarge, color = PaleRedText)
            else ->
                Text("Unlock All", style = MaterialTheme.typography.labelLarge, color = Panel)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Learn
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LearnSection(onTour: () -> Unit, onResetTips: () -> Unit) {
    GroupCard {
        ListRow(
            title    = "Feature tour",
            subtitle = "Swipe through what SleepSlide can do",
            leading  = { SettingIcon(Glyph.Compass) },
            trailing = { Chevron() },
            onClick  = onTour,
            modifier = Modifier.coachMarkAnchor("tour"),
        )
        RowDivider(inset = DividerInset)
        ListRow(
            title    = "Show tips again",
            subtitle = "Replay the first-visit pointers on every screen",
            leading  = { SettingIcon(Glyph.Info) },
            trailing = { Chevron() },
            onClick  = onResetTips,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Playback
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlaybackSection(
    state: SettingsVMState.PlaybackSettingsState,
    onFadeIn: (Long) -> Unit,
    onFadeOut: (Long) -> Unit,
    onTimerDuration: (Long) -> Unit,
    onNightLockDefault: (Boolean) -> Unit,
) {
    GroupCard {
        StepSelectorRow(
            glyph    = Glyph.Volume,
            label    = "Fade in",
            sublabel = state.fadeInLabel,
            steps    = FADE_IN_STEPS_MS,
            selected = state.defaultFadeInMs,
            onSelect = onFadeIn,
            modifier = Modifier.coachMarkAnchor("fades"),
        )
        RowDivider(inset = DividerInset)
        StepSelectorRow(
            glyph    = Glyph.VolumeOff,
            label    = "Fade out",
            sublabel = state.fadeOutLabel,
            steps    = FADE_OUT_STEPS_MS,
            selected = state.defaultFadeOutMs,
            onSelect = onFadeOut,
        )
        RowDivider(inset = DividerInset)
        StepSelectorRow(
            glyph    = Glyph.Timer,
            label    = "Default timer",
            sublabel = state.timerLabel,
            steps    = DEFAULT_TIMER_STEPS_MS,
            selected = state.lastTimerDurationMs,
            onSelect = onTimerDuration,
        )
        RowDivider(inset = DividerInset)
        ToggleRow(
            glyph    = Glyph.Lock,
            label    = "Night lock by default",
            sublabel = "Automatically lock screen during playback",
            enabled  = state.isNightLockDefault,
            onToggle = onNightLockDefault,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Appearance
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppearanceSection(
    state: SettingsVMState.AppearanceState,
    onThemeMode: (Domain.ThemeMode) -> Unit,
    onStartHour: (Int) -> Unit,
    onEndHour: (Int) -> Unit,
) {
    GroupCard {
        Column(modifier = Modifier.padding(vertical = 14.dp)) {
            RowHeader(glyph = Glyph.Moon, label = "Theme")
            Spacer(Modifier.height(12.dp))
            ChipRow(Domain.ThemeMode.values().toList(), selected = { it == state.themeMode }) { mode ->
                SelectChip(
                    label    = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = mode == state.themeMode,
                    onClick  = { onThemeMode(mode) },
                )
            }
        }

        // Scheduled time window — only visible when SCHEDULED selected
        AnimatedVisibility(
            visible = state.showScheduleOptions,
            enter   = expandVertically(tween(250)) + fadeIn(tween(200)),
            exit    = shrinkVertically(tween(200)) + fadeOut(tween(150)),
        ) {
            Column {
                RowDivider(inset = DividerInset)
                HourStepRow(
                    glyph    = Glyph.Moon,
                    label    = "Dark from",
                    hour     = state.darkModeStartHour,
                    onSelect = onStartHour,
                )
                RowDivider(inset = DividerInset)
                HourStepRow(
                    glyph    = Glyph.Sun,
                    label    = "Light from",
                    hour     = state.darkModeEndHour,
                    onSelect = onEndHour,
                )
            }
        }
    }
}

@Composable
private fun HourStepRow(glyph: Glyph, label: String, hour: Int, onSelect: (Int) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 14.dp)) {
        RowHeader(glyph = glyph, label = label, value = "${hour}:00")
        Spacer(Modifier.height(12.dp))
        ChipRow(listOf(20, 21, 22, 23, 0, 6, 7, 8, 9), selected = { it == hour }) { h ->
            SelectChip(label = "${h}:00", selected = h == hour, onClick = { onSelect(h) })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Wind-down reminder
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WindDownSection(
    state: SettingsVMState.WindDownState,
    onToggle: (Boolean) -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    onTest: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GroupCard {
            // Master toggle
            ToggleRow(
                glyph    = Glyph.Bell,
                label    = "Daily reminder",
                sublabel = if (state.isEnabled)
                    "Fires at ${state.formattedTime} every day"
                else
                    "Get a gentle nudge before your wind-down time",
                enabled  = state.isEnabled,
                onToggle = onToggle,
                modifier = Modifier.coachMarkAnchor("wind_down"),
            )

            // Time picker — only shown when enabled
            AnimatedVisibility(
                visible = state.isEnabled,
                enter   = expandVertically(tween(250)) + fadeIn(tween(200)),
                exit    = shrinkVertically(tween(200)) + fadeOut(tween(150)),
            ) {
                Column {
                    RowDivider(inset = DividerInset)
                    Column(modifier = Modifier.padding(vertical = 14.dp)) {
                        RowHeader(glyph = Glyph.Clock, label = "Reminder time", value = state.formattedTime)
                        Spacer(Modifier.height(14.dp))
                        ChipCaption("Hour")
                        // Evening hours are the relevant ones
                        ChipRow((18..23).toList(), selected = { it == state.hour }) { h ->
                            SelectChip(
                                label    = "${h}:00",
                                selected = h == state.hour,
                                onClick  = { onTimeChange(h, state.minute) },
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        ChipCaption("Minute")
                        ChipRow(listOf(0, 15, 30, 45), selected = { it == state.minute }) { m ->
                            SelectChip(
                                label    = "%02d".format(m),
                                selected = m == state.minute,
                                onClick  = { onTimeChange(state.hour, m) },
                            )
                        }
                    }
                    RowDivider(inset = DividerInset)
                    ListRow(
                        title    = "Send test notification",
                        subtitle = "Preview what the reminder looks like",
                        leading  = { SettingIcon(Glyph.Sparkle) },
                        trailing = { Chevron() },
                        onClick  = onTest,
                    )
                }
            }
        }

        // SCHEDULE_EXACT_ALARM permission warning — API 31+
        AnimatedVisibility(
            visible = state.isEnabled && !state.exactAlarmPermissionGranted,
            enter   = expandVertically(tween(250)) + fadeIn(tween(200)),
            exit    = shrinkVertically(tween(200)) + fadeOut(tween(150)),
        ) {
            SurfaceCard(
                modifier   = Modifier.fillMaxWidth(),
                background = PaleYellow,
                border     = PaleYellow,
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    GlyphIcon(Glyph.Info, PaleYellowText, size = 18.dp, modifier = Modifier.padding(top = 1.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Precise timing requires permission",
                            style = MaterialTheme.typography.titleSmall,
                            color = PaleYellowText,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Enable 'Alarms & reminders' for the exact time. Without it, the reminder may arrive a few minutes late.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PaleYellowText.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Accessibility
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccessibilitySection(
    state: SettingsVMState.AccessibilityState,
    onHighContrast: (Boolean) -> Unit,
    onBatteryOpt: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onResetBatteryPrompt: () -> Unit,
) {
    val status = state.batteryOptStatus
    val (circleBg, circleFg) = when (status) {
        SettingsVMState.BatteryOptStatus.Granted    -> PaleGreen to PaleGreenText
        SettingsVMState.BatteryOptStatus.NotGranted -> PaleYellow to PaleYellowText
        SettingsVMState.BatteryOptStatus.Unknown    -> SurfaceMuted to Charcoal
    }

    GroupCard {
        ToggleRow(
            glyph    = Glyph.Eye,
            label    = "High contrast",
            sublabel = "Increases colour contrast for readability",
            enabled  = state.isHighContrastEnabled,
            onToggle = onHighContrast,
        )
        RowDivider(inset = DividerInset)
        // Battery optimisation — shows real status + action
        ListRow(
            title    = "Background audio",
            subtitle = when (status) {
                SettingsVMState.BatteryOptStatus.Granted    -> "Battery optimisation disabled — audio won't stop"
                SettingsVMState.BatteryOptStatus.NotGranted -> "Enable to prevent audio stopping at night"
                SettingsVMState.BatteryOptStatus.Unknown    -> "Tap to check optimisation status"
            },
            leading  = { IconCircle(circleBg) { GlyphIcon(Glyph.Battery, circleFg, size = 18.dp) } },
            trailing = {
                when (status) {
                    SettingsVMState.BatteryOptStatus.Granted    -> CheckBadge()
                    SettingsVMState.BatteryOptStatus.NotGranted -> SmallAction("Fix", PaleYellowText, White, onBatteryOpt)
                    SettingsVMState.BatteryOptStatus.Unknown    -> Chevron()
                }
            },
            onClick  = if (status != SettingsVMState.BatteryOptStatus.Granted) onBatteryOpt else null,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// About
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AboutSection(
    state: SettingsVMState.AboutState,
    dataReset: SettingsVMState.DataResetState,
    onRetakeOnboarding: () -> Unit,
    onResetData: () -> Unit,
) {
    val canReset = dataReset is SettingsVMState.DataResetState.Idle || dataReset is SettingsVMState.DataResetState.Error
    val isDone   = dataReset is SettingsVMState.DataResetState.Done

    GroupCard {
        ListRow(
            title    = "Version",
            leading  = { SettingIcon(Glyph.Info) },
            trailing = {
                Text(state.appVersion, style = MaterialTheme.typography.bodyMedium, color = MutedGray)
            },
        )
        RowDivider(inset = DividerInset)
        ListRow(
            title    = "Retake onboarding",
            subtitle = "Choose your sound profile again",
            leading  = { SettingIcon(Glyph.Compass) },
            trailing = { Chevron() },
            onClick  = onRetakeOnboarding,
        )
        RowDivider(inset = DividerInset)
        // Data reset
        ListRow(
            title      = "Reset preferences",
            subtitle   = "Clears all settings — does not delete presets",
            titleColor = if (isDone) PaleGreenText else PaleRedText,
            leading    = {
                IconCircle(if (isDone) PaleGreen else PaleRed) {
                    GlyphIcon(if (isDone) Glyph.Check else Glyph.Trash, if (isDone) PaleGreenText else PaleRedText, size = 18.dp)
                }
            },
            trailing   = {
                when (dataReset) {
                    is SettingsVMState.DataResetState.Resetting -> LoadingDots(MutedGray)
                    is SettingsVMState.DataResetState.Done      -> CheckBadge()
                    else -> SmallAction("Reset", PaleRed, PaleRedText, onResetData, enabled = canReset)
                }
            },
            onClick    = if (canReset) onResetData else null,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialogs
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DataResetDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = White,
        shape            = RoundedCornerShape(Dimens.panelRadius),
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
private fun UpgradePromptDialog(price: String?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = White,
        shape            = RoundedCornerShape(Dimens.panelRadius),
        icon = {
            IconCircle(PaleYellow, size = 48.dp) { GlyphIcon(Glyph.Sparkle, PaleYellowText, size = 22.dp) }
        },
        title = {
            Text(stringResource(R.string.purchase_prompt_title), style = MaterialTheme.typography.headlineSmall, color = Charcoal)
        },
        text = {
            Text(
                stringResource(R.string.purchase_prompt_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    price?.let { stringResource(R.string.purchase_prompt_confirm_priced, it) }
                        ?: stringResource(R.string.purchase_prompt_confirm),
                    style = MaterialTheme.typography.labelLarge,
                    color = Charcoal,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.purchase_prompt_dismiss), style = MaterialTheme.typography.labelLarge, color = MutedGray)
            }
        },
    )
}

@Composable
private fun RetakeOnboardingDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = White,
        shape            = RoundedCornerShape(Dimens.panelRadius),
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

// ─────────────────────────────────────────────────────────────────────────────
// Rows
// ─────────────────────────────────────────────────────────────────────────────

/** Dividers start under the title, past the leading circle — the grouped-list convention. */
private val DividerInset = 16.dp + Dimens.leadingCircle + 14.dp

/** A grouped-list row with a switch. The whole row toggles; the switch only shows state. */
@Composable
private fun ToggleRow(
    glyph: Glyph,
    label: String,
    sublabel: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListRow(
        title    = label,
        subtitle = sublabel,
        leading  = { SettingIcon(glyph) },
        trailing = { SleepSwitch(checked = enabled) },
        onClick  = { onToggle(!enabled) },
        modifier = modifier.semantics { contentDescription = "$label, ${if (enabled) "on" else "off"}" },
    )
}

/** Header (glyph · label · current value) over a scrolling row of choices. */
@Composable
private fun StepSelectorRow(
    glyph: Glyph,
    label: String,
    sublabel: String,
    steps: List<Pair<Long, String>>,
    selected: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        RowHeader(glyph = glyph, label = label, value = sublabel)
        Spacer(Modifier.height(12.dp))
        ChipRow(steps, selected = { it.first == selected }) { (ms, stepLabel) ->
            SelectChip(label = stepLabel, selected = ms == selected, onClick = { onSelect(ms) })
        }
    }
}

@Composable
private fun RowHeader(glyph: Glyph, label: String, value: String? = null) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(glyph)
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            style    = MaterialTheme.typography.titleSmall,
            color    = Charcoal,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Spacer(Modifier.width(12.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MutedGray)
        }
    }
}

/**
 * Chips line up with the row's title and scroll to the card's edge. Opens scrolled so the
 * [selected] chip is in view, with its left neighbour peeking as a hint that the row scrolls.
 */
@Composable
private fun <T> ChipRow(items: List<T>, selected: (T) -> Boolean, chip: @Composable (T) -> Unit) {
    val selectedIndex = items.indexOfFirst(selected)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedIndex - 1).coerceAtLeast(0))
    LazyRow(
        state                 = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding        = PaddingValues(start = DividerInset, end = 16.dp),
    ) {
        items(items) { chip(it) }
    }
}

@Composable
private fun ChipCaption(text: String) {
    Text(
        text,
        style    = MaterialTheme.typography.labelSmall,
        color    = MutedGray,
        modifier = Modifier.padding(start = DividerInset, bottom = 8.dp),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Micro-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingIcon(glyph: Glyph) {
    IconCircle(SurfaceMuted) { GlyphIcon(glyph, Charcoal, size = 18.dp) }
}

@Composable
private fun Chevron() = GlyphIcon(Glyph.ChevronRight, MutedGray, size = 16.dp)

@Composable
private fun SmallAction(
    label: String,
    background: Color,
    contentColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = contentColor)
    }
}

@Composable
private fun CheckBadge() {
    IconCircle(PaleGreen, size = 28.dp) { GlyphIcon(Glyph.Check, PaleGreenText, size = 14.dp) }
}

@Composable
private fun SkeletonBox(height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(Dimens.panelRadius))
            .background(Border),
    )
}
