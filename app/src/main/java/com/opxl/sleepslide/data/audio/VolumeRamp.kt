package com.opxl.sleepslide.data.audio

import android.os.SystemClock
import kotlinx.coroutines.delay

/**
 * Drives a fade by elapsed time, not by step count: each step reads [clock] and applies
 * how far through [durationMs] it really is (0f → 1f). A slow step makes the next one
 * jump ahead instead of stretching the fade, so it always ends on time — the sleep
 * timer stops playback on the same clock and expects the fade to have reached silence.
 *
 * [onProgress] is always called with 1f last.
 */
internal suspend fun rampVolume(
    durationMs: Long,
    stepMs: Long,
    clock: () -> Long = SystemClock::elapsedRealtime,
    onProgress: (Float) -> Unit,
) {
    if (durationMs <= 0L) {
        onProgress(1f)
        return
    }
    val start = clock()
    while (true) {
        val progress = ((clock() - start).toFloat() / durationMs).coerceIn(0f, 1f)
        onProgress(progress)
        if (progress >= 1f) return
        delay(stepMs)
    }
}
