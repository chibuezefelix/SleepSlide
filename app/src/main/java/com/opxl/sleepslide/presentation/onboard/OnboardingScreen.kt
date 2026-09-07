package com.opxl.sleepslide.presentation.onboard


import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.presentation.onboard.OnboardingVMState.ONBOARDING_PATHS
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.MutedGray
import com.opxl.sleepslide.ui.theme.PaleBlue
import com.opxl.sleepslide.ui.theme.PaleBlueText
import com.opxl.sleepslide.ui.theme.PaleGreen
import com.opxl.sleepslide.ui.theme.PaleGreenText
import com.opxl.sleepslide.ui.theme.PaleYellow
import com.opxl.sleepslide.ui.theme.PaleYellowText
import com.opxl.sleepslide.ui.theme.SurfaceMuted
import com.opxl.sleepslide.ui.theme.WarmWhite
import com.opxl.sleepslide.ui.theme.White
import kotlinx.coroutines.launch

// Screen

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Notification permission launcher — Android 13+
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OnboardingVMState.OnboardingEvent.NavigateToHome ->
                    onComplete()
                is OnboardingVMState.OnboardingEvent.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onNotificationPermissionResult(true)
                    }
                }
                is OnboardingVMState.OnboardingEvent.OpenBatterySettings -> {
                    onOpenBatterySettings()
                    viewModel.onReturnFromBatterySettings()
                }
                is OnboardingVMState.OnboardingEvent.ShowError ->
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmWhite)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        // Step progress dots
        StepProgressBar(
            current = uiState.step,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
        )

        // Animated step content
        AnimatedContent(
            targetState   = uiState.step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(tween(350)) { if (forward) it else -it } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(300)) { if (forward) -it else it } + fadeOut(tween(250)))
            },
            label         = "onboarding_step",
            modifier      = Modifier.fillMaxSize(),
        ) { step ->
            when (step) {
                is OnboardingVMState.OnboardingStep.Welcome       -> WelcomeStep(
                    onContinue = { viewModel.onWelcomeContinue() },
                )
                is OnboardingVMState.OnboardingStep.PathSelection -> PathSelectionStep(
                    selectedPath = uiState.selectedPath,
                    isSaving     = uiState.isSaving,
                    onSelect     = viewModel::onPathSelected,
                    onContinue   = { viewModel.onPathSelectionContinue() },
                    onSkip       = { viewModel.onSkip() },
                    onBack       = { viewModel.onBack() },
                )
                is OnboardingVMState.OnboardingStep.Permissions   -> PermissionsStep(
                    state        = uiState.permissionsState,
                    onNotification = { viewModel.requestNotificationPermission() },
                    onBattery    = { viewModel.requestBatteryOptimisation() },
                    onContinue   = { viewModel.onPermissionsContinue() },
                    onBack       = { viewModel.onBack() },
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter),
        ) { data ->
            Snackbar(
                snackbarData   = data,
                containerColor = Charcoal,
                contentColor   = White,
                shape          = RoundedCornerShape(8.dp),
            )
        }
    }
}

// Extension for AnimatedContent transition direction
private val OnboardingVMState.OnboardingStep.ordinal: Int get() = when (this) {
    is OnboardingVMState.OnboardingStep.Welcome       -> 0
    is OnboardingVMState.OnboardingStep.PathSelection -> 1
    is OnboardingVMState.OnboardingStep.Permissions   -> 2
}

// Step progress

@Composable
private fun StepProgressBar(current: OnboardingVMState.OnboardingStep, modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        listOf(OnboardingVMState.OnboardingStep.Welcome, OnboardingVMState.OnboardingStep.PathSelection, OnboardingVMState.OnboardingStep.Permissions)
            .forEachIndexed { index, step ->
                val isPast    = index < current.ordinal
                val isCurrent = index == current.ordinal
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .width(if (isCurrent) 20.dp else 12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                isCurrent -> Charcoal
                                isPast    -> MutedGray
                                else      -> Border
                            }
                        ),
                )
            }
    }
}

// Step 1 — Welcome

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Moon glyph — drawn primitive, no asset
        MoonGlyph()

        Spacer(Modifier.height(40.dp))

        Text(
            text      = "SleepSlide",
            style     = MaterialTheme.typography.displaySmall,
            color     = Charcoal,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text      = "Sound that works while you sleep.",
            style     = MaterialTheme.typography.bodyLarge,
            color     = MutedGray,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text      = "No account. No subscription. No ads during playback.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MutedGray.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(64.dp))

        PrimaryButton(
            label   = "Get started",
            onClick = onContinue,
        )

        Spacer(Modifier.height(16.dp))

        // Feature pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            FeaturePill("Offline sounds")
            FeaturePill("No subscription")
            FeaturePill("Gapless loop")
        }

        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

// Step 2 — Path selection

@Composable
private fun PathSelectionStep(
    selectedPath: Domain.OnboardingPath?,
    isSaving: Boolean,
    onSelect: (Domain.OnboardingPath) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(72.dp))

        Text(
            text      = "What brings you here?",
            style     = MaterialTheme.typography.headlineMedium,
            color     = Charcoal,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text      = "We'll show you the most useful sounds first. You can change this later.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MutedGray,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(40.dp))

        ONBOARDING_PATHS.forEach { option ->
            PathCard(
                option     = option,
                isSelected = selectedPath == option.path,
                onSelect   = { onSelect(option.path) },
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.weight(1f))

        PrimaryButton(
            label   = if (isSaving) "Setting up…" else "Continue",
            enabled = selectedPath != null && !isSaving,
            onClick = onContinue,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text  = "Skip — I'll explore on my own",
            style = MaterialTheme.typography.labelLarge,
            color = MutedGray,
            modifier = Modifier
                .clickable(onClick = onSkip)
                .padding(vertical = 8.dp)
                .semantics { contentDescription = "Skip onboarding" },
        )

        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun PathCard(
    option: OnboardingVMState.PathOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue   = if (isSelected) Charcoal else White,
        animationSpec = tween(250),
        label         = "path_bg_${option.path}",
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isSelected) Charcoal else Border,
        animationSpec = tween(250),
        label         = "path_border_${option.path}",
    )
    val textColor    = if (isSelected) White else Charcoal
    val subColor     = if (isSelected) White.copy(alpha = 0.7f) else MutedGray
    val pillBg       = if (isSelected) White.copy(alpha = 0.15f) else SurfaceMuted
    val pillFg       = if (isSelected) White.copy(alpha = 0.8f) else MutedGray

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .padding(20.dp)
            .semantics {
                contentDescription = "${option.title}, ${if (isSelected) "selected" else "not selected"}"
            },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text  = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CheckMark(White)
                    }
                }
            }

            Text(
                text  = option.description,
                style = MaterialTheme.typography.bodyMedium,
                color = subColor,
            )

            Text(
                text  = option.whoIsThisFor,
                style = MaterialTheme.typography.bodySmall,
                color = subColor.copy(alpha = 0.6f),
            )

            // Sound pills
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                option.sounds.forEach { sound ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(pillBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text  = sound,
                            style = MaterialTheme.typography.labelSmall,
                            color = pillFg,
                        )
                    }
                }
            }
        }
    }
}

// Step 3 — Permissions

@Composable
private fun PermissionsStep(
    state: OnboardingVMState.PermissionsState,
    onNotification: () -> Unit,
    onBattery: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(72.dp))

        Text(
            text      = "One more thing",
            style     = MaterialTheme.typography.headlineMedium,
            color     = Charcoal,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text      = "These help your sound play all night without stopping.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MutedGray,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(40.dp))

        // Notification permission
        PermissionCard(
            title       = "Notifications",
            description = "Shows a persistent control so you can pause or stop from the lock screen.",
            isGranted   = state.notificationGranted,
            onRequest   = onNotification,
        )

        Spacer(Modifier.height(12.dp))

        // Battery optimisation
        PermissionCard(
            title       = "Background audio",
            description = "Prevents Android from stopping the sound at night when your phone is idle.",
            isGranted   = state.batteryOptGranted,
            onRequest   = onBattery,
        )

        Spacer(Modifier.weight(1f))

        val allGranted = state.notificationGranted && state.batteryOptGranted

        PrimaryButton(
            label   = if (allGranted) "All set — let's go" else "Continue anyway",
            onClick  = onContinue,
            bg      = if (allGranted) Charcoal else SurfaceMuted,
            textColor = if (allGranted) White else MutedGray,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text  = "You can always enable these in Settings",
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isGranted) PaleGreen else White)
            .border(
                1.dp,
                if (isGranted) PaleGreenText else Border,
                RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isGranted) PaleGreenText else Charcoal,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isGranted) PaleGreenText.copy(alpha = 0.7f) else MutedGray,
                )
            }
            Spacer(Modifier.width(12.dp))
            if (isGranted) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PaleGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    CheckMark(PaleGreenText)
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Charcoal)
                        .clickable(onClick = onRequest)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .semantics { contentDescription = "Enable $title" },
                ) {
                    Text(
                        "Enable",
                        style = MaterialTheme.typography.labelLarge,
                        color = White,
                    )
                }
            }
        }
    }
}

// Shared components

@Composable
private fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    bg: Color = Charcoal,
    textColor: Color = White,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) bg else SurfaceMuted)
            .border(
                1.dp,
                if (enabled) bg else Border,
                RoundedCornerShape(10.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (enabled) textColor else MutedGray,
        )
    }
}

@Composable
private fun FeaturePill(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceMuted)
            .border(1.dp, Border, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MutedGray,
        )
    }
}

@Composable
private fun MoonGlyph() {
    Box(
        modifier = Modifier
            .size(80.dp)
            .drawBehind {
                val r = size.minDimension / 2f
                drawArc(
                    color      = Charcoal,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter  = false,
                    style      = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
                )
                // Inner cut-out circle for crescent effect
                drawCircle(
                    color  = WarmWhite,
                    radius = r * 0.7f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.65f, size.height * 0.35f),
                )
            }
    )
}

@Composable
private fun CheckMark(tint: Color) {
    Box(Modifier.size(12.dp).drawBehind {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, size.height * 0.5f)
            lineTo(size.width * 0.38f, size.height)
            lineTo(size.width, 0f)
        }
        drawPath(
            path  = path,
            color = tint,
            style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round),
        )
    })
}