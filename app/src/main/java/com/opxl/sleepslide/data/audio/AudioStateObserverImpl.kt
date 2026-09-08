package com.opxl.sleepslide.data.audio

import com.opxl.sleepslide.data.AudioServiceHolder
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.observer.AudioStateObserver
import com.opxl.sleepslide.domain.service.AudioService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class AudioStateObserverImpl @Inject constructor(
    private val audioServiceHolder: AudioServiceHolder,
    @com.opxl.sleepslide.di.ApplicationScope private val scope: CoroutineScope,
) : AudioStateObserver {


    private val idleState = Domain.AudioState(
        playbackStatus = Domain.PlaybackStatus.IDLE,
        audioFocusStatus = Domain.AudioFocusStatus.NONE,
    )
    // Flat-maps the holder's service flow so when the service binds/unbinds
    // all downstream observers automatically switch to the live or idle state.
    private val serviceStateFlow: StateFlow<Domain.AudioState> =
        audioServiceHolder.service
            .flatMapLatest { service ->
                service?.audioState ?: flowOf(idleState)
            }
            .stateIn(
                scope   = scope,
                started = SharingStarted.Eagerly,
                initialValue = idleState,
            )
    override val audioState: StateFlow<Domain.AudioState> =serviceStateFlow

    override val activeMix: Flow<Domain.SoundMix?> =
        serviceStateFlow.map { it.activeMix }.distinctUntilChanged()

    override val playbackStatus: Flow<Domain.PlaybackStatus> =
        serviceStateFlow.map { it.playbackStatus }.distinctUntilChanged()

    override val audioFocusStatus: Flow<Domain.AudioFocusStatus> =
        serviceStateFlow.map { it.audioFocusStatus }.distinctUntilChanged()

    override val isPlaying: Flow<Boolean> =
        serviceStateFlow.map { it.playbackStatus == Domain.PlaybackStatus.PLAYING }.distinctUntilChanged()

    override val isPlayingInBackground: Flow<Boolean> =
        serviceStateFlow.map { it.isPlayingInBackground }.distinctUntilChanged()

    override val isBluetoothConnected: Flow<Boolean> =
        serviceStateFlow.map { it.isBluetoothConnected }.distinctUntilChanged()

    override val isNightLockEnabled: Flow<Boolean> =
        serviceStateFlow.map { it.isNightLockEnabled }.distinctUntilChanged()

    override val fadeInProgress: Flow<Float?> =
        serviceStateFlow.map { it.fadeInProgress }.distinctUntilChanged()
}
