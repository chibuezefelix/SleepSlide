package com.opxl.sleepslide.presentation.widget

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import com.opxl.sleepslide.R
import androidx.core.content.ContextCompat

/** Whether the system can show its own "add this tile" prompt (Android 13+). */
val canRequestAddTile: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

enum class AddTileResult { ADDED, ALREADY_ADDED, DECLINED, UNAVAILABLE }

/** Asks the system to offer the Sleep now tile. [onResult] runs on the main thread. */
fun requestAddSleepTile(context: Context, onResult: (AddTileResult) -> Unit) {
    if (!canRequestAddTile) {
        onResult(AddTileResult.UNAVAILABLE)
        return
    }
    val manager = context.getSystemService(StatusBarManager::class.java)
    if (manager == null) {
        onResult(AddTileResult.UNAVAILABLE)
        return
    }
    runCatching {
        manager.requestAddTileService(
            ComponentName(context, SleepTileService::class.java),
            context.getString(R.string.tile_label),
            Icon.createWithResource(context, R.drawable.ic_moon),
            ContextCompat.getMainExecutor(context),
        ) { code ->
            onResult(
                when (code) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED     -> AddTileResult.ADDED
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> AddTileResult.ALREADY_ADDED
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> AddTileResult.DECLINED
                    else                                                    -> AddTileResult.UNAVAILABLE
                }
            )
        }
    }.onFailure { onResult(AddTileResult.UNAVAILABLE) }
}
