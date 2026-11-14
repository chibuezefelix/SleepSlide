package com.opxl.sleepslide.domain.service

import com.opxl.sleepslide.domain.model.Domain
import kotlinx.coroutines.flow.StateFlow

interface AudioService {

    val audioState: StateFlow<Domain.AudioState>

    /**
     * Starts [mix]. When audio focus is denied the call fails (status ERROR) unless
     * [proceedWithoutAudioFocus] is set, in which case it plays anyway, without ducking or pausing
     * other apps. Headless starts (tile, widget, reminder button) set it: Android 15 denies focus to
     * an app started from the background, and a bedtime tap must not fail silently over that.
     */
    suspend fun play(mix: Domain.SoundMix, proceedWithoutAudioFocus: Boolean = false)

    suspend fun pause()

    suspend fun resume()

    suspend fun stop()

    suspend fun setLayerVolume(position: Int, volume: Float)

    suspend fun setMasterVolume(volume: Float)

    suspend fun muteLayer(position: Int)

    suspend fun unmuteLayer(position: Int)

    suspend fun addLayer(layer: Domain.SoundLayer)

    suspend fun removeLayer(position: Int)

    suspend fun swapLayer(position: Int, layer: Domain.SoundLayer)

    suspend fun crossfadeTo(mix: Domain.SoundMix, durationMs: Long = 3_000L)

    suspend fun fadeOut(durationMs: Long)

    /** Abort a running fade-out and return to full volume. No-op if not fading out. */
    suspend fun cancelFadeOut()

    suspend fun fadeIn(durationMs: Long)

    suspend fun enableNightLock()

    suspend fun disableNightLock()

    fun release()
}