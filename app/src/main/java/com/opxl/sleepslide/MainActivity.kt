package com.opxl.sleepslide

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.opxl.sleepslide.data.AudioServiceHolder
import com.opxl.sleepslide.data.audio.AudioServiceImpl
import com.opxl.sleepslide.data.purchase.PurchaseServiceImpl
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.repository.UserPreferencesRepository
import com.opxl.sleepslide.ui.theme.SleepSlideTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var audioServiceHolder: AudioServiceHolder
    @Inject lateinit var purchaseService: PurchaseServiceImpl
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository


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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // User denied — audio will still play but notification won't show.
            // We do not re-ask; the user made a deliberate choice.
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, AudioServiceImpl::class.java).also {
            bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        // Keep splash visible until preferences are loaded for the first time
        var preferencesReady = false
        splashScreen.setKeepOnScreenCondition { !preferencesReady }
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SleepSlideTheme {
//                NavGraph()
            }
        }

        requestNotificationPermissionIfNeeded()

        // Start observing after setContent so flows have collectors
        observeThemeAndNightLock()
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

    /**
     * Observes two concerns in one flow:
     * 1. Dark/light status bar icons tracking the user's theme choice
     * 2. keepScreenOn when night-lock is active — prevents accidental taps but
     *    also means we must NOT dim the screen — that is the system's job via
     *    the ambient display / proximity sensor.
     */
    private fun observeThemeAndNightLock() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                userPreferencesRepository.observe()
                    .distinctUntilChanged { old, new ->
                        old.themeMode == new.themeMode &&
                                old.isNightLockEnabledByDefault == new.isNightLockEnabledByDefault
                    }
                    .collectLatest { prefs ->
                        val isDark = resolveIsDark(prefs.themeMode, prefs.darkModeStartHour, prefs.darkModeEndHour)
                        applyStatusBarAppearance(isDark)
                    }
            }
        }
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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

    private fun resolveIsDark(
        mode: Domain.ThemeMode,
        darkStartHour: Int,
        darkEndHour: Int,
    ): Boolean = when (mode) {
        Domain.ThemeMode.DARK   -> true
        Domain.ThemeMode.LIGHT  -> false
        Domain.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        Domain.ThemeMode.SCHEDULED -> {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            if (darkStartHour > darkEndHour) {
                // Overnight window e.g. 21:00 → 07:00
                hour >= darkStartHour || hour < darkEndHour
            } else {
                hour in darkStartHour until darkEndHour
            }
        }
    }

    private fun isSystemInDarkTheme(): Boolean {
        val uiMode = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

}
