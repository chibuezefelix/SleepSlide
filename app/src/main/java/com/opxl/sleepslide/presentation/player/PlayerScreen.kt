package com.opxl.sleepslide.presentation.player


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.DarkBackground
import com.opxl.sleepslide.ui.theme.DarkBorder
import com.opxl.sleepslide.ui.theme.DarkSurface
import com.opxl.sleepslide.ui.theme.DarkText
import com.opxl.sleepslide.ui.theme.DarkTextSecondary
import com.opxl.sleepslide.ui.theme.LocalSleepSlideColors
import com.opxl.sleepslide.ui.theme.MutedGray
import com.opxl.sleepslide.ui.theme.PaleBlue
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
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Sheet / dialog visibility
    var showTimerSheet      by rememberSaveable { mutableStateOf(false) }
    var showSaveDialog      by rememberSaveable { mutableStateOf(false) }
    var showUnsavedDialog   by rememberSaveable { mutableStateOf(false) }

    // One-shot event handler
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PlayerEvent.NavigateBack              -> onNavigateBack()
                is PlayerEvent.NavigateToLibrary         -> onNavigateToLibrary()
                is PlayerEvent.ShowTimerPicker           -> showTimerSheet = true
                is PlayerEvent.ShowSavePresetDialog      -> showSaveDialog = true
                is PlayerEvent.ShowUnsavedChangesDialog  -> showUnsavedDialog = true
                is PlayerEvent.RequestNotificationPermission -> onRequestNotificationPermission()
                is PlayerEvent.ShowError                 ->
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                is PlayerEvent.ShowInfo                  ->
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                is PlayerEvent.ShowServiceUnavailable    ->
                    scope.launch { snackbarHostState.showSnackbar("Audio service is starting") }
                is PlayerEvent.AudioInterrupted          -> {
                    val msg = when (event.reason) {
                        InterruptionReason.PHONE_CALL     -> "Sound paused for call — will resume"
                        InterruptionReason.ANOTHER_APP    -> "Sound paused by another app"
                        InterruptionReason.BLUETOOTH_LOST -> "Headphones disconnected"
                    }
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
                is PlayerEvent.AudioRestoredAfterCall    ->
                    scope.launch { snackbarHostState.showSnackbar("Sound resumed") }
                is PlayerEvent.TimerFinished             ->
                    scope.launch { snackbarHostState.showSnackbar("Sleep timer ended") }
                is PlayerEvent.NightLockEnabled          ->
                    scope.launch { snackbarHostState.showSnackbar("Night lock on — long press to unlock") }
                else -> Unit
            }
        }
    }

    // Night lock covers the entire screen — rendered above Scaffold
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost     = { SnackbarHost(snackbarHostState) },
            containerColor   = WarmWhite,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .verticalScroll(rememberScrollState()),
            ) {
                PlayerTopBar(
                    preset        = uiState.preset,
                    onBack        = { viewModel.onBackRequested() },
                    onSave        = { viewModel.requestSaveAsPreset() },
                    onUpdate      = { viewModel.updateActivePreset() },
                )

                Spacer(Modifier.height(24.dp))

                PlaybackHub(
                    playback  = uiState.playback,
                    timer     = uiState.timer,
                    bluetooth = uiState.isBluetoothConnected,
                    onPlay    = { viewModel.resume() },
                    onPause   = { viewModel.pause() },
                    onStop    = { viewModel.stop() },
                    onTimer   = { viewModel.requestTimer() },
                )

                Spacer(Modifier.height(32.dp))

                AnimatedVisibility(
                    visible = uiState.playback is PlayerPlaybackState.Interrupted,
                    enter   = fadeIn(tween(250)) + slideInVertically { -it },
                    exit    = fadeOut(tween(200)) + slideOutVertically { -it },
                ) {
                    val reason = (uiState.playback as? PlayerPlaybackState.Interrupted)?.reason
                    InterruptionBanner(reason = reason)
                }

                MixerSection(
                    mixer       = uiState.mixer,
                    onVolume    = { pos, vol -> viewModel.onLayerVolumeChanged(pos, vol) },
                    onDragEnd   = { pos -> viewModel.onLayerVolumeDragEnded(pos) },
                    onMute      = { pos -> viewModel.muteLayer(pos) },
                    onUnmute    = { pos -> viewModel.unmuteLayer(pos) },
                    onRemove    = { pos -> viewModel.removeLayer(pos) },
                    onAddSound  = { viewModel.onAddSoundRequested() },
                    masterVolume        = uiState.mixer.masterVolume,
                    onMasterVolume      = { viewModel.onMasterVolumeChanged(it) },
                    onMasterDragEnd     = { viewModel.onMasterVolumeDragEnded() },
                )

                Spacer(Modifier.height(32.dp))

                TimerStrip(
                    timer           = uiState.timer,
                    onAddTime       = { viewModel.addFiveMinutes() },
                    onReduceTime    = { viewModel.reduceFiveMinutes() },
                    onCancel        = { viewModel.cancelTimer() },
                    onSetTimer      = { viewModel.requestTimer() },
                )

                Spacer(Modifier.height(16.dp))
                NightLockToggleRow(
                    nightLock = uiState.nightLock,
                    onEnable  = { viewModel.enableNightLock() },
                    onDisable = { viewModel.disableNightLock() },
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)  // ← here, on the modifier
                        .verticalScroll(rememberScrollState()),
                ) {
                    // content
                    Spacer(Modifier.height(40.dp))
                    // remove the bare windowInsetsPadding call
                }
            }
        }

        // Night lock overlay — sits above everything, intercepts all touches
        if (uiState.nightLock != NightLockState.Disabled) {
            NightLockOverlay(
                state                 = uiState.nightLock,
                onLongPressStarted    = { viewModel.onNightLockLongPressStarted() },
                onLongPressCompleted  = { viewModel.onNightLockLongPressCompleted() },
                onLongPressCancelled  = { viewModel.onNightLockLongPressCancelled() },
            )
        }
    }


    if (showTimerSheet) {
        TimerPickerSheet(
            currentTimer    = uiState.timer,
            lastDuration    = null, // ViewModel surfaces this via preferences
            onDismiss       = { showTimerSheet = false },
            onSelect        = { durationMs ->
                showTimerSheet = false
                viewModel.startTimer(durationMs)
            },
            onCancel        = {
                showTimerSheet = false
                viewModel.cancelTimer()
            },
        )
    }

    if (showSaveDialog) {
        SavePresetDialog(
            existingName = (uiState.preset as? ActivePresetState.Loaded)?.preset?.name ?: "",
            onDismiss    = { showSaveDialog = false },
            onSave       = { name, emoji ->
                showSaveDialog = false
                viewModel.saveAsNewPreset(name, emoji)
            },
        )
    }

    if (showUnsavedDialog) {
        UnsavedChangesDialog(
            onSave    = {
                showUnsavedDialog = false
                viewModel.updateActivePreset()
            },
            onDiscard = {
                showUnsavedDialog = false
                viewModel.discardChangesAndNavigateBack()
            },
            onDismiss = { showUnsavedDialog = false },
        )
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun PlayerTopBar(
    preset: ActivePresetState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onUpdate: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Back chevron
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .background(White)
                .clickable(onClick = onBack)
                .semantics { contentDescription = "Back" },
            contentAlignment = Alignment.Center,
        ) {
            ChevronLeft(tint = Charcoal)
        }

        // Preset name / title
        Column(
            modifier            = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (preset) {
                is ActivePresetState.Loaded -> {
                    Text(
                        text      = preset.preset.name,
                        style     = MaterialTheme.typography.titleMedium,
                        color     = Charcoal,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    if (preset.isDirty) {
                        Text(
                            text  = "Unsaved changes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGray,
                        )
                    }
                }
                is ActivePresetState.Saving -> {
                    Text(
                        text  = "Saving…",
                        style = MaterialTheme.typography.titleMedium,
                        color = MutedGray,
                    )
                }
                is ActivePresetState.None -> {
                    Text(
                        text  = "Now playing",
                        style = MaterialTheme.typography.titleMedium,
                        color = Charcoal,
                    )
                }
                else -> Unit
            }
        }

        // Save / update action
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        preset is ActivePresetState.Loaded && preset.isDirty -> Charcoal
                        preset is ActivePresetState.None -> SurfaceMuted
                        else -> SurfaceMuted
                    }
                )
                .border(
                    1.dp,
                    if (preset is ActivePresetState.Loaded && preset.isDirty) Charcoal else Border,
                    RoundedCornerShape(8.dp),
                )
                .clickable(
                    onClick = {
                        when (preset) {
                            is ActivePresetState.Loaded -> if (preset.isDirty) onUpdate() else Unit
                            else -> onSave()
                        }
                    }
                )
                .semantics {
                    contentDescription = when {
                        preset is ActivePresetState.Loaded && preset.isDirty -> "Save changes"
                        else -> "Save as preset"
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            // Floppy / save icon — drawn as primitive
            SaveIcon(
                tint = when {
                    preset is ActivePresetState.Loaded && preset.isDirty -> White
                    else -> MutedGray
                }
            )
        }
    }
}

// ── Playback hub ──────────────────────────────────────────────────────────────

@Composable
private fun PlaybackHub(
    playback: PlayerPlaybackState,
    timer: PlayerTimerState,
    bluetooth: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onTimer: () -> Unit,
) {
    val isPlaying = playback is PlayerPlaybackState.Playing
    val isLoading = playback is PlayerPlaybackState.Loading
    val isFadingIn = playback is PlayerPlaybackState.FadingIn
    val fadeProgress = (playback as? PlayerPlaybackState.Playing)?.fadeInProgress

    // Timer arc progress
    val timerFraction = (timer as? PlayerTimerState.Active)?.progressFraction ?: 0f
    val isFading = (timer as? PlayerTimerState.Active)?.isFading == true

    val arcColor by animateColorAsState(
        targetValue = when {
            isFading  -> PaleYellowText
            isPlaying -> Charcoal
            else      -> Border
        },
        animationSpec = tween(600),
        label = "arc_color",
    )

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Central arc + transport
        Box(
            modifier         = Modifier
                .size(200.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Timer arc drawn behind controls
            TimerArc(
                progress = timerFraction,
                color    = arcColor,
                modifier = Modifier.fillMaxSize(),
            )

            // Fade-in progress arc overlay
            if (isFadingIn && fadeProgress != null) {
                FadeInArc(
                    progress = fadeProgress,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Central button cluster
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MainTransportButton(
                    playback  = playback,
                    onPlay    = onPlay,
                    onPause   = onPause,
                )

                AnimatedVisibility(visible = isPlaying || playback is PlayerPlaybackState.Paused) {
                    StopButtonSmall(onClick = onStop)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Status line
        PlaybackStatusLine(playback = playback, bluetooth = bluetooth)

        Spacer(Modifier.height(16.dp))

        // Timer trigger button
        TimerTriggerButton(timer = timer, onClick = onTimer)
    }
}

@Composable
private fun TimerArc(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.drawBehind {
            val strokeWidth = 2.dp.toPx()
            val radius = size.minDimension / 2f - strokeWidth / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Background track
            drawArc(
                color       = Border,
                startAngle  = -90f,
                sweepAngle  = 360f,
                useCenter   = false,
                topLeft     = Offset(center.x - radius, center.y - radius),
                size        = Size(radius * 2, radius * 2),
                style       = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Progress arc
            if (progress > 0f) {
                drawArc(
                    color       = color,
                    startAngle  = -90f,
                    sweepAngle  = 360f * progress,
                    useCenter   = false,
                    topLeft     = Offset(center.x - radius, center.y - radius),
                    size        = Size(radius * 2, radius * 2),
                    style       = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
    )
}

@Composable
private fun FadeInArc(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.drawBehind {
            val strokeWidth = 3.dp.toPx()
            val radius = size.minDimension / 2f - strokeWidth
            val center = Offset(size.width / 2f, size.height / 2f)
            drawArc(
                color       = PaleBlueText.copy(alpha = 0.4f),
                startAngle  = -90f,
                sweepAngle  = 360f * progress,
                useCenter   = false,
                topLeft     = Offset(center.x - radius, center.y - radius),
                size        = Size(radius * 2, radius * 2),
                style       = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    )
}

@Composable
private fun MainTransportButton(
    playback: PlayerPlaybackState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
) {
    val isLoading  = playback is PlayerPlaybackState.Loading ||
            playback is PlayerPlaybackState.FadingIn
    val isPlaying  = playback is PlayerPlaybackState.Playing

    val bgColor by animateColorAsState(
        targetValue    = if (isPlaying) Charcoal else White,
        animationSpec  = tween(300),
        label          = "transport_bg",
    )
    val iconTint by animateColorAsState(
        targetValue    = if (isPlaying) White else Charcoal,
        animationSpec  = tween(300),
        label          = "transport_icon",
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, Border, CircleShape)
            .clickable(
                enabled = !isLoading,
                onClick = if (isPlaying) onPause else onPlay,
            )
            .semantics {
                contentDescription = when {
                    isLoading -> "Loading"
                    isPlaying -> "Pause"
                    else      -> "Play"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState   = isLoading to isPlaying,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label         = "transport_icon_state",
        ) { (loading, playing) ->
            when {
                loading -> LoadingDots(tint = MutedGray)
                playing -> PauseIcon(tint = iconTint, sizeDp = 22)
                else    -> PlayIcon(tint = iconTint, sizeDp = 22)
            }
        }
    }
}

@Composable
private fun StopButtonSmall(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(SurfaceMuted)
            .border(1.dp, Border, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Stop" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MutedGray),
        )
    }
}

@Composable
private fun PlaybackStatusLine(
    playback: PlayerPlaybackState,
    bluetooth: Boolean,
) {
    val (label, color) = when (playback) {
        is PlayerPlaybackState.Playing    ->
            if (playback.isPlayingInBackground) "Playing in background" to PaleGreenText
            else "Playing" to MutedGray
        is PlayerPlaybackState.Paused     -> "Paused" to MutedGray
        is PlayerPlaybackState.FadingIn   -> "Fading in…" to PaleBlueText
        is PlayerPlaybackState.FadingOut  -> when (playback.triggeredBy) {
            FadeOutTrigger.TIMER -> "Timer ending…" to PaleYellowText
            FadeOutTrigger.USER  -> "Fading out…" to MutedGray
        }
        is PlayerPlaybackState.Loading    -> "Starting…" to MutedGray
        is PlayerPlaybackState.Interrupted -> when (playback.reason) {
            InterruptionReason.PHONE_CALL     -> "Call in progress" to PaleYellowText
            InterruptionReason.ANOTHER_APP    -> "Interrupted" to PaleRedText
            InterruptionReason.BLUETOOTH_LOST -> "Headphones disconnected" to PaleRedText
        }
        is PlayerPlaybackState.Error      -> "Error — tap play to retry" to PaleRedText
        is PlayerPlaybackState.ServiceUnavailable -> "Connecting…" to MutedGray
        is PlayerPlaybackState.Idle       -> "Ready" to MutedGray
    }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (bluetooth && playback is PlayerPlaybackState.Playing) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(PaleBlueText),
            )
        }
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}

@Composable
private fun TimerTriggerButton(
    timer: PlayerTimerState,
    onClick: () -> Unit,
) {
    val label = when (timer) {
        is PlayerTimerState.Active   -> timer.remainingLabel
        is PlayerTimerState.Finished -> "Timer ended"
        is PlayerTimerState.Idle     -> "Set timer"
    }
    val bg = when (timer) {
        is PlayerTimerState.Active  ->
            if (timer.isFading) PaleYellow else SurfaceMuted
        else -> SurfaceMuted
    }
    val textColor = when (timer) {
        is PlayerTimerState.Active -> if (timer.isFading) PaleYellowText else Charcoal
        else -> MutedGray
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, Border, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .semantics {
                contentDescription = when (timer) {
                    is PlayerTimerState.Active -> "Timer: ${timer.remainingLabel}. Tap to adjust"
                    else -> "Set sleep timer"
                }
            },
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
    }
}

// ── Interruption banner ───────────────────────────────────────────────────────

@Composable
private fun InterruptionBanner(reason: InterruptionReason?) {
    reason ?: return
    val (bg, fg, msg) = when (reason) {
        InterruptionReason.PHONE_CALL     ->
            Triple(PaleYellow, PaleYellowText, "Sound paused — will resume after call")
        InterruptionReason.ANOTHER_APP    ->
            Triple(PaleRed, PaleRedText, "Another app took audio — tap play to resume")
        InterruptionReason.BLUETOOTH_LOST ->
            Triple(PaleRed, PaleRedText, "Headphones disconnected")
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text  = msg,
            style = MaterialTheme.typography.bodySmall,
            color = fg,
        )
    }
}


@Composable
private fun MixerSection(
    mixer: MixerState,
    onVolume: (Int, Float) -> Unit,
    onDragEnd: (Int) -> Unit,
    onMute: (Int) -> Unit,
    onUnmute: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAddSound: () -> Unit,
    masterVolume: Float,
    onMasterVolume: (Float) -> Unit,
    onMasterDragEnd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        SectionDivider(label = "Mix")

        Spacer(Modifier.height(16.dp))

        if (mixer.layers.isEmpty()) {
            MixerEmptyState(onAddSound = onAddSound)
        } else {
            mixer.layers.forEach { layer ->
                LayerRow(
                    layer    = layer,
                    onVolume = { vol -> onVolume(layer.position, vol) },
                    onDragEnd = { onDragEnd(layer.position) },
                    onMute   = { onMute(layer.position) },
                    onUnmute = { onUnmute(layer.position) },
                    onRemove = { onRemove(layer.position) },
                )
                Spacer(Modifier.height(8.dp))
            }

            // Master volume — only when 2+ layers
            if (mixer.layers.size > 1) {
                Spacer(Modifier.height(8.dp))
                MasterVolumeRow(
                    volume   = masterVolume,
                    onChange = onMasterVolume,
                    onDragEnd = onMasterDragEnd,
                )
            }

            // Add layer slot
            if (mixer.canAddLayer) {
                Spacer(Modifier.height(8.dp))
                AddLayerButton(onClick = onAddSound)
            }
        }
    }
}

@Composable
private fun MixerEmptyState(onAddSound: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .background(SurfaceMuted)
            .clickable(onClick = onAddSound)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "No sounds yet",
                style = MaterialTheme.typography.titleSmall,
                color = Charcoal,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Tap to browse the sound library",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
        }
    }
}

@Composable
private fun LayerRow(
    layer: LayerUiState,
    onVolume: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMute: () -> Unit,
    onUnmute: () -> Unit,
    onRemove: () -> Unit,
) {
    val extended = LocalSleepSlideColors.current
    val (pillBg, pillText) = when (layer.sound.category) {
        Domain.SoundCategory.TINNITUS -> PaleBlue to PaleBlueText
        Domain.SoundCategory.NATURE   -> PaleGreen to PaleGreenText
        else                   -> extended.mutedSurface to MutedGray
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .background(White)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        // Label row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Category pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(pillBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text  = layer.sound.category.name
                            .lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = pillText,
                    )
                }
                Text(
                    text     = layer.sound.title,
                    style    = MaterialTheme.typography.titleSmall,
                    color    = if (layer.isMuted) MutedGray else Charcoal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Mute toggle
                MuteButton(
                    isMuted  = layer.isMuted,
                    onMute   = onMute,
                    onUnmute = onUnmute,
                )
                // Remove button
                RemoveButton(onClick = onRemove)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Volume slider
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text  = "${(layer.volume * 100).toInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = MutedGray,
                modifier = Modifier.width(28.dp),
            )
            Slider(
                value         = layer.volume,
                onValueChange = onVolume,
                onValueChangeFinished = onDragEnd,
                modifier      = Modifier.weight(1f),
                colors        = SliderDefaults.colors(
                    thumbColor            = if (layer.isMuted) MutedGray else Charcoal,
                    activeTrackColor      = if (layer.isMuted) MutedGray else Charcoal,
                    inactiveTrackColor    = Border,
                ),
            )
        }
    }
}

@Composable
private fun MasterVolumeRow(
    volume: Float,
    onChange: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text  = "Master",
            style = MaterialTheme.typography.labelSmall,
            color = MutedGray,
            modifier = Modifier.width(48.dp),
        )
        Slider(
            value                = volume,
            onValueChange        = onChange,
            onValueChangeFinished = onDragEnd,
            modifier             = Modifier.weight(1f),
            colors               = SliderDefaults.colors(
                thumbColor         = Charcoal,
                activeTrackColor   = Charcoal,
                inactiveTrackColor = Border,
            ),
        )
        Text(
            text  = "${(volume * 100).toInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = MutedGray,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun AddLayerButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(SurfaceMuted)
                .border(1.dp, Border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxWidth(0.5f).height(1.dp).background(MutedGray))
            Box(Modifier.width(1.dp).fillMaxHeight(0.5f).background(MutedGray))
        }
        Text(
            text  = "Add a sound",
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
        )
    }
}

@Composable
private fun MuteButton(
    isMuted: Boolean,
    onMute: () -> Unit,
    onUnmute: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isMuted) SurfaceMuted else White)
            .border(1.dp, Border, RoundedCornerShape(4.dp))
            .clickable(onClick = if (isMuted) onUnmute else onMute)
            .semantics {
                contentDescription = if (isMuted) "Unmute layer" else "Mute layer"
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isMuted) {
            // Two short vertical bars with a slash — simplified
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MutedGray),
            )
        } else {
            // Speaker icon — two rectangles
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                Box(Modifier.width(3.dp).height(8.dp).background(Charcoal))
                Box(Modifier.width(3.dp).height(6.dp).background(Charcoal))
            }
        }
    }
}

@Composable
private fun RemoveButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(White)
            .border(1.dp, Border, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Remove layer" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(1.5.dp)
                .background(MutedGray),
        )
    }
}

// ── Timer strip ───────────────────────────────────────────────────────────────

@Composable
private fun TimerStrip(
    timer: PlayerTimerState,
    onAddTime: () -> Unit,
    onReduceTime: () -> Unit,
    onCancel: () -> Unit,
    onSetTimer: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        SectionDivider(label = "Timer")
        Spacer(Modifier.height(12.dp))

        when (timer) {
            is PlayerTimerState.Idle, PlayerTimerState.Finished -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .background(SurfaceMuted)
                        .clickable(onClick = onSetTimer)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = if (timer is PlayerTimerState.Finished) "Timer ended — set another"
                        else "Set a sleep timer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                }
            }
            is PlayerTimerState.Active -> {
                ActiveTimerStrip(
                    timer      = timer,
                    onAdd      = onAddTime,
                    onReduce   = onReduceTime,
                    onCancel   = onCancel,
                )
            }
        }
    }
}

@Composable
private fun ActiveTimerStrip(
    timer: PlayerTimerState.Active,
    onAdd: () -> Unit,
    onReduce: () -> Unit,
    onCancel: () -> Unit,
) {
    val bg = if (timer.isFading) PaleYellow else SurfaceMuted
    val fg = if (timer.isFading) PaleYellowText else Charcoal
    val sub = if (timer.isFading) PaleYellowText.copy(alpha = 0.7f) else MutedGray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (timer.isFading) PaleYellowText else Border, RoundedCornerShape(8.dp))
            .background(bg)
            .padding(16.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text  = timer.remainingLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    color = fg,
                )
                Text(
                    text  = if (timer.isFading) "Fading out now" else "Until sleep",
                    style = MaterialTheme.typography.bodySmall,
                    color = sub,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TimerAdjustButton(label = "−5m", onClick = onReduce, tint = fg)
                TimerAdjustButton(label = "+5m", onClick = onAdd,    tint = fg)
                TimerAdjustButton(label = "✕", onClick = onCancel, tint = sub)
            }
        }
    }
}

@Composable
private fun TimerAdjustButton(label: String, onClick: () -> Unit, tint: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(White.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}


@Composable
private fun NightLockToggleRow(
    nightLock: NightLockState,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .background(if (nightLock != NightLockState.Disabled) Charcoal else White)
            .clickable(
                onClick = if (nightLock == NightLockState.Disabled) onEnable else onDisable
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics {
                contentDescription = if (nightLock == NightLockState.Disabled)
                    "Enable night lock" else "Night lock enabled — tap to disable"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text  = "Night lock",
                style = MaterialTheme.typography.titleSmall,
                color = if (nightLock != NightLockState.Disabled) White else Charcoal,
            )
            Text(
                text  = if (nightLock != NightLockState.Disabled)
                    "Tap locked — long press to unlock"
                else "Disable accidental taps while you sleep",
                style = MaterialTheme.typography.bodySmall,
                color = if (nightLock != NightLockState.Disabled)
                    White.copy(alpha = 0.6f) else MutedGray,
            )
        }

        // Toggle indicator
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (nightLock != NightLockState.Disabled) White.copy(alpha = 0.2f)
                    else Border
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = if (nightLock != NightLockState.Disabled) "ON" else "OFF",
                style = MaterialTheme.typography.labelSmall,
                color = if (nightLock != NightLockState.Disabled) White else MutedGray,
            )
        }
    }
}


@Composable
fun NightLockOverlay(
    state: NightLockState,
    onLongPressStarted: () -> Unit,
    onLongPressCompleted: () -> Unit,
    onLongPressCancelled: () -> Unit,
) {
    val unlocking = state == NightLockState.Unlocking
    val progress by animateFloatAsState(
        targetValue   = if (unlocking) 1f else 0f,
        animationSpec = if (unlocking) tween(1_500) else tween(100),
        label         = "unlock_progress",
        finishedListener = { value ->
            if (value >= 1f) onLongPressCompleted()
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.92f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPressStarted() },
                    onPress = { offset ->
                        tryAwaitRelease()
                        if (unlocking) onLongPressCancelled()
                    }
                )
            }
            .semantics { contentDescription = "Night lock active. Long press to unlock." },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Lock icon primitive
            LockIcon(
                tint   = if (unlocking) DarkText.copy(alpha = 0.5f) else DarkText,
                sizeDp = 32,
            )

            Text(
                text  = if (unlocking) "Keep holding…" else "Night lock",
                style = MaterialTheme.typography.titleMedium,
                color = DarkText,
            )
            Text(
                text  = "Long press to unlock",
                style = MaterialTheme.typography.bodySmall,
                color = DarkTextSecondary,
            )

            // Unlock progress ring
            if (unlocking) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .drawBehind {
                            val strokeWidth = 2.dp.toPx()
                            val radius = size.minDimension / 2f - strokeWidth
                            val center = Offset(size.width / 2f, size.height / 2f)
                            drawArc(
                                color      = DarkText.copy(alpha = 0.2f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter  = false,
                                topLeft    = Offset(center.x - radius, center.y - radius),
                                size       = Size(radius * 2, radius * 2),
                                style      = Stroke(strokeWidth, cap = StrokeCap.Round),
                            )
                            drawArc(
                                color      = DarkText,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter  = false,
                                topLeft    = Offset(center.x - radius, center.y - radius),
                                size       = Size(radius * 2, radius * 2),
                                style      = Stroke(strokeWidth, cap = StrokeCap.Round),
                            )
                        },
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerPickerSheet(
    currentTimer: PlayerTimerState,
    lastDuration: Long?,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    onCancel: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = WarmWhite,
        dragHandle       = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Border),
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
        ) {
            Text(
                text  = "Sleep timer",
                style = MaterialTheme.typography.headlineSmall,
                color = Charcoal,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Sound fades out gently before stopping",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
            Spacer(Modifier.height(24.dp))

            // Quick chips
            val labels = listOf("15m", "30m", "45m", "1h", "1h 30m", "2h")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(TIMER_PRESETS_MS.zip(labels)) { (durationMs, label) ->
                    val isActive = currentTimer is PlayerTimerState.Active &&
                            currentTimer.totalDurationMs == durationMs
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) Charcoal else SurfaceMuted)
                            .border(1.dp, if (isActive) Charcoal else Border, RoundedCornerShape(6.dp))
                            .clickable { onSelect(durationMs) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text  = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isActive) White else Charcoal,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Cancel active timer
            if (currentTimer is PlayerTimerState.Active) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .clickable(onClick = onCancel)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "Cancel timer",
                        style = MaterialTheme.typography.labelLarge,
                        color = PaleRedText,
                    )
                }
            }
        }
    }
}


@Composable
private fun SavePresetDialog(
    existingName: String,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String?) -> Unit,
) {
    var name  by rememberSaveable { mutableStateOf(existingName) }
    var emoji by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = WarmWhite,
        title = {
            Text(
                text  = "Save preset",
                style = MaterialTheme.typography.headlineSmall,
                color = Charcoal,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value          = name,
                    onValueChange  = { name = it },
                    label          = { Text("Name", style = MaterialTheme.typography.bodySmall) },
                    singleLine     = true,
                    modifier       = Modifier.fillMaxWidth(),
                    colors         = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Charcoal,
                        unfocusedBorderColor = Border,
                        focusedLabelColor    = Charcoal,
                        unfocusedLabelColor  = MutedGray,
                    ),
                )
                OutlinedTextField(
                    value         = emoji,
                    onValueChange = { if (it.length <= 2) emoji = it },
                    label         = {
                        Text("Emoji (optional)", style = MaterialTheme.typography.bodySmall)
                    },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Charcoal,
                        unfocusedBorderColor = Border,
                        focusedLabelColor    = Charcoal,
                        unfocusedLabelColor  = MutedGray,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name, emoji.ifBlank { null }) },
            ) {
                Text(
                    text  = "Save",
                    style = MaterialTheme.typography.labelLarge,
                    color = Charcoal,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text  = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MutedGray,
                )
            }
        },
    )
}


@Composable
private fun UnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = WarmWhite,
        title = {
            Text(
                text  = "Save changes?",
                style = MaterialTheme.typography.headlineSmall,
                color = Charcoal,
            )
        },
        text = {
            Text(
                text  = "You've modified this mix. Save changes to your preset before leaving?",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(
                    text  = "Save",
                    style = MaterialTheme.typography.labelLarge,
                    color = Charcoal,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text(
                    text  = "Discard",
                    style = MaterialTheme.typography.labelLarge,
                    color = PaleRedText,
                )
            }
        },
    )
}


@Composable
private fun SectionDivider(label: String) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text  = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MutedGray,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Border),
        )
    }
}

@Composable
private fun LoadingDots(tint: Color) {
    val transition = rememberInfiniteTransition(label = "loading")
    val alpha by transition.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loading_alpha",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .alpha(alpha)
                    .background(tint),
            )
        }
    }
}

@Composable
private fun PlayIcon(tint: Color, sizeDp: Int) {
    Box(
        modifier = Modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .background(
                    color = tint,
                    shape = object : androidx.compose.ui.graphics.Shape {
                        override fun createOutline(
                            size: androidx.compose.ui.geometry.Size,
                            layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                            density: androidx.compose.ui.unit.Density,
                        ) = androidx.compose.ui.graphics.Outline.Generic(
                            androidx.compose.ui.graphics.Path().apply {
                                moveTo(size.width * 0.15f, 0f)
                                lineTo(size.width, size.height / 2f)
                                lineTo(size.width * 0.15f, size.height)
                                close()
                            }
                        )
                    }
                )
        )
    }
}

@Composable
private fun PauseIcon(tint: Color, sizeDp: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((sizeDp * 0.2f).dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Box(Modifier.width((sizeDp * 0.28f).dp).height(sizeDp.dp).background(tint))
        Box(Modifier.width((sizeDp * 0.28f).dp).height(sizeDp.dp).background(tint))
    }
}

@Composable
private fun LockIcon(tint: Color, sizeDp: Int) {
    Box(modifier = Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // Shackle
            Box(
                modifier = Modifier
                    .size(width = (sizeDp * 0.5f).dp, height = (sizeDp * 0.4f).dp)
                    .border(
                        width = 2.dp,
                        color = tint,
                        shape = RoundedCornerShape(
                            topStart = 50.dp, topEnd = 50.dp,
                            bottomStart = 0.dp, bottomEnd = 0.dp,
                        ),
                    )
            )
            // Body
            Box(
                modifier = Modifier
                    .size(width = (sizeDp * 0.7f).dp, height = (sizeDp * 0.5f).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(tint),
            )
        }
    }
}

@Composable
private fun ChevronLeft(tint: Color) {
    Box(
        modifier          = Modifier.size(16.dp),
        contentAlignment  = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .drawBehind {
                    val p = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width, 0f)
                        lineTo(0f, size.height / 2f)
                        lineTo(size.width, size.height)
                    }
                    drawPath(
                        path  = p,
                        color = tint,
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
        )
    }
}

@Composable
private fun SaveIcon(tint: Color) {
    Box(modifier = Modifier.size(16.dp).drawBehind {
        val s = size
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(2.dp.toPx(), 0f)
            lineTo(s.width - 2.dp.toPx(), 0f)
            lineTo(s.width, 2.dp.toPx())
            lineTo(s.width, s.height)
            lineTo(0f, s.height)
            lineTo(0f, 2.dp.toPx())
            close()
        }
        drawPath(p, tint)
        drawRect(
            color   = if (tint == White) DarkSurface else WarmWhite,
            topLeft = Offset(4.dp.toPx(), 0f),
            size    = Size(s.width - 8.dp.toPx(), 5.dp.toPx()),
        )
    })
}