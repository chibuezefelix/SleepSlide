package com.opxl.sleepslide.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opxl.sleepslide.R
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.presentation.components.categoryColors
import com.opxl.sleepslide.presentation.components.Glow
import com.opxl.sleepslide.presentation.components.GlowCorner
import com.opxl.sleepslide.presentation.components.glow
import com.opxl.sleepslide.presentation.components.CircleIconButton
import com.opxl.sleepslide.presentation.components.DashedCard
import com.opxl.sleepslide.presentation.components.GhostIconButton
import com.opxl.sleepslide.presentation.components.Glyph
import com.opxl.sleepslide.presentation.components.GlyphIcon
import com.opxl.sleepslide.presentation.components.IconCircle
import com.opxl.sleepslide.presentation.components.PrimaryPill
import com.opxl.sleepslide.presentation.components.SectionLabel
import com.opxl.sleepslide.presentation.components.SurfaceCard
import com.opxl.sleepslide.presentation.components.Tag
import com.opxl.sleepslide.presentation.components.displayTitle
import com.opxl.sleepslide.presentation.components.glyph
import com.opxl.sleepslide.presentation.scene.SceneBackdrop
import com.opxl.sleepslide.presentation.tutorial.CoachMarkHost
import com.opxl.sleepslide.domain.repository.CoachMarkScreens
import com.opxl.sleepslide.presentation.tutorial.TutorialViewModel
import com.opxl.sleepslide.presentation.tutorial.coachMarkAnchor
import com.opxl.sleepslide.presentation.tutorial.tutorialViewModel
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.Dimens
import com.opxl.sleepslide.ui.theme.ImageScrim
import com.opxl.sleepslide.ui.theme.MutedGray
import com.opxl.sleepslide.ui.theme.OnImage
import com.opxl.sleepslide.ui.theme.OnPanel
import com.opxl.sleepslide.ui.theme.PaleBlue
import com.opxl.sleepslide.ui.theme.PaleBlueText
import com.opxl.sleepslide.ui.theme.PaleGreenText
import com.opxl.sleepslide.ui.theme.PaleRed
import com.opxl.sleepslide.ui.theme.PaleRedText
import com.opxl.sleepslide.ui.theme.PaleYellow
import com.opxl.sleepslide.ui.theme.PaleYellowText
import com.opxl.sleepslide.ui.theme.Panel
import com.opxl.sleepslide.ui.theme.SurfaceMuted
import com.opxl.sleepslide.ui.theme.WarmWhite
import com.opxl.sleepslide.ui.theme.White

@Composable
fun HomeScreen(
    onNavigateToPlayer: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToPreset: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    tutorial: TutorialViewModel = tutorialViewModel(CoachMarkScreens.HOME),
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

    CoachMarkHost(viewModel = tutorial, labels = HOME_COACH_MARKS) {
        Scaffold(
            snackbarHost   = { SnackbarHost(snackbarHostState) },
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
                        greeting       = uiState.greeting,
                        bluetooth      = uiState.bluetooth,
                        onLibraryClick = { viewModel.requestNewPreset() },
                    )
                }

                // The hero: what's playing, or what to pick up next. Always present once the
                // screen has loaded, so the page has one anchor instead of bars sliding in and out.
                item {
                    val screen = uiState.screen
                    if (screen !is HomeViewState.ScreenState.Loading) {
                        HomeHero(
                            playback   = uiState.playback,
                            timer      = uiState.timer,
                            resumeCard = (screen as? HomeViewState.ScreenState.Ready)?.resumeCard,
                            onPlayPause = {
                                val pb = uiState.playback
                                if (pb is HomeViewState.PlaybackUiState.Active &&
                                    pb.status == Domain.PlaybackStatus.PLAYING
                                ) viewModel.pausePlayback()
                                else viewModel.resumePlayback()
                            },
                            onStop   = { viewModel.stopPlayback() },
                            onExpand = { viewModel.navigateToPlayer() },
                            onResume = { viewModel.resumeLastPlayed() },
                            onBrowse = { viewModel.requestNewPreset() },
                        )
                    }
                }

                item {
                    AnimatedContent(
                        targetState    = uiState.screen,
                        transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) },
                        label          = "home_screen_body",
                    ) { screen ->
                        when (screen) {
                            is HomeViewState.ScreenState.Loading -> HomeLoadingBody()
                            is HomeViewState.ScreenState.Empty   -> HomeEmptyBody(
                                onBrowse = { viewModel.requestNewPreset() }
                            )
                            is HomeViewState.ScreenState.Error   -> HomeErrorBody(message = screen.message)
                            is HomeViewState.ScreenState.Ready   -> HomeReadyBody(
                                screen        = screen,
                                uiState       = uiState,
                                onPresetClick = { viewModel.launchPreset(it) },
                                onPresetLong  = { viewModel.navigateToPreset(it.id) },
                                onSoundClick  = { viewModel.onSoundClicked(it) },
                                onAddPreset   = { viewModel.requestNewPreset() },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Anchor key → label for the first-visit coach marks; only laid-out targets are shown. */
private val HOME_COACH_MARKS = listOf(
    "library"     to R.string.coach_home_library,
    "now_playing" to R.string.coach_home_now_playing,
    "resume"      to R.string.coach_home_resume,
    "preset"      to R.string.coach_home_preset,
    "add_mix"     to R.string.coach_home_add_mix,
    "browse"      to R.string.coach_home_browse,
)

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeHeader(
    greeting: HomeViewState.GreetingUiState,
    bluetooth: HomeViewState.BluetoothUiState,
    onLibraryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Dimens.gutter)
            .padding(top = 12.dp, bottom = 20.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            AppLogo()
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (bluetooth is HomeViewState.BluetoothUiState.Connected) BluetoothBadge()
                CircleIconButton(
                    onClick            = onLibraryClick,
                    contentDescription = "Browse sounds",
                    modifier           = Modifier.coachMarkAnchor("library"),
                ) { GlyphIcon(Glyph.Plus, Charcoal, size = 18.dp) }
            }
        }

        if (greeting.headline.isNotBlank()) {
            Spacer(Modifier.height(24.dp))
            Text(
                text  = greeting.headline,
                style = MaterialTheme.typography.headlineLarge,
                color = Charcoal,
            )
            if (greeting.subtext.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = greeting.subtext,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                )
            }
        }
    }
}

@Composable
private fun AppLogo() {
    Image(
        painter            = painterResource(R.drawable.img_app_logo),
        contentDescription = "SleepSlide",
        contentScale       = ContentScale.Crop,
        modifier           = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(Dimens.hairline, Border, RoundedCornerShape(10.dp)),
    )
}

@Composable
private fun BluetoothBadge() {
    IconCircle(
        background = PaleBlue,
        size       = Dimens.iconButton,
        modifier   = Modifier.semantics { contentDescription = "Bluetooth headphones connected" },
    ) { GlyphIcon(Glyph.Bluetooth, PaleBlueText, size = 18.dp) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero
// ─────────────────────────────────────────────────────────────────────────────

/**
 * One photo card that carries the current session: transport controls while something is
 * playing, a "Continue" action when nothing is, and a nudge to build a first mix otherwise.
 */
@Composable
private fun HomeHero(
    playback: HomeViewState.PlaybackUiState,
    timer: HomeViewState.TimerUiState,
    resumeCard: HomeViewState.ResumeCardState?,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onExpand: () -> Unit,
    onResume: () -> Unit,
    onBrowse: () -> Unit,
) {
    val active      = playback as? HomeViewState.PlaybackUiState.Active
    val interrupted = playback as? HomeViewState.PlaybackUiState.AudioFocusLost
    val isPlaying   = active?.status == Domain.PlaybackStatus.PLAYING

    val resumeName = when (resumeCard) {
        is HomeViewState.ResumeCardState.FromPreset       -> resumeCard.preset.name
        is HomeViewState.ResumeCardState.FromEphemeralMix -> "Last session"
        null -> null
    }
    val resumeSounds = when (resumeCard) {
        is HomeViewState.ResumeCardState.FromPreset       -> resumeCard.preset.mix.layers.map { it.sound }
        is HomeViewState.ResumeCardState.FromEphemeralMix -> resumeCard.mix.layers.map { it.sound }
        null -> emptyList()
    }

    val activeSounds = active?.activeMix?.layers?.map { it.sound }.orEmpty()
    val heroSounds   = if (activeSounds.isNotEmpty()) activeSounds else resumeSounds
    val context = LocalContext.current

    // What the card says, and what tapping it does
    val label: String
    val title: String
    val subtitle: String
    val onTap: () -> Unit
    when {
        interrupted != null -> {
            label    = "Paused"
            title    = "Sound paused"
            subtitle = if (interrupted.reason == Domain.AudioFocusLostReason.PHONE_CALL)
                "Resuming after your call" else "Another app took over audio"
            onTap    = onExpand
        }
        active != null -> {
            label    = if (isPlaying) "Now playing" else "Paused"
            title    = active.activePresetName ?: resumeName ?: "Your mix"
            subtitle = activeSounds.joinToString(" · ") { it.displayTitle(context) }
            onTap    = onExpand
        }
        resumeCard != null -> {
            label    = "Continue"
            title    = resumeName.orEmpty()
            subtitle = resumeSounds.joinToString(" · ") { it.displayTitle(context) }
            onTap    = onResume
        }
        else -> {
            label    = "Tonight"
            title    = "Build your first mix"
            subtitle = "Layer up to three sounds and save them"
            onTap    = onBrowse
        }
    }

    SceneBackdrop(
        sounds      = heroSounds,
        isPlaying   = isPlaying,
        height      = 224.dp,
        showCaption = false,
        onTap       = onTap,
        modifier    = Modifier.padding(bottom = 4.dp),
        glow        = Glow(OnImage, animated = true, corner = GlowCorner.TopEnd),
    ) {
        // Timer badge
        if (timer is HomeViewState.TimerUiState.Active) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .clip(CircleShape)
                    .background(ImageScrim.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GlyphIcon(Glyph.Timer, OnImage, size = 14.dp)
                Text(timer.remainingLabel, style = MaterialTheme.typography.labelMedium, color = OnImage)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isPlaying) PlayingDot(OnImage)
                Text(
                    text  = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnImage.copy(alpha = 0.72f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text     = title,
                        style    = MaterialTheme.typography.titleLarge,
                        color    = OnImage,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text     = subtitle,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = OnImage.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.coachMarkAnchor(
                        if (active != null || interrupted != null) "now_playing" else "resume"
                    ),
                ) {
                    when {
                        interrupted != null -> HeroGhostButton(Glyph.Stop, "Stop playback", onStop)
                        active != null -> {
                            HeroGhostButton(Glyph.Stop, "Stop playback", onStop)
                            HeroMainButton(
                                glyph              = if (isPlaying) Glyph.Pause else Glyph.Play,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                onClick            = onPlayPause,
                            )
                        }
                        resumeCard != null -> HeroMainButton(Glyph.Play, "Play", onResume)
                        else -> HeroMainButton(Glyph.Plus, "Browse sounds", onBrowse)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMainButton(glyph: Glyph, contentDescription: String, onClick: () -> Unit) {
    GhostIconButton(
        onClick            = onClick,
        contentDescription = contentDescription,
        size               = 52.dp,
        background         = OnImage,
    ) { GlyphIcon(glyph, ImageScrim, size = 22.dp) }
}

@Composable
private fun HeroGhostButton(glyph: Glyph, contentDescription: String, onClick: () -> Unit) {
    GhostIconButton(
        onClick            = onClick,
        contentDescription = contentDescription,
        size               = 40.dp,
        background         = OnImage.copy(alpha = 0.16f),
    ) { GlyphIcon(glyph, OnImage, size = 20.dp) }
}

@Composable
private fun PlayingDot(tint: Color) {
    Box(Modifier.size(6.dp).clip(CircleShape).background(tint))
}

// ─────────────────────────────────────────────────────────────────────────────
// Body
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeReadyBody(
    screen: HomeViewState.ScreenState.Ready,
    uiState: HomeViewState.HomeUiState,
    onPresetClick: (Domain.Preset) -> Unit,
    onPresetLong: (Domain.Preset) -> Unit,
    onSoundClick: (Domain.Sound) -> Unit,
    onAddPreset: () -> Unit,
) {
    val active    = uiState.playback as? HomeViewState.PlaybackUiState.Active
    val activeId  = active?.activePresetId
    val playingId = activeId.takeIf { active?.status == Domain.PlaybackStatus.PLAYING }
    Column {
        if (screen.pinnedPresets.isNotEmpty()) {
            HomeSectionLabel("Pinned")
            PresetGrid(
                presets     = screen.pinnedPresets,
                activeId    = activeId,
                playingId   = playingId,
                onClick     = onPresetClick,
                onLongClick = onPresetLong,
                anchorFirst = true,
            )
        }

        if (screen.recentPresets.isNotEmpty()) {
            HomeSectionLabel("Recent")
            PresetGrid(
                presets     = screen.recentPresets,
                activeId    = activeId,
                playingId   = playingId,
                onClick     = onPresetClick,
                onLongClick = onPresetLong,
                anchorFirst = screen.pinnedPresets.isEmpty(),
            )
        }

        Spacer(Modifier.height(12.dp))
        if (!uiState.hasReachedFreePresetLimit) {
            AddPresetButton(onClick = onAddPreset)
        } else {
            PresetLimitBanner()
        }

        if (screen.recentSounds.isNotEmpty()) {
            HomeSectionLabel("Sounds you've used")
            RecentSoundsRow(sounds = screen.recentSounds, onClick = onSoundClick)
        }

        if (screen.totalListenedMs > 0L) {
            StatsRow(totalListenedMs = screen.totalListenedMs)
        }
    }
}

@Composable
private fun HomeSectionLabel(text: String) {
    SectionLabel(text, modifier = Modifier.padding(horizontal = Dimens.gutter))
}

@Composable
private fun PresetGrid(
    presets: List<Domain.Preset>,
    activeId: Long?,
    playingId: Long?,
    onClick: (Domain.Preset) -> Unit,
    onLongClick: (Domain.Preset) -> Unit,
    anchorFirst: Boolean = false,   // first card is the coach mark target for "your presets"
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.gutter),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        presets.chunked(2).forEachIndexed { rowIndex, rowPresets ->
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowPresets.forEachIndexed { colIndex, preset ->
                    PresetCard(
                        preset      = preset,
                        isActive    = preset.id == activeId,
                        isPlaying   = preset.id == playingId,
                        onClick     = { onClick(preset) },
                        onLongClick = { onLongClick(preset) },
                        modifier    = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(if (anchorFirst && rowIndex == 0 && colIndex == 0) Modifier.coachMarkAnchor("preset") else Modifier),
                    )
                }
                if (rowPresets.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetCard(
    preset: Domain.Preset,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg     by animateColorAsState(if (isActive) Panel else White,  tween(250), label = "preset_bg")
    val stroke by animateColorAsState(if (isActive) Panel else Border, tween(250), label = "preset_border")
    val textColor = if (isActive) OnPanel else Charcoal
    val subColor  = if (isActive) OnPanel.copy(alpha = 0.6f) else MutedGray
    val shape = RoundedCornerShape(Dimens.cardRadius)

    val context = LocalContext.current
    val soundNames = preset.mix.layers.take(3).joinToString(" · ") { it.sound.displayTitle(context) }
    val layerCount = preset.mix.layers.size
    // The tile carries the hue of its lead sound; on the inverted active tile the light is neutral.
    val leadCategory = preset.mix.layers.firstOrNull()?.sound?.category
    val glowTint = when {
        isActive             -> OnPanel
        leadCategory != null -> categoryColors(leadCategory).second
        else                 -> Charcoal
    }

    Column(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .glow(Glow(glowTint, animated = isPlaying))
            .border(Dimens.hairline, stroke, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(16.dp)
            .semantics {
                contentDescription = "${preset.name}, $layerCount sounds" +
                        if (isActive) ", currently playing" else ""
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                isActive -> IconCircle(OnPanel.copy(alpha = 0.14f), size = 36.dp) {
                    GlyphIcon(Glyph.WaveBars, OnPanel, size = 18.dp)
                }
                preset.emoji != null -> IconCircle(SurfaceMuted, size = 36.dp) {
                    Text(preset.emoji, style = MaterialTheme.typography.titleMedium)
                }
                else -> IconCircle(SurfaceMuted, size = 36.dp) {
                    Text("$layerCount", style = MaterialTheme.typography.labelMedium, color = MutedGray)
                }
            }
            Spacer(Modifier.weight(1f))
            if (preset.isPinned && !isActive) GlyphIcon(Glyph.Pin, MutedGray, size = 14.dp)
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text     = preset.name,
            style    = MaterialTheme.typography.titleMedium,
            color    = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text     = soundNames.ifBlank { "$layerCount sound layers" },
            style    = MaterialTheme.typography.bodySmall,
            color    = subColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddPresetButton(onClick: () -> Unit) {
    DashedCard(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.gutter)
            .coachMarkAnchor("add_mix"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircle(SurfaceMuted, size = 36.dp) { GlyphIcon(Glyph.Plus, Charcoal, size = 16.dp) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Build a new mix", style = MaterialTheme.typography.titleSmall, color = Charcoal)
                Text("Pick sounds from the library", style = MaterialTheme.typography.bodySmall, color = MutedGray)
            }
            GlyphIcon(Glyph.ChevronRight, MutedGray, size = 14.dp)
        }
    }
}

@Composable
private fun PresetLimitBanner() {
    SurfaceCard(
        modifier   = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.gutter),
        background = PaleYellow,
        border     = Color.Transparent,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlyphIcon(Glyph.Sparkle, PaleYellowText, size = 18.dp)
            Text(
                text  = "10 preset limit reached — unlock all for unlimited",
                style = MaterialTheme.typography.bodySmall,
                color = PaleYellowText,
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
        contentPadding        = PaddingValues(horizontal = Dimens.gutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(sounds, key = { it.id }) { sound ->
            SoundPill(sound = sound, onClick = { onClick(sound) })
        }
    }
}

@Composable
private fun SoundPill(sound: Domain.Sound, onClick: () -> Unit) {
    val title = sound.displayTitle()
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(White)
            .border(Dimens.hairline, Border, CircleShape)
            .combinedClickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp, top = 9.dp, bottom = 9.dp)
            .semantics { contentDescription = "$title sound" },
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GlyphIcon(sound.category.glyph, categoryTint(sound.category), size = 16.dp)
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = Charcoal)
    }
}

@Composable
private fun categoryTint(category: Domain.SoundCategory): Color = when (category) {
    Domain.SoundCategory.TINNITUS -> PaleBlueText
    Domain.SoundCategory.NATURE   -> PaleGreenText
    Domain.SoundCategory.ASMR     -> PaleYellowText
    else                          -> MutedGray
}

@Composable
private fun StatsRow(totalListenedMs: Long) {
    val hours   = totalListenedMs / 3_600_000L
    val minutes = (totalListenedMs % 3_600_000L) / 60_000L
    val value   = when {
        hours > 0   -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else        -> "<1m"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.gutter)
            .padding(top = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text("Time spent in calm", style = MaterialTheme.typography.bodySmall, color = MutedGray)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Charcoal)
    }
}

@Composable
private fun HomeLoadingBody() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.gutter, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(224.dp)
                .clip(RoundedCornerShape(Dimens.panelRadius))
                .background(SurfaceMuted),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(2) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(120.dp)
                        .clip(RoundedCornerShape(Dimens.cardRadius))
                        .background(SurfaceMuted),
                )
            }
        }
    }
}

@Composable
private fun HomeEmptyBody(onBrowse: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.gutter, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No sounds yet", style = MaterialTheme.typography.headlineSmall, color = Charcoal)
        Spacer(Modifier.height(6.dp))
        Text(
            text  = "Pick a sound from the library to build your first mix.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGray,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryPill(
            text     = "Browse sounds",
            onClick  = onBrowse,
            modifier = Modifier.coachMarkAnchor("browse"),
        )
    }
}

@Composable
private fun HomeErrorBody(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.gutter, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Tag("Error", PaleRed, PaleRedText)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MutedGray)
    }
}
