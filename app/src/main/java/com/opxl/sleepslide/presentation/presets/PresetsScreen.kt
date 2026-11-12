package com.opxl.sleepslide.presentation.presets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
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
import com.opxl.sleepslide.domain.repository.CoachMarkScreens
import com.opxl.sleepslide.presentation.components.CircleIconButton
import com.opxl.sleepslide.presentation.components.GhostIconButton
import com.opxl.sleepslide.presentation.components.Glyph
import com.opxl.sleepslide.presentation.components.GlyphIcon
import com.opxl.sleepslide.presentation.components.GroupPosition
import com.opxl.sleepslide.presentation.components.IconCircle
import com.opxl.sleepslide.presentation.components.PremiumPill
import com.opxl.sleepslide.presentation.components.PrimaryPill
import com.opxl.sleepslide.presentation.components.ScreenTitle
import com.opxl.sleepslide.presentation.components.SearchField
import com.opxl.sleepslide.presentation.components.SectionLabel
import com.opxl.sleepslide.presentation.components.SleepTextInput
import com.opxl.sleepslide.presentation.components.SurfaceCard
import com.opxl.sleepslide.presentation.components.Tag
import com.opxl.sleepslide.presentation.components.displayTitle
import com.opxl.sleepslide.presentation.components.groupPosition
import com.opxl.sleepslide.presentation.components.groupedRow
import com.opxl.sleepslide.presentation.components.listenedDuration
import com.opxl.sleepslide.presentation.tutorial.CoachMarkHost
import com.opxl.sleepslide.presentation.tutorial.TutorialViewModel
import com.opxl.sleepslide.presentation.tutorial.coachMarkAnchor
import com.opxl.sleepslide.presentation.tutorial.tutorialViewModel
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.Dimens
import com.opxl.sleepslide.ui.theme.MutedGray
import com.opxl.sleepslide.ui.theme.PaleBlue
import com.opxl.sleepslide.ui.theme.PaleBlueText
import com.opxl.sleepslide.ui.theme.PaleRed
import com.opxl.sleepslide.ui.theme.PaleRedText
import com.opxl.sleepslide.ui.theme.PaleYellow
import com.opxl.sleepslide.ui.theme.PaleYellowText
import com.opxl.sleepslide.ui.theme.SurfaceMuted
import com.opxl.sleepslide.ui.theme.WarmWhite
import com.opxl.sleepslide.ui.theme.White
import kotlinx.coroutines.launch

// Screen entry point

@Composable
fun PresetsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (presetId: Long) -> Unit,
    onNavigateToLibrary: () -> Unit,
    viewModel: PresetsViewModel = hiltViewModel(),
    tutorial: TutorialViewModel = tutorialViewModel(CoachMarkScreens.PRESETS),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var pendingDeleteId: Long? by rememberSaveable { mutableStateOf(null) }
    var pendingDeleteName: String by rememberSaveable { mutableStateOf("") }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showBulkDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var bulkDeleteCount by rememberSaveable { mutableStateOf(0) }
    var renameTarget: Domain.Preset? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PresetsEvent.NavigateToPlayer  -> onNavigateToPlayer(event.presetId)
                is PresetsEvent.NavigateToLibrary -> onNavigateToLibrary()

                is PresetsEvent.ShowRenameDialog  -> renameTarget = event.preset

                is PresetsEvent.ShowDeleteConfirmation -> {
                    pendingDeleteId   = event.presetId
                    pendingDeleteName = event.presetName
                    showDeleteDialog  = true
                }

                is PresetsEvent.ShowBulkDeleteConfirmation -> {
                    bulkDeleteCount      = event.count
                    showBulkDeleteDialog = true
                }

                is PresetsEvent.PresetDeleted -> {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message     = context.getString(
                                R.string.presets_snack_deleted,
                                event.presetName ?: context.getString(R.string.presets_fallback_name),
                            ),
                            actionLabel = context.getString(R.string.action_undo),
                            duration    = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDelete()
                        }
                    }
                }

                is PresetsEvent.UndoDelete ->
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.presets_snack_restored, event.preset.name)) }

                is PresetsEvent.ShowPresetLimitReached ->
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.presets_snack_limit_reached, event.limit))
                    }

                is PresetsEvent.ShowServiceUnavailable ->
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.library_snack_service_starting)) }

                is PresetsEvent.ShowError ->
                    scope.launch { snackbarHostState.showSnackbar(event.message.asString(context)) }

                is PresetsEvent.ShowInfo ->
                    scope.launch { snackbarHostState.showSnackbar(event.message.asString(context)) }
            }
        }
    }

    CoachMarkHost(viewModel = tutorial, labels = PRESETS_COACH_MARKS) {
        Scaffold(
            snackbarHost   = {
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
                PresetsTopBar(
                    uiState       = uiState,
                    onBack        = onNavigateBack,
                    onAddPreset   = { viewModel.navigateToLibrary() },
                    onSelectAll   = { viewModel.selectAll() },
                    onExitSelect  = { viewModel.exitSelectionMode() },
                    onBulkDelete  = { viewModel.requestBulkDelete() },
                    onDoneReorder = { viewModel.disableReorderMode() },
                )

                AnimatedVisibility(
                    visible = uiState.screen is PresetsScreenState.Ready,
                    enter   = fadeIn(tween(200)),
                    exit    = fadeOut(tween(150)),
                ) {
                    SearchField(
                        value            = uiState.searchQuery,
                        onValueChange    = viewModel::onSearchQueryChanged,
                        hint             = stringResource(R.string.presets_search_hint),
                        onClear          = { viewModel.clearSearch() },
                        clearDescription = stringResource(R.string.library_cd_clear_search),
                        enabled          = uiState.selectionMode is SelectionMode.None && !uiState.reorderMode,
                        modifier         = Modifier
                            .padding(horizontal = Dimens.gutter)
                            .padding(top = 14.dp, bottom = 4.dp)
                            .coachMarkAnchor("search"),
                    )
                }

                AnimatedContent(
                    targetState    = uiState.screen,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                    label          = "presets_body",
                ) { screen ->
                    when (screen) {
                        is PresetsScreenState.Loading -> PresetsLoadingSkeleton()
                        is PresetsScreenState.Empty   -> PresetsEmptyState(
                            onBrowse = { viewModel.navigateToLibrary() }
                        )
                        is PresetsScreenState.Error   -> PresetsErrorState(screen.message.asString())
                        is PresetsScreenState.Ready   -> PresetsReadyBody(
                            screen       = screen,
                            uiState      = uiState,
                            onLaunch     = { viewModel.launchPreset(it) },
                            onLongPress  = { viewModel.enterSelectionMode(it.id) },
                            onToggle     = { viewModel.toggleSelection(it.id) },
                            onPin        = { viewModel.pin(it.id) },
                            onUnpin      = { viewModel.unpin(it.id) },
                            onRename     = { viewModel.requestRename(it) },
                            onDelete     = { viewModel.requestDelete(it) },
                            loadStats    = { ids -> viewModel.loadPlayStats(ids) },
                        )
                    }
                }
            }
        }
    }

    // Dialogs

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            presetName = pendingDeleteName,
            onConfirm  = {
                showDeleteDialog = false
                pendingDeleteId?.let { viewModel.confirmDelete(it) }
                pendingDeleteId = null
            },
            onDismiss  = {
                showDeleteDialog = false
                pendingDeleteId  = null
            },
        )
    }

    if (showBulkDeleteDialog) {
        BulkDeleteConfirmDialog(
            count     = bulkDeleteCount,
            onConfirm = {
                showBulkDeleteDialog = false
                viewModel.confirmBulkDelete()
            },
            onDismiss = { showBulkDeleteDialog = false },
        )
    }

    renameTarget?.let { preset ->
        RenameDialog(
            preset    = preset,
            onConfirm = { newName ->
                viewModel.rename(preset.id, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }
}

/** Anchor key → label for the first-visit coach marks; only laid-out targets are shown. */
private val PRESETS_COACH_MARKS = listOf(
    "add"    to R.string.coach_presets_add,
    "search" to R.string.coach_presets_search,
    "preset" to R.string.coach_presets_preset,
    "browse" to R.string.coach_presets_browse,
)

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PresetsTopBar(
    uiState: PresetsUiState,
    onBack: () -> Unit,
    onAddPreset: () -> Unit,
    onSelectAll: () -> Unit,
    onExitSelect: () -> Unit,
    onBulkDelete: () -> Unit,
    onDoneReorder: () -> Unit,
) {
    val isSelecting = uiState.selectionMode is SelectionMode.Active
    val selectedCount = (uiState.selectionMode as? SelectionMode.Active)?.selectedIds?.size ?: 0

    AnimatedContent(
        targetState    = Triple(isSelecting, uiState.reorderMode, selectedCount),
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
        label          = "topbar_mode",
    ) { (selecting, reordering, count) ->
        when {
            selecting  -> SelectionTopBar(
                count       = count,
                total       = uiState.presetCount,
                onClose     = onExitSelect,
                onSelectAll = onSelectAll,
                onDelete    = onBulkDelete,
            )
            reordering -> ReorderTopBar(onDone = onDoneReorder)
            else       -> DefaultTopBar(
                presetCount     = uiState.presetCount,
                hasReachedLimit = uiState.hasReachedLimit,
                onBack          = onBack,
                onAdd           = onAddPreset,
            )
        }
    }
}

@Composable
private fun DefaultTopBar(
    presetCount: Int,
    hasReachedLimit: Boolean,
    onBack: () -> Unit,
    onAdd: () -> Unit,
) {
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
            CircleIconButton(
                onClick            = onAdd,
                filled             = !hasReachedLimit,
                contentDescription = stringResource(R.string.presets_cd_add),
                modifier           = Modifier.coachMarkAnchor("add"),
            ) { GlyphIcon(Glyph.Plus, if (hasReachedLimit) MutedGray else White, size = 18.dp) }
        }
        Spacer(Modifier.height(18.dp))
        ScreenTitle(
            title    = stringResource(R.string.tab_presets),
            subtitle = if (presetCount > 0) stringResource(R.string.presets_count_saved, presetCount) else null,
        )
    }
}

@Composable
private fun SelectionTopBar(
    count: Int,
    total: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
) {
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
            CircleIconButton(onClick = onClose, contentDescription = stringResource(R.string.presets_cd_exit_selection)) {
                GlyphIcon(Glyph.Close, Charcoal, size = 16.dp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (count < total) {
                    TextButton(onClick = onSelectAll) {
                        Text(
                            text  = stringResource(R.string.presets_select_all),
                            style = MaterialTheme.typography.labelLarge,
                            color = Charcoal,
                        )
                    }
                }
                CircleIconButton(
                    onClick            = onDelete,
                    contentDescription = stringResource(R.string.presets_cd_delete_selected),
                    background         = if (count > 0) PaleRed else White,
                    border             = if (count > 0) PaleRed else Border,
                    enabled            = count > 0,
                ) { GlyphIcon(Glyph.Trash, if (count > 0) PaleRedText else MutedGray, size = 18.dp) }
            }
        }
        Spacer(Modifier.height(18.dp))
        ScreenTitle(
            title    = stringResource(R.string.presets_selected_count, count, total),
        )
    }
}

@Composable
private fun ReorderTopBar(onDone: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.gutter, vertical = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text  = stringResource(R.string.presets_drag_to_reorder),
            style = MaterialTheme.typography.titleSmall,
            color = MutedGray,
        )
        TextButton(onClick = onDone) {
            Text(
                text  = stringResource(R.string.action_done),
                style = MaterialTheme.typography.labelLarge,
                color = Charcoal,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Body states
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PresetsLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.gutter, vertical = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(Dimens.cardRadius))
                .background(SurfaceMuted),
        )
    }
}

@Composable
private fun PresetsEmptyState(onBrowse: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconCircle(SurfaceMuted, size = 64.dp) { GlyphIcon(Glyph.Sparkle, MutedGray, size = 26.dp) }
        Spacer(Modifier.height(20.dp))
        Text(
            text  = stringResource(R.string.presets_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = Charcoal,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text      = stringResource(R.string.presets_empty_body),
            style     = MaterialTheme.typography.bodyMedium,
            color     = MutedGray,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryPill(
            text     = stringResource(R.string.presets_browse_sounds),
            onClick  = onBrowse,
            modifier = Modifier.coachMarkAnchor("browse"),
        )
    }
}

@Composable
private fun PresetsErrorState(message: String) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Tag(stringResource(R.string.label_error), PaleRed, PaleRedText)
        Text(
            text      = message,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MutedGray,
            textAlign = TextAlign.Center,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ready body
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PresetsReadyBody(
    screen: PresetsScreenState.Ready,
    uiState: PresetsUiState,
    onLaunch: (Domain.Preset) -> Unit,
    onLongPress: (Domain.Preset) -> Unit,
    onToggle: (Domain.Preset) -> Unit,
    onPin: (Domain.Preset) -> Unit,
    onUnpin: (Domain.Preset) -> Unit,
    onRename: (Domain.Preset) -> Unit,
    onDelete: (Domain.Preset) -> Unit,
    loadStats: (List<Long>) -> Unit,
) {
    val listState = rememberLazyListState()
    val isSelecting = uiState.selectionMode is SelectionMode.Active

    val displayItems = if (screen.isFiltered) screen.filtered
    else screen.pinned + screen.unpinned
    val firstId = displayItems.firstOrNull()?.preset?.id   // coach mark target

    // Load stats when the list first becomes visible — single call, not per-item
    LaunchedEffect(displayItems.map { it.preset.id }) {
        loadStats(displayItems.map { it.preset.id })
    }

    fun LazyListScope.presetGroup(items: List<PresetItemState>, reorderMode: Boolean) {
        val count = items.size
        itemsIndexed(items = items, key = { _, item -> item.preset.id }) { index, item ->
            PresetRow(
                item        = item,
                position    = groupPosition(index, count),
                isSelecting = isSelecting,
                reorderMode = reorderMode,
                showPin     = screen.isFiltered && item.preset.isPinned,
                onTap       = { if (isSelecting) onToggle(item.preset) else onLaunch(item.preset) },
                onLongPress = { onLongPress(item.preset) },
                onPin       = { onPin(item.preset) },
                onUnpin     = { onUnpin(item.preset) },
                onRename    = { onRename(item.preset) },
                onDelete    = { onDelete(item.preset) },
                modifier    = if (item.preset.id == firstId) Modifier.coachMarkAnchor("preset") else Modifier,
            )
        }
    }

    LazyColumn(
        state          = listState,
        contentPadding = PaddingValues(horizontal = Dimens.gutter, vertical = 8.dp),
        modifier       = Modifier.fillMaxSize(),
    ) {
        if (uiState.hasReachedLimit && !screen.isFiltered) {
            item(key = "limit_banner") { LimitBanner() }
        }

        if (!screen.isFiltered) {
            if (screen.pinned.isNotEmpty()) {
                item(key = "pinned_header") {
                    SectionLabel(stringResource(R.string.presets_section_pinned), topPadding = 12.dp)
                }
                presetGroup(screen.pinned, uiState.reorderMode)
            }
            if (screen.unpinned.isNotEmpty()) {
                item(key = "unpinned_header") {
                    if (screen.pinned.isNotEmpty()) {
                        SectionLabel(stringResource(R.string.presets_section_saved), topPadding = 24.dp)
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }
                }
                presetGroup(screen.unpinned, uiState.reorderMode)
            }
        } else {
            if (screen.filtered.isEmpty()) {
                item(key = "no_results") {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text  = stringResource(R.string.presets_no_match),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedGray,
                        )
                    }
                }
            } else {
                item(key = "filtered_gap") { Spacer(Modifier.height(8.dp)) }
                presetGroup(screen.filtered, reorderMode = false)
            }
        }

        item(key = "bottom_spacer") {
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preset row
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetRow(
    item: PresetItemState,
    position: GroupPosition,
    isSelecting: Boolean,
    reorderMode: Boolean,
    showPin: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val preset = item.preset

    val bgColor by animateColorAsState(
        targetValue   = if (item.isSelected) PaleBlue else White,
        animationSpec = tween(250),
        label         = "row_bg_${preset.id}",
    )
    val cardDescription = listOfNotNull(
        preset.name,
        stringResource(R.string.presets_cd_playing).takeIf { item.isActive },
        stringResource(R.string.presets_cd_selected).takeIf { item.isSelected },
        stringResource(R.string.presets_cd_pinned).takeIf { preset.isPinned },
        stringResource(R.string.presets_cd_locked_layers).takeIf { item.hasLockedLayers },
    ).joinToString(", ")
    val moreLabel = stringResource(R.string.presets_cd_more_options, preset.name)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .groupedRow(position, background = bgColor)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress, role = Role.Button)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
            .semantics { contentDescription = cardDescription },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading — selection circle, reorder handle, or the preset's mark
        when {
            isSelecting -> SelectionIndicator(isSelected = item.isSelected)
            reorderMode -> Box(Modifier.size(Dimens.leadingCircle), contentAlignment = Alignment.Center) {
                GlyphIcon(Glyph.Drag, MutedGray, size = 20.dp)
            }
            else        -> PresetLeadingIcon(preset = preset, isActive = item.isActive)
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text     = preset.name,
                    style    = MaterialTheme.typography.titleSmall,
                    color    = Charcoal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (showPin) GlyphIcon(Glyph.Pin, MutedGray, size = 12.dp)
            }
            Spacer(Modifier.height(2.dp))
            val layerSummary = preset.mix.layers
                .take(3)
                .map { it.sound.displayTitle() }
                .joinToString(" · ")
            Text(
                text     = layerSummary.ifBlank {
                    pluralStringResource(R.plurals.presets_sound_count, preset.mix.layers.size, preset.mix.layers.size)
                },
                style    = MaterialTheme.typography.bodySmall,
                color    = MutedGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(10.dp))

        // Trailing — premium flag, listening time, more
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (item.hasLockedLayers) PremiumPill()
            if (item.totalListenedMs > 0L && !item.hasLockedLayers) {
                Text(
                    text  = listenedDuration(item.totalListenedMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                )
            }
            if (!isSelecting && !reorderMode) {
                Box {
                    GhostIconButton(onClick = { showMenu = true }, contentDescription = moreLabel) {
                        GlyphIcon(Glyph.More, MutedGray, size = 20.dp)
                    }
                    PresetContextMenu(
                        expanded  = showMenu,
                        isPinned  = preset.isPinned,
                        onDismiss = { showMenu = false },
                        onPin     = { showMenu = false; onPin() },
                        onUnpin   = { showMenu = false; onUnpin() },
                        onRename  = { showMenu = false; onRename() },
                        onDelete  = { showMenu = false; onDelete() },
                    )
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun PresetLeadingIcon(preset: Domain.Preset, isActive: Boolean) {
    when {
        isActive -> IconCircle(Charcoal) { GlyphIcon(Glyph.WaveBars, White, size = 18.dp) }
        preset.emoji != null -> IconCircle(SurfaceMuted) {
            Text(text = preset.emoji, style = MaterialTheme.typography.titleMedium)
        }
        else -> IconCircle(SurfaceMuted) {
            Text(
                text  = "${preset.mix.layers.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MutedGray,
            )
        }
    }
}

@Composable
private fun SelectionIndicator(isSelected: Boolean) {
    Box(Modifier.size(Dimens.leadingCircle), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) PaleBlueText else Color.Transparent)
                .border(1.5.dp, if (isSelected) PaleBlueText else Border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) GlyphIcon(Glyph.Check, White, size = 12.dp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Context menu · banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PresetContextMenu(
    expanded: Boolean,
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(
        expanded         = expanded,
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(16.dp),
        containerColor   = White,
        border           = androidx.compose.foundation.BorderStroke(Dimens.hairline, Border),
    ) {
        DropdownMenuItem(
            text        = { MenuLabel(stringResource(if (isPinned) R.string.presets_menu_unpin else R.string.presets_menu_pin)) },
            leadingIcon = { GlyphIcon(Glyph.Pin, MutedGray, size = 18.dp) },
            onClick     = if (isPinned) onUnpin else onPin,
        )
        DropdownMenuItem(
            text        = { MenuLabel(stringResource(R.string.presets_menu_rename)) },
            leadingIcon = { GlyphIcon(Glyph.Sliders, MutedGray, size = 18.dp) },
            onClick     = onRename,
        )
        DropdownMenuItem(
            text        = { MenuLabel(stringResource(R.string.action_delete), color = PaleRedText) },
            leadingIcon = { GlyphIcon(Glyph.Trash, PaleRedText, size = 18.dp) },
            onClick     = onDelete,
        )
    }
}

@Composable
private fun MenuLabel(text: String, color: Color = Charcoal) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
}

@Composable
private fun LimitBanner() {
    SurfaceCard(
        modifier       = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        background     = PaleYellow,
        border         = Color.Transparent,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlyphIcon(Glyph.Sparkle, PaleYellowText, size = 18.dp)
            Text(
                text     = stringResource(R.string.presets_limit_banner, FREE_PRESET_LIMIT),
                style    = MaterialTheme.typography.bodySmall,
                color    = PaleYellowText,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialogs
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeleteConfirmDialog(
    presetName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = White,
        shape            = RoundedCornerShape(Dimens.panelRadius),
        title = {
            Text(
                text  = stringResource(R.string.presets_delete_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Charcoal,
            )
        },
        text = {
            Text(
                text  = stringResource(R.string.presets_delete_body, presetName),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete), style = MaterialTheme.typography.labelLarge, color = PaleRedText)
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
private fun BulkDeleteConfirmDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = White,
        shape            = RoundedCornerShape(Dimens.panelRadius),
        title = {
            Text(
                text  = pluralStringResource(R.plurals.presets_bulk_delete_title, count, count),
                style = MaterialTheme.typography.headlineSmall,
                color = Charcoal,
            )
        },
        text = {
            Text(
                text  = stringResource(R.string.presets_bulk_delete_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.presets_delete_all), style = MaterialTheme.typography.labelLarge, color = PaleRedText)
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
private fun RenameDialog(
    preset: Domain.Preset,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(preset.id) { mutableStateOf(preset.name) }
    val isValid = name.trim().isNotBlank() && name.trim() != preset.name
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = White,
        shape            = RoundedCornerShape(Dimens.panelRadius),
        title = {
            Text(
                text  = stringResource(R.string.presets_rename_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Charcoal,
            )
        },
        text = {
            SleepTextInput(
                value          = name,
                onValueChange  = { name = it },
                hint           = stringResource(R.string.preset_field_name),
                focusRequester = focusRequester,
                active         = isValid,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid) onConfirm(name.trim()) },
                enabled = isValid,
            ) {
                Text(
                    text  = stringResource(R.string.action_save),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isValid) Charcoal else MutedGray,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelLarge, color = MutedGray)
            }
        },
    )
}
