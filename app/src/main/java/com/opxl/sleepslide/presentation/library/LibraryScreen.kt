package com.opxl.sleepslide.presentation.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
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
import com.opxl.sleepslide.presentation.components.GhostIconButton
import com.opxl.sleepslide.presentation.components.Glyph
import com.opxl.sleepslide.presentation.components.GlyphIcon
import com.opxl.sleepslide.presentation.components.IconCircle
import com.opxl.sleepslide.presentation.components.LineSlider
import com.opxl.sleepslide.presentation.components.LineSliderColors
import com.opxl.sleepslide.presentation.components.PremiumPill
import com.opxl.sleepslide.presentation.components.ScreenTitle
import com.opxl.sleepslide.presentation.components.SearchField
import com.opxl.sleepslide.presentation.components.SectionLabel
import com.opxl.sleepslide.presentation.components.SelectChip
import com.opxl.sleepslide.presentation.components.SleepTextInput
import com.opxl.sleepslide.presentation.components.Tag
import com.opxl.sleepslide.presentation.components.categoryColors
import com.opxl.sleepslide.presentation.components.displayTitle
import com.opxl.sleepslide.presentation.components.glyph
import com.opxl.sleepslide.presentation.components.groupPosition
import com.opxl.sleepslide.presentation.components.groupedRow
import com.opxl.sleepslide.presentation.components.labelRes
import com.opxl.sleepslide.presentation.tutorial.CoachMarkHost
import com.opxl.sleepslide.presentation.tutorial.TutorialViewModel
import com.opxl.sleepslide.presentation.tutorial.coachMarkAnchor
import com.opxl.sleepslide.presentation.tutorial.tutorialViewModel
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.Dimens
import com.opxl.sleepslide.ui.theme.MutedGray
import com.opxl.sleepslide.ui.theme.OnPanel
import com.opxl.sleepslide.ui.theme.PaleBlue
import com.opxl.sleepslide.ui.theme.PaleBlueText
import com.opxl.sleepslide.ui.theme.PaleRed
import com.opxl.sleepslide.ui.theme.PaleRedText
import com.opxl.sleepslide.ui.theme.Panel
import com.opxl.sleepslide.ui.theme.SurfaceMuted
import com.opxl.sleepslide.ui.theme.WarmWhite
import com.opxl.sleepslide.ui.theme.White
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onUpgrade: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    tutorial: TutorialViewModel = tutorialViewModel(CoachMarkScreens.LIBRARY),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    val currentOnUpgrade by rememberUpdatedState(onUpgrade)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryVMState.LibraryEvent.NavigateToPlayer ->
                    onNavigateToPlayer()
                is LibraryVMState.LibraryEvent.ShowSavePresetDialog ->
                    showSaveDialog = true
                is LibraryVMState.LibraryEvent.ShowUpgradePrompt ->
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message     = context.getString(R.string.library_snack_premium_sound, event.soundTitle.asString(context)),
                            actionLabel = context.getString(R.string.action_upgrade),
                            duration    = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) currentOnUpgrade()
                    }
                is LibraryVMState.LibraryEvent.ShowMaxLayersReached ->
                    scope.launch { snackbarHostState.showSnackbar(context.resources.getQuantityString(R.plurals.library_snack_max_layers, 3, 3)) }
                is LibraryVMState.LibraryEvent.ShowHeadphonesHint ->
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.headphones_note)) }
                is LibraryVMState.LibraryEvent.ShowServiceUnavailable ->
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.library_snack_service_starting)) }
                is LibraryVMState.LibraryEvent.ShowError ->
                    scope.launch { snackbarHostState.showSnackbar(event.message.asString(context)) }
                is LibraryVMState.LibraryEvent.ShowInfo ->
                    scope.launch { snackbarHostState.showSnackbar(event.message.asString(context)) }
                is LibraryVMState.LibraryEvent.PresetSaved ->
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.library_snack_preset_saved, event.name)) }
                is LibraryVMState.LibraryEvent.SoundAlreadyInMix ->
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.library_snack_already_in_mix, event.soundTitle.asString(context))) }
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
                        shape          = RoundedCornerShape(Dimens.controlRadius),
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
                    filter    = uiState.filter,
                    activeMix = uiState.activeMix,
                    onBack    = onNavigateBack,
                    onSearch  = viewModel::onSearchQueryChanged,
                    onClear   = { viewModel.clearSearch() },
                    onSave    = { viewModel.requestSaveAsPreset() },
                )

                AnimatedContent(
                    targetState    = uiState.catalogue,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                    label          = "library_body",
                ) { catalogue ->
                    when (catalogue) {
                        is LibraryVMState.CatalogueState.Loading -> LibraryLoadingSkeleton()
                        is LibraryVMState.CatalogueState.Error   -> LibraryErrorState(catalogue.message.asString())
                        is LibraryVMState.CatalogueState.Ready   -> LibraryReadyBody(
                            catalogue        = catalogue,
                            activeMix        = uiState.activeMix,
                            playback         = uiState.playback,
                            onTabSelect      = viewModel::selectCategory,
                            onSoundTap       = viewModel::onSoundTapped,
                            onSoundLongPress = viewModel::onSoundLongPressed,
                            onLayerVolume    = viewModel::onLayerVolumeChanged,
                            onLayerDragEnd   = viewModel::onLayerVolumeDragEnded,
                            onMasterVolume   = viewModel::onMasterVolumeChanged,
                            onMasterDragEnd  = { viewModel.onMasterVolumeDragEnded() },
                            onMute           = viewModel::muteLayer,
                            onUnmute         = viewModel::unmuteLayer,
                            onPlay           = { viewModel.playCurrentMix() },
                            onPause          = { viewModel.pausePlayback() },
                            onResume         = { viewModel.resumePlayback() },
                        )
                    }
                }
            }
        }
    }

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
    "search"    to R.string.coach_library_search,
    "tabs"      to R.string.coach_library_tabs,
    "sound"     to R.string.coach_library_sound,
    "mix_panel" to R.string.coach_library_mix_panel,
    "save"      to R.string.coach_library_save,
)

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LibraryTopBar(
    filter: LibraryVMState.FilterState,
    activeMix: LibraryVMState.MixBuilderState,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
) {
    val hasMix  = activeMix is LibraryVMState.MixBuilderState.Active
    val isDirty = (activeMix as? LibraryVMState.MixBuilderState.Active)?.isDirty == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.gutter)
            .padding(top = 12.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CircleIconButton(onClick = onBack, contentDescription = stringResource(R.string.cd_back)) {
                GlyphIcon(Glyph.ChevronLeft, Charcoal, size = 16.dp)
            }
            // Save button — only visible when the mix has sounds
            AnimatedVisibility(
                visible = hasMix,
                enter   = fadeIn(tween(200)),
                exit    = fadeOut(tween(150)),
            ) {
                CircleIconButton(
                    onClick            = onSave,
                    filled             = isDirty,
                    contentDescription = stringResource(R.string.player_cd_save_as_preset),
                    modifier           = Modifier.coachMarkAnchor("save"),
                ) { GlyphIcon(Glyph.Save, if (isDirty) White else Charcoal, size = 18.dp) }
            }
        }

        Spacer(Modifier.height(18.dp))
        ScreenTitle(title = stringResource(R.string.tab_library))
        Spacer(Modifier.height(14.dp))

        SearchField(
            value            = filter.searchQuery,
            onValueChange    = onSearch,
            hint             = stringResource(R.string.library_search_hint),
            onClear          = onClear,
            clearDescription = stringResource(R.string.library_cd_clear_search),
            active           = filter.isSearchActive,
            modifier         = Modifier.coachMarkAnchor("search"),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Body states
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LibraryLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.gutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                Box(Modifier.width(72.dp).height(36.dp).clip(CircleShape).background(SurfaceMuted))
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(Dimens.cardRadius))
                .background(SurfaceMuted),
        )
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
        Tag(stringResource(R.string.label_error), PaleRed, PaleRedText)
        Spacer(Modifier.height(10.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MutedGray, textAlign = TextAlign.Center)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ready body
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LibraryReadyBody(
    catalogue: LibraryVMState.CatalogueState.Ready,
    activeMix: LibraryVMState.MixBuilderState,
    playback: LibraryVMState.LibraryPlaybackState,
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
        CategoryTabs(
            tabs     = catalogue.tabs,
            selected = catalogue.selectedTab,
            onSelect = { onTabSelect(it.category) },
        )

        LazyColumn(
            state          = listState,
            contentPadding = PaddingValues(horizontal = Dimens.gutter, vertical = 4.dp),
            modifier       = Modifier.weight(1f),
        ) {
            // Shelves — recent and most played (hidden during search/filter)
            if (catalogue.showShelves) {
                if (catalogue.recentSounds.isNotEmpty()) {
                    item(key = "recent_header") {
                        SectionLabel(stringResource(R.string.library_recently_played), topPadding = 12.dp)
                    }
                    item(key = "recent_row") {
                        SoundShelf(
                            items       = catalogue.recentSounds,
                            onTap       = { onSoundTap(it.sound) },
                            onLongPress = { onSoundLongPress(it.sound) },
                        )
                    }
                }
                if (catalogue.mostPlayed.isNotEmpty() && catalogue.mostPlayed != catalogue.recentSounds) {
                    item(key = "most_header") {
                        SectionLabel(stringResource(R.string.library_most_played), topPadding = 20.dp)
                    }
                    item(key = "most_row") {
                        SoundShelf(
                            items       = catalogue.mostPlayed,
                            onTap       = { onSoundTap(it.sound) },
                            onLongPress = { onSoundLongPress(it.sound) },
                        )
                    }
                }
                item(key = "divider") {
                    SectionLabel(
                        text       = stringResource(R.string.library_all_sounds),
                        topPadding = 24.dp,
                        trailing   = {
                            Text(
                                text  = "${catalogue.displayedSounds.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedGray,
                            )
                        },
                    )
                }
            } else {
                item(key = "top_gap") { Spacer(Modifier.height(8.dp)) }
            }

            if (catalogue.displayedSounds.isEmpty()) {
                item(key = "empty") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.library_no_match), style = MaterialTheme.typography.bodyMedium, color = MutedGray)
                    }
                }
            }

            val count = catalogue.displayedSounds.size
            itemsIndexed(
                items = catalogue.displayedSounds,
                key   = { _, item -> item.sound.id },
            ) { index, item ->
                SoundRow(
                    item        = item,
                    position    = groupPosition(index, count),
                    onTap       = { onSoundTap(item.sound) },
                    onLongPress = { onSoundLongPress(item.sound) },
                    modifier    = if (index == 0) Modifier.coachMarkAnchor("sound") else Modifier,
                )
            }

            item(key = "bottom") {
                Spacer(Modifier.height(16.dp))
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }

        // Mix builder — rises from the bottom once the mix has layers
        AnimatedVisibility(
            visible = activeMix is LibraryVMState.MixBuilderState.Active,
            enter   = slideInVertically { it } + fadeIn(tween(250)),
            exit    = slideOutVertically { it } + fadeOut(tween(200)),
        ) {
            val mix = activeMix as? LibraryVMState.MixBuilderState.Active ?: return@AnimatedVisibility
            MixBuilderPanel(
                mix             = mix,
                playback        = playback,
                onLayerVolume   = onLayerVolume,
                onDragEnd       = onLayerDragEnd,
                onMasterVolume  = onMasterVolume,
                onMasterDragEnd = onMasterDragEnd,
                onMute          = onMute,
                onUnmute        = onUnmute,
                onPlay          = onPlay,
                onPause         = onPause,
                onResume        = onResume,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category tabs · shelves
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryTabs(
    tabs: List<LibraryVMState.CategoryTab>,
    selected: LibraryVMState.CategoryTab,
    onSelect: (LibraryVMState.CategoryTab) -> Unit,
) {
    LazyRow(
        modifier              = Modifier.coachMarkAnchor("tabs"),
        contentPadding        = PaddingValues(horizontal = Dimens.gutter, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tabs, key = { it.label }) { tab ->
            val tabLabel = stringResource(tab.label)
            val tabDescription = pluralStringResource(R.plurals.library_cd_tab, tab.count, tabLabel, tab.count)
            SelectChip(
                label    = tabLabel,
                selected = tab == selected,
                onClick  = { onSelect(tab) },
                modifier = Modifier.semantics { contentDescription = tabDescription },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SoundShelf(
    items: List<LibraryVMState.SoundItemState>,
    onTap: (LibraryVMState.SoundItemState) -> Unit,
    onLongPress: (LibraryVMState.SoundItemState) -> Unit,
) {
    // Bleed past the list's gutter so chips scroll to the screen edge, first chip still on the gutter.
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding        = PaddingValues(horizontal = Dimens.gutter),
        modifier              = Modifier.layout { measurable, constraints ->
            val bleed = Dimens.gutter.roundToPx()
            val placeable = measurable.measure(
                constraints.copy(maxWidth = constraints.maxWidth + bleed * 2, minWidth = constraints.minWidth + bleed * 2)
            )
            layout(constraints.maxWidth, placeable.height) { placeable.place(-bleed, 0) }
        },
    ) {
        items(items, key = { it.sound.id }) { item ->
            val (_, categoryFg) = categoryColors(item.sound.category)
            val isIn  = item.isInActiveMix
            val title = item.sound.displayTitle()
            val chipDescription = listOfNotNull(
                title,
                stringResource(R.string.library_cd_in_mix).takeIf { isIn },
                stringResource(R.string.library_cd_premium).takeIf { item.isPremiumLocked },
            ).joinToString(", ")

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isIn) Charcoal else White)
                    .border(Dimens.hairline, if (isIn) Charcoal else Border, CircleShape)
                    .combinedClickable(onClick = { onTap(item) }, onLongClick = { onLongPress(item) }, role = Role.Button)
                    .alpha(if (item.isPremiumLocked) 0.55f else 1f)
                    .padding(start = 12.dp, end = 16.dp, top = 9.dp, bottom = 9.dp)
                    .semantics { contentDescription = chipDescription },
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    item.isPremiumLocked -> GlyphIcon(Glyph.Lock, if (isIn) White else MutedGray, size = 14.dp)
                    isIn                 -> GlyphIcon(Glyph.Check, White, size = 14.dp)
                    else                 -> GlyphIcon(item.sound.category.glyph, categoryFg, size = 16.dp)
                }
                Text(
                    text  = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isIn) White else Charcoal,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sound row (one row of the grouped list)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SoundRow(
    item: LibraryVMState.SoundItemState,
    position: com.opxl.sleepslide.presentation.components.GroupPosition,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isIn       = item.isInActiveMix
    val locked     = item.isPremiumLocked
    val previewing = item.isPreviewPlaying
    val showsHeadphonesTag = item.sound.needsHeadphones
    val title = item.sound.displayTitle()
    val rowDescription = listOfNotNull(
        title,
        stringResource(if (isIn) R.string.library_cd_in_mix_tap_remove else R.string.library_cd_tap_add),
        stringResource(R.string.library_cd_premium).takeIf { locked },
        stringResource(R.string.library_cd_previewing).takeIf { previewing },
        stringResource(R.string.library_cd_headphones).takeIf { showsHeadphonesTag },
    ).joinToString(", ")

    val (circleBg, circleFg) = categoryColors(item.sound.category)
    val categoryLabel = stringResource(item.sound.category.labelRes)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .groupedRow(position)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress, role = Role.Button)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { contentDescription = rowDescription },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading — what this row is right now
        when {
            previewing -> IconCircle(PaleBlue) { GlyphIcon(Glyph.WaveBars, PaleBlueText, size = 18.dp) }
            isIn       -> IconCircle(Charcoal) {
                Text("${(item.mixPosition ?: 0) + 1}", style = MaterialTheme.typography.labelLarge, color = White)
            }
            locked     -> IconCircle(SurfaceMuted) { GlyphIcon(Glyph.Lock, MutedGray, size = 18.dp) }
            else       -> IconCircle(circleBg, glow = circleFg) { GlyphIcon(item.sound.category.glyph, circleFg, size = 18.dp) }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = title,
                style    = MaterialTheme.typography.titleSmall,
                color    = if (locked && !isIn) MutedGray else Charcoal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (showsHeadphonesTag) GlyphIcon(Glyph.Headphones, MutedGray, size = 12.dp)
                Text(
                    text  = if (showsHeadphonesTag) stringResource(R.string.library_category_headphones, categoryLabel) else categoryLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Trailing — add / in mix / premium
        when {
            locked && !isIn -> PremiumPill()
            isIn -> IconCircle(Charcoal, size = 30.dp) { GlyphIcon(Glyph.Check, White, size = 14.dp) }
            else -> IconCircle(SurfaceMuted, size = 30.dp, border = Border) { GlyphIcon(Glyph.Plus, Charcoal, size = 14.dp) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mix builder panel
// ─────────────────────────────────────────────────────────────────────────────

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
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    val expandLabel = stringResource(if (isExpanded) R.string.library_cd_collapse_mix else R.string.library_cd_expand_mix)
    val transportLabel = stringResource(
        when { isPlaying -> R.string.library_cd_pause_mix; isPaused -> R.string.library_cd_resume_mix; else -> R.string.library_cd_play_mix }
    )
    val chevronRotation by animateFloatAsState(if (isExpanded) 0f else 180f, label = "mix_chevron")
    val shape = RoundedCornerShape(topStart = Dimens.panelRadius, topEnd = Dimens.panelRadius)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .coachMarkAnchor("mix_panel")
            .clip(shape)
            .background(Panel)
            .border(Dimens.hairline, OnPanel.copy(alpha = 0.08f), shape)
            .padding(horizontal = Dimens.gutter)
            .padding(top = 14.dp, bottom = 14.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text  = stringResource(R.string.library_mix_header).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnPanel.copy(alpha = 0.55f),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = pluralStringResource(R.plurals.library_mix_header_count, mix.layers.size, mix.layers.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = OnPanel,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                GhostIconButton(
                    onClick            = { isExpanded = !isExpanded },
                    contentDescription = expandLabel,
                    size               = 40.dp,
                    background         = OnPanel.copy(alpha = 0.08f),
                ) { GlyphIcon(Glyph.ChevronDown, OnPanel, Modifier.rotate(chevronRotation), size = 16.dp) }

                GhostIconButton(
                    onClick            = when { isPlaying -> onPause; isPaused -> onResume; else -> onPlay },
                    contentDescription = transportLabel,
                    size               = 44.dp,
                    background         = OnPanel,
                ) { GlyphIcon(if (isPlaying) Glyph.Pause else Glyph.Play, Panel, size = 20.dp) }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter   = expandVertically() + fadeIn(tween(200)),
            exit    = shrinkVertically() + fadeOut(tween(150)),
        ) {
            Column {
                Spacer(Modifier.height(14.dp))
                mix.layers.sortedBy { it.position }.forEach { layer ->
                    LayerSliderRow(
                        layer     = layer,
                        onVolume  = { onLayerVolume(layer.position, it) },
                        onDragEnd = { onDragEnd(layer.position) },
                        onMute    = { onMute(layer.position) },
                        onUnmute  = { onUnmute(layer.position) },
                    )
                    Spacer(Modifier.height(6.dp))
                }

                if (mix.layers.size > 1) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(Dimens.hairline)
                            .background(OnPanel.copy(alpha = 0.1f)),
                    )
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                            GlyphIcon(Glyph.Sliders, OnPanel.copy(alpha = 0.7f), size = 16.dp)
                        }
                        Text(
                            text     = stringResource(R.string.player_master),
                            style    = MaterialTheme.typography.labelLarge,
                            color    = OnPanel.copy(alpha = 0.85f),
                            modifier = Modifier.width(96.dp),
                        )
                        LineSlider(
                            value                 = mix.masterVolume,
                            onValueChange         = onMasterVolume,
                            onValueChangeFinished = onMasterDragEnd,
                            modifier              = Modifier.weight(1f),
                            colors                = LineSliderColors.dark(),
                        )
                        Text(
                            text      = "${(mix.masterVolume * 100).toInt()}",
                            style     = MaterialTheme.typography.labelSmall,
                            color     = OnPanel.copy(alpha = 0.45f),
                            textAlign = TextAlign.End,
                            modifier  = Modifier.width(28.dp),
                        )
                    }
                }
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
    val title = layer.sound.displayTitle()
    val muteLabel = stringResource(if (layer.isMuted) R.string.library_cd_unmute_sound else R.string.library_cd_mute_sound, title)
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GhostIconButton(
            onClick            = if (layer.isMuted) onUnmute else onMute,
            contentDescription = muteLabel,
            size               = 32.dp,
            background         = OnPanel.copy(alpha = if (layer.isMuted) 0.04f else 0.08f),
        ) {
            GlyphIcon(
                if (layer.isMuted) Glyph.VolumeOff else Glyph.Volume,
                OnPanel.copy(alpha = if (layer.isMuted) 0.4f else 0.85f),
                size = 16.dp,
            )
        }
        Text(
            text     = title,
            style    = MaterialTheme.typography.labelLarge,
            color    = if (layer.isMuted) OnPanel.copy(alpha = 0.35f) else OnPanel.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp),
        )
        LineSlider(
            value                 = layer.volume,
            onValueChange         = onVolume,
            onValueChangeFinished = onDragEnd,
            modifier              = Modifier.weight(1f),
            colors                = LineSliderColors.dark(muted = layer.isMuted),
        )
        Text(
            text      = "${(layer.volume * 100).toInt()}",
            style     = MaterialTheme.typography.labelSmall,
            color     = OnPanel.copy(alpha = 0.45f),
            textAlign = TextAlign.End,
            modifier  = Modifier.width(28.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Save dialog
// ─────────────────────────────────────────────────────────────────────────────

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
        containerColor   = White,
        shape            = RoundedCornerShape(Dimens.panelRadius),
        title = {
            Text(stringResource(R.string.player_save_preset_title), style = MaterialTheme.typography.headlineSmall, color = Charcoal)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SleepTextInput(
                    value          = name,
                    onValueChange  = { name = it },
                    hint           = stringResource(R.string.preset_field_name),
                    focusRequester = focusRequester,
                )
                SleepTextInput(
                    value         = emoji,
                    onValueChange = { if (it.length <= 2) emoji = it },
                    hint          = stringResource(R.string.preset_field_emoji),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name, emoji.ifBlank { null }) },
                enabled = name.isNotBlank(),
            ) {
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
