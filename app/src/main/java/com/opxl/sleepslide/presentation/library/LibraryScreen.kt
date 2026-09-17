package com.opxl.sleepslide.presentation.library

import com.opxl.sleepslide.domain.model.Domain

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opxl.sleepslide.domain.repository.CoachMarkScreens
import com.opxl.sleepslide.presentation.tutorial.CoachMarkHost
import com.opxl.sleepslide.presentation.tutorial.TutorialViewModel
import com.opxl.sleepslide.presentation.tutorial.coachMarkAnchor
import com.opxl.sleepslide.presentation.tutorial.tutorialViewModel
import com.opxl.sleepslide.presentation.components.LineSlider
import com.opxl.sleepslide.presentation.components.ChevronLeft
import com.opxl.sleepslide.presentation.components.LineSliderColors
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
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


@Composable
fun LibraryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    tutorial: TutorialViewModel = tutorialViewModel(CoachMarkScreens.LIBRARY),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryVMState.LibraryEvent.NavigateToPlayer ->
                    onNavigateToPlayer()
                is LibraryVMState.LibraryEvent.ShowSavePresetDialog ->
                    showSaveDialog = true
                is LibraryVMState.LibraryEvent.ShowUpgradePrompt ->
                    scope.launch {
                        snackbarHostState.showSnackbar("\"${event.soundTitle}\" is a premium sound — upgrade to unlock")
                    }
                is LibraryVMState.LibraryEvent.ShowMaxLayersReached ->
                    scope.launch { snackbarHostState.showSnackbar("Maximum 3 sounds in a mix") }
                is LibraryVMState.LibraryEvent.ShowServiceUnavailable ->
                    scope.launch { snackbarHostState.showSnackbar("Audio service is starting") }
                is LibraryVMState.LibraryEvent.ShowError ->
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                is LibraryVMState.LibraryEvent.ShowInfo ->
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                is LibraryVMState.LibraryEvent.PresetSaved ->
                    scope.launch { snackbarHostState.showSnackbar("\"${event.name}\" saved") }
                is LibraryVMState.LibraryEvent.SoundAlreadyInMix ->
                    scope.launch { snackbarHostState.showSnackbar("${event.soundTitle} is already in the mix") }
            }
        }
    }

    CoachMarkHost(viewModel = tutorial, labels = LIBRARY_COACH_MARKS) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            LibraryTopBar(
                filter      = uiState.filter,
                activeMix   = uiState.activeMix,
                onBack      = onNavigateBack,
                onSearch    = viewModel::onSearchQueryChanged,
                onClear     = { viewModel.clearSearch() },
                onSave      = { viewModel.requestSaveAsPreset() },
            )

            AnimatedContent(
                targetState   = uiState.catalogue,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label         = "library_body",
            ) { catalogue ->
                when (catalogue) {
                    is LibraryVMState.CatalogueState.Loading -> LibraryLoadingSkeleton()
                    is LibraryVMState.CatalogueState.Error   -> LibraryErrorState(catalogue.message)
                    is LibraryVMState.CatalogueState.Ready   -> LibraryReadyBody(
                        catalogue = catalogue,
                        activeMix = uiState.activeMix,
                        playback  = uiState.playback,
                        tier      = uiState.entitlementTier,
                        onTabSelect       = viewModel::selectCategory,
                        onSoundTap        = viewModel::onSoundTapped,
                        onSoundLongPress  = viewModel::onSoundLongPressed,
                        onLayerVolume     = viewModel::onLayerVolumeChanged,
                        onLayerDragEnd    = viewModel::onLayerVolumeDragEnded,
                        onMasterVolume    = viewModel::onMasterVolumeChanged,
                        onMasterDragEnd   = { viewModel.onMasterVolumeDragEnded() },
                        onMute            = viewModel::muteLayer,
                        onUnmute          = viewModel::unmuteLayer,
                        onPlay            = { viewModel.playCurrentMix() },
                        onPause           = { viewModel.pausePlayback() },
                        onResume          = { viewModel.resumePlayback() },
                    )
                }
            }
        }
    }
    } // CoachMarkHost

    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { showSaveDialog = false },
            onSave    = { name, emoji ->
                showSaveDialog = false
                viewModel.saveAsPreset(name, emoji)
            },
        )
    }
}

/** Anchor key → label for the first-visit coach marks; only laid-out targets are shown. */
private val LIBRARY_COACH_MARKS = listOf(
    "search"    to "Search sounds by name",
    "tabs"      to "Filter by category",
    "sound"     to "Tap to add to your mix, long-press to preview",
    "mix_panel" to "Your mix — balance the sounds and press play",
    "save"      to "Save your mix as a preset",
)

// Top bar

@Composable
private fun LibraryTopBar(
    filter: LibraryVMState.FilterState,
    activeMix: LibraryVMState.MixBuilderState,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
) {
    val hasMix = activeMix is LibraryVMState.MixBuilderState.Active
    val isDirty = (activeMix as? LibraryVMState.MixBuilderState.Active)?.isDirty == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SmallIconButton(onClick = onBack, description = "Back") { ChevronLeft(Charcoal) }

            // Inline search field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .coachMarkAnchor("search")
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceMuted)
                    .border(
                        1.dp,
                        if (filter.isSearchActive) Charcoal else Border,
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SearchDot(MutedGray)
                    BasicTextField(
                        value         = filter.searchQuery,
                        onValueChange = onSearch,
                        singleLine    = true,
                        textStyle     = MaterialTheme.typography.bodyMedium.copy(color = Charcoal),
                        cursorBrush   = SolidColor(Charcoal),
                        modifier      = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Box {
                                if (filter.searchQuery.isEmpty()) {
                                    Text(
                                        "Search sounds…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MutedGray,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                    if (filter.isSearchActive) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(MutedGray.copy(alpha = 0.2f))
                                .clickable(onClick = onClear)
                                .semantics { contentDescription = "Clear search" },
                            contentAlignment = Alignment.Center,
                        ) {
                            XIcon(MutedGray)
                        }
                    }
                }
            }

            // Save button — only visible when mix has sounds
            AnimatedVisibility(
                visible = hasMix,
                enter   = fadeIn(tween(200)),
                exit    = fadeOut(tween(150)),
            ) {
                SmallIconButton(
                    onClick     = onSave,
                    description = "Save as preset",
                    background  = if (isDirty) Charcoal else SurfaceMuted,
                    borderColor = if (isDirty) Charcoal else Border,
                    modifier    = Modifier.coachMarkAnchor("save"),
                ) {
                    SaveDot(tint = if (isDirty) White else MutedGray)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HRule()
    }
}

// Body states

@Composable
private fun LibraryLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Tab skeleton
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                Box(
                    Modifier
                        .width(60.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Border),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        repeat(6) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Border),
            )
        }
    }
}

@Composable
private fun LibraryErrorState(message: String) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(PaleRed)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text("Error", style = MaterialTheme.typography.labelSmall, color = PaleRedText)
        }
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MutedGray)
    }
}

// Ready body

@Composable
private fun LibraryReadyBody(
    catalogue: LibraryVMState.CatalogueState.Ready,
    activeMix: LibraryVMState.MixBuilderState,
    playback: LibraryVMState.LibraryPlaybackState,
    tier: Domain.EntitlementTier,
    onTabSelect: (Domain.SoundCategory?) -> Unit,
    onSoundTap: (Domain.Sound) -> Unit,
    onSoundLongPress: (Domain.Sound) -> Unit,
    onLayerVolume: (Int, Float) -> Unit,
    onLayerDragEnd: (Int) -> Unit,
    onMasterVolume: (Float) -> Unit,
    onMasterDragEnd: () -> Unit,
    onMute: (Int) -> Unit,
    onUnmute: (Int) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
) {
    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {

        // Category tabs
        CategoryTabs(
            tabs       = catalogue.tabs,
            selected   = catalogue.selectedTab,
            onSelect   = { onTabSelect(it.category) },
        )

        // Sound list + shelves
        LazyColumn(
            state           = listState,
            contentPadding  = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier        = Modifier.weight(1f),
        ) {
            // Shelves — recent and most played (hidden during search/filter)
            if (catalogue.showShelves) {
                if (catalogue.recentSounds.isNotEmpty()) {
                    item(key = "recent_header") { SectionLabel("Recently played") }
                    item(key = "recent_row") {
                        SoundShelf(
                            items     = catalogue.recentSounds,
                            onTap     = { onSoundTap(it.sound) },
                            onLongPress = { onSoundLongPress(it.sound) },
                        )
                    }
                }
                if (catalogue.mostPlayed.isNotEmpty() && catalogue.mostPlayed != catalogue.recentSounds) {
                    item(key = "most_header") { SectionLabel("Most played") }
                    item(key = "most_row") {
                        SoundShelf(
                            items       = catalogue.mostPlayed,
                            onTap       = { onSoundTap(it.sound) },
                            onLongPress = { onSoundLongPress(it.sound) },
                        )
                    }
                }
                item(key = "divider") { SectionLabel("All sounds") }
            }

            // No results state
            if (catalogue.displayedSounds.isEmpty()) {
                item(key = "empty") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No sounds match", style = MaterialTheme.typography.bodyMedium, color = MutedGray)
                    }
                }
            }

            itemsIndexed(
                items = catalogue.displayedSounds,
                key   = { _, item -> item.sound.id },
            ) { index, item ->
                SoundRow(
                    item        = item,
                    onTap       = { onSoundTap(item.sound) },
                    onLongPress = { onSoundLongPress(item.sound) },
                    modifier    = if (index == 0) Modifier.coachMarkAnchor("sound") else Modifier,
                )
            }

            item(key = "bottom") {
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }

        // Mix builder bottom sheet — always visible when mix has layers
        AnimatedVisibility(
            visible = activeMix is LibraryVMState.MixBuilderState.Active,
            enter   = slideInVertically { it } + fadeIn(tween(250)),
            exit    = slideOutVertically { it } + fadeOut(tween(200)),
        ) {
            val mix = activeMix as? LibraryVMState.MixBuilderState.Active ?: return@AnimatedVisibility
            MixBuilderPanel(
                mix           = mix,
                playback      = playback,
                onLayerVolume = onLayerVolume,
                onDragEnd     = onLayerDragEnd,
                onMasterVolume = onMasterVolume,
                onMasterDragEnd = onMasterDragEnd,
                onMute        = onMute,
                onUnmute      = onUnmute,
                onPlay        = onPlay,
                onPause       = onPause,
                onResume      = onResume,
            )
        }
    }
}

// Category tabs

@Composable
private fun CategoryTabs(
    tabs: List<LibraryVMState.CategoryTab>,
    selected: LibraryVMState.CategoryTab,
    onSelect: (LibraryVMState.CategoryTab) -> Unit,
) {
    LazyRow(
        modifier              = Modifier.coachMarkAnchor("tabs"),
        contentPadding        = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tabs, key = { it.label }) { tab ->
            val isSelected = tab == selected
            val bg by animateColorAsState(if (isSelected) Charcoal else SurfaceMuted, tween(200), "tab_${tab.label}")
            val fg by animateColorAsState(if (isSelected) White else MutedGray, tween(200), "tab_fg_${tab.label}")

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(bg)
                    .border(1.dp, if (isSelected) Charcoal else Border, RoundedCornerShape(6.dp))
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .semantics { contentDescription = "${tab.label}, ${tab.count} sounds" },
            ) {
                Text(
                    text  = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                )
            }
        }
    }
}

// Sound shelf (horizontal)

@Composable
private fun SoundShelf(
    items: List<LibraryVMState.SoundItemState>,
    onTap: (LibraryVMState.SoundItemState) -> Unit,
    onLongPress: (LibraryVMState.SoundItemState) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.sound.id }) { item ->
            val (pillBg, pillFg) = categoryColors(item.sound.category)
            val isIn = item.isInActiveMix

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isIn) Charcoal else pillBg)
                    .border(1.dp, if (isIn) Charcoal else Border, RoundedCornerShape(6.dp))
                    .clickable { onTap(item) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .alpha(if (item.isPremiumLocked) 0.5f else 1f)
                    .semantics {
                        contentDescription = buildString {
                            append(item.sound.title)
                            if (isIn) append(", in mix")
                            if (item.isPremiumLocked) append(", premium")
                        }
                    },
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (item.isPremiumLocked) {
                        LockDot(if (isIn) White.copy(alpha = 0.7f) else pillFg)
                    } else if (isIn) {
                        ActiveDot(White.copy(alpha = 0.8f))
                    }
                    Text(
                        text  = item.sound.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isIn) White else pillFg,
                    )
                }
            }
        }
    }
}

// Sound row (vertical list)

@Composable
private fun SoundRow(
    item: LibraryVMState.SoundItemState,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isIn      = item.isInActiveMix
    val locked    = item.isPremiumLocked
    val previewing = item.isPreviewPlaying

    val bgColor by animateColorAsState(
        targetValue   = when { isIn -> Charcoal; previewing -> PaleBlue; else -> White },
        animationSpec = tween(200),
        label         = "row_bg_${item.sound.id}",
    )
    val borderColor by animateColorAsState(
        targetValue   = when { isIn -> Charcoal; previewing -> PaleBlueText; else -> Border },
        animationSpec = tween(200),
        label         = "row_border_${item.sound.id}",
    )

    val (pillBg, pillFg) = categoryColors(item.sound.category)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onTap)
            .alpha(if (locked && !isIn) 0.6f else 1f)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics {
                contentDescription = buildString {
                    append(item.sound.title)
                    if (isIn) append(", in mix — tap to remove")
                    else append(", tap to add to mix")
                    if (locked) append(", premium")
                    if (previewing) append(", previewing")
                }
            },
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Leading indicator
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            when {
                previewing -> PreviewingWave(PaleBlueText)
                isIn       -> InMixIndicator(item.mixPosition)
                locked     -> LockBox()
                else       -> CategoryDot(pillBg, pillFg, item.sound.category)
            }
        }

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = item.sound.title,
                style    = MaterialTheme.typography.titleSmall,
                color    = when { isIn -> White; previewing -> PaleBlueText; else -> Charcoal },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = item.sound.category.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = when { isIn -> White.copy(alpha = 0.6f); previewing -> PaleBlueText.copy(alpha = 0.7f); else -> MutedGray },
            )
        }

        // Trailing — add / remove / lock / download
        when {
            locked -> Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(PaleYellow)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text("Premium", style = MaterialTheme.typography.labelSmall, color = PaleYellowText)
            }
            isIn -> Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                MinusIcon(White)
            }
            else -> Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(SurfaceMuted)
                    .border(1.dp, Border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                PlusIcon(MutedGray)
            }
        }
    }
}

// Mix builder panel

@Composable
private fun MixBuilderPanel(
    mix: LibraryVMState.MixBuilderState.Active,
    playback: LibraryVMState.LibraryPlaybackState,
    onLayerVolume: (Int, Float) -> Unit,
    onDragEnd: (Int) -> Unit,
    onMasterVolume: (Float) -> Unit,
    onMasterDragEnd: () -> Unit,
    onMute: (Int) -> Unit,
    onUnmute: (Int) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
) {
    val isPlaying = playback is LibraryVMState.LibraryPlaybackState.Playing
    val isPaused  = playback is LibraryVMState.LibraryPlaybackState.Paused

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .coachMarkAnchor("mix_panel")
            .background(Charcoal)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Panel header
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                "MIX",
                style = MaterialTheme.typography.labelSmall,
                color = White.copy(alpha = 0.5f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Play / Pause / Resume
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(White.copy(alpha = 0.1f))
                        .border(1.dp, White.copy(alpha = 0.2f), CircleShape)
                        .clickable(
                            onClick = when {
                                isPlaying -> onPause
                                isPaused  -> onResume
                                else      -> onPlay
                            }
                        )
                        .semantics {
                            contentDescription = when {
                                isPlaying -> "Pause mix"; isPaused -> "Resume mix"; else -> "Play mix"
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        isPlaying -> PauseIcon(White)
                        else      -> PlayIcon(White)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Layer rows
        mix.layers.sortedBy { it.position }.forEach { layer ->
            LayerSliderRow(
                layer     = layer,
                onVolume  = { onLayerVolume(layer.position, it) },
                onDragEnd = { onDragEnd(layer.position) },
                onMute    = { onMute(layer.position) },
                onUnmute  = { onUnmute(layer.position) },
            )
            Spacer(Modifier.height(8.dp))
        }

        // Master volume when 2+ layers
        if (mix.layers.size > 1) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Master",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = White.copy(alpha = 0.5f),
                    modifier = Modifier.width(48.dp),
                )
                LineSlider(
                    value                 = mix.masterVolume,
                    onValueChange         = onMasterVolume,
                    onValueChangeFinished = onMasterDragEnd,
                    modifier              = Modifier.weight(1f),
                    colors                = LineSliderColors.dark(),
                )
            }
        }

        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun LayerSliderRow(
    layer: LibraryVMState.MixLayerState,
    onVolume: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMute: () -> Unit,
    onUnmute: () -> Unit,
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Mute toggle
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (layer.isMuted) White.copy(alpha = 0.05f)
                    else White.copy(alpha = 0.1f)
                )
                .border(1.dp, White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .clickable(onClick = if (layer.isMuted) onUnmute else onMute)
                .semantics { contentDescription = if (layer.isMuted) "Unmute ${layer.sound.title}" else "Mute ${layer.sound.title}" },
            contentAlignment = Alignment.Center,
        ) {
            if (layer.isMuted) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(1.dp)).background(White.copy(alpha = 0.3f)))
            } else {
                SpeakerDots(White.copy(alpha = 0.7f))
            }
        }

        // Sound name
        Text(
            text     = layer.sound.title,
            style    = MaterialTheme.typography.labelMedium,
            color    = if (layer.isMuted) White.copy(alpha = 0.3f) else White.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(80.dp),
        )

        // Volume slider
        LineSlider(
            value                 = layer.volume,
            onValueChange         = onVolume,
            onValueChangeFinished = onDragEnd,
            modifier              = Modifier.weight(1f),
            colors                = LineSliderColors.dark(muted = layer.isMuted),
        )

        // Percentage label
        Text(
            text  = "${(layer.volume * 100).toInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = White.copy(alpha = 0.4f),
            modifier = Modifier.width(24.dp),
        )
    }
}

// Save dialog

@Composable
private fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
) {
    var name  by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = WarmWhite,
        title = {
            Text("Save preset", style = MaterialTheme.typography.headlineSmall, color = Charcoal)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceMuted)
                        .border(1.dp, if (name.isNotBlank()) Charcoal else Border, RoundedCornerShape(6.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    BasicTextField(
                        value         = name,
                        onValueChange = { name = it },
                        singleLine    = true,
                        textStyle     = MaterialTheme.typography.bodyMedium.copy(color = Charcoal),
                        cursorBrush   = SolidColor(Charcoal),
                        modifier      = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        decorationBox = { inner ->
                            Box {
                                if (name.isEmpty()) {
                                    Text("Name", style = MaterialTheme.typography.bodyMedium, color = MutedGray)
                                }
                                inner()
                            }
                        },
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceMuted)
                        .border(1.dp, Border, RoundedCornerShape(6.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    BasicTextField(
                        value         = emoji,
                        onValueChange = { if (it.length <= 2) emoji = it },
                        singleLine    = true,
                        textStyle     = MaterialTheme.typography.bodyMedium.copy(color = Charcoal),
                        cursorBrush   = SolidColor(Charcoal),
                        modifier      = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            Box {
                                if (emoji.isEmpty()) {
                                    Text("Emoji (optional)", style = MaterialTheme.typography.bodyMedium, color = MutedGray)
                                }
                                inner()
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name, emoji.ifBlank { null }) },
                enabled = name.isNotBlank(),
            ) {
                Text("Save", style = MaterialTheme.typography.labelLarge,
                    color = if (name.isNotBlank()) Charcoal else MutedGray)
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
// Micro-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(label: String) {
    Text(
        text     = label.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = MutedGray,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun HRule() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
}

@Composable
private fun SmallIconButton(
    onClick: () -> Unit,
    description: String,
    background: Color  = White,
    borderColor: Color = Border,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
        content          = { content() },
    )
}

@Composable
private fun InMixIndicator(position: Int?) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(White.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = "${(position ?: 0) + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = White,
        )
    }
}

@Composable
private fun CategoryDot(bg: Color, fg: Color, category: Domain.SoundCategory) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = category.name.first().uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
        )
    }
}

@Composable
private fun LockBox() {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(PaleYellow),
        contentAlignment = Alignment.Center,
    ) {
        LockDot(PaleYellowText)
    }
}

@Composable
private fun PreviewingWave(tint: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        listOf(8.dp, 14.dp, 10.dp).forEach { h ->
            Box(Modifier.width(2.dp).height(h).clip(RoundedCornerShape(1.dp)).background(tint))
        }
    }
}

@Composable
private fun ActiveDot(tint: Color) {
    Box(Modifier.size(6.dp).clip(CircleShape).background(tint))
}

@Composable
private fun SearchDot(tint: Color) {
    Box(Modifier.size(14.dp).drawBehind {
        val r = 4.5.dp.toPx()
        drawCircle(tint, r, Offset(r, r), style = Stroke(1.5.dp.toPx()))
        val d = r * 0.707f
        drawLine(tint, Offset(r + d, r + d), Offset(size.width, size.height), 1.5.dp.toPx())
    })
}

@Composable
private fun XIcon(tint: Color) {
    Box(Modifier.size(8.dp).drawBehind {
        val s = 1.5.dp.toPx()
        drawLine(tint, Offset(0f, 0f), Offset(size.width, size.height), s)
        drawLine(tint, Offset(size.width, 0f), Offset(0f, size.height), s)
    })
}

@Composable
private fun SaveDot(tint: Color) {
    Box(Modifier.size(14.dp).drawBehind {
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(1.dp.toPx(), 0f); lineTo(size.width - 1.dp.toPx(), 0f)
            lineTo(size.width, 1.dp.toPx()); lineTo(size.width, size.height)
            lineTo(0f, size.height); lineTo(0f, 1.dp.toPx()); close()
        }
        drawPath(p, tint)
    })
}

@Composable
private fun LockDot(tint: Color) {
    Box(Modifier.size(10.dp).drawBehind {
        val s = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)
        val r = size.minDimension / 3.5f
        drawArc(tint, 0f, -180f, false,
            Offset(size.width / 2f - r, 0f), Size(r * 2, r * 2), style = s)
        drawRect(tint, Offset(0f, size.height / 2f),
            Size(size.width, size.height / 2f), style = s)
    })
}

@Composable
private fun PlusIcon(tint: Color) {
    Box(Modifier.size(10.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(1.5.dp).background(tint))
        Box(Modifier.width(1.5.dp).height(10.dp).background(tint))
    }
}

@Composable
private fun MinusIcon(tint: Color) {
    Box(Modifier.width(10.dp).height(1.5.dp).background(tint))
}

@Composable
private fun PlayIcon(tint: Color) {
    Box(Modifier.size(10.dp).background(
        tint,
        shape = object : androidx.compose.ui.graphics.Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                density: androidx.compose.ui.unit.Density,
            ) = androidx.compose.ui.graphics.Outline.Generic(
                androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, 0f); lineTo(size.width, size.height / 2f); lineTo(0f, size.height); close()
                }
            )
        }
    ))
}

@Composable
private fun PauseIcon(tint: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.width(3.dp).height(10.dp).background(tint))
        Box(Modifier.width(3.dp).height(10.dp).background(tint))
    }
}

@Composable
private fun SpeakerDots(tint: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        Box(Modifier.width(3.dp).height(8.dp).background(tint))
        Box(Modifier.width(3.dp).height(6.dp).background(tint))
    }
}

private fun categoryColors(category: Domain.SoundCategory) = when (category) {
    Domain.SoundCategory.TINNITUS -> PaleBlue to PaleBlueText
    Domain.SoundCategory.NATURE   -> PaleGreen to PaleGreenText
    else                   -> SurfaceMuted to MutedGray
}

