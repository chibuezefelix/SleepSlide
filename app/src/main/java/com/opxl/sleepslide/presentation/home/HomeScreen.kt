package com.opxl.sleepslide.presentation.home


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opxl.sleepslide.R
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.DarkBackground
import com.opxl.sleepslide.ui.theme.DarkBorder
import com.opxl.sleepslide.ui.theme.DarkSurface
import com.opxl.sleepslide.ui.theme.DarkText
import com.opxl.sleepslide.ui.theme.LocalSleepSlideColors
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


@Composable
fun HomeScreen(
    onNavigateToPlayer: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToPreset: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }


    // One-shot event handler
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeViewState.HomeEvent.NavigateToPlayer         -> onNavigateToPlayer()
                is HomeViewState.HomeEvent.NavigateToLibrary        -> onNavigateToLibrary()
                is HomeViewState.HomeEvent.NavigateToPreset         -> onNavigateToPreset(event.presetId)
                is HomeViewState.HomeEvent.ShowError                -> snackbarHostState.showSnackbar(event.message)
                is HomeViewState.HomeEvent.ShowServiceUnavailableSnackbar ->
                    snackbarHostState.showSnackbar("Audio service is starting up")
                is HomeViewState.HomeEvent.ShowPresetLimitReached   ->
                    snackbarHostState.showSnackbar("You've reached ${event.limit} presets on the free plan")
                is HomeViewState.HomeEvent.AudioFocusRestoredAfterCall ->
                    snackbarHostState.showSnackbar("Sound resumed after your call")
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = WarmWhite,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            item {
                HomeHeader(
                    greeting        = uiState.greeting,
                    bluetooth       = uiState.bluetooth,
                    onLibraryClick  = { viewModel.requestNewPreset() },
                )
            }

item{
            AnimatedVisibility(
                visible = uiState.playback is HomeViewState.PlaybackUiState.Active ||
                        uiState.playback is HomeViewState.PlaybackUiState.AudioFocusLost,
                enter   = fadeIn(tween(300)) + slideInVertically { -it },
                exit    = fadeOut(tween(200)),
            ) {
                ActivePlaybackBar(
                    playback       = uiState.playback,
                    timer          = uiState.timer,
                    onPlayPause    = {
                        val pb = uiState.playback
                        if (pb is HomeViewState.PlaybackUiState.Active &&
                            pb.status == Domain.PlaybackStatus.PLAYING
                        ) viewModel.pausePlayback()
                        else viewModel.resumePlayback()
                    },
                    onStop         = { viewModel.stopPlayback() },
                    onExpandPlayer = { viewModel.navigateToPlayer() },
                )
            }
        }

            item {
                val screen = uiState.screen
                if (screen is HomeViewState.ScreenState.Ready && screen.resumeCard != null) {
                    SectionLabel("Continue")
                    ResumeCard(
                        resumeCard  = screen.resumeCard,
                        isPlaying   = uiState.playback is HomeViewState.PlaybackUiState.Active &&
                                (uiState.playback as HomeViewState.PlaybackUiState.Active).status ==Domain.PlaybackStatus.PLAYING,
                        onClick     = { viewModel.resumeLastPlayed() },
                    )
                }
            }


            item {
                AnimatedContent(
                    targetState = uiState.screen,
                    transitionSpec = {
                        fadeIn(tween(350)) togetherWith fadeOut(tween(200))
                    },
                    label = "home_screen_body",
                ) { screen ->
                    when (screen) {
                        is HomeViewState.ScreenState.Loading -> HomeLoadingBody()
                        is HomeViewState.ScreenState.Empty   -> HomeEmptyBody(
                            onBrowse = { viewModel.requestNewPreset() }
                        )
                        is HomeViewState.ScreenState.Error   -> HomeErrorBody(message = screen.message)
                        is HomeViewState.ScreenState.Ready   -> HomeReadyBody(
                            screen          = screen,
                            uiState         = uiState,
                            onPresetClick   = { viewModel.launchPreset(it) },
                            onPresetLong    = { viewModel.navigateToPreset(it.id) },
                            onSoundClick    = { viewModel.onSoundClicked(it) },
                            onAddPreset     = { viewModel.requestNewPreset() },
                        )
                    }
                }}




        }//<!--!>Scaffold
}

}


@Composable
fun HomeHeader(
    greeting: HomeViewState.GreetingUiState,
    bluetooth: HomeViewState.BluetoothUiState,
    onLibraryClick: () -> Unit
){


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text  = greeting.headline,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Charcoal,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = greeting.subtext,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (bluetooth is HomeViewState.BluetoothUiState.Connected) {
                    BluetoothChip()
                }
                LibraryIconButton(onClick = onLibraryClick)
            }
        }

        Spacer(Modifier.height(24.dp))
        Divider()
    }

}


@Composable
private fun BluetoothChip() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PaleBlue)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .semantics { contentDescription = "Bluetooth headphones connected" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(PaleBlueText),
        )
        Text(
            text  = "BT",
            style = MaterialTheme.typography.labelSmall,
            color = PaleBlueText,
        )
    }
}

@Composable
private fun LibraryIconButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .background(White)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Browse sounds" },
        contentAlignment = Alignment.Center,
    ) {
        // Plus icon — SVG primitive, 16×16
        Box(
            modifier = Modifier.size(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxWidth().height(1.5.dp).background(Charcoal))
            Box(Modifier.width(1.5.dp).height(16.dp).background(Charcoal))
        }
    }
}



@Composable
private fun ActivePlaybackBar(
    playback: HomeViewState.PlaybackUiState,
    timer: HomeViewState.TimerUiState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onExpandPlayer: () -> Unit,
) {
    val isPlaying = playback is HomeViewState.PlaybackUiState.Active &&
            (playback as HomeViewState.PlaybackUiState.Active).status == Domain.PlaybackStatus.PLAYING

    val presetName = (playback as? HomeViewState.PlaybackUiState.Active)?.activePresetName

    val interrupted = playback is HomeViewState.PlaybackUiState.AudioFocusLost

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (interrupted) PaleYellow else Charcoal)
            .clickable(onClick = onExpandPlayer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        interrupted -> "Sound paused"
                        isPlaying   -> presetName ?: "Playing"
                        else        -> "Paused"
                    },
                    style    = MaterialTheme.typography.titleSmall,
                    color    = if (interrupted) PaleYellowText else White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (timer is HomeViewState.TimerUiState.Active) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = "Stops in ${timer.remainingLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (interrupted) PaleYellowText.copy(alpha = 0.7f)
                        else White.copy(alpha = 0.6f),
                    )
                }
                if (playback is HomeViewState.PlaybackUiState.AudioFocusLost &&
                    playback.reason == Domain.AudioFocusLostReason.PHONE_CALL
                ) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = "Resuming after call",
                        style = MaterialTheme.typography.bodySmall,
                        color = PaleYellowText.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!interrupted) {
                    PlayPauseButton(
                        isPlaying = isPlaying,
                        tint      = White,
                        onClick   = onPlayPause,
                    )
                }
                StopButton(
                    tint    = if (interrupted) PaleYellowText else White.copy(alpha = 0.5f),
                    onClick = onStop,
                )
            }
        }
    }
}


@Composable
private fun ResumeCard(
    resumeCard:HomeViewState. ResumeCardState,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val name = when (resumeCard) {
        is HomeViewState.ResumeCardState.FromPreset       -> resumeCard.preset.name
        is HomeViewState.ResumeCardState.FromEphemeralMix -> "Last session"
    }
    val layerCount = when (resumeCard) {
        is HomeViewState.ResumeCardState.FromPreset       -> resumeCard.preset.mix.layers.size
        is HomeViewState.ResumeCardState.FromEphemeralMix -> resumeCard.mix.layers.size
    }
    val soundNames = when (resumeCard) {
        is HomeViewState.ResumeCardState.FromPreset       ->
            resumeCard.preset.mix.layers.joinToString(" · ") { it.sound.title }
        is HomeViewState.ResumeCardState.FromEphemeralMix ->
            resumeCard.mix.layers.joinToString(" · ") { it.sound.title }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .background(White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = name,
                style    = MaterialTheme.typography.titleMedium,
                color    = Charcoal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text     = soundNames.ifBlank { "$layerCount sound${if (layerCount != 1) "s" else ""}" },
                style    = MaterialTheme.typography.bodySmall,
                color    = MutedGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(12.dp))

        PlayPauseButton(
            isPlaying = isPlaying,
            tint      = Charcoal,
            onClick   = onClick,
        )
    }
}



@Composable
private fun HomeReadyBody(
    screen: HomeViewState.ScreenState.Ready,
    uiState: HomeViewState.HomeUiState,
    onPresetClick: (Domain.Preset) -> Unit,
    onPresetLong: (Domain.Preset) -> Unit,
    onSoundClick: (Domain.Sound) -> Unit,
    onAddPreset: () -> Unit,
) {
    Column {
        // Pinned presets
        if (screen.pinnedPresets.isNotEmpty()) {
            SectionLabel("Pinned")
            PresetGrid(
                presets   = screen.pinnedPresets,
                activeId  = (uiState.playback as? HomeViewState.PlaybackUiState.Active)?.activePresetId,
                onClick   = onPresetClick,
                onLongClick = onPresetLong,
            )
        }

        // Recent presets
        if (screen.recentPresets.isNotEmpty()) {
            SectionLabel("Recent")
            PresetGrid(
                presets   = screen.recentPresets,
                activeId  = (uiState.playback as? HomeViewState.PlaybackUiState.Active)?.activePresetId,
                onClick   = onPresetClick,
                onLongClick = onPresetLong,
            )
        }

        // Add preset CTA — only shown when not at limit
        if (!uiState.hasReachedFreePresetLimit) {
            AddPresetButton(onClick = onAddPreset)
        } else {
            PresetLimitBanner()
        }

        // Recently played sounds
        if (screen.recentSounds.isNotEmpty()) {
            SectionLabel("Sounds you've used")
            RecentSoundsRow(
                sounds  = screen.recentSounds,
                onClick = onSoundClick,
            )
        }

        // Stats row
        if (screen.totalListenedMs > 0L) {
            StatsRow(totalListenedMs = screen.totalListenedMs)
        }
    }
}

@Composable
private fun PresetGrid(
    presets: List<Domain.Preset>,
    activeId: Long?,
    onClick: (Domain.Preset) -> Unit,
    onLongClick: (Domain.Preset) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presets.chunked(2).forEachIndexed { rowIndex, rowPresets ->
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowPresets.forEachIndexed { colIndex, preset ->
                    PresetCard(
                        preset      = preset,
                        isActive    = preset.id == activeId,
                        onClick     = { onClick(preset) },
                        onLongClick = { onLongClick(preset) },
                        modifier    = Modifier.weight(1f),
                        animDelay   = (rowIndex * 2 + colIndex) * 60,
                    )
                }
                // Pad incomplete rows
                if (rowPresets.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}



@Composable
private fun PresetCard(
    preset: Domain.Preset,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    animDelay: Int = 0,
) {
    val extended = LocalSleepSlideColors.current

    val bgColor    = if (isActive) Charcoal else White
    val textColor  = if (isActive) White else Charcoal
    val subColor   = if (isActive) White.copy(alpha = 0.6f) else MutedGray
    val borderColor = if (isActive) Color.Transparent else Border

    val soundNames = preset.mix.layers
        .take(3)
        .joinToString(" · ") { it.sound.title }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(16.dp)
            .semantics {
                contentDescription = "${preset.name}, ${preset.mix.layers.size} sounds" +
                        if (isActive) ", currently playing" else ""
            },
    ) {
        Column {
            // Emoji or active indicator
            if (preset.emoji != null) {
                Text(
                    text  = preset.emoji,
                    style = MaterialTheme.typography.headlineSmall,
                )
            } else if (isActive) {
                ActivePulse()
            } else {
                SoundCountBadge(
                    count      = preset.mix.layers.size,
                    background = extended.mutedSurface,
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text     = preset.name,
                style    = MaterialTheme.typography.titleSmall,
                color    = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text     = soundNames.ifBlank { "${preset.mix.layers.size} sound layers" },
                style    = MaterialTheme.typography.bodySmall,
                color    = subColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}



@Composable
private fun RecentSoundsRow(
    sounds: List<Domain.Sound>,
    onClick: (Domain.Sound) -> Unit,
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(sounds) { _, sound ->
            SoundPill(sound = sound, onClick = { onClick(sound) })
        }
    }
    Spacer(Modifier.height(24.dp))
}


@Composable
private fun StatsRow(totalListenedMs: Long) {
    val hours   = totalListenedMs / 3_600_000L
    val minutes = (totalListenedMs % 3_600_000L) / 60_000L
    val label   = when {
        hours > 0   -> "${hours}h ${minutes}m of calm"
        minutes > 0 -> "${minutes}m of calm"
        else        -> "Your first session"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceMuted)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
        )
        Text(
            text  = "Total",
            style = MaterialTheme.typography.labelSmall,
            color = MutedGray.copy(alpha = 0.6f),
        )
    }
    Spacer(Modifier.height(24.dp))
}


@Composable
private fun SoundPill(
    sound: Domain.Sound,
    onClick: () -> Unit,
) {
    val extended = LocalSleepSlideColors.current
    val (bg, fg) = when (sound.category.name) {
        "TINNITUS" -> PaleBlue to PaleBlueText
        "NATURE"   -> PaleGreen to PaleGreenText
        else       -> extended.mutedSurface to MutedGray
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
            .semantics { contentDescription = "${sound.title} sound" },
    ) {
        Text(
            text  = sound.title,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
        )
    }
}



@Composable
private fun AddPresetButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceMuted),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxWidth(0.5f).height(1.5.dp).background(Charcoal))
            Box(Modifier.width(1.5.dp).fillMaxHeight(0.5f).background(Charcoal))
        }
        Text(
            text  = "Build a new mix",
            style = MaterialTheme.typography.titleSmall,
            color = Charcoal,
        )
    }
    Spacer(Modifier.height(8.dp))
}



@Composable
private fun PresetLimitBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PaleYellow)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(PaleYellowText),
        )
        Text(
            text  = "10 preset limit reached — unlock all for unlimited",
            style = MaterialTheme.typography.bodySmall,
            color = PaleYellowText,
        )
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun HomeLoadingBody() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Border),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}


@Composable
private fun HomeEmptyBody(onBrowse: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text  = "No sounds yet",
            style = MaterialTheme.typography.headlineSmall,
            color = Charcoal,
        )
        Text(
            text  = "Pick a sound from the library to build your first mix.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGray,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Charcoal)
                .clickable(onClick = onBrowse)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(
                text  = "Browse sounds",
                style = MaterialTheme.typography.labelLarge,
                color = White,
            )
        }
    }
}


@Composable
private fun HomeErrorBody(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(LocalSleepSlideColors.current.paleRed)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text  = "Error",
                style = MaterialTheme.typography.labelMedium,
                color = LocalSleepSlideColors.current.paleRedText,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text  = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGray,
        )
    }
}



@Composable
private fun SectionLabel(label: String) {
    Text(
        text     = label.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = MutedGray,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 10.dp),
    )
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border),
    )
}



@Composable
private fun ActivePulse() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { i ->
            val height = when (i) { 0 -> 8.dp; 1 -> 14.dp; else -> 8.dp }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(height)
                    .clip(RoundedCornerShape(1.dp))
                    .background(White.copy(alpha = 0.7f)),
            )
        }
    }
}


@Composable
private fun SoundCountBadge(count: Int, background: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text  = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MutedGray,
        )
    }
}



@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (isPlaying) "Pause" else "Play"
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isPlaying) {
            // Two vertical bars = pause
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.width(2.5.dp).height(12.dp).background(tint))
                Box(Modifier.width(2.5.dp).height(12.dp).background(tint))
            }
        } else {
            // Right-pointing triangle = play
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = tint,
                        shape = object : androidx.compose.ui.graphics.Shape {
                            override fun createOutline(
                                size: androidx.compose.ui.geometry.Size,
                                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                                density: androidx.compose.ui.unit.Density,
                            ) = androidx.compose.ui.graphics.Outline.Generic(
                                androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, 0f)
                                    lineTo(size.width, size.height / 2)
                                    lineTo(0f, size.height)
                                    close()
                                }
                            )
                        }
                    ),
            )
        }
    }
}



@Composable
private fun StopButton(
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Stop playback" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(tint),
        )
    }
}

