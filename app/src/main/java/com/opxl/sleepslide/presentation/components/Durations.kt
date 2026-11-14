package com.opxl.sleepslide.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.opxl.sleepslide.R

private const val SECOND_MS = 1_000L
private const val MINUTE_MS = 60 * SECOND_MS
private const val HOUR_MS   = 60 * MINUTE_MS

/**
 * "30 seconds", "1 minute", "90 minutes", "2 hours" — the largest unit that divides [ms]
 * evenly, so a setting reads back exactly as chosen.
 */
fun durationText(ms: Long): UiText = when {
    ms >= HOUR_MS && ms % HOUR_MS == 0L     -> UiText.Plural(R.plurals.duration_hours, (ms / HOUR_MS).toInt())
    ms >= MINUTE_MS && ms % MINUTE_MS == 0L -> UiText.Plural(R.plurals.duration_minutes, (ms / MINUTE_MS).toInt())
    else                                    -> UiText.Plural(R.plurals.duration_seconds, (ms / SECOND_MS).toInt())
}

/** Compact chip label: "15m", "1h", "1h 30m". */
@Composable
fun shortDuration(ms: Long): String {
    val h = (ms / HOUR_MS).toInt()
    val m = ((ms % HOUR_MS) / MINUTE_MS).toInt()
    return when {
        h > 0 && m > 0 -> stringResource(R.string.duration_short_hours_minutes, h, m)
        h > 0          -> stringResource(R.string.duration_short_hours, h)
        else           -> stringResource(R.string.duration_short_minutes, m)
    }
}

/** Total listening time on a preset card: "<1m", "45m", "2h 10m". */
@Composable
fun listenedDuration(ms: Long): String {
    val h = (ms / HOUR_MS).toInt()
    val m = ((ms % HOUR_MS) / MINUTE_MS).toInt()
    return when {
        h > 0 -> stringResource(R.string.duration_short_hours_minutes, h, m)
        m > 0 -> stringResource(R.string.duration_short_minutes, m)
        else  -> stringResource(R.string.duration_short_under_minute)
    }
}
