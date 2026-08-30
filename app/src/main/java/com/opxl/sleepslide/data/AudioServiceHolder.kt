package com.opxl.sleepslide.data


import com.opxl.sleepslide.domain.service.AudioService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hilt-injectable bridge to the system-managed [AudioServiceImpl].
 *
 * [AudioServiceImpl] extends [MediaSessionService] — Android owns its lifecycle
 * and Hilt cannot provide it as a singleton. This holder is provided as a
 * singleton instead. [MainActivity] writes the reference on bind/unbind.
 * ViewModels read it safely — a null service means audio is not yet running.
 */
@Singleton
class AudioServiceHolder @Inject constructor() {

    private val _service = MutableStateFlow<AudioService?>(null)
    val service: Flow<AudioService?> = _service.asStateFlow()

    val current: AudioService? get() = _service.value

    internal fun attach(audioService: AudioService) {
        _service.value = audioService
    }

    internal fun detach() {
        _service.value = null
    }
}