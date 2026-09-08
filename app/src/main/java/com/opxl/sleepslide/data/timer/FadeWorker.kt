package com.opxl.sleepslide.data.timer



import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opxl.sleepslide.data.AudioServiceHolder
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.service.AudioService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class FadeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val audioServiceHolder: AudioServiceHolder,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_FADE_DURATION_MS  = "fade_duration_ms"
        const val WORK_NAME             = "FadeWorker"
        private const val DEFAULT_FADE  = 60_000L
        private const val MAX_ATTEMPTS  = 3
    }



//    override suspend fun doWork(): Result {
//        if (runAttemptCount >= MAX_ATTEMPTS) return Result.failure()
//
//        val audioService = audioServiceHolder.service
//        val playbackStatus = audioService.value?.audioState?.value?.playbackStatus
//        if (playbackStatus == Domain.PlaybackStatus.IDLE || playbackStatus == Domain.PlaybackStatus.PAUSED) {
//            return Result.success()
//        }
//
//        val fadeDurationMs = inputData.getLong(KEY_FADE_DURATION_MS, DEFAULT_FADE)
//
//        return runCatching {
//            audioService.fadeOut(fadeDurationMs)
//            audioService.stop()
//        }.fold(
//            onSuccess = { Result.success() },
//            onFailure = { Result.retry() },
//        )


    override suspend fun doWork(): Result = withContext(Dispatchers.Main) {
        val service = audioServiceHolder.current

        // If the service is not bound, check if audio is already idle before retrying.
        // Retrying when idle would be pointless and wastes battery.
        if (service == null) {
            return@withContext if (runAttemptCount >= MAX_ATTEMPTS) {
                Result.failure()
            } else {
                Result.retry()
            }
        }

        // If audio is already stopped/idle, the fade already completed — succeed silently.
        val status = service.audioState.value.playbackStatus
        if (status == Domain.PlaybackStatus.IDLE || status == Domain.PlaybackStatus.PAUSED) {
            return@withContext Result.success()
        }

        runCatching {
            val fadeDurationMs = inputData.getLong(KEY_FADE_DURATION_MS, DEFAULT_FADE)
            service.fadeOut(fadeDurationMs)
            service.stop()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
            },
        )
    }
}