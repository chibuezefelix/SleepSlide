package com.opxl.sleepslide.presentation.navigation


import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
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
import com.opxl.sleepslide.ui.theme.SurfaceMuted
import com.opxl.sleepslide.ui.theme.White


object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME       = "home"
    const val PLAYER     = "player"
    const val LIBRARY    = "library"
    const val PRESETS    = "presets"
    const val SETTINGS   = "settings"
    const val TUTORIAL   = "tutorial"

    // Deep link base — matches notification tap to open player
    const val DEEP_LINK_BASE = "sleepslide://app"
    const val PLAYER_DEEP_LINK = "$DEEP_LINK_BASE/player"
    const val PRESETS_DEEP_LINK = "$DEEP_LINK_BASE/presets"
}

/** Top-level destinations reachable from the bottom bar. Player and Onboarding are not tabs. */
private enum class AppTab(val route: String, val label: String) {
    HOME(Routes.HOME,         "Home"),
    PRESETS(Routes.PRESETS,   "Presets"),
    LIBRARY(Routes.LIBRARY,   "Library"),
    SETTINGS(Routes.SETTINGS, "Settings");

    companion object {
        fun forRoute(route: String?): AppTab? = entries.firstOrNull { it.route == route }
    }
}

// Glyphs drawn like the rest of the app's icons (stroked paths, no icon library).
@Composable
private fun TabGlyph(tab: AppTab, tint: Color) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width; val h = size.height
        val stroke = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (tab) {
            AppTab.HOME -> {
                // roof + walls + door
                drawPath(Path().apply {
                    moveTo(w * 0.10f, h * 0.50f); lineTo(w * 0.50f, h * 0.14f); lineTo(w * 0.90f, h * 0.50f)
                    moveTo(w * 0.22f, h * 0.42f); lineTo(w * 0.22f, h * 0.88f); lineTo(w * 0.78f, h * 0.88f); lineTo(w * 0.78f, h * 0.42f)
                    moveTo(w * 0.42f, h * 0.88f); lineTo(w * 0.42f, h * 0.62f); lineTo(w * 0.58f, h * 0.62f); lineTo(w * 0.58f, h * 0.88f)
                }, tint, style = stroke)
            }
            AppTab.PRESETS -> {
                // five-point star
                val cx = w / 2f; val cy = h * 0.54f; val outer = w * 0.42f; val inner = outer * 0.45f
                drawPath(Path().apply {
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) outer else inner
                        val a = Math.toRadians((-90 + i * 36).toDouble())
                        val x = cx + (r * Math.cos(a)).toFloat(); val y = cy + (r * Math.sin(a)).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }, tint, style = stroke)
            }
            AppTab.LIBRARY -> {
                // three list rows with bullets
                for (i in 0..2) {
                    val y = h * (0.24f + i * 0.26f)
                    drawCircle(tint, radius = 1.6.dp.toPx(), center = Offset(w * 0.16f, y))
                    drawLine(tint, Offset(w * 0.34f, y), Offset(w * 0.88f, y), stroke.width, StrokeCap.Round)
                }
            }
            AppTab.SETTINGS -> {
                // three sliders with knobs at different positions
                val knobs = listOf(0.62f, 0.34f, 0.72f)
                for (i in 0..2) {
                    val y = h * (0.24f + i * 0.26f)
                    drawLine(tint, Offset(w * 0.12f, y), Offset(w * 0.88f, y), stroke.width, StrokeCap.Round)
                    drawCircle(tint, radius = 2.6.dp.toPx(), center = Offset(w * knobs[i], y))
                }
            }
        }
    }
}

@Composable
private fun SleepSlideBottomBar(current: AppTab, onSelect: (AppTab) -> Unit) {
    NavigationBar(
        containerColor = White,
        tonalElevation = 0.dp,
    ) {
        AppTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == current,
                onClick  = { onSelect(tab) },
                icon     = { TabGlyph(tab, tint = if (tab == current) Charcoal else MutedGray) },
                label    = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                colors   = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Charcoal,
                    selectedTextColor   = Charcoal,
                    indicatorColor      = SurfaceMuted,
                    unselectedIconColor = MutedGray,
                    unselectedTextColor = MutedGray,
                ),
            )
        }
    }
}


private val enterTransition = slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300))
private val exitTransition  = slideOutHorizontally(tween(250)) { -it / 4 } + fadeOut(tween(250))
private val popEnter        = slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300))
private val popExit         = slideOutHorizontally(tween(250)) { it / 4 } + fadeOut(tween(250))


@Composable
fun NavGraph(
    userPreferencesRepository: UserPreferencesRepository,
    navController: NavHostController = rememberNavController(),
) {
    val prefs by userPreferencesRepository.observe()
        .collectAsStateWithLifecycle(
            initialValue = null
        )

    // Determine start destination — null prefs means preferences not yet loaded
    // Hold start destination until first prefs emission to avoid flash to wrong route
    val startDestination = when {
        prefs == null                        -> null // still loading
        !prefs!!.hasCompletedOnboarding      -> Routes.ONBOARDING
        else                                 -> Routes.HOME
    } ?: return // Do not compose NavHost until we know the start destination

    val context = LocalContext.current

    // Battery optimisation settings launcher — shared across all screens that need it
    val batterySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* result delivered via ViewModel.refreshBatteryOptStatus() */ }

    fun openBatterySettings() {
        runCatching {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            batterySettingsLauncher.launch(intent)
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

    // Bottom bar only on top-level screens; the Player is a detail screen and Onboarding is modal.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = AppTab.forRoute(backStackEntry?.destination?.route)

    fun openTab(tab: AppTab) {
        if (tab == currentTab) return
        navController.navigate(tab.route) {
            // One instance per tab, state kept when switching, back always returns to Home
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (currentTab != null) SleepSlideBottomBar(current = currentTab, onSelect = ::openTab)
        },
    ) { outerPadding ->
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        enterTransition  = { enterTransition },
        exitTransition   = { exitTransition },
        popEnterTransition = { popEnter },
        popExitTransition  = { popExit },
        modifier = Modifier.padding(bottom = outerPadding.calculateBottomPadding()),
    ) {

        composable(route = Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.HOME) {
                        // Remove onboarding from back stack — back press from home exits app
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
                onOpenBatterySettings = { openBatterySettings() },
            )
        }

        composable(route = Routes.HOME) {
            HomeScreen(
                onNavigateToPlayer  = { navController.navigate(Routes.PLAYER) },
                onNavigateToLibrary = { navController.navigate(Routes.LIBRARY) },
                onNavigateToPreset  = { presetId ->
                    navController.navigate("${Routes.PLAYER}?presetId=$presetId")
                },
            )
        }

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
        ) {
            // MainActivity owns the POST_NOTIFICATIONS launcher and provides it via CompositionLocal
            val notificationPermissionRequester = LocalNotificationPermissionRequester.current
            PlayerScreen(
                onNavigateBack    = { navController.popBackStack() },
                onNavigateToLibrary = {
                    navController.navigate(Routes.LIBRARY) {
                        // Keep player on back stack — user returns to it from library
                        launchSingleTop = true
                    }
                },
                onRequestNotificationPermission = notificationPermissionRequester::request,
            )
        }

        composable(route = Routes.LIBRARY) {
            LibraryScreen(
                onNavigateBack    = { navController.popBackStack() },
                onNavigateToPlayer = {
                    navController.navigate(Routes.PLAYER) {
                        // If player is already on the stack, bring it to the front
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(route = Routes.PRESETS) {
            PresetsScreen(
                onNavigateBack    = { navController.popBackStack() },
                onNavigateToPlayer = { presetId ->
                    navController.navigate("${Routes.PLAYER}?presetId=$presetId") {
                        launchSingleTop = true
                    }
                },
                onNavigateToLibrary = {
                    navController.navigate(Routes.LIBRARY) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(route = Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack       = { navController.popBackStack() },
                onNavigateToOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        // Clear entire back stack — onboarding retake is a fresh start
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onOpenBatterySettings = { openBatterySettings() },
                onNavigateToTutorial = { navController.navigate(Routes.TUTORIAL) { launchSingleTop = true } },
            )
        }

        // Feature tour — a sheet, so it rises from the bottom instead of sliding in sideways
        composable(
            route              = Routes.TUTORIAL,
            enterTransition    = { slideInVertically(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition     = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition  = { slideOutVertically(tween(250)) { it } + fadeOut(tween(250)) },
        ) {
            TutorialScreen(onClose = { navController.popBackStack() })
        }
    }
    } // Scaffold
}
