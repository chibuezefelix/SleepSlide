package com.opxl.sleepslide.domain.service

import com.opxl.sleepslide.domain.model.Domain
import kotlinx.coroutines.flow.StateFlow

interface AudioService {

    val audioState: StateFlow<Domain.AudioState>

    suspend fun play(mix: Domain.SoundMix)

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

    suspend fun fadeIn(durationMs: Long)

    suspend fun enableNightLock()

    suspend fun disableNightLock()

    fun release()
}