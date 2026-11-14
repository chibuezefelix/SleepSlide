package com.opxl.sleepslide.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.opxl.sleepslide.domain.service.WindDownNotificationService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives the daily AlarmManager broadcast and shows the wind-down notification.
 *
 * Annotated with @AndroidEntryPoint so Hilt can inject into a BroadcastReceiver.
 * [WindDownNotificationServiceImpl.showScheduledNotification] also re-schedules
 * the alarm for the next day since AlarmManager is one-shot, not repeating.
 *
 * Declared in AndroidManifest.xml with android:exported="false" — only the
 * AlarmManager within this process can trigger it.
 */
@AndroidEntryPoint
class WindDownAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var windDownNotificationService: WindDownNotificationService

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WindDownAlarmReceiver", "Alarm received — firing wind-down notification")
        runCatching {
            // Cast to impl to access the internal showScheduledNotification which
            // both shows the notification AND re-schedules for tomorrow.
            (windDownNotificationService as WindDownNotificationServiceImpl)
                .showScheduledNotification()
        }.onFailure { e ->
            Log.e("WindDownAlarmReceiver", "Failed to show notification", e)
        }
    }
}