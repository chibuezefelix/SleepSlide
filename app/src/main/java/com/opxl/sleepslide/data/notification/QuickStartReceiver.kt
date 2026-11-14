package com.opxl.sleepslide.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.opxl.sleepslide.di.ApplicationScope
import com.opxl.sleepslide.domain.service.QuickStartService
import com.opxl.sleepslide.domain.service.WindDownNotificationService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

private const val TAG = "QuickStartReceiver"

// goAsync() allows about 10 s; stay well inside it.
private const val BUDGET_MS = 8_000L

/**
 * The reminder's "Start now" button. Starts the user's usual night without opening the app.
 *
 * Tapping a notification action is one of the cases Android allows to start a foreground
 * service from the background. Not exported: only our own PendingIntent reaches it.
 */
@AndroidEntryPoint
class QuickStartReceiver : BroadcastReceiver() {

    @Inject lateinit var quickStart: QuickStartService
    @Inject lateinit var windDown: WindDownNotificationService
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                val outcome = runCatching {
                    withTimeout(BUDGET_MS) {
                        quickStart.start(QuickStartService.Options(maxAgeMs = null, applyTimer = true))
                    }
                }.getOrElse {
                    Log.e(TAG, "Quick start failed", it)
                    QuickStartService.Outcome.Failed(it.message ?: "Quick start failed")
                }
                Log.d(TAG, "Outcome: $outcome")
                windDown.onQuickStartResult(outcome)
            } finally {
                pending.finish()
            }
        }
    }
}
