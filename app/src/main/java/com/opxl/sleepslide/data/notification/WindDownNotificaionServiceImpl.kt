package com.opxl.sleepslide.data.notification

import com.opxl.sleepslide.domain.model.Domain



import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.opxl.sleepslide.R

import com.opxl.sleepslide.domain.service.WindDownNotificationService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG                   = "WindDownService"
private const val CHANNEL_ID            = "wind_down_reminder"
private const val CHANNEL_NAME          = "Sleep & Wind-Down"
private const val NOTIFICATION_ID       = 9_001
private const val REQUEST_CODE_DAILY    = 9_002
private const val REQUEST_CODE_IMMEDIATE = 9_003
private const val DEEP_LINK_URI = "sleepslide://app/presets?autoPlay=false"

@Singleton
class WindDownNotificationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : WindDownNotificationService {

    private val alarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        ensureChannelCreated()
    }


    override fun scheduleDailyReminder(hour: Int, minute: Int) {
        if (!canScheduleExactAlarms()) {
            Log.w(TAG, "SCHEDULE_EXACT_ALARM not granted — falling back to inexact alarm")
        }

        val triggerAtMs = nextOccurrenceMs(hour, minute)
        val pendingIntent = buildDailyPendingIntent()

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent,
                )
            } else {
                // Fallback — may drift by ~10 min but never crashes
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent,
                )
            }
            Log.d(TAG, "Wind-down alarm scheduled for $hour:$minute (next: ${java.util.Date(triggerAtMs)})")
        }.onFailure { e ->
            Log.e(TAG, "Failed to schedule wind-down alarm", e)
        }
    }

    override fun cancelDailyReminder() {
        runCatching {
            alarmManager.cancel(buildDailyPendingIntent())
            Log.d(TAG, "Wind-down alarm cancelled")
        }.onFailure { e ->
            Log.e(TAG, "Failed to cancel wind-down alarm", e)
        }
    }

    override fun isScheduled(): Boolean = runCatching {
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY,
            Intent(context, WindDownAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) != null
    }.getOrDefault(false)

    // ── Show notification ─────────────────────────────────────────────────────

    override fun showImmediateNotification() {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted — skipping notification")
            return
        }
        if (!notificationManager.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled at system level — skipping")
            return
        }
        buildAndShowNotification(requestCode = REQUEST_CODE_IMMEDIATE)
    }

    internal fun showScheduledNotification() {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted — skipping scheduled notification")
            return
        }
        if (!notificationManager.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled at system level — skipping")
            return
        }

        buildAndShowNotification(requestCode = REQUEST_CODE_DAILY)

        // Re-schedule for the next day — AlarmManager one-shot, not repeating
        runCatching {
            val prefs = readStoredWindDownPrefs()
            if (prefs != null && prefs.isEnabled) {
                scheduleDailyReminder(prefs.hour, prefs.minute)
            }
        }.onFailure { e ->
            Log.e(TAG, "Failed to re-schedule after firing", e)
        }
    }

    private fun buildAndShowNotification(requestCode: Int) {
        val (title, body) = resolveNotificationCopy()

        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(DEEP_LINK_URI)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            setPackage(context.packageName)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            requestCode,
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // No vibration — this is a sleep-context notification
            .setVibrate(null)
            // No sound — alarm already fires at user-defined quiet hour
            .setSilent(true)
            .build()

        runCatching {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }.onFailure { e ->
            Log.e(TAG, "Failed to show notification", e)
        }
    }


    private fun ensureChannelCreated() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description           = "Daily wind-down reminder before sleep"
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility  = NotificationCompat.VISIBILITY_PUBLIC
        }
        runCatching { notificationManager.createNotificationChannel(channel) }
    }


    private fun resolveNotificationCopy(): Pair<String, String> {
        val path = runCatching { readOnboardingPath() }.getOrDefault(Domain.OnboardingPath.GENERAL)
        return when (path) {
            Domain.OnboardingPath.TINNITUS -> Pair(
                "Time to quiet the noise",
                "Your tinnitus relief sounds are ready. Tap to start winding down.",
            )
            Domain.OnboardingPath.GENERAL, Domain.OnboardingPath.NONE -> Pair(
                "Time to wind down",
                "A few minutes of calm sound can make a real difference. Tap to start.",
            )
        }
    }


    /**
     * Calculates the next occurrence of [hour]:[minute] from now.
     * If the time today has already passed, schedules for tomorrow.
     */
    private fun nextOccurrenceMs(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    private fun buildDailyPendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY,
            Intent(context, WindDownAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun canScheduleExactAlarms(): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }.getOrDefault(false)

    private fun hasNotificationPermission(): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }.getOrDefault(false)

    // Synchronous prefs read for use inside alarm receiver without coroutines
    private fun readStoredWindDownPrefs(): WindDownPrefs? = runCatching {
        val sharedPrefs = context.getSharedPreferences("wind_down_cache", Context.MODE_PRIVATE)
        WindDownPrefs(
            isEnabled = sharedPrefs.getBoolean("enabled", false),
            hour      = sharedPrefs.getInt("hour", 21),
            minute    = sharedPrefs.getInt("minute", 30),
        )
    }.getOrNull()

    private fun readOnboardingPath(): Domain.OnboardingPath {
        val sharedPrefs = context.getSharedPreferences("wind_down_cache", Context.MODE_PRIVATE)
        val name = sharedPrefs.getString("onboarding_path", Domain.OnboardingPath.GENERAL.name)
            ?: Domain.OnboardingPath.GENERAL.name
        return runCatching { Domain.OnboardingPath.valueOf(name) }.getOrDefault(Domain.OnboardingPath.GENERAL)
    }

    /**
     * Writes a lightweight SharedPreferences cache so [WindDownAlarmReceiver]
     * and [BootReceiver] can read the scheduled state synchronously without
     * needing a coroutine or DataStore access from a BroadcastReceiver.
     */
    fun writeCache(enabled: Boolean, hour: Int, minute: Int, path: Domain.OnboardingPath) {
        runCatching {
            context.getSharedPreferences("wind_down_cache", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("enabled", enabled)
                .putInt("hour", hour)
                .putInt("minute", minute)
                .putString("onboarding_path", path.name)
                .apply()
        }
    }

    private data class WindDownPrefs(val isEnabled: Boolean, val hour: Int, val minute: Int)
}