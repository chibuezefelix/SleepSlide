package com.opxl.sleepslide.data.timer



import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.service.AudioService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class FadeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val audioService: AudioService,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_FADE_DURATION_MS  = "fade_duration_ms"
        const val WORK_NAME             = "FadeWorker"
        private const val DEFAULT_FADE  = 60_000L
        private const val MAX_ATTEMPTS  = 3
    }

    override suspend fun doWork(): Result {
        if (runAttemptCount >= MAX_ATTEMPTS) return Result.failure()

        val playbackStatus = audioService.audioState.value.playbackStatus
        if (playbackStatus == Domain.PlaybackStatus.IDLE || playbackStatus == Domain.PlaybackStatus.PAUSED) {
            return Result.success()
        }

        val fadeDurationMs = inputData.getLong(KEY_FADE_DURATION_MS, DEFAULT_FADE)

        return runCatching {
            audioService.fadeOut(fadeDurationMs)
            audioService.stop()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }
}