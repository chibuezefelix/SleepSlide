package com.opxl.sleepslide
import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.opxl.sleepslide.data.repository.PlayHistoryRepositoryImpl
import com.opxl.sleepslide.data.repository.SoundRepositoryImpl
import com.opxl.sleepslide.data.purchase.PurchaseServiceImpl
import com.opxl.sleepslide.di.ApplicationScope
import com.opxl.sleepslide.domain.model.Domain
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
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
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope



    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()

    override fun onCreate() {
        super.onCreate()
        initRevenueCat()
        runStartupSequence()
    }


    private fun initRevenueCat() {
        runCatching {
            if (BuildConfig.DEBUG) {
                Purchases.logLevel = LogLevel.DEBUG
            }
            Purchases.configure(
                PurchasesConfiguration.Builder(
                    context = this,
                    apiKey  = BuildConfig.REVENUECAT_KEY,
                ).build()
            )
        }.onFailure { e ->
            Log.e(TAG, "RevenueCat init failed — app continues in free tier", e)
        }
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
}