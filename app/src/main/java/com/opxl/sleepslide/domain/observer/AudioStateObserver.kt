package com.opxl.sleepslide.domain.observer

import com.opxl.sleepslide.domain.model.Domain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AudioStateObserver {

    val audioState: StateFlow<Domain.AudioState>

    val activeMix: Flow<Domain.SoundMix?>

    val playbackStatus: Flow<Domain.PlaybackStatus>

    val audioFocusStatus: Flow<Domain.AudioFocusStatus>

    val isPlaying: Flow<Boolean>

    val isPlayingInBackground: Flow<Boolean>

    val isBluetoothConnected: Flow<Boolean>

    val isNightLockEnabled: Flow<Boolean>

    val fadeInProgress: Flow<Float?>
}