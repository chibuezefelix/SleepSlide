package com.opxl.sleepslide.data.audio

import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.observer.AudioStateObserver
import com.opxl.sleepslide.domain.service.AudioService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioStateObserverImpl @Inject constructor(
    private val audioService: AudioService,
) : AudioStateObserver {

    override val audioState: StateFlow<Domain.AudioState> = audioService.audioState

    override val activeMix: Flow<Domain.SoundMix?> =
        audioService.audioState.map { it.activeMix }.distinctUntilChanged()

    override val playbackStatus: Flow<Domain.PlaybackStatus> =
        audioService.audioState.map { it.playbackStatus }.distinctUntilChanged()

    override val audioFocusStatus: Flow<Domain.AudioFocusStatus> =
        audioService.audioState.map { it.audioFocusStatus }.distinctUntilChanged()

    override val isPlaying: Flow<Boolean> =
        audioService.audioState.map { it.playbackStatus == Domain.PlaybackStatus.PLAYING }.distinctUntilChanged()

    override val isPlayingInBackground: Flow<Boolean> =
        audioService.audioState.map { it.isPlayingInBackground }.distinctUntilChanged()

    override val isBluetoothConnected: Flow<Boolean> =
        audioService.audioState.map { it.isBluetoothConnected }.distinctUntilChanged()

    override val isNightLockEnabled: Flow<Boolean> =
        audioService.audioState.map { it.isNightLockEnabled }.distinctUntilChanged()

    override val fadeInProgress: Flow<Float?> =
        audioService.audioState.map { it.fadeInProgress }.distinctUntilChanged()
}
