package com.opxl.sleepslide.data.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.opxl.sleepslide.data.AudioServiceHolder
import com.opxl.sleepslide.di.ApplicationScope
import com.opxl.sleepslide.domain.model.Domain
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires at sleep-timer end + grace. Reaching the stop here means the in-service stop
 * ([SleepTimerController]) did not run — it is logged as a warning so it gets noticed.
 * If the process died, the holder is empty and there is nothing playing to stop.
 */
@AndroidEntryPoint
class TimerBackstopReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TOKEN  = "timer_token"
        const val REQUEST_CODE = 4201
        private const val TAG  = "TimerBackstop"
        private const val FADE_MS = 5_000L
    }

    @Inject lateinit var timerService: TimerServiceImpl
    @Inject lateinit var audioServiceHolder: AudioServiceHolder
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val token = intent.getLongExtra(EXTRA_TOKEN, -1L)
        val pending = goAsync()
        scope.launch(Dispatchers.Main) {
            try {
                if (!timerService.onBackstopFired(token)) return@launch
                val service = audioServiceHolder.current ?: return@launch
                if (service.audioState.value.playbackStatus == Domain.PlaybackStatus.IDLE) return@launch

                Log.w(TAG, "Sleep timer ended but playback was still running — stopping")
                runCatching {
                    service.fadeOut(FADE_MS)
                    service.stop()
                }.onFailure { Log.e(TAG, "Backstop stop failed", it) }
            } finally {
                pending.finish()
            }
        }
    }
}
