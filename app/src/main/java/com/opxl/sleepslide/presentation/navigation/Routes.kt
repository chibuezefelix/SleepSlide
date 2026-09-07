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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
import com.opxl.sleepslide.presentation.player.PlayerScreen
import com.opxl.sleepslide.presentation.presets.PresetsScreen
import com.opxl.sleepslide.presentation.settings.SettingsScreen


object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME       = "home"
    const val PLAYER     = "player"
    const val LIBRARY    = "library"
    const val PRESETS    = "presets"
    const val SETTINGS   = "settings"

    // Deep link base — matches notification tap to open player
    const val DEEP_LINK_BASE = "sleepslide://app"
    const val PLAYER_DEEP_LINK = "$DEEP_LINK_BASE/player"
    const val PRESETS_DEEP_LINK = "$DEEP_LINK_BASE/presets"
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

    NavHost(
        navController    = navController,
        startDestination = startDestination,
        enterTransition  = { enterTransition },
        exitTransition   = { exitTransition },
        popEnterTransition = { popEnter },
        popExitTransition  = { popExit },
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
            PlayerScreen(
                onNavigateBack    = { navController.popBackStack() },
                onNavigateToLibrary = {
                    navController.navigate(Routes.LIBRARY) {
                        // Keep player on back stack — user returns to it from library
                        launchSingleTop = true
                    }
                },
                onRequestNotificationPermission = {
                    // Delegate to MainActivity via a side-effect — MainActivity owns the launcher
                },
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
            )
        }
    }
}