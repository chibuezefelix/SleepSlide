package com.opxl.sleepslide.presentation.navigation

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.opxl.sleepslide.R
import com.opxl.sleepslide.domain.repository.UserPreferencesRepository
import com.opxl.sleepslide.presentation.home.HomeScreen
import com.opxl.sleepslide.presentation.library.LibraryScreen
import com.opxl.sleepslide.presentation.onboard.OnboardingScreen
import com.opxl.sleepslide.presentation.permission.LocalNotificationPermissionRequester
import com.opxl.sleepslide.presentation.player.PlayerScreen
import com.opxl.sleepslide.presentation.presets.PresetsScreen
import com.opxl.sleepslide.presentation.settings.SettingsScreen
import com.opxl.sleepslide.presentation.tutorial.TutorialScreen
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.MutedGray
import com.opxl.sleepslide.ui.theme.WarmWhite
import kotlinx.coroutines.flow.first


object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME       = "home"
    const val PLAYER     = "player"
    const val LIBRARY    = "library"
    const val PRESETS    = "presets"
    const val SETTINGS   = "settings"
    const val TUTORIAL   = "tutorial"

    const val DEEP_LINK_BASE    = "sleepslide://app"
    const val PLAYER_DEEP_LINK  = "$DEEP_LINK_BASE/player"
    const val PRESETS_DEEP_LINK = "$DEEP_LINK_BASE/presets"
}

// ─────────────────────────────────────────────────────────────────────────────
// Tabs
// ─────────────────────────────────────────────────────────────────────────────

/** Top-level destinations reachable from the bottom bar. Player, Tutorial and Onboarding are not tabs. */
private enum class AppTab(val route: String, val label: String, @DrawableRes val icon: Int) {
    HOME(Routes.HOME,         "Home",     R.drawable.ic_tab_home),
    LIBRARY(Routes.LIBRARY,   "Library",  R.drawable.ic_tab_library),
    PRESETS(Routes.PRESETS,   "Presets",  R.drawable.ic_tab_presets),
    SETTINGS(Routes.SETTINGS, "Settings", R.drawable.ic_tab_settings);

    companion object {
        /**
         * Matches on the base route, so the registered pattern "presets?autoPlay={autoPlay}"
         * still resolves to [PRESETS]. An exact match would hide the bar on the Presets tab.
         */
        fun forRoute(route: String?): AppTab? {
            val base = route?.substringBefore('?') ?: return null
            return entries.firstOrNull { it.route == base }
        }
    }
}

// Pushes (Home → Player, Settings → Tutorial…) slide sideways like a stack.
private val pushEnter = slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300))
private val pushExit  = slideOutHorizontally(tween(250)) { -it / 4 } + fadeOut(tween(250))
private val popEnter  = slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300))
private val popExit   = slideOutHorizontally(tween(250)) { it / 4 } + fadeOut(tween(250))

// Tabs are siblings, not a stack: a sideways slide would point "forward" or "back" depending
// on tap order. A short crossfade reads the same in every direction and keeps the bar snappy.
private val tabEnter = fadeIn(tween(150))
private val tabExit  = fadeOut(tween(150))

/** True when both ends of this transition are bottom-bar tabs, whichever direction. */
private val AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch: Boolean
    get() = AppTab.forRoute(initialState.destination.route) != null &&
            AppTab.forRoute(targetState.destination.route) != null

// ─────────────────────────────────────────────────────────────────────────────
// NavGraph entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NavGraph(
    userPreferencesRepository: UserPreferencesRepository,
    navController: NavHostController = rememberNavController(),
) {
    // Resolved once. Collecting prefs here would recompose the whole graph on every preference
    // write (last-played preset, volumes…), and a flip of hasCompletedOnboarding would swap
    // startDestination — rebuilding the graph and resetting the back stack mid-navigation.
    // Onboarding and "retake onboarding" navigate explicitly, so the first value is enough.
    val startDestination by produceState<String?>(initialValue = null, userPreferencesRepository) {
        value = if (userPreferencesRepository.observe().first().hasCompletedOnboarding) Routes.HOME
                else Routes.ONBOARDING
    }
    val start = startDestination ?: return

    val context = LocalContext.current
    val batterySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    val openBatterySettings: () -> Unit = remember(context, batterySettingsLauncher) {
        {
            runCatching {
                batterySettingsLauncher.launch(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            }.onFailure {
                // Fallback for OEMs that block the direct intent
                runCatching {
                    batterySettingsLauncher.launch(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                }
            }
        }
    }

    // The bar floats over the NavHost instead of sitting in a Scaffold slot. As a slot, showing or
    // hiding it changes the content padding, so both screens of a Home → Player transition jump
    // by the bar height on the first frame. Here tab pages reserve the space themselves
    // (see [TabPage]) and the bar just slides away.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmWhite),
    ) {
        NavHost(
            navController      = navController,
            startDestination   = start,
            modifier           = Modifier.fillMaxSize(),
            enterTransition    = { if (isTabSwitch) tabEnter else pushEnter },
            exitTransition     = { if (isTabSwitch) tabExit  else pushExit },
            popEnterTransition = { if (isTabSwitch) tabEnter else popEnter },
            popExitTransition  = { if (isTabSwitch) tabExit  else popExit },
        ) {

            composable(route = Routes.ONBOARDING) { entry ->
                OnboardingScreen(
                    onComplete = {
                        entry.ifResumed {
                            navController.navigate(Routes.HOME) {
                                // Back from Home exits the app instead of returning here
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    },
                    onOpenBatterySettings = openBatterySettings,
                )
            }

            composable(route = Routes.HOME) { entry ->
                TabPage {
                    HomeScreen(
                        onNavigateToPlayer  = {
                            entry.ifResumed { navController.navigate(Routes.PLAYER) { launchSingleTop = true } }
                        },
                        onNavigateToLibrary = {
                            entry.ifResumed { navController.navigateToTab(AppTab.LIBRARY) }
                        },
                        onNavigateToPreset  = { presetId ->
                            entry.ifResumed {
                                navController.navigate("${Routes.PLAYER}?presetId=$presetId") {
                                    launchSingleTop = true
                                }
                            }
                        },
                    )
                }
            }

            // Player is NOT a tab — it sits above the tab layer, no bottom bar
            composable(
                route = "${Routes.PLAYER}?presetId={presetId}",
                arguments = listOf(
                    navArgument("presetId") {
                        type         = NavType.LongType
                        defaultValue = -1L
                    }
                ),
                deepLinks = listOf(
                    navDeepLink { uriPattern = Routes.PLAYER_DEEP_LINK }
                ),
            ) { entry ->
                // MainActivity owns the POST_NOTIFICATIONS launcher and provides it via CompositionLocal
                val notificationPermissionRequester = LocalNotificationPermissionRequester.current
                PlayerScreen(
                    onNavigateBack      = { entry.ifResumed { navController.popIfNotRoot() } },
                    onNavigateToLibrary = { entry.ifResumed { navController.navigateToTab(AppTab.LIBRARY) } },
                    onRequestNotificationPermission = notificationPermissionRequester::request,
                )
            }

            composable(route = Routes.LIBRARY) { entry ->
                TabPage {
                    LibraryScreen(
                        onNavigateBack     = { entry.ifResumed { navController.popIfNotRoot() } },
                        onNavigateToPlayer = {
                            entry.ifResumed { navController.navigate(Routes.PLAYER) { launchSingleTop = true } }
                        },
                    )
                }
            }

            composable(
                route     = "${Routes.PRESETS}?autoPlay={autoPlay}",
                arguments = listOf(
                    navArgument("autoPlay") {
                        type         = NavType.BoolType
                        defaultValue = false
                    }
                ),
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "${Routes.DEEP_LINK_BASE}/presets?autoPlay={autoPlay}"
                    }
                ),
            ) { entry ->
                TabPage {
                    PresetsScreen(
                        onNavigateBack      = { entry.ifResumed { navController.popIfNotRoot() } },
                        onNavigateToPlayer  = { presetId ->
                            entry.ifResumed {
                                navController.navigate("${Routes.PLAYER}?presetId=$presetId") {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onNavigateToLibrary = {
                            entry.ifResumed { navController.navigateToTab(AppTab.LIBRARY) }
                        },
                    )
                }
            }

            composable(route = Routes.SETTINGS) { entry ->
                TabPage {
                    SettingsScreen(
                        onNavigateBack = { entry.ifResumed { navController.popIfNotRoot() } },
                        onNavigateToOnboarding = {
                            entry.ifResumed {
                                navController.navigate(Routes.ONBOARDING) {
                                    // Clear entire back stack — onboarding retake is a fresh start
                                    popUpTo(Routes.HOME) { inclusive = true }
                                }
                            }
                        },
                        onOpenBatterySettings = openBatterySettings,
                        onNavigateToTutorial  = {
                            entry.ifResumed { navController.navigate(Routes.TUTORIAL) { launchSingleTop = true } }
                        },
                    )
                }
            }

            // Feature tour — a sheet, so it rises from the bottom instead of sliding in sideways
            composable(
                route              = Routes.TUTORIAL,
                enterTransition    = { slideInVertically(tween(300)) { it } + fadeIn(tween(300)) },
                exitTransition     = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(200)) },
                popExitTransition  = { slideOutVertically(tween(250)) { it } + fadeOut(tween(250)) },
            ) { entry ->
                TutorialScreen(onClose = { entry.ifResumed { navController.popIfNotRoot() } })
            }
        }

        TabBarHost(
            navController = navController,
            modifier      = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Navigation helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Runs [action] only while this destination is settled on screen. Drops a second tap that lands
 * before the first navigation takes effect (double Player pushes, double back = blank screen),
 * taps on a screen that is still animating in or out, and one-shot VM events delivered while
 * the screen is leaving or returning (which would otherwise yank the user somewhere else).
 */
private inline fun NavBackStackEntry.ifResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}

/** Pops unless this is the last entry — popping the root leaves an empty, blank NavHost. */
private fun NavController.popIfNotRoot() {
    if (previousBackStackEntry != null) popBackStack()
}

/**
 * Switches to [tab] with one back stack slot per tab: back from any tab returns to Home, each
 * tab keeps its scroll/UI state, and Home is never duplicated.
 *
 * Reads the live back stack rather than composed state, so two taps inside one frame behave.
 */
private fun NavController.navigateToTab(tab: AppTab) {
    // Re-selecting the current tab would pop it with saveState and immediately restore it:
    // a full teardown and rebuild of the screen plus a crossfade into itself.
    if (AppTab.forRoute(currentDestination?.route) == tab) return

    // Drop screens stacked above the tabs (Player, Tutorial) without saving them. Otherwise
    // saveState files them under the tab below, and returning to that tab later reopens the
    // Player — with no bottom bar and no obvious way back.
    while (AppTab.forRoute(currentDestination?.route) == null && previousBackStackEntry != null) {
        popBackStack()
    }
    if (AppTab.forRoute(currentDestination?.route) == tab) return

    navigate(tab.route) {
        // By route, not graph.findStartDestination(): after first-run onboarding the graph's
        // start is ONBOARDING, which is no longer on the stack, so popUpTo would silently do
        // nothing and tabs would pile up (Home → Library → Presets → Library…).
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState    = true
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom navigation bar
// ─────────────────────────────────────────────────────────────────────────────

/** Bar height above the system navigation inset: vertical padding + icon + gaps + indicator. */
private val TabBarFixedHeight = 64.dp

/** Full bar content height. The label line follows font scale, so it is added in sp → dp. */
@Composable
private fun tabBarContentHeight(): Dp {
    val lineHeight = MaterialTheme.typography.labelSmall.lineHeight
    val density = LocalDensity.current
    return remember(lineHeight, density) { with(density) { TabBarFixedHeight + lineHeight.toDp() } }
}

/**
 * A tab destination: reserves room for the floating bar and consumes the bottom system inset,
 * so the screen's own navigationBars spacers and Scaffold insets don't pad a second time.
 * Static per destination — the space stays put while the bar slides, so nothing reflows.
 */
@Composable
private fun TabPage(content: @Composable () -> Unit) {
    val barHeight = tabBarContentHeight()
    val navBars = WindowInsets.navigationBars
    val insets = remember(navBars, barHeight) {
        navBars.only(WindowInsetsSides.Bottom).add(WindowInsets(bottom = barHeight))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(insets),
    ) {
        content()
    }
}

/** Mutable, non-snapshot memory of the last tab shown — never read to trigger recomposition. */
private class LastTab(var tab: AppTab)

/**
 * Owns the back stack observation so a navigation recomposes only the bar, not the NavHost.
 */
@Composable
private fun TabBarHost(navController: NavHostController, modifier: Modifier) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = AppTab.forRoute(backStackEntry?.destination?.route)

    // While the bar slides away under a pushed screen, keep the tab it came from highlighted
    // instead of flashing to "nothing selected".
    val lastTab = remember { LastTab(AppTab.HOME) }
    val barTab = currentTab?.also { lastTab.tab = it } ?: lastTab.tab

    AnimatedVisibility(
        visible  = currentTab != null,
        modifier = modifier,
        enter    = slideInVertically(tween(300)) { it },
        exit     = slideOutVertically(tween(250)) { it },
    ) {
        SleepSlideBottomBar(
            selected = barTab,
            onSelect = { tab -> navController.navigateToTab(tab) },
        )
    }
}

@Composable
private fun SleepSlideBottomBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(WarmWhite),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(tabBarContentHeight())
                .padding(horizontal = 8.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            AppTab.entries.forEach { tab ->
                TabButton(
                    tab      = tab,
                    selected = tab == selected,
                    onSelect = onSelect,
                )
            }
        }

        // Top border line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Border),
        )
    }
}

@Composable
private fun TabButton(
    tab: AppTab,
    selected: Boolean,
    onSelect: (AppTab) -> Unit,
) {
    val tint = if (selected) Charcoal else MutedGray

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            // selectable + Role.Tab: TalkBack announces "Library, tab, selected, 2 of 4"
            // from the label below — no separate contentDescription to read twice.
            .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab) })
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter            = painterResource(tab.icon),
            contentDescription = null,   // the label names the tab
            tint               = tint,
            modifier           = Modifier.size(22.dp),
        )

        Text(
            text     = tab.label,
            style    = MaterialTheme.typography.labelSmall,
            color    = tint,
            maxLines = 1,
        )

        // Active indicator — always laid out so selecting a tab never shifts the column
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 2.dp)
                .drawBehind {
                    if (selected) drawRoundRect(Charcoal, cornerRadius = CornerRadius(1.dp.toPx()))
                },
        )
    }
}
