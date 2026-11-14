package com.opxl.sleepslide.data


import com.opxl.sleepslide.domain.service.AudioService
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
    val service: StateFlow<AudioService?> = _service.asStateFlow()

    val current: AudioService? get() = _service.value

    private val _foreground = MutableStateFlow(false)

    /**
     * True once the service has called startForeground(). A service that was started but is not yet
     * foreground cannot get audio focus on Android 15 (focus is denied), and must not be stopped
     * (Android kills the app for stopping it before it promotes itself). Callers that start the
     * service themselves wait for this before playing or cleaning up.
     */
    val foreground: StateFlow<Boolean> = _foreground.asStateFlow()

    internal fun attach(audioService: AudioService) {
        _service.value = audioService
    }

    internal fun detach() {
        _service.value = null
        _foreground.value = false
    }

    internal fun markForeground(value: Boolean) {
        _foreground.value = value
    }
}