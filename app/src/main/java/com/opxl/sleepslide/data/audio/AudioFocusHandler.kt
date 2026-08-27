
package com.opxl.sleepslide.data.audio
/*Author Chibueze Felix
* Date  27 Aug 2026
* Project SleepSlide
* Licence MIT LICENSE
* */

import android.media.AudioFocusRequest
import android.media.AudioManager
import com.opxl.sleepslide.domain.model.Domain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioFocusHandler @Inject constructor(
    private val audioManager: AudioManager,
) {
    private val _focusStatus = MutableStateFlow(Domain.AudioFocusStatus.GAINED)
    val focusStatus: StateFlow<Domain.AudioFocusStatus> = _focusStatus.asStateFlow()

    private var onFocusGained: (() -> Unit)? = null
    private var onFocusLost: (() -> Unit)? = null
    private var onFocusLostTransient: (() -> Unit)? = null
    private var onDuck: (() -> Unit)? = null
    private var onUnduck: (() -> Unit)? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                _focusStatus.value = Domain.AudioFocusStatus.GAINED
                onUnduck?.invoke()
                onFocusGained?.invoke()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                _focusStatus.value = Domain.AudioFocusStatus.LOST
                onFocusLost?.invoke()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                _focusStatus.value = Domain.AudioFocusStatus.LOST_TRANSIENT
                onFocusLostTransient?.invoke()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                _focusStatus.value = Domain.AudioFocusStatus.DUCK
                onDuck?.invoke()
            }
        }
    }

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setAcceptsDelayedFocusGain(true)
        .setOnAudioFocusChangeListener(focusChangeListener)
        .build()

    fun registerCallbacks(
        onGained: () -> Unit,
        onLost: () -> Unit,
        onLostTransient: () -> Unit,
        onDuck: () -> Unit,
        onUnduck: () -> Unit,
    ) {
        this.onFocusGained       = onGained
        this.onFocusLost         = onLost
        this.onFocusLostTransient = onLostTransient
        this.onDuck              = onDuck
        this.onUnduck            = onUnduck
    }

    fun request(): Boolean =
        audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    fun abandon() {
        audioManager.abandonAudioFocusRequest(focusRequest)
        _focusStatus.value = Domain.AudioFocusStatus.LOST
    }

    fun clearCallbacks() {
        onFocusGained        = null
        onFocusLost          = null
        onFocusLostTransient = null
        onDuck               = null
        onUnduck             = null
    }
}
