package com.opxl.sleepslide.presentation.presets


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.presentation.components.ChevronLeft
import com.opxl.sleepslide.presentation.components.CheckIcon
import com.opxl.sleepslide.domain.repository.CoachMarkScreens
import com.opxl.sleepslide.presentation.tutorial.CoachMarkHost
import com.opxl.sleepslide.presentation.tutorial.TutorialViewModel
import com.opxl.sleepslide.presentation.tutorial.coachMarkAnchor
import com.opxl.sleepslide.presentation.tutorial.tutorialViewModel
import com.opxl.sleepslide.presentation.components.PremiumPill
import com.opxl.sleepslide.ui.theme.OnPanel
import com.opxl.sleepslide.ui.theme.Panel
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalContext
import com.opxl.sleepslide.R
import com.opxl.sleepslide.presentation.components.displayTitle
import com.opxl.sleepslide.presentation.components.listenedDuration

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
                    snackbarData    = data,
                    containerColor  = Charcoal,
                    contentColor    = White,
                    actionColor     = PaleBlueText,
                    shape           = RoundedCornerShape(8.dp),
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
                uiState      = uiState,
                onBack       = onNavigateBack,
                onAddPreset  = { viewModel.navigateToLibrary() },
                onSelectAll  = { viewModel.selectAll() },
                onExitSelect = { viewModel.exitSelectionMode() },
                onBulkDelete = { viewModel.requestBulkDelete() },
                onDoneReorder = { viewModel.disableReorderMode() },
            )

            AnimatedVisibility(
                visible = uiState.screen is PresetsScreenState.Ready,
                enter   = fadeIn(tween(200)),
                exit    = fadeOut(tween(150)),
            ) {
                SearchBar(
                    query     = uiState.searchQuery,
                    onChange  = viewModel::onSearchQueryChanged,
                    onClear   = { viewModel.clearSearch() },
                    enabled   = uiState.selectionMode is SelectionMode.None && !uiState.reorderMode,
                )
            }

            AnimatedContent(
                targetState   = uiState.screen,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label         = "presets_body",
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
                        onReorder    = viewModel::onReorder,
                        onReorderEnd = viewModel::onReorderCommitted,
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
    } // CoachMarkHost

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

// Top bar

/** Anchor key → label for the first-visit coach marks; only laid-out targets are shown. */
private val PRESETS_COACH_MARKS = listOf(
    "add"    to R.string.coach_presets_add,
    "search" to R.string.coach_presets_search,
    "preset" to R.string.coach_presets_preset,
    "browse" to R.string.coach_presets_browse,
)

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
        targetState   = Triple(isSelecting, uiState.reorderMode, selectedCount),
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
        label         = "topbar_mode",
    ) { (selecting, reordering, count) ->
        when {
            selecting  -> SelectionTopBar(
                count        = count,
                total        = uiState.presetCount,
                onClose      = onExitSelect,
                onSelectAll  = onSelectAll,
                onDelete     = onBulkDelete,
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
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SmallIconButton(onClick = onBack, description = stringResource(R.string.cd_back)) {
            ChevronLeft(Charcoal)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = stringResource(R.string.tab_presets),
                style = MaterialTheme.typography.titleMedium,
                color = Charcoal,
            )
            if (presetCount > 0) {
                Text(
                    text  = stringResource(R.string.presets_count_saved, presetCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                )
            }
        }

        SmallIconButton(
            onClick     = onAdd,
            description = stringResource(R.string.presets_cd_add),
            background  = if (hasReachedLimit) SurfaceMuted else Charcoal,
            borderColor = if (hasReachedLimit) Border else Charcoal,
            modifier    = Modifier.coachMarkAnchor("add"),
        ) {
            PlusIcon(tint = if (hasReachedLimit) MutedGray else White)
        }
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
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SmallIconButton(onClick = onClose, description = stringResource(R.string.presets_cd_exit_selection)) {
            CloseIcon(Charcoal)
        }

        Text(
            text  = stringResource(R.string.presets_selected_count, count, total),
            style = MaterialTheme.typography.titleSmall,
            color = Charcoal,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (count < total) {
                TextButton(onClick = onSelectAll) {
                    Text(
                        text  = stringResource(R.string.presets_select_all),
                        style = MaterialTheme.typography.labelLarge,
                        color = Charcoal,
                    )
                }
            }
            SmallIconButton(
                onClick     = onDelete,
                description = stringResource(R.string.presets_cd_delete_selected),
                background  = if (count > 0) PaleRed else SurfaceMuted,
                borderColor = if (count > 0) PaleRedText else Border,
            ) {
                TrashIcon(tint = if (count > 0) PaleRedText else MutedGray)
            }
        }
    }
}

@Composable
private fun ReorderTopBar(onDone: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
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

// Search bar

@Composable
private fun SearchBar(
    query: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    enabled: Boolean,
) {
    val focusRequester = remember { FocusRequester() }
    val clearSearchLabel = stringResource(R.string.library_cd_clear_search)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .coachMarkAnchor("search")
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceMuted)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchIcon(MutedGray)
            BasicTextField(
                value         = query,
                onValueChange = { if (enabled) onChange(it) },
                enabled       = enabled,
                singleLine    = true,
                textStyle     = MaterialTheme.typography.bodyMedium.copy(color = Charcoal),
                cursorBrush   = SolidColor(Charcoal),
                modifier      = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text  = stringResource(R.string.presets_search_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedGray,
                            )
                        }
                        inner()
                    }
                },
            )
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MutedGray.copy(alpha = 0.2f))
                        .clickable(onClick = onClear)
                        .semantics { contentDescription = clearSearchLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    CloseIcon(MutedGray)
                }
            }
        }
    }
}

// Body states

@Composable
private fun PresetsLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Border),
            )
        }
    }
}

@Composable
private fun PresetsEmptyState(onBrowse: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text  = stringResource(R.string.presets_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = Charcoal,
        )
        Text(
            text  = stringResource(R.string.presets_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGray,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .coachMarkAnchor("browse")
                .clip(RoundedCornerShape(6.dp))
                .background(Charcoal)
                .clickable(onClick = onBrowse)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(
                text  = stringResource(R.string.presets_browse_sounds),
                style = MaterialTheme.typography.labelLarge,
                color = White,
            )
        }
    }
}

@Composable
private fun PresetsErrorState(message: String) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(PaleRed)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text  = stringResource(R.string.label_error),
                style = MaterialTheme.typography.labelSmall,
                color = PaleRedText,
            )
        }
        Text(
            text  = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGray,
        )
    }
}

// Ready body

@Composable
private fun PresetsReadyBody(
    screen: PresetsScreenState.Ready,
    uiState: PresetsUiState,
    onLaunch: (Domain.Preset) -> Unit,
    onLongPress: (Domain.Preset) -> Unit,
    onToggle: (Domain.Preset) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onReorderEnd: (List<Long>) -> Unit,
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

    LazyColumn(
        state           = listState,
        contentPadding  = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier        = Modifier.fillMaxSize(),
    ) {
        // Free tier limit banner
        if (uiState.hasReachedLimit && !screen.isFiltered) {
            item(key = "limit_banner") {
                LimitBanner()
            }
        }

        // Pinned section header
        if (screen.pinned.isNotEmpty() && !screen.isFiltered) {
            item(key = "pinned_header") {
                SectionLabel(stringResource(R.string.presets_section_pinned))
            }
            items(
                items    = screen.pinned,
                key      = { it.preset.id },
            ) { item ->
                PresetCard(
                    item        = item,
                    isSelecting = isSelecting,
                    reorderMode = uiState.reorderMode,
                    onTap       = {
                        if (isSelecting) onToggle(item.preset) else onLaunch(item.preset)
                    },
                    onLongPress = { onLongPress(item.preset) },
                    onPin       = { onPin(item.preset) },
                    onUnpin     = { onUnpin(item.preset) },
                    onRename    = { onRename(item.preset) },
                    onDelete    = { onDelete(item.preset) },
                    modifier    = if (item.preset.id == firstId) Modifier.coachMarkAnchor("preset") else Modifier,
                )
            }
        }

        // Unpinned section header
        if (screen.unpinned.isNotEmpty() && !screen.isFiltered) {
            item(key = "unpinned_header") {
                if (screen.pinned.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.presets_section_saved))
                }
            }
            items(
                items = screen.unpinned,
                key   = { it.preset.id },
            ) { item ->
                PresetCard(
                    item        = item,
                    isSelecting = isSelecting,
                    reorderMode = uiState.reorderMode,
                    onTap       = {
                        if (isSelecting) onToggle(item.preset) else onLaunch(item.preset)
                    },
                    onLongPress = { onLongPress(item.preset) },
                    onPin       = { onPin(item.preset) },
                    onUnpin     = { onUnpin(item.preset) },
                    onRename    = { onRename(item.preset) },
                    onDelete    = { onDelete(item.preset) },
                    modifier    = if (item.preset.id == firstId) Modifier.coachMarkAnchor("preset") else Modifier,
                )
            }
        }

        // Filtered results
        if (screen.isFiltered) {
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
                items(
                    items = screen.filtered,
                    key   = { it.preset.id },
                ) { item ->
                    PresetCard(
                        item        = item,
                        isSelecting = isSelecting,
                        reorderMode = false,
                        onTap       = {
                            if (isSelecting) onToggle(item.preset) else onLaunch(item.preset)
                        },
                        onLongPress = { onLongPress(item.preset) },
                        onPin       = { onPin(item.preset) },
                        onUnpin     = { onUnpin(item.preset) },
                        onRename    = { onRename(item.preset) },
                        onDelete    = { onDelete(item.preset) },
                        modifier    = if (item.preset.id == firstId) Modifier.coachMarkAnchor("preset") else Modifier,
                    )
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

// Preset card

@Composable
private fun PresetCard(
    item: PresetItemState,
    isSelecting: Boolean,
    reorderMode: Boolean,
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
        targetValue   = when {
            item.isActive    -> Charcoal
            item.isSelected  -> PaleBlue
            else             -> White
        },
        animationSpec = tween(250),
        label         = "card_bg_${preset.id}",
    )
    val borderColor by animateColorAsState(
        targetValue   = when {
            item.isActive   -> Panel
            item.isSelected -> PaleBlueText
            else            -> Border
        },
        animationSpec = tween(250),
        label         = "card_border_${preset.id}",
    )
    val textColor   = if (item.isActive) OnPanel else Charcoal
    val cardDescription = listOfNotNull(
        preset.name,
        stringResource(R.string.presets_cd_playing).takeIf { item.isActive },
        stringResource(R.string.presets_cd_selected).takeIf { item.isSelected },
        stringResource(R.string.presets_cd_pinned).takeIf { preset.isPinned },
        stringResource(R.string.presets_cd_locked_layers).takeIf { item.hasLockedLayers },
    ).joinToString(", ")
    val moreLabel = stringResource(R.string.presets_cd_more_options, preset.name)
    val subColor    = if (item.isActive) OnPanel.copy(alpha = 0.6f) else MutedGray

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onTap)
            .semantics { contentDescription = cardDescription },
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Left indicator — selection checkbox, reorder handle, or emoji/waveform
            when {
                isSelecting -> SelectionIndicator(
                    isSelected = item.isSelected,
                    isActive   = item.isActive,
                )
                reorderMode -> ReorderHandle(tint = if (item.isActive) OnPanel.copy(alpha = 0.5f) else MutedGray)
                else        -> PresetLeadingIcon(
                    preset   = preset,
                    isActive = item.isActive,
                )
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = preset.name,
                    style    = MaterialTheme.typography.titleSmall,
                    color    = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
                    color    = subColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.totalListenedMs > 0L) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = listenedDuration(item.totalListenedMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = subColor.copy(alpha = 0.7f),
                    )
                }
            }

            // Premium layers are skipped at launch on the free tier — flag it before the tap
            if (item.hasLockedLayers) PremiumPill()

            // Trailing actions
            if (!isSelecting && !reorderMode) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (item.isActive) OnPanel.copy(alpha = 0.1f) else SurfaceMuted
                            )
                            .clickable { showMenu = true }
                            .semantics { contentDescription = moreLabel },
                        contentAlignment = Alignment.Center,
                    ) {
                        DotsIcon(tint = if (item.isActive) OnPanel.copy(alpha = 0.7f) else MutedGray)
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
            }
        }
    }
}

@Composable
private fun PresetLeadingIcon(preset: Domain.Preset, isActive: Boolean) {
    Box(
        modifier         = Modifier.size(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isActive -> ActiveWaveform()
            preset.emoji != null -> Text(
                text  = preset.emoji,
                style = MaterialTheme.typography.titleMedium,
            )
            preset.isPinned -> PinIcon(MutedGray)
            else -> {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceMuted),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "${preset.mix.layers.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedGray,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionIndicator(isSelected: Boolean, isActive: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected && isActive -> OnPanel
                    isSelected            -> PaleBlueText
                    else                  -> Color.Transparent
                }
            )
            .border(
                width = 1.5.dp,
                color = when {
                    isSelected && isActive -> OnPanel
                    isSelected            -> PaleBlueText
                    isActive              -> OnPanel.copy(alpha = 0.5f)
                    else                  -> Border
                },
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            CheckIcon(tint = if (isActive) Panel else White, size = 10.dp)
        }
    }
}

@Composable
private fun ActiveWaveform() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        listOf(10.dp, 16.dp, 12.dp, 16.dp, 10.dp).forEach { h ->
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(h)
                    .clip(RoundedCornerShape(1.dp))
                    .background(OnPanel.copy(alpha = 0.8f)),
            )
        }
    }
}

@Composable
private fun ReorderHandle(tint: Color) {
    Column(
        modifier              = Modifier.size(20.dp),
        verticalArrangement   = Arrangement.spacedBy(3.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
    ) {
        repeat(3) {
            Box(
                Modifier
                    .width(14.dp)
                    .height(1.5.dp)
                    .background(tint)
            )
        }
    }
}

// Context menu

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
        modifier         = Modifier
            .background(White)
            .border(1.dp, Border, RoundedCornerShape(8.dp)),
    ) {
        DropdownMenuItem(
            text    = { MenuLabel(stringResource(if (isPinned) R.string.presets_menu_unpin else R.string.presets_menu_pin)) },
            onClick = if (isPinned) onUnpin else onPin,
        )
        DropdownMenuItem(
            text    = { MenuLabel(stringResource(R.string.presets_menu_rename)) },
            onClick = onRename,
        )
        DropdownMenuItem(
            text    = { MenuLabel(stringResource(R.string.action_delete), color = PaleRedText) },
            onClick = onDelete,
        )
    }
}

@Composable
private fun MenuLabel(text: String, color: Color = Charcoal) {
    Text(
        text  = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
    )
}

// Banners and labels

@Composable
private fun LimitBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PaleYellow)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(PaleYellowText),
        )
        Text(
            text  = stringResource(R.string.presets_limit_banner, FREE_PRESET_LIMIT),
            style = MaterialTheme.typography.bodySmall,
            color = PaleYellowText,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text     = label.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = MutedGray,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

// Dialogs

@Composable
private fun DeleteConfirmDialog(
    presetName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = WarmWhite,
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
        containerColor   = WarmWhite,
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
        containerColor   = WarmWhite,
        title = {
            Text(
                text  = stringResource(R.string.presets_rename_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Charcoal,
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceMuted)
                    .border(1.dp, if (isValid) Charcoal else Border, RoundedCornerShape(6.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                BasicTextField(
                    value         = name,
                    onValueChange = { name = it },
                    singleLine    = true,
                    textStyle     = MaterialTheme.typography.bodyMedium.copy(color = Charcoal),
                    cursorBrush   = SolidColor(Charcoal),
                    modifier      = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
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

// Micro-components — all drawn, no bitmap resources

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
private fun PlusIcon(tint: Color) {
    Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(1.5.dp).background(tint))
        Box(Modifier.width(1.5.dp).height(14.dp).background(tint))
    }
}

@Composable
private fun CloseIcon(tint: Color) {
    Box(Modifier.size(10.dp).drawBehind {
        val s = Stroke(1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(tint, Offset(0f, 0f), Offset(size.width, size.height), s.width)
        drawLine(tint, Offset(size.width, 0f), Offset(0f, size.height), s.width)
    })
}

@Composable
private fun TrashIcon(tint: Color) {
    Box(Modifier.size(14.dp).drawBehind {
        val s = Stroke(1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(tint, Offset(0f, 3.dp.toPx()), Offset(size.width, 3.dp.toPx()), s.width)
        drawRect(tint, Offset(2.dp.toPx(), 3.dp.toPx()),
            Size(size.width - 4.dp.toPx(), size.height - 3.dp.toPx()), style = s)
        drawLine(tint, Offset(4.dp.toPx(), 0f), Offset(size.width - 4.dp.toPx(), 0f), s.width)
    })
}

@Composable
private fun DotsIcon(tint: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) {
            Box(Modifier.size(3.dp).clip(CircleShape).background(tint))
        }
    }
}

@Composable
private fun SearchIcon(tint: Color) {
    Box(Modifier.size(14.dp).drawBehind {
        val r  = 4.5.dp.toPx()
        val cx = r; val cy = r
        drawCircle(tint, r, Offset(cx, cy),
            style = Stroke(1.5.dp.toPx()))
        val s  = Stroke(1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        val d  = (r * 0.707f)
        drawLine(tint, Offset(cx + d, cy + d), Offset(size.width, size.height), s.width)
    })
}

@Composable
private fun PinIcon(tint: Color) {
    Box(Modifier.size(14.dp).drawBehind {
        val s = Stroke(1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawCircle(tint, size.minDimension / 3f,
            Offset(size.width / 2f, size.height / 3f), style = s)
        drawLine(tint, Offset(size.width / 2f, size.height / 3f * 2f),
            Offset(size.width / 2f, size.height), s.width)
    })
}