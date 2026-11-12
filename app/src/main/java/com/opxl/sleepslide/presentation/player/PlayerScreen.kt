package com.opxl.sleepslide.presentation.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opxl.sleepslide.R
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.model.Domain.needsHeadphones
import com.opxl.sleepslide.domain.repository.CoachMarkScreens
import com.opxl.sleepslide.presentation.components.CircleIconButton
import com.opxl.sleepslide.presentation.components.DashedCard
import com.opxl.sleepslide.presentation.components.GhostIconButton
import com.opxl.sleepslide.presentation.components.Glyph
import com.opxl.sleepslide.presentation.components.GlyphIcon
import com.opxl.sleepslide.presentation.components.Glow
import com.opxl.sleepslide.presentation.components.GroupCard
import com.opxl.sleepslide.presentation.components.IconCircle
import com.opxl.sleepslide.presentation.components.LineSlider
import com.opxl.sleepslide.presentation.components.LineSliderColors
import com.opxl.sleepslide.presentation.components.LoadingDots
import com.opxl.sleepslide.presentation.components.RowDivider
import com.opxl.sleepslide.presentation.components.SecondaryPill
import com.opxl.sleepslide.presentation.components.SectionLabel
import com.opxl.sleepslide.presentation.components.SelectChip
import com.opxl.sleepslide.presentation.components.SleepTextInput
import com.opxl.sleepslide.presentation.components.SurfaceCard
import com.opxl.sleepslide.presentation.components.displayTitle
import com.opxl.sleepslide.presentation.components.glyph
import com.opxl.sleepslide.presentation.components.labelRes
import com.opxl.sleepslide.presentation.components.shortDuration
import com.opxl.sleepslide.presentation.permission.isNotificationPermissionGranted
import com.opxl.sleepslide.presentation.scene.SceneBackdrop
import com.opxl.sleepslide.presentation.tutorial.CoachMarkHost
import com.opxl.sleepslide.presentation.tutorial.TutorialViewModel
import com.opxl.sleepslide.presentation.tutorial.coachMarkAnchor
import com.opxl.sleepslide.presentation.tutorial.tutorialViewModel
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.DarkBackground
import com.opxl.sleepslide.ui.theme.DarkText
import com.opxl.sleepslide.ui.theme.DarkTextSecondary
import com.opxl.sleepslide.ui.theme.Dimens
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
    onRequestNotificationPermission: (onResult: (granted: Boolean) -> Unit) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
    tutorial: TutorialViewModel = tutorialViewModel(CoachMarkScreens.PLAYER),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showTimerSheet    by rememberSaveable { mutableStateOf(false) }
    var showSaveDialog    by rememberSaveable { mutableStateOf(false) }
    var showUnsavedDialog by rememberSaveable { mutableStateOf(false) }

    // Seed the permission gate so first play can ask just-in-time (Android 13+)
    LaunchedEffect(Unit) {
        viewModel.checkNotificationPermission(context.isNotificationPermissionGranted())
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PlayerEvent.NavigateBack             -> onNavigateBack()
                is PlayerEvent.NavigateToLibrary        -> onNavigateToLibrary()
                is PlayerEvent.ShowTimerPicker          -> showTimerSheet = true
                is PlayerEvent.ShowSavePresetDialog     -> showSaveDialog = true
                is PlayerEvent.ShowUnsavedChangesDialog -> showUnsavedDialog = true
                is PlayerEvent.RequestNotificationPermission ->
                    onRequestNotificationPermission { granted ->
                        viewModel.onNotificationPermissionResult(granted)
                    }
                is PlayerEvent.ShowError                ->
                    scope.launch { snackbarHostState.showSnackbar(event.message.asString(context)) }
                is PlayerEvent.ShowInfo                 ->
                    scope.launch { snackbarHostState.showSnackbar(event.message.asString(context)) }
                is PlayerEvent.ShowServiceUnavailable   -> Unit
                is PlayerEvent.AudioInterrupted         ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(
                                when (event.reason) {
                                    InterruptionReason.PHONE_CALL     -> R.string.player_snack_paused_call
                                    InterruptionReason.ANOTHER_APP    -> R.string.player_snack_paused_other_app
                                    InterruptionReason.BLUETOOTH_LOST -> R.string.player_headphones_disconnected
                                }
                            )
                        )
                    }
                is PlayerEvent.AudioRestoredAfterCall   ->
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.player_snack_resumed)) }
                is PlayerEvent.TimerFinished            ->
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.player_snack_timer_ended)) }
                is PlayerEvent.NightLockEnabled         ->
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.player_snack_night_lock_on)) }
                else -> Unit
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CoachMarkHost(viewModel = tutorial, labels = PLAYER_COACH_MARKS) {
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

                    // Photo backdrop chosen for the mix + time of day; drifts while playing
                    SceneBackdrop(
                        sounds    = uiState.mixer.layers.map { it.sound },
                        isPlaying = uiState.playback is PlayerPlaybackState.Playing ||
                                    uiState.playback is PlayerPlaybackState.FadingIn,
                    )

                    Spacer(Modifier.height(28.dp))

                    PlaybackHub(
                        playback  = uiState.playback,
                        timer     = uiState.timer,
                        nightLock = uiState.nightLock,
                        bluetooth = uiState.isBluetoothConnected,
                        onPlay    = { viewModel.resume() },
                        onPause   = { viewModel.pause() },
                        onStop    = { viewModel.stop() },
                        onTimer   = { viewModel.requestTimer() },
                        onLockOn  = { viewModel.enableNightLock() },
                        onLockOff = { viewModel.disableNightLock() },
                    )

                    AnimatedVisibility(
                        visible = uiState.playback is PlayerPlaybackState.Interrupted,
                        enter   = fadeIn(tween(250)) + slideInVertically { -it },
                        exit    = fadeOut(tween(200)) + slideOutVertically { -it },
                    ) {
                        InterruptionBanner(
                            reason = (uiState.playback as? PlayerPlaybackState.Interrupted)?.reason
                        )
                    }

                    AnimatedVisibility(
                        visible = uiState.timer is PlayerTimerState.Active,
                        enter   = fadeIn(tween(250)) + slideInVertically { -it / 2 },
                        exit    = fadeOut(tween(200)),
                    ) {
                        val active = uiState.timer as? PlayerTimerState.Active
                        if (active != null) {
                            ActiveTimerCard(
                                timer    = active,
                                onAdd    = { viewModel.addFiveMinutes() },
                                onReduce = { viewModel.reduceFiveMinutes() },
                                onCancel = { viewModel.cancelTimer() },
                            )
                        }
                    }

                    MixerSection(
                        mixer                 = uiState.mixer,
                        isPlaying             = uiState.playback is PlayerPlaybackState.Playing ||
                                                uiState.playback is PlayerPlaybackState.FadingIn,
                        isHeadphonesConnected = uiState.isHeadphonesConnected,
                        onVolume              = viewModel::onLayerVolumeChanged,
                        onDragEnd             = viewModel::onLayerVolumeDragEnded,
                        onMute                = viewModel::muteLayer,
                        onUnmute              = viewModel::unmuteLayer,
                        onRemove              = viewModel::removeLayer,
                        onAddSound            = { viewModel.onAddSoundRequested() },
                        masterVolume          = uiState.mixer.masterVolume,
                        onMasterVolume        = viewModel::onMasterVolumeChanged,
                        onMasterDragEnd       = { viewModel.onMasterVolumeDragEnded() },
                    )

                    Spacer(Modifier.height(40.dp))
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
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

/** Anchor key → label for the first-visit coach marks; only laid-out targets are shown. */
private val PLAYER_COACH_MARKS = listOf(
    "save"       to R.string.coach_player_save,
    "transport"  to R.string.coach_player_transport,
    "timer"      to R.string.coach_player_timer,
    "layer"      to R.string.coach_player_layer,
    "add_sound"  to R.string.coach_player_add_sound,
    "night_lock" to R.string.coach_player_night_lock,
)

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

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
            .padding(horizontal = Dimens.gutter, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        CircleIconButton(onClick = onBack, contentDescription = stringResource(R.string.cd_back)) {
            GlyphIcon(Glyph.ChevronLeft, Charcoal, size = 16.dp)
        }

        Column(
            modifier            = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text      = when (preset) {
                    is ActivePresetState.Loaded -> preset.preset.name
                    is ActivePresetState.Saving -> stringResource(R.string.player_saving)
                    else                        -> stringResource(R.string.player_now_playing)
                },
                style     = MaterialTheme.typography.titleMedium,
                color     = if (preset is ActivePresetState.Saving) MutedGray else Charcoal,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (isDirtyLoaded) {
                Text(
                    text  = stringResource(R.string.player_unsaved_changes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                )
            }
        }

        CircleIconButton(
            onClick            = { if (isDirtyLoaded) onUpdate() else onSave() },
            filled             = isDirtyLoaded,
            contentDescription = stringResource(if (isDirtyLoaded) R.string.player_cd_save_changes else R.string.player_cd_save_as_preset),
            modifier           = Modifier.coachMarkAnchor("save"),
        ) {
            GlyphIcon(Glyph.Save, if (isDirtyLoaded) White else Charcoal, size = 18.dp)
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
    nightLock: NightLockState,
    bluetooth: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onTimer: () -> Unit,
    onLockOn: () -> Unit,
    onLockOff: () -> Unit,
) {
    val isPlaying     = playback is PlayerPlaybackState.Playing
    val isFadingIn    = playback is PlayerPlaybackState.FadingIn
    val fadeProgress  = (playback as? PlayerPlaybackState.Playing)?.fadeInProgress
    val activeTimer   = timer as? PlayerTimerState.Active
    val timerFraction = activeTimer?.progressFraction ?: 0f
    val timerFading   = activeTimer?.isFading == true
    val canStop       = isPlaying || playback is PlayerPlaybackState.Paused
    val lockOn        = nightLock != NightLockState.Disabled

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
            modifier         = Modifier.size(168.dp),
            contentAlignment = Alignment.Center,
        ) {
            TimerArc(progress = timerFraction, color = arcColor, modifier = Modifier.fillMaxSize())
            if (isFadingIn && fadeProgress != null) {
                FadeInArc(progress = fadeProgress, modifier = Modifier.fillMaxSize())
            }
            MainTransportButton(playback = playback, onPlay = onPlay, onPause = onPause)
        }

        Spacer(Modifier.height(14.dp))
        PlaybackStatusLine(playback = playback, bluetooth = bluetooth)
        Spacer(Modifier.height(24.dp))

        // Secondary actions — labelled circles, so each reads at a glance in the dark
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment     = Alignment.Top,
        ) {
            ActionCircle(
                glyph              = Glyph.Stop,
                label              = stringResource(R.string.player_cd_stop),
                contentDescription = stringResource(R.string.player_cd_stop),
                enabled            = canStop,
                onClick            = onStop,
            )
            ActionCircle(
                glyph              = Glyph.Timer,
                label              = activeTimer?.remainingLabel
                    ?: stringResource(if (timer is PlayerTimerState.Finished) R.string.player_timer_ended else R.string.player_set_timer),
                contentDescription = activeTimer?.let { stringResource(R.string.player_cd_timer_active, it.remainingLabel) }
                    ?: stringResource(R.string.player_cd_set_sleep_timer),
                onClick            = onTimer,
                tint               = if (timerFading) PaleYellowText else null,
                active             = activeTimer != null,
                modifier           = Modifier.coachMarkAnchor("timer"),
            )
            ActionCircle(
                glyph              = Glyph.Lock,
                label              = stringResource(R.string.player_night_lock),
                contentDescription = stringResource(if (lockOn) R.string.player_cd_night_lock_on else R.string.player_cd_night_lock_off),
                onClick            = if (lockOn) onLockOff else onLockOn,
                active             = lockOn,
                modifier           = Modifier.coachMarkAnchor("night_lock"),
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** A round action with its name under it. `active` fills the circle with ink. */
@Composable
private fun ActionCircle(
    glyph: Glyph,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    tint: Color? = null,
) {
    val fg = tint ?: if (active) White else Charcoal
    Column(
        modifier            = modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircleIconButton(
            onClick            = onClick,
            contentDescription = contentDescription,
            size               = 52.dp,
            filled             = active,
            enabled            = enabled,
            background         = when { active -> Charcoal; tint != null -> PaleYellow; else -> White },
            border             = when { active -> Charcoal; tint != null -> PaleYellow; else -> Border },
        ) { GlyphIcon(glyph, fg, size = 22.dp) }
        Text(
            text      = label,
            style     = MaterialTheme.typography.labelMedium,
            color     = tint ?: if (active) Charcoal else MutedGray,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TimerArc(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val track = Border
    Box(modifier = modifier.drawBehind {
        val stroke = 2.dp.toPx()
        val r      = size.minDimension / 2f - stroke / 2f
        val c      = Offset(size.width / 2f, size.height / 2f)
        val tl     = Offset(c.x - r, c.y - r)
        val sz     = Size(r * 2, r * 2)
        val style  = Stroke(stroke, cap = StrokeCap.Round)
        drawArc(track, -90f, 360f, false, tl, sz, style = style)
        if (progress > 0f) drawArc(color, -90f, 360f * progress, false, tl, sz, style = style)
    })
}

@Composable
private fun FadeInArc(progress: Float, modifier: Modifier = Modifier) {
    val arc = PaleBlueText.copy(alpha = 0.4f)
    Box(modifier = modifier.drawBehind {
        val stroke = 3.dp.toPx()
        val r      = size.minDimension / 2f - stroke
        val c      = Offset(size.width / 2f, size.height / 2f)
        drawArc(
            arc, -90f, 360f * progress, false,
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
    val transportLabel = stringResource(
        when { isLoading -> R.string.player_cd_loading; isPlaying -> R.string.player_cd_pause; else -> R.string.player_cd_play }
    )

    val bgColor  by animateColorAsState(if (isPlaying) Charcoal else White,    tween(300), label = "tb")
    val iconTint by animateColorAsState(if (isPlaying) White    else Charcoal, tween(300), label = "ti")

    Box(
        modifier = Modifier
            .size(84.dp)
            .coachMarkAnchor("transport")
            .clip(CircleShape)
            .background(bgColor)
            .border(Dimens.hairline, if (isPlaying) Charcoal else Border, CircleShape)
            .clickable(enabled = !isLoading, onClick = if (isPlaying) onPause else onPlay, role = Role.Button)
            .semantics { contentDescription = transportLabel },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState    = isLoading to isPlaying,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label          = "transport",
        ) { (loading, playing) ->
            when {
                loading -> LoadingDots(MutedGray)
                playing -> GlyphIcon(Glyph.Pause, iconTint, size = 30.dp)
                else    -> GlyphIcon(Glyph.Play, iconTint, size = 30.dp)
            }
        }
    }
}

@Composable
private fun PlaybackStatusLine(playback: PlayerPlaybackState, bluetooth: Boolean) {
    val (label, color) = when (playback) {
        is PlayerPlaybackState.Playing      ->
            if (playback.isPlayingInBackground) R.string.player_status_playing to PaleGreenText
            else R.string.player_status_playing to MutedGray
        is PlayerPlaybackState.Paused       -> R.string.player_status_paused to MutedGray
        is PlayerPlaybackState.FadingIn     -> R.string.player_status_fading_in to PaleBlueText
        is PlayerPlaybackState.FadingOut    -> when (playback.triggeredBy) {
            FadeOutTrigger.TIMER -> R.string.player_status_timer_ending to PaleYellowText
            FadeOutTrigger.USER  -> R.string.player_status_fading_out to MutedGray
        }
        is PlayerPlaybackState.Loading      -> R.string.player_status_starting to MutedGray
        is PlayerPlaybackState.Interrupted  -> when (playback.reason) {
            InterruptionReason.PHONE_CALL     -> R.string.player_status_call to PaleYellowText
            InterruptionReason.ANOTHER_APP    -> R.string.player_status_interrupted to PaleRedText
            InterruptionReason.BLUETOOTH_LOST -> R.string.player_headphones_disconnected to PaleRedText
        }
        is PlayerPlaybackState.Error        -> R.string.player_status_error to PaleRedText
        is PlayerPlaybackState.ServiceUnavailable -> R.string.player_status_connecting to MutedGray
        is PlayerPlaybackState.Idle         -> R.string.player_status_ready to MutedGray
    }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (bluetooth && playback is PlayerPlaybackState.Playing) {
            GlyphIcon(Glyph.Bluetooth, PaleBlueText, size = 14.dp)
        }
        Text(text = stringResource(label), style = MaterialTheme.typography.labelLarge, color = color)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Interruption banner · active timer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InterruptionBanner(reason: InterruptionReason?) {
    reason ?: return
    val (bg, fg, msg) = when (reason) {
        InterruptionReason.PHONE_CALL     -> Triple(PaleYellow, PaleYellowText, stringResource(R.string.player_banner_call))
        InterruptionReason.ANOTHER_APP    -> Triple(PaleRed,    PaleRedText,    stringResource(R.string.player_banner_other_app))
        InterruptionReason.BLUETOOTH_LOST -> Triple(PaleRed,    PaleRedText,    stringResource(R.string.player_headphones_disconnected))
    }
    SurfaceCard(
        modifier       = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.gutter)
            .padding(top = 20.dp),
        background     = bg,
        border         = Color.Transparent,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(text = msg, style = MaterialTheme.typography.bodySmall, color = fg)
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
    val sub = if (fading) PaleYellowText.copy(alpha = 0.75f) else MutedGray

    SurfaceCard(
        modifier       = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.gutter)
            .padding(top = 20.dp),
        background     = if (fading) PaleYellow else White,
        border         = if (fading) Color.Transparent else Border,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column {
                Text(timer.remainingLabel, style = MaterialTheme.typography.displaySmall, color = fg)
                Text(
                    stringResource(if (fading) R.string.player_timer_fading_now else R.string.player_timer_until_sleep),
                    style = MaterialTheme.typography.bodySmall, color = sub,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TimerChip(stringResource(R.string.player_timer_minus_5), onReduce, fg)
                TimerChip(stringResource(R.string.player_timer_plus_5), onAdd, fg)
                GhostIconButton(
                    onClick            = onCancel,
                    contentDescription = stringResource(R.string.player_cancel_timer),
                    size               = 36.dp,
                    background         = if (fading) PaleYellowText.copy(alpha = 0.12f) else SurfaceMuted,
                ) { GlyphIcon(Glyph.Close, sub, size = 14.dp) }
            }
        }
    }
}

@Composable
private fun TimerChip(label: String, onClick: () -> Unit, tint: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(SurfaceMuted)
            .border(Dimens.hairline, Border, CircleShape)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mixer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MixerSection(
    mixer: MixerState,
    isPlaying: Boolean,
    isHeadphonesConnected: Boolean,
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
            .padding(horizontal = Dimens.gutter),
    ) {
        SectionLabel(
            text     = stringResource(R.string.player_section_mix),
            trailing = if (mixer.layers.isNotEmpty()) ({
                Text(
                    text  = "${mixer.layers.size} of ${mixer.layers.size + mixer.availableSlots.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                )
            }) else null,
        )

        if (mixer.layers.isEmpty()) {
            EmptyMixerCard(onAddSound)
        } else {
            GroupCard(glow = Glow(Charcoal, animated = isPlaying)) {
                mixer.layers.forEachIndexed { index, layer ->
                    LayerRow(
                        layer              = layer,
                        showHeadphonesNote = layer.sound.needsHeadphones && !isHeadphonesConnected,
                        modifier           = if (index == 0) Modifier.coachMarkAnchor("layer") else Modifier,
                        onVolume           = { vol -> onVolume(layer.position, vol) },
                        onDragEnd          = { onDragEnd(layer.position) },
                        onMute             = { onMute(layer.position) },
                        onUnmute           = { onUnmute(layer.position) },
                        onRemove           = { onRemove(layer.position) },
                    )
                    if (index < mixer.layers.lastIndex || mixer.layers.size > 1) RowDivider()
                }
                if (mixer.layers.size > 1) {
                    MasterVolumeRow(volume = masterVolume, onChange = onMasterVolume, onDragEnd = onMasterDragEnd)
                }
            }

            if (mixer.canAddLayer) {
                Spacer(Modifier.height(10.dp))
                AddLayerButton(onAddSound)
            }
        }
    }
}

@Composable
private fun EmptyMixerCard(onAddSound: () -> Unit) {
    DashedCard(
        onClick        = onAddSound,
        modifier       = Modifier
            .fillMaxWidth()
            .coachMarkAnchor("add_sound"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconCircle(SurfaceMuted, size = 44.dp) { GlyphIcon(Glyph.Plus, Charcoal, size = 18.dp) }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.player_empty_title), style = MaterialTheme.typography.titleSmall, color = Charcoal)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.player_empty_subtitle), style = MaterialTheme.typography.bodySmall, color = MutedGray)
        }
    }
}

@Composable
private fun LayerRow(
    layer: LayerUiState,
    showHeadphonesNote: Boolean,
    onVolume: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMute: () -> Unit,
    onUnmute: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (circleBg, circleFg) = categoryColors(layer.sound.category)
    val titleColor = if (layer.isMuted) MutedGray else Charcoal

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircle(
                background = if (layer.isMuted) SurfaceMuted else circleBg,
                glow       = circleFg.takeUnless { layer.isMuted },
            ) {
                GlyphIcon(layer.sound.category.glyph, if (layer.isMuted) MutedGray else circleFg, size = 18.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = layer.sound.displayTitle(),
                    style    = MaterialTheme.typography.titleSmall,
                    color    = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text  = stringResource(if (layer.isMuted) R.string.player_layer_muted else layer.sound.category.labelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                )
            }
            GhostIconButton(
                onClick            = if (layer.isMuted) onUnmute else onMute,
                contentDescription = stringResource(if (layer.isMuted) R.string.cd_unmute else R.string.cd_mute),
            ) {
                GlyphIcon(if (layer.isMuted) Glyph.VolumeOff else Glyph.Volume, if (layer.isMuted) MutedGray else Charcoal, size = 18.dp)
            }
            GhostIconButton(
                onClick            = onRemove,
                contentDescription = stringResource(R.string.player_cd_remove_layer),
            ) { GlyphIcon(Glyph.Minus, MutedGray, size = 18.dp) }
        }

        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            LineSlider(
                value                 = layer.volume,
                onValueChange         = onVolume,
                onValueChangeFinished = onDragEnd,
                modifier              = Modifier.weight(1f),
                colors                = LineSliderColors.light(muted = layer.isMuted),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text      = "${(layer.volume * 100).toInt()}",
                style     = MaterialTheme.typography.labelSmall,
                color     = MutedGray,
                textAlign = TextAlign.End,
                modifier  = Modifier.width(32.dp).padding(end = 8.dp),
            )
        }

        if (showHeadphonesNote) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier              = Modifier.padding(end = 8.dp, bottom = 4.dp),
            ) {
                GlyphIcon(Glyph.Headphones, MutedGray, size = 12.dp)
                Text(
                    text  = stringResource(R.string.headphones_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                )
            }
        }
    }
}

@Composable
private fun MasterVolumeRow(volume: Float, onChange: (Float) -> Unit, onDragEnd: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCircle(SurfaceMuted) { GlyphIcon(Glyph.Sliders, Charcoal, size = 18.dp) }
        Spacer(Modifier.width(14.dp))
        Text(
            text     = stringResource(R.string.player_master),
            style    = MaterialTheme.typography.titleSmall,
            color    = Charcoal,
            modifier = Modifier.width(64.dp),
        )
        LineSlider(
            value                 = volume,
            onValueChange         = onChange,
            onValueChangeFinished = onDragEnd,
            modifier              = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text      = "${(volume * 100).toInt()}",
            style     = MaterialTheme.typography.labelSmall,
            color     = MutedGray,
            textAlign = TextAlign.End,
            modifier  = Modifier.width(32.dp).padding(end = 8.dp),
        )
    }
}

@Composable
private fun AddLayerButton(onClick: () -> Unit) {
    DashedCard(
        onClick        = onClick,
        modifier       = Modifier
            .fillMaxWidth()
            .coachMarkAnchor("add_sound"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircle(SurfaceMuted, size = 36.dp) { GlyphIcon(Glyph.Plus, Charcoal, size = 16.dp) }
            Spacer(Modifier.width(14.dp))
            Text(
                text     = stringResource(R.string.player_add_sound),
                style    = MaterialTheme.typography.titleSmall,
                color    = Charcoal,
                modifier = Modifier.weight(1f),
            )
            GlyphIcon(Glyph.ChevronRight, MutedGray, size = 14.dp)
        }
    }
}

@Composable
private fun categoryColors(category: Domain.SoundCategory): Pair<Color, Color> = when (category) {
    Domain.SoundCategory.TINNITUS -> PaleBlue   to PaleBlueText
    Domain.SoundCategory.NATURE   -> PaleGreen  to PaleGreenText
    Domain.SoundCategory.ASMR     -> PaleYellow to PaleYellowText
    else                          -> SurfaceMuted to Charcoal
}

// ─────────────────────────────────────────────────────────────────────────────
// Night lock overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NightLockOverlay(
    state: NightLockState,
    onLongPressStarted: () -> Unit,
    onLongPressCompleted: () -> Unit,
    onLongPressCancelled: () -> Unit,
) {
    val unlocking = state == NightLockState.Unlocking
    val overlayLabel = stringResource(R.string.player_cd_night_lock_overlay)
    val progress by animateFloatAsState(
        targetValue      = if (unlocking) 1f else 0f,
        animationSpec    = if (unlocking) tween(1_500) else tween(100),
        label            = "unlock",
        finishedListener = { if (it >= 1f) onLongPressCompleted() },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.94f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPressStarted() },
                    onPress = {
                        tryAwaitRelease()
                        if (unlocking) onLongPressCancelled()
                    },
                )
            }
            .semantics { contentDescription = overlayLabel },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                UnlockProgressRing(if (unlocking) progress else 0f)
                GlyphIcon(Glyph.Lock, if (unlocking) DarkText.copy(alpha = 0.5f) else DarkText, size = 28.dp)
            }
            Text(
                stringResource(if (unlocking) R.string.player_keep_holding else R.string.player_night_lock),
                style = MaterialTheme.typography.titleMedium, color = DarkText,
            )
            Text(
                stringResource(R.string.player_long_press_unlock),
                style = MaterialTheme.typography.bodySmall, color = DarkTextSecondary,
            )
        }
    }
}

@Composable
private fun UnlockProgressRing(progress: Float) {
    Box(modifier = Modifier.size(72.dp).drawBehind {
        val stroke = 2.dp.toPx()
        val r      = size.minDimension / 2f - stroke
        val c      = Offset(size.width / 2f, size.height / 2f)
        val tl     = Offset(c.x - r, c.y - r)
        val sz     = Size(r * 2, r * 2)
        val style  = Stroke(stroke, cap = StrokeCap.Round)
        drawArc(DarkText.copy(alpha = 0.18f), -90f, 360f,           false, tl, sz, style = style)
        drawArc(DarkText,                     -90f, 360f * progress, false, tl, sz, style = style)
    })
}

// ─────────────────────────────────────────────────────────────────────────────
// Timer picker sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
        shape            = RoundedCornerShape(topStart = Dimens.panelRadius, topEnd = Dimens.panelRadius),
        dragHandle       = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Border),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.gutter)
                .padding(bottom = 36.dp),
        ) {
            Text(stringResource(R.string.player_timer_sheet_title), style = MaterialTheme.typography.headlineSmall, color = Charcoal)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.player_timer_sheet_subtitle), style = MaterialTheme.typography.bodyMedium, color = MutedGray)
            Spacer(Modifier.height(20.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
            ) {
                TIMER_PRESETS_MS.forEach { ms ->
                    val active = currentTimer is PlayerTimerState.Active && currentTimer.totalDurationMs == ms
                    SelectChip(label = shortDuration(ms), selected = active, onClick = { onSelect(ms) })
                }
            }

            if (currentTimer is PlayerTimerState.Active) {
                Spacer(Modifier.height(24.dp))
                SecondaryPill(
                    text         = stringResource(R.string.player_cancel_timer),
                    onClick      = onCancel,
                    contentColor = PaleRedText,
                    modifier     = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialogs
// ─────────────────────────────────────────────────────────────────────────────

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
        containerColor   = White,
        shape            = RoundedCornerShape(Dimens.panelRadius),
        title   = { Text(stringResource(R.string.player_save_preset_title), style = MaterialTheme.typography.headlineSmall, color = Charcoal) },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SleepTextInput(value = name, onValueChange = { name = it }, hint = stringResource(R.string.preset_field_name))
                SleepTextInput(
                    value         = emoji,
                    onValueChange = { if (it.length <= 2) emoji = it },
                    hint          = stringResource(R.string.preset_field_emoji),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name, emoji.ifBlank { null }) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save), style = MaterialTheme.typography.labelLarge,
                    color = if (name.isNotBlank()) Charcoal else MutedGray)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelLarge, color = MutedGray)
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
        containerColor   = White,
        shape            = RoundedCornerShape(Dimens.panelRadius),
        title   = { Text(stringResource(R.string.player_unsaved_title), style = MaterialTheme.typography.headlineSmall, color = Charcoal) },
        text    = {
            Text(stringResource(R.string.player_unsaved_body), style = MaterialTheme.typography.bodyMedium, color = MutedGray)
        },
        confirmButton  = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.action_save), style = MaterialTheme.typography.labelLarge, color = Charcoal)
            }
        },
        dismissButton  = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.action_discard), style = MaterialTheme.typography.labelLarge, color = PaleRedText)
            }
        },
    )
}
