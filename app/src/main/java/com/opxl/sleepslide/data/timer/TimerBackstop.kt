package com.opxl.sleepslide.data.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Safety net that stops playback if the sleep timer's normal stop never ran. */
interface TimerBackstop {
    fun schedule(token: Long, triggerAtElapsedMs: Long)
    fun cancel()
}

/**
 * Inexact while-idle alarm: it may fire a few minutes late in Doze, which is fine for
 * a backstop, and needs no SCHEDULE_EXACT_ALARM grant. Rescheduling replaces the
 * previous alarm because the PendingIntent request code is fixed.
 */
@Singleton
class AlarmTimerBackstop @Inject constructor(
    @ApplicationContext private val context: Context,
) : TimerBackstop {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(token: Long, triggerAtElapsedMs: Long) {
        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtElapsedMs,
                pendingIntent(token),
            )
        }
    }

    override fun cancel() {
        runCatching { alarmManager.cancel(pendingIntent(token = 0L)) }
    }

    private fun pendingIntent(token: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        TimerBackstopReceiver.REQUEST_CODE,
        Intent(context, TimerBackstopReceiver::class.java)
            .putExtra(TimerBackstopReceiver.EXTRA_TOKEN, token),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
