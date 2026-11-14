package com.opxl.sleepslide.presentation.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.ContextCompat

/**
 * Bridge between the Activity-owned `POST_NOTIFICATIONS` launcher and any screen that
 * needs to ask for the permission just-in-time (e.g. PlayerScreen on first play).
 *
 * [MainActivity] registers the launcher once and provides an implementation through
 * [LocalNotificationPermissionRequester]; screens call [request] and receive the grant
 * result in [onResult]. On devices below Android 13 the permission does not exist, so
 * implementations must invoke [onResult] with `true` immediately.
 */
fun interface NotificationPermissionRequester {
    fun request(onResult: (granted: Boolean) -> Unit)
}

val LocalNotificationPermissionRequester = staticCompositionLocalOf<NotificationPermissionRequester> {
    error("No NotificationPermissionRequester provided — wrap the content in CompositionLocalProvider from MainActivity")
}

/** True when notifications can be posted — always true below Android 13. */
fun Context.isNotificationPermissionGranted(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
