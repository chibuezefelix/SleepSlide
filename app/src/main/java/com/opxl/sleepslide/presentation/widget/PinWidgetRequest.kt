package com.opxl.sleepslide.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/**
 * Asks the launcher to offer the Sleep now widget (Android 8+, launcher permitting).
 * Returns false when the launcher cannot pin widgets, so the caller can explain the manual way.
 */
fun requestPinSleepWidget(context: Context): Boolean = runCatching {
    val manager = AppWidgetManager.getInstance(context)
    manager.isRequestPinAppWidgetSupported &&
        manager.requestPinAppWidget(ComponentName(context, SleepWidgetReceiver::class.java), null, null)
}.getOrDefault(false)
