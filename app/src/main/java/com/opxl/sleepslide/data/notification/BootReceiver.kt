package com.opxl.sleepslide.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.opxl.sleepslide.domain.service.WindDownNotificationService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Re-registers the daily wind-down alarm after BOOT_COMPLETED.
 *
 * AlarmManager alarms are wiped on device reboot. This receiver restores
 * the alarm from the lightweight SharedPreferences cache written by
 * [WindDownNotificationServiceImpl.writeCache]. DataStore is not used here
 * because reading it requires a coroutine scope, which is not available in
 * a BroadcastReceiver without goAsync() — the SharedPreferences cache is
 * synchronous and purpose-built for this scenario.
 *
 * Declared in AndroidManifest.xml with:
 *   android:exported="true"
 *   <intent-filter>
 *     <action android:name="android.intent.action.BOOT_COMPLETED"/>
 *   </intent-filter>
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var windDownNotificationService: WindDownNotificationService

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d("BootReceiver", "Boot completed — checking wind-down schedule")

        runCatching {
            val prefs = context
                .getSharedPreferences("wind_down_cache", Context.MODE_PRIVATE)

            val enabled = prefs.getBoolean("enabled", false)
            if (!enabled) {
                Log.d("BootReceiver", "Wind-down disabled — skipping re-schedule")
                return
            }

            val hour   = prefs.getInt("hour",   21)
            val minute = prefs.getInt("minute", 30)

            windDownNotificationService.scheduleDailyReminder(hour, minute)
            Log.d("BootReceiver", "Wind-down alarm re-scheduled for $hour:$minute")
        }.onFailure { e ->
            Log.e("BootReceiver", "Failed to re-schedule wind-down alarm after boot", e)
        }
    }
}