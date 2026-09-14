package com.opxl.sleepslide
import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.opxl.sleepslide.data.repository.PlayHistoryRepositoryImpl
import com.opxl.sleepslide.data.repository.SoundRepositoryImpl
import com.opxl.sleepslide.data.observer.PlaySessionTracker
import com.opxl.sleepslide.data.purchase.PurchaseServiceImpl
import com.opxl.sleepslide.di.ApplicationScope
import com.opxl.sleepslide.domain.model.Domain
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@HiltAndroidApp
class SleepSlideApp : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "SleepSlideApp"
        private val HISTORY_PRUNE_THRESHOLD_MS = TimeUnit.DAYS.toMillis(90)
    }

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var soundRepository: SoundRepositoryImpl
    @Inject lateinit var playHistoryRepository: PlayHistoryRepositoryImpl
    @Inject lateinit var purchaseService: PurchaseServiceImpl
    @Inject lateinit var playSessionTracker: PlaySessionTracker
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope
    @Inject lateinit var windDownRepository: com.opxl.sleepslide.domain.repository.WindDownRepository
    @Inject lateinit var windDownNotificationService: com.opxl.sleepslide.domain.service.WindDownNotificationService
    @Inject lateinit var userPreferencesRepository: com.opxl.sleepslide.domain.repository.UserPreferencesRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()

    override fun onCreate() {
        super.onCreate()
        // RevenueCat is configured by PurchaseModule.providePurchases() during Hilt
        // injection above — it must exist before PurchaseService is constructed.
        playSessionTracker.start()
        runStartupSequence()
    }


    // Startup sequence tasks that can run concurrently and don't block the main thread
    /**
     * All startup work runs on the ApplicationScope (DefaultDispatcher — background).
     * Each task is wrapped independently so a failure in one never blocks another.
     * Order matters: seed first so the DB is populated before any query runs.
     */
    private fun runStartupSequence() {
        appScope.launch { seedSoundsIfNeeded() }
        appScope.launch { closeStaleAudioSessions() }
        appScope.launch { pruneOldHistory() }
        appScope.launch { syncEntitlement() }
        appScope.launch { cleanOrphanedDownloads() }
        appScope.launch { syncWindDownCache() }
    }


    private suspend fun seedSoundsIfNeeded() {
        runCatching {
            soundRepository.seedBundledSounds()
        }.onFailure { e ->
            Log.e(TAG, "Sound seeding failed", e)
        }
    }

    /**
     * handle force-killed mid-session
     */
    private suspend fun closeStaleAudioSessions() {
        runCatching {
            playHistoryRepository.closeAnyActiveSessions(
                Domain.StopReason.ERROR
            )
        }.onFailure { e ->
            Log.e(TAG, "Stale session close failed", e)
        }
    }

    private suspend fun pruneOldHistory() {
        runCatching {
            val threshold = System.currentTimeMillis() - HISTORY_PRUNE_THRESHOLD_MS
            playHistoryRepository.pruneOlderThan(threshold)
        }.onFailure { e ->
            Log.e(TAG, "History pruning failed", e)
        }
    }

    private suspend fun syncEntitlement() {
        runCatching {
            purchaseService.refresh()
        }.onFailure { e ->
            Log.e(TAG, "Entitlement sync failed — continuing with cached tier", e)
        }
    }


    private suspend fun cleanOrphanedDownloads() {
        runCatching {
            soundRepository.deleteOrphanedDownloads()
        }.onFailure { e ->
            Log.e(TAG, "Orphaned download cleanup failed", e)
        }
    }

    /**
     * Refreshes the lightweight SharedPreferences cache that [BootReceiver]
     * and [WindDownAlarmReceiver] read synchronously without coroutines.
     *
     * This must run on every cold start because:
     * 1. The user may have changed wind-down settings in a previous session.
     * 2. DataStore is the source of truth but is async — receivers cannot
     *    use it from onReceive without goAsync() complexity.
     * 3. The cache bridges the gap: DataStore → SharedPreferences → Receiver.
     *
     * Also re-schedules the alarm if wind-down is enabled and no alarm is
     * currently pending. This covers the edge case where the alarm was lost
     * due to an app update, a forced-stop, or a permission revocation that
     * happened between sessions.
     */
    private suspend fun syncWindDownCache() {
        runCatching {
            val config = windDownRepository.get()
            val prefs  = userPreferencesRepository.get()

            // Refresh the SharedPreferences cache — always, regardless of enabled state
            (windDownNotificationService
                    as? com.opxl.sleepslide.data.notification.WindDownNotificationServiceImpl)
                ?.writeCache(
                    enabled = config.isEnabled,
                    hour    = config.hour,
                    minute  = config.minute,
                    path    = prefs.onboardingPath,
                )

            // Re-schedule if enabled but alarm is not currently pending
            // (handles app updates, force-stop, and permission revocation)
            if (config.isEnabled && !windDownNotificationService.isScheduled()) {
                Log.d(TAG, "Wind-down enabled but alarm missing — re-scheduling")
                windDownNotificationService.scheduleDailyReminder(config.hour, config.minute)
            }
        }.onFailure { e ->
            Log.e(TAG, "Wind-down cache sync failed", e)
        }
    }
}