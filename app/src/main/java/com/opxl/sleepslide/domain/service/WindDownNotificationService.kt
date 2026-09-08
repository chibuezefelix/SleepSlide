package com.opxl.sleepslide.domain.service

interface WindDownNotificationService {

    /**
     * Schedules a daily exact-time alarm at [hour]:[minute].
     * Replaces any previously scheduled alarm — callers do not need
     * to cancel before rescheduling.
     */
    fun scheduleDailyReminder(hour: Int, minute: Int)

    /**
     * Cancels the daily alarm if one is scheduled.
     * Safe to call when no alarm is active.
     */
    fun cancelDailyReminder()

    /**
     * Fires the notification immediately without waiting for the alarm.
     * Used by the "Send test notification" button in Settings.
     */
    fun showImmediateNotification()

    /**
     * Returns true if an exact-alarm PendingIntent is currently active.
     * Used to detect stale state after device reboot or permission revocation.
     */

    fun isScheduled(): Boolean
}