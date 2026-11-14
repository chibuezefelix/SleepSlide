package com.opxl.sleepslide.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.opxl.sleepslide.domain.service.WindDownNotificationService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The system sends this when the user grants or revokes "Alarms & reminders" while
 * the app is closed. Re-schedules the wind-down reminder so it uses an exact alarm
 * when allowed and the inexact fallback otherwise. Reads the same
 * `wind_down_cache` as [BootReceiver].
 */
@AndroidEntryPoint
class ExactAlarmPermissionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var windDownNotificationService: WindDownNotificationService

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        runCatching {
            val prefs = context.getSharedPreferences("wind_down_cache", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("enabled", false)) return
            windDownNotificationService.scheduleDailyReminder(
                prefs.getInt("hour", 21),
                prefs.getInt("minute", 30),
            )
            Log.d("ExactAlarmReceiver", "Wind-down alarm re-scheduled after permission change")
        }.onFailure { e ->
            Log.e("ExactAlarmReceiver", "Failed to re-schedule after permission change", e)
        }
    }

    private companion object {
        const val ACTION = "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}
