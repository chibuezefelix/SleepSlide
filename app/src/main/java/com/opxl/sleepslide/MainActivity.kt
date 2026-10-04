package com.opxl.sleepslide

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.opxl.sleepslide.data.AudioServiceHolder
import com.opxl.sleepslide.data.audio.AudioServiceImpl
import com.opxl.sleepslide.data.purchase.PurchaseServiceImpl
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.repository.UserPreferencesRepository
import com.opxl.sleepslide.domain.service.PlaybackGate
import com.opxl.sleepslide.presentation.navigation.NavGraph
import com.opxl.sleepslide.presentation.navigation.UpgradeRequests
import com.opxl.sleepslide.presentation.permission.LocalNotificationPermissionRequester
import com.opxl.sleepslide.presentation.permission.NotificationPermissionRequester
import com.opxl.sleepslide.presentation.permission.isNotificationPermissionGranted
import com.opxl.sleepslide.ui.theme.SleepSlideTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var audioServiceHolder: AudioServiceHolder
    @Inject lateinit var purchaseService: PurchaseServiceImpl
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var playbackGate: PlaybackGate
    @Inject lateinit var upgradeRequests: UpgradeRequests


    private var isAudioServiceBound = false

    private val audioServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val service = (binder as AudioServiceImpl.LocalBinder).getService()
            audioServiceHolder.attach(service)
            isAudioServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            // System killed the service (OOM etc.) — detach and rebind on next start
            audioServiceHolder.detach()
            isAudioServiceBound = false
        }

        override fun onBindingDied(name: ComponentName) {
            // Binding died — unbind cleanly and rebind
            unbindAudioServiceSafely()
            bindAudioService()
        }

        override fun onNullBinding(name: ComponentName) {
            // Should never happen with LocalBinder but handle defensively
            audioServiceHolder.detach()
            isAudioServiceBound = false
        }
    }

    /**
     * Single POST_NOTIFICATIONS launcher for the whole app. Screens ask through
     * [LocalNotificationPermissionRequester]; the result is handed back to whichever
     * screen asked. If the Activity is recreated while the system dialog is up the
     * pending callback is lost, which is fine — screens re-check on entry.
     */
    private var pendingNotificationPermissionResult: ((Boolean) -> Unit)? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Denied — audio still plays but the media notification won't show.
        // We do not re-ask; the requesting screen records the user's choice.
        pendingNotificationPermissionResult?.invoke(granted)
        pendingNotificationPermissionResult = null
    }

    private val notificationPermissionRequester = NotificationPermissionRequester { onResult ->
        if (isNotificationPermissionGranted()) {
            onResult(true)
        } else {
            pendingNotificationPermissionResult = onResult
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, AudioServiceImpl::class.java).also {
            bindService(it, audioServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        // Keep splash visible until preferences are loaded for the first time
        var preferencesReady = false
        splashScreen.setKeepOnScreenCondition { !preferencesReady }
        // Fade the system splash out (mark lifting slightly) into the in-app BrandSplash.
        splashScreen.setOnExitAnimationListener { provider ->
            provider.iconView.animate().scaleX(1.15f).scaleY(1.15f).setDuration(300L).start()
            provider.view.animate()
                .alpha(0f)
                .setDuration(300L)
                .withEndAction { provider.remove() }
                .start()
        }
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // The Settings theme choice drives the Compose theme; SYSTEM and the fallback
            // before prefs load follow the device. The hour ticks so SCHEDULED flips on time.
            val prefs by userPreferencesRepository.observe().collectAsStateWithLifecycle(initialValue = null)
            val systemDark = isSystemInDarkTheme()
            val hour by produceState(currentHour()) {
                while (true) {
                    delay(60_000L.milliseconds)
                    value = currentHour()
                }
            }
            val darkTheme = prefs?.let {
                resolveIsDark(it.themeMode, it.darkModeStartHour, it.darkModeEndHour, systemDark, hour)
            } ?: systemDark
            LaunchedEffect(darkTheme) { applyStatusBarAppearance(darkTheme) }

            SleepSlideTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalNotificationPermissionRequester provides notificationPermissionRequester,
                ) {
                    NavGraph(
                        userPreferencesRepository = userPreferencesRepository,
                        playbackGate              = playbackGate,
                        upgradeRequests           = upgradeRequests,
                    )
                }
            }
        }

        // Start observing after setContent so flows have collectors
        observeAudioStateForWindowFlags()
        observeBatteryOptimisationPrompt()

        lifecycleScope.launch {
            userPreferencesRepository.observe().collect {
                preferencesReady = true
            }
        }
    }


    override fun onResume() {
        super.onResume()
        // WeakReference — safe to rebind every resume; PurchaseService guards against null activity
        purchaseService.bindActivity(this)
    }
    override fun onPause() {
        // Unbind purchase activity reference before going to background
        purchaseService.unbindActivity()
        super.onPause()
    }

    override fun onStop() {
        unbindAudioServiceSafely()
        super.onStop()
    }

    override fun onDestroy() {
        // Final safety net — clear window flags
        clearKeepScreenOn()
        super.onDestroy()
    }

    /**
     * Start as foreground service first — ensures the service survives even if
     * binding is dropped. Binding gives us the [AudioServiceImpl] reference.
     */
    private fun startAndBindAudioService() {
        val intent = Intent(this, AudioServiceImpl::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindAudioService()
    }

    private fun bindAudioService() {
        if (isAudioServiceBound) return
        val intent = Intent(this, AudioServiceImpl::class.java)
        runCatching {
            bindService(intent, audioServiceConnection, Context.BIND_AUTO_CREATE)
        }.onFailure {
            // Defensive — if bind fails, holder stays null and ViewModels handle gracefully
            audioServiceHolder.detach()
        }
    }

    private fun unbindAudioServiceSafely() {
        if (!isAudioServiceBound) return
        isAudioServiceBound = false
        audioServiceHolder.detach()
        runCatching { unbindService(audioServiceConnection) }
    }

    private fun observeAudioStateForWindowFlags() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                audioServiceHolder.service.collectLatest { service ->
                    if (service == null) return@collectLatest
                    combine(
                        service.audioState.map { it.isNightLockEnabled }.distinctUntilChanged(),
                        service.audioState.map { it.playbackStatus }.distinctUntilChanged(),
                    ) { nightLock, status ->
                        nightLock && status == Domain.PlaybackStatus.PLAYING
                    }.distinctUntilChanged().collectLatest { keepOn ->
                        if (keepOn) setKeepScreenOn() else clearKeepScreenOn()
                    }
                }
            }
        }
    }


    /**
     * Battery optimisation prompt — shown once, politely, never more than once.
     * Most apps either never ask (audio dies at 3AM) or spam the user.
     * We check if we're already excluded before prompting.
     */
    private fun observeBatteryOptimisationPrompt() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                userPreferencesRepository.observe()
                    .map {
                        !it.hasDismissedBatteryOptPrompt && it.batteryOptPromptShownCount == 0
                    }
                    .distinctUntilChanged()
                    .collectLatest { shouldPrompt ->
                        if (shouldPrompt && !isIgnoringBatteryOptimisations()) {
                            userPreferencesRepository.recordBatteryOptPromptShown()
                            showBatteryOptimisationPrompt()
                        }
                    }
            }
        }
    }
    private fun isIgnoringBatteryOptimisations(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }



    private fun setKeepScreenOn() {
        runCatching {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun clearKeepScreenOn() {
        runCatching {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun applyStatusBarAppearance(isDark: Boolean) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        // Light appearance = dark icons (for light backgrounds)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark
    }

    private fun showBatteryOptimisationPrompt() {
        runCatching {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }.onFailure {
            // Device does not support this intent (some OEMs) — fall back to app battery settings
            runCatching {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
        }
    }

    private fun resolveIsDark(
        mode: Domain.ThemeMode,
        darkStartHour: Int,
        darkEndHour: Int,
        systemDark: Boolean,
        hour: Int,
    ): Boolean = when (mode) {
        Domain.ThemeMode.DARK   -> true
        Domain.ThemeMode.LIGHT  -> false
        Domain.ThemeMode.SYSTEM -> systemDark
        Domain.ThemeMode.SCHEDULED ->
            if (darkStartHour > darkEndHour) {
                // Overnight window e.g. 21:00 → 07:00
                hour >= darkStartHour || hour < darkEndHour
            } else {
                hour in darkStartHour until darkEndHour
            }
    }

    private fun currentHour(): Int =
        java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

}
