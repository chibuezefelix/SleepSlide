package com.opxl.sleepslide.presentation.player


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
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
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.DarkBackground
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

    var showTimerSheet    by rememberSaveable { mutableStateOf(false) }
    var showSaveDialog    by rememberSaveable { mutableStateOf(false) }
    var showUnsavedDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PlayerEvent.NavigateBack             -> onNavigateBack()
                is PlayerEvent.NavigateToLibrary        -> onNavigateToLibrary()
                is PlayerEvent.ShowTimerPicker          -> showTimerSheet = true
                is PlayerEvent.ShowSavePresetDialog     -> showSaveDialog = true
                is PlayerEvent.ShowUnsavedChangesDialog -> showUnsavedDialog = true
                is PlayerEvent.RequestNotificationPermission -> onRequestNotificationPermission()
                is PlayerEvent.ShowError                ->
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                is PlayerEvent.ShowInfo                 ->
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                is PlayerEvent.ShowServiceUnavailable   ->
                    scope.launch { snackbarHostState.showSnackbar("Audio service is starting") }
                is PlayerEvent.AudioInterrupted         ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            when (event.reason) {
                                InterruptionReason.PHONE_CALL     -> "Sound paused for call — will resume"
                                InterruptionReason.ANOTHER_APP    -> "Sound paused by another app"
                                InterruptionReason.BLUETOOTH_LOST -> "Headphones disconnected"
                            }
                        )
                    }
                is PlayerEvent.AudioRestoredAfterCall   ->
                    scope.launch { snackbarHostState.showSnackbar("Sound resumed") }
                is PlayerEvent.TimerFinished            ->
                    scope.launch { snackbarHostState.showSnackbar("Sleep timer ended") }
                is PlayerEvent.NightLockEnabled         ->
                    scope.launch { snackbarHostState.showSnackbar("Night lock on — long press to unlock") }
                else -> Unit
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost   = { SnackbarHost(snackbarHostState) },
            containerColor = WarmWhite,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            ) {
                PlayerTopBar(
                    preset   = uiState.preset,
                    onBack   = { viewModel.onBackRequested() },
                    onSave   = { viewModel.requestSaveAsPreset() },
                    onUpdate = { viewModel.updateActivePreset() },
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

                Spacer(Modifier.height(24.dp))

                AnimatedVisibility(
                    visible = uiState.playback is PlayerPlaybackState.Interrupted,
                    enter   = fadeIn(tween(250)) + slideInVertically { -it },
                    exit    = fadeOut(tween(200)) + slideOutVertically { -it },
                ) {
                    InterruptionBanner(
                        reason = (uiState.playback as? PlayerPlaybackState.Interrupted)?.reason
                    )
                }

                Spacer(Modifier.height(8.dp))

                MixerSection(
                    mixer           = uiState.mixer,
                    onVolume        = viewModel::onLayerVolumeChanged,
                    onDragEnd       = viewModel::onLayerVolumeDragEnded,
                    onMute          = viewModel::muteLayer,
                    onUnmute        = viewModel::unmuteLayer,
                    onRemove        = viewModel::removeLayer,
                    onAddSound      = { viewModel.onAddSoundRequested() },
                    masterVolume    = uiState.mixer.masterVolume,
                    onMasterVolume  = viewModel::onMasterVolumeChanged,
                    onMasterDragEnd = { viewModel.onMasterVolumeDragEnded() },
                )

                Spacer(Modifier.height(24.dp))

                TimerStrip(
                    timer        = uiState.timer,
                    onAddTime    = { viewModel.addFiveMinutes() },
                    onReduceTime = { viewModel.reduceFiveMinutes() },
                    onCancel     = { viewModel.cancelTimer() },
                    onSetTimer   = { viewModel.requestTimer() },
                )

                Spacer(Modifier.height(16.dp))

                NightLockToggleRow(
                    nightLock = uiState.nightLock,
                    onEnable  = { viewModel.enableNightLock() },
                    onDisable = { viewModel.disableNightLock() },
                )

                Spacer(Modifier.height(40.dp))
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }

        if (uiState.nightLock != NightLockState.Disabled) {
            NightLockOverlay(
                state                = uiState.nightLock,
                onLongPressStarted   = { viewModel.onNightLockLongPressStarted() },
                onLongPressCompleted = { viewModel.onNightLockLongPressCompleted() },
                onLongPressCancelled = { viewModel.onNightLockLongPressCancelled() },
            )
        }
    }

    if (showTimerSheet) {
        TimerPickerSheet(
            currentTimer = uiState.timer,
            onDismiss    = { showTimerSheet = false },
            onSelect     = { ms ->
                showTimerSheet = false
                viewModel.startTimer(ms)
            },
            onCancel = {
                showTimerSheet = false
                viewModel.cancelTimer()
            },
        )
    }

    if (showSaveDialog) {
        SavePresetDialog(
            existingName = (uiState.preset as? ActivePresetState.Loaded)?.preset?.name.orEmpty(),
            onDismiss    = { showSaveDialog = false },
            onSave       = { name, emoji ->
                showSaveDialog = false
                viewModel.saveAsNewPreset(name, emoji)
            },
        )
    }

    if (showUnsavedDialog) {
        UnsavedChangesDialog(
            onSave = {
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

// Top bar

@Composable
private fun PlayerTopBar(
    preset: ActivePresetState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onUpdate: () -> Unit,
) {
    val isDirtyLoaded = preset is ActivePresetState.Loaded && preset.isDirty

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick             = onBack,
            contentDescription  = "Back",
        ) { ChevronLeft(tint = Charcoal) }

        Column(
            modifier            = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text      = when (preset) {
                    is ActivePresetState.Loaded  -> preset.preset.name
                    is ActivePresetState.Saving  -> "Saving…"
                    else                         -> "Now playing"
                },
                style     = MaterialTheme.typography.titleMedium,
                color     = if (preset is ActivePresetState.Saving) MutedGray else Charcoal,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (isDirtyLoaded) {
                Text(
                    text  = "Unsaved changes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                )
            }
        }

        IconButton(
            onClick             = { if (isDirtyLoaded) onUpdate() else onSave() },
            background          = if (isDirtyLoaded) Charcoal else SurfaceMuted,
            borderColor         = if (isDirtyLoaded) Charcoal else Border,
            contentDescription  = if (isDirtyLoaded) "Save changes" else "Save as preset",
        ) {
            SaveIcon(tint = if (isDirtyLoaded) White else MutedGray)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Playback hub
// ─────────────────────────────────────────────────────────────────────────────

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
    val isPlaying   = playback is PlayerPlaybackState.Playing
    val isFadingIn  = playback is PlayerPlaybackState.FadingIn
    val fadeProgress = (playback as? PlayerPlaybackState.Playing)?.fadeInProgress
    val timerFraction = (timer as? PlayerTimerState.Active)?.progressFraction ?: 0f
    val timerFading   = (timer as? PlayerTimerState.Active)?.isFading == true

    val arcColor by animateColorAsState(
        targetValue   = when { timerFading -> PaleYellowText; isPlaying -> Charcoal; else -> Border },
        animationSpec = tween(600),
        label         = "arc_color",
    )

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier         = Modifier
                .size(200.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            TimerArc(progress = timerFraction, color = arcColor, modifier = Modifier.fillMaxSize())

            if (isFadingIn && fadeProgress != null) {
                FadeInArc(progress = fadeProgress, modifier = Modifier.fillMaxSize())
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MainTransportButton(playback = playback, onPlay = onPlay, onPause = onPause)

                AnimatedVisibility(visible = isPlaying || playback is PlayerPlaybackState.Paused) {
                    StopButtonSmall(onClick = onStop)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        PlaybackStatusLine(playback = playback, bluetooth = bluetooth)
        Spacer(Modifier.height(16.dp))
        TimerTriggerButton(timer = timer, onClick = onTimer)
    }
}

@Composable
private fun TimerArc(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.drawBehind {
        val stroke = 2.dp.toPx()
        val r      = size.minDimension / 2f - stroke / 2f
        val c      = Offset(size.width / 2f, size.height / 2f)
        val tl     = Offset(c.x - r, c.y - r)
        val sz     = Size(r * 2, r * 2)
        val style  = Stroke(stroke, cap = StrokeCap.Round)
        drawArc(Border, -90f, 360f, false, tl, sz, style = style)
        if (progress > 0f) drawArc(color, -90f, 360f * progress, false, tl, sz, style = style)
    })
}

@Composable
private fun FadeInArc(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.drawBehind {
        val stroke = 3.dp.toPx()
        val r      = size.minDimension / 2f - stroke
        val c      = Offset(size.width / 2f, size.height / 2f)
        drawArc(
            PaleBlueText.copy(alpha = 0.4f), -90f, 360f * progress, false,
            Offset(c.x - r, c.y - r), Size(r * 2, r * 2),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    })
}

@Composable
private fun MainTransportButton(
    playback: PlayerPlaybackState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
) {
    val isLoading = playback is PlayerPlaybackState.Loading || playback is PlayerPlaybackState.FadingIn
    val isPlaying = playback is PlayerPlaybackState.Playing

    val bgColor  by animateColorAsState(if (isPlaying) Charcoal else White,  tween(300), "tb")
    val iconTint by animateColorAsState(if (isPlaying) White    else Charcoal, tween(300), "ti")

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, Border, CircleShape)
            .clickable(enabled = !isLoading, onClick = if (isPlaying) onPause else onPlay)
            .semantics {
                contentDescription = when {
                    isLoading -> "Loading"; isPlaying -> "Pause"; else -> "Play"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState   = isLoading to isPlaying,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label         = "transport",
        ) { (loading, playing) ->
            when {
                loading -> LoadingDots(MutedGray)
                playing -> PauseIcon(iconTint, 22)
                else    -> PlayIcon(iconTint, 22)
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
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(MutedGray))
    }
}

@Composable
private fun PlaybackStatusLine(playback: PlayerPlaybackState, bluetooth: Boolean) {
    val (label, color) = when (playback) {
        is PlayerPlaybackState.Playing      ->
            if (playback.isPlayingInBackground) "Playing in background" to PaleGreenText
            else "Playing" to MutedGray
        is PlayerPlaybackState.Paused       -> "Paused" to MutedGray
        is PlayerPlaybackState.FadingIn     -> "Fading in…" to PaleBlueText
        is PlayerPlaybackState.FadingOut    -> when (playback.triggeredBy) {
            FadeOutTrigger.TIMER -> "Timer ending…" to PaleYellowText
            FadeOutTrigger.USER  -> "Fading out…" to MutedGray
        }
        is PlayerPlaybackState.Loading      -> "Starting…" to MutedGray
        is PlayerPlaybackState.Interrupted  -> when (playback.reason) {
            InterruptionReason.PHONE_CALL     -> "Call in progress" to PaleYellowText
            InterruptionReason.ANOTHER_APP    -> "Interrupted" to PaleRedText
            InterruptionReason.BLUETOOTH_LOST -> "Headphones disconnected" to PaleRedText
        }
        is PlayerPlaybackState.Error        -> "Error — tap play to retry" to PaleRedText
        is PlayerPlaybackState.ServiceUnavailable -> "Connecting…" to MutedGray
        is PlayerPlaybackState.Idle         -> "Ready" to MutedGray
    }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (bluetooth && playback is PlayerPlaybackState.Playing) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(PaleBlueText))
        }
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun TimerTriggerButton(timer: PlayerTimerState, onClick: () -> Unit) {
    val active   = timer as? PlayerTimerState.Active
    val label    = active?.remainingLabel ?: if (timer is PlayerTimerState.Finished) "Timer ended" else "Set timer"
    val bg       = if (active?.isFading == true) PaleYellow else SurfaceMuted
    val fg       = if (active?.isFading == true) PaleYellowText else if (active != null) Charcoal else MutedGray
    val desc     = active?.let { "Timer: ${it.remainingLabel}. Tap to adjust" } ?: "Set sleep timer"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, Border, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .semantics { contentDescription = desc },
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Interruption banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InterruptionBanner(reason: InterruptionReason?) {
    reason ?: return
    val (bg, fg, msg) = when (reason) {
        InterruptionReason.PHONE_CALL     -> Triple(PaleYellow,  PaleYellowText, "Sound paused — will resume after call")
        InterruptionReason.ANOTHER_APP    -> Triple(PaleRed,     PaleRedText,    "Another app took audio — tap play to resume")
        InterruptionReason.BLUETOOTH_LOST -> Triple(PaleRed,     PaleRedText,    "Headphones disconnected")
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = msg, style = MaterialTheme.typography.bodySmall, color = fg)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mixer
// ─────────────────────────────────────────────────────────────────────────────

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
        SectionDivider("Mix")
        Spacer(Modifier.height(16.dp))

        if (mixer.layers.isEmpty()) {
            EmptyMixerCard(onAddSound)
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

            if (mixer.layers.size > 1) {
                Spacer(Modifier.height(4.dp))
                MasterVolumeRow(volume = masterVolume, onChange = onMasterVolume, onDragEnd = onMasterDragEnd)
            }

            if (mixer.canAddLayer) {
                Spacer(Modifier.height(8.dp))
                AddLayerButton(onAddSound)
            }
        }
    }
}

@Composable
private fun EmptyMixerCard(onAddSound: () -> Unit) {
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
            Text("No sounds yet",             style = MaterialTheme.typography.titleSmall, color = Charcoal)
            Spacer(Modifier.height(4.dp))
            Text("Tap to browse the library", style = MaterialTheme.typography.bodySmall,  color = MutedGray)
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
    val (pillBg, pillFg) = when (layer.sound.category) {
        Domain.SoundCategory.TINNITUS -> PaleBlue  to PaleBlueText
        Domain.SoundCategory.NATURE   -> PaleGreen to PaleGreenText
        else                          -> extended.mutedSurface to MutedGray
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .background(White)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier          = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryPill(label = layer.sound.category.name, bg = pillBg, fg = pillFg)
                Text(
                    text     = layer.sound.title,
                    style    = MaterialTheme.typography.titleSmall,
                    color    = if (layer.isMuted) MutedGray else Charcoal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MuteButton(layer.isMuted, onMute, onUnmute)
                RemoveButton(onRemove)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text     = "${(layer.volume * 100).toInt()}",
                style    = MaterialTheme.typography.labelSmall,
                color    = MutedGray,
                modifier = Modifier.width(28.dp),
            )
            Slider(
                value                 = layer.volume,
                onValueChange         = onVolume,
                onValueChangeFinished = onDragEnd,
                modifier              = Modifier.weight(1f),
                colors                = playerSliderColors(muted = layer.isMuted),
            )
        }
    }
}

@Composable
private fun CategoryPill(label: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text  = label.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = fg,
        )
    }
}

@Composable
private fun MasterVolumeRow(volume: Float, onChange: (Float) -> Unit, onDragEnd: () -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Master", style = MaterialTheme.typography.labelSmall, color = MutedGray,
            modifier = Modifier.width(48.dp))
        Slider(
            value = volume, onValueChange = onChange, onValueChangeFinished = onDragEnd,
            modifier = Modifier.weight(1f), colors = playerSliderColors(),
        )
        Text(
            text      = "${(volume * 100).toInt()}",
            style     = MaterialTheme.typography.labelSmall,
            color     = MutedGray,
            textAlign = TextAlign.End,
            modifier  = Modifier.width(28.dp),
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
        PlusCircle()
        Text("Add a sound", style = MaterialTheme.typography.bodySmall, color = MutedGray)
    }
}

@Composable
private fun MuteButton(isMuted: Boolean, onMute: () -> Unit, onUnmute: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isMuted) SurfaceMuted else White)
            .border(1.dp, Border, RoundedCornerShape(4.dp))
            .clickable(onClick = if (isMuted) onUnmute else onMute)
            .semantics { contentDescription = if (isMuted) "Unmute" else "Mute" },
        contentAlignment = Alignment.Center,
    ) {
        if (isMuted) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(1.dp)).background(MutedGray))
        } else {
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
        Box(Modifier.width(10.dp).height(1.5.dp).background(MutedGray))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Timer strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TimerStrip(
    timer: PlayerTimerState,
    onAddTime: () -> Unit,
    onReduceTime: () -> Unit,
    onCancel: () -> Unit,
    onSetTimer: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        SectionDivider("Timer")
        Spacer(Modifier.height(12.dp))
        when (timer) {
            is PlayerTimerState.Active   -> ActiveTimerCard(timer, onAddTime, onReduceTime, onCancel)
            is PlayerTimerState.Finished -> TimerIdleCard("Timer ended — set another", onSetTimer)
            is PlayerTimerState.Idle     -> TimerIdleCard("Set a sleep timer", onSetTimer)
        }
    }
}

@Composable
private fun TimerIdleCard(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .background(SurfaceMuted)
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MutedGray)
    }
}

@Composable
private fun ActiveTimerCard(
    timer: PlayerTimerState.Active,
    onAdd: () -> Unit,
    onReduce: () -> Unit,
    onCancel: () -> Unit,
) {
    val fading = timer.isFading
    val fg  = if (fading) PaleYellowText else Charcoal
    val sub = if (fading) PaleYellowText.copy(alpha = 0.7f) else MutedGray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (fading) PaleYellowText else Border, RoundedCornerShape(8.dp))
            .background(if (fading) PaleYellow else SurfaceMuted)
            .padding(16.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column {
                Text(timer.remainingLabel, style = MaterialTheme.typography.headlineSmall, color = fg)
                Text(
                    if (fading) "Fading out now" else "Until sleep",
                    style = MaterialTheme.typography.bodySmall, color = sub,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TimerChip("−5m", onReduce, fg)
                TimerChip("+5m", onAdd,    fg)
                TimerChip("✕",  onCancel, sub)
            }
        }
    }
}

@Composable
private fun TimerChip(label: String, onClick: () -> Unit, tint: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(White.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Night lock toggle row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NightLockToggleRow(
    nightLock: NightLockState,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
) {
    val isOn = nightLock != NightLockState.Disabled
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .background(if (isOn) Charcoal else White)
            .clickable(onClick = if (isOn) onDisable else onEnable)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics {
                contentDescription =
                    if (isOn) "Night lock enabled — tap to disable" else "Enable night lock"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                "Night lock",
                style = MaterialTheme.typography.titleSmall,
                color = if (isOn) White else Charcoal,
            )
            Text(
                if (isOn) "Tap locked — long press to unlock" else "Disable accidental taps while you sleep",
                style = MaterialTheme.typography.bodySmall,
                color = if (isOn) White.copy(alpha = 0.6f) else MutedGray,
            )
        }
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isOn) White.copy(alpha = 0.2f) else Border),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (isOn) "ON" else "OFF",
                style = MaterialTheme.typography.labelSmall,
                color = if (isOn) White else MutedGray,
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
        targetValue      = if (unlocking) 1f else 0f,
        animationSpec    = if (unlocking) tween(1_500) else tween(100),
        label            = "unlock",
        finishedListener = { if (it >= 1f) onLongPressCompleted() },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.92f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPressStarted() },
                    onPress = {
                        tryAwaitRelease()
                        if (unlocking) onLongPressCancelled()
                    },
                )
            }
            .semantics { contentDescription = "Night lock active. Long press to unlock." },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LockIcon(tint = if (unlocking) DarkText.copy(alpha = 0.5f) else DarkText, sizeDp = 32)
            Text(if (unlocking) "Keep holding…" else "Night lock",
                style = MaterialTheme.typography.titleMedium, color = DarkText)
            Text("Long press to unlock",
                style = MaterialTheme.typography.bodySmall, color = DarkTextSecondary)

            if (unlocking) {
                UnlockProgressRing(progress)
            }
        }
    }
}

@Composable
private fun UnlockProgressRing(progress: Float) {
    Box(modifier = Modifier.size(56.dp).drawBehind {
        val stroke = 2.dp.toPx()
        val r      = size.minDimension / 2f - stroke
        val c      = Offset(size.width / 2f, size.height / 2f)
        val tl     = Offset(c.x - r, c.y - r)
        val sz     = Size(r * 2, r * 2)
        val style  = Stroke(stroke, cap = StrokeCap.Round)
        drawArc(DarkText.copy(alpha = 0.2f), -90f, 360f,            false, tl, sz, style = style)
        drawArc(DarkText,                    -90f, 360f * progress,  false, tl, sz, style = style)
    })
}

// Timer picker sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerPickerSheet(
    currentTimer: PlayerTimerState,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    onCancel: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = WarmWhite,
        dragHandle       = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Border),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
        ) {
            Text("Sleep timer",       style = MaterialTheme.typography.headlineSmall, color = Charcoal)
            Spacer(Modifier.height(4.dp))
            Text("Sound fades out gently before stopping",
                style = MaterialTheme.typography.bodySmall, color = MutedGray)
            Spacer(Modifier.height(24.dp))

            val labels = listOf("15m", "30m", "45m", "1h", "1h 30m", "2h")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TIMER_PRESETS_MS.zip(labels)) { (ms, label) ->
                    val active = currentTimer is PlayerTimerState.Active &&
                            currentTimer.totalDurationMs == ms
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (active) Charcoal else SurfaceMuted)
                            .border(1.dp, if (active) Charcoal else Border, RoundedCornerShape(6.dp))
                            .clickable { onSelect(ms) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge,
                            color = if (active) White else Charcoal)
                    }
                }
            }

            if (currentTimer is PlayerTimerState.Active) {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .clickable(onClick = onCancel)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Cancel timer", style = MaterialTheme.typography.labelLarge, color = PaleRedText)
                }
            }
        }
    }
}

// Dialogs

@Composable
private fun SavePresetDialog(
    existingName: String,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
) {
    var name  by rememberSaveable { mutableStateOf(existingName) }
    var emoji by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = WarmWhite,
        title   = { Text("Save preset",  style = MaterialTheme.typography.headlineSmall, color = Charcoal) },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SleepTextField(value = name, label = "Name", onValueChange = { name = it })
                SleepTextField(
                    value         = emoji,
                    label         = "Emoji (optional)",
                    onValueChange = { if (it.length <= 2) emoji = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name, emoji.ifBlank { null }) }) {
                Text("Save", style = MaterialTheme.typography.labelLarge, color = Charcoal)
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
private fun UnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = WarmWhite,
        title   = { Text("Save changes?",  style = MaterialTheme.typography.headlineSmall, color = Charcoal) },
        text    = {
            Text(
                "You've modified this mix. Save changes to your preset before leaving?",
                style = MaterialTheme.typography.bodyMedium, color = MutedGray,
            )
        },
        confirmButton  = {
            TextButton(onClick = onSave) {
                Text("Save",    style = MaterialTheme.typography.labelLarge, color = Charcoal)
            }
        },
        dismissButton  = {
            TextButton(onClick = onDiscard) {
                Text("Discard", style = MaterialTheme.typography.labelLarge, color = PaleRedText)
            }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared micro-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SleepTextField(value: String, label: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, style = MaterialTheme.typography.bodySmall) },
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

@Composable
private fun IconButton(
    onClick: () -> Unit,
    contentDescription: String,
    background: Color  = White,
    borderColor: Color = Border,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
        content          = { content() },
    )
}

@Composable
private fun SectionDivider(label: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MutedGray)
        Box(Modifier.weight(1f).height(1.dp).background(Border))
    }
}

@Composable
private fun PlusCircle() {
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
}

@Composable
private fun LoadingDots(tint: Color) {
    val alpha by rememberInfiniteTransition(label = "load").animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { Box(Modifier.size(5.dp).clip(CircleShape).alpha(alpha).background(tint)) }
    }
}

private val TriangleShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(Path().apply {
            moveTo(size.width * 0.15f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(size.width * 0.15f, size.height)
            close()
        })
}

@Composable
private fun PlayIcon(tint: Color, sizeDp: Int) {
    Box(Modifier.size(sizeDp.dp).background(tint, TriangleShape))
}

@Composable
private fun PauseIcon(tint: Color, sizeDp: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy((sizeDp * 0.2f).dp)) {
        Box(Modifier.width((sizeDp * 0.28f).dp).height(sizeDp.dp).background(tint))
        Box(Modifier.width((sizeDp * 0.28f).dp).height(sizeDp.dp).background(tint))
    }
}

@Composable
private fun LockIcon(tint: Color, sizeDp: Int) {
    Box(Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(
                Modifier
                    .size(width = (sizeDp * 0.5f).dp, height = (sizeDp * 0.4f).dp)
                    .border(2.dp, tint, RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp))
            )
            Box(
                Modifier
                    .size(width = (sizeDp * 0.7f).dp, height = (sizeDp * 0.5f).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(tint)
            )
        }
    }
}

@Composable
private fun ChevronLeft(tint: Color) {
    Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(8.dp).drawBehind {
            drawPath(
                Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(0f, size.height / 2f)
                    lineTo(size.width, size.height)
                },
                color = tint,
                style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round),
            )
        })
    }
}

@Composable
private fun SaveIcon(tint: Color) {
    Box(Modifier.size(16.dp).drawBehind {
        val p = Path().apply {
            moveTo(2.dp.toPx(), 0f); lineTo(size.width - 2.dp.toPx(), 0f)
            lineTo(size.width, 2.dp.toPx()); lineTo(size.width, size.height)
            lineTo(0f, size.height); lineTo(0f, 2.dp.toPx()); close()
        }
        drawPath(p, tint)
        drawRect(
            color   = if (tint == White) DarkSurface else WarmWhite,
            topLeft = Offset(4.dp.toPx(), 0f),
            size    = Size(size.width - 8.dp.toPx(), 5.dp.toPx()),
        )
    })
}

@Composable
private fun playerSliderColors(muted: Boolean = false) = SliderDefaults.colors(
    thumbColor         = if (muted) MutedGray else Charcoal,
    activeTrackColor   = if (muted) MutedGray else Charcoal,
    inactiveTrackColor = Border,
)