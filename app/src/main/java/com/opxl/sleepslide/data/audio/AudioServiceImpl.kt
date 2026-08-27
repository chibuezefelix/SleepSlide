package com.opxl.sleepslide.data.audio

import com.opxl.sleepslide.domain.model.Domain


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.opxl.sleepslide.R
//import com.opxl.sleepslide.domain.model.AudioFocusStatus
//import com.opxl.sleepslide.domain.model.PlaybackStatus
import com.opxl.sleepslide.domain.service.AudioService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AudioServiceImpl : MediaSessionService(), AudioService {

    companion object {
        const val NOTIFICATION_ID    = 1001
        const val CHANNEL_ID         = "sleepslide_playback"
        private const val FADE_STEP  = 50L
        private const val DUCK_FACTOR = 0.2f
    }

    inner class LocalBinder : Binder() {
        fun getService(): AudioService = this@AudioServiceImpl
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder {
        super.onBind(intent)
        binder
        return binder
    }

    // Injected after onCreate — lateinit is safe inside a Service
    @Inject lateinit var audioFocusHandler: AudioFocusHandler
    @Inject lateinit var bluetoothReceiver: BluetoothReceiver
    @Inject lateinit var playerLayerManager: PlayerLayerManager

    // Service-scoped coroutine scope — cancelled in onDestroy, not tied to ApplicationScope
    // Using SupervisorJob so a child failure does not cancel sibling coroutines
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mediaSession: MediaSession? = null

    private val _audioState = MutableStateFlow(
        Domain.AudioState(
            playbackStatus   = Domain.PlaybackStatus.IDLE,
            audioFocusStatus = Domain.AudioFocusStatus.GAINED,
        )
    )
    override val audioState: StateFlow<Domain.AudioState> = _audioState.asStateFlow()

    private var fadeJob: Job?      = null
    private var crossfadeJob: Job? = null

    // Tracks whether audio was playing when a transient focus loss occurred
    private var pausedByTransientLoss = false

    // Source-of-truth volumes — separate from ExoPlayer so we can restore after duck/fade
    private val layerVolumes = mutableMapOf(0 to 1f, 1 to 1f, 2 to 1f)
    private val mutedLayers  = mutableSetOf<Int>()

    // Lifecycle

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        buildMediaSession()
        registerBluetoothReceiver()
        registerFocusCallbacks()
        observePlayerErrors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForegroundSafely()
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val status = _audioState.value.playbackStatus
        if (status != Domain.PlaybackStatus.PLAYING && status != Domain.PlaybackStatus.FADING_IN) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        cancelFades()
        playerLayerManager.releaseAll()
        audioFocusHandler.abandon()
        audioFocusHandler.clearCallbacks()
        runCatching { unregisterReceiver(bluetoothReceiver) }
        mediaSession?.release()
        mediaSession = null
        serviceScope.cancel()
        super.onDestroy()
    }

    //  AudioService

    override suspend fun play(mix: Domain.SoundMix) {
        if (mix.isEmpty) return

        if (!audioFocusHandler.request()) {
            _audioState.update { it.copy(playbackStatus = Domain.PlaybackStatus.ERROR) }
            return
        }

        cancelFades()
        playerLayerManager.stopAll()
        layerVolumes.clear()
        mutedLayers.clear()

        mix.layers.forEach { layer ->
            layerVolumes[layer.position] = layer.volume
            if (layer.isMuted) mutedLayers += layer.position
            playerLayerManager.load(layer)
        }

        _audioState.update {
            it.copy(
                playbackStatus   = Domain.PlaybackStatus.LOADING,
                activeMix        = mix,
                audioFocusStatus = Domain.AudioFocusStatus.GAINED,
                fadeInProgress   = null,
            )
        }

        playerLayerManager.playAll()
        fadeIn(mix.fadeInDurationMs)
    }

    override suspend fun pause() {
        playerLayerManager.pauseAll()
        _audioState.update { it.copy(playbackStatus = Domain.PlaybackStatus.PAUSED) }
    }

    override suspend fun resume() {
        if (!audioFocusHandler.request()) return
        playerLayerManager.playAll()
        _audioState.update { it.copy(playbackStatus = Domain.PlaybackStatus.PLAYING) }
    }

    override suspend fun stop() {
        cancelFades()
        playerLayerManager.stopAll()
        audioFocusHandler.abandon()
        layerVolumes.clear()
        mutedLayers.clear()
        _audioState.update {
            it.copy(
                playbackStatus        = Domain.PlaybackStatus.IDLE,
                activeMix             = null,
                activePresetId        = null,
                audioFocusStatus      = Domain.AudioFocusStatus.LOST,
                isPlayingInBackground = false,
                fadeInProgress        = null,
            )
        }
    }

    override suspend fun setLayerVolume(position: Int, volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        layerVolumes[position] = clamped
        if (position !in mutedLayers) {
            val master = _audioState.value.activeMix?.masterVolume ?: 1f
            playerLayerManager.setVolume(position, clamped, master)
        }
        _audioState.update { it.copy(activeMix = it.activeMix?.updateLayerVolume(position, clamped)) }
    }

    override suspend fun setMasterVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        playerLayerManager.setMasterVolume(clamped, layerVolumes)
        _audioState.update { it.copy(activeMix = it.activeMix?.copy(masterVolume = clamped)) }
    }

    override suspend fun muteLayer(position: Int) {
        mutedLayers += position
        playerLayerManager.mute(position)
        _audioState.update { it.copy(activeMix = it.activeMix?.updateLayerMuted(position, true)) }
    }

    override suspend fun unmuteLayer(position: Int) {
        mutedLayers -= position
        val vol    = layerVolumes[position] ?: 1f
        val master = _audioState.value.activeMix?.masterVolume ?: 1f
        playerLayerManager.unmute(position, vol, master)
        _audioState.update { it.copy(activeMix = it.activeMix?.updateLayerMuted(position, false)) }
    }

    override suspend fun addLayer(layer: Domain.SoundLayer) {
        val mix = _audioState.value.activeMix ?: return
        if (mix.layers.size >= 3) return
        layerVolumes[layer.position] = layer.volume
        if (layer.isMuted) mutedLayers += layer.position
        playerLayerManager.load(layer)
        playerLayerManager.play(layer.position)
        _audioState.update {
            it.copy(activeMix = it.activeMix?.copy(layers = it.activeMix.layers + layer))
        }
    }

    override suspend fun removeLayer(position: Int) {
        playerLayerManager.stop(position)
        layerVolumes -= position
        mutedLayers  -= position
        _audioState.update {
            it.copy(activeMix = it.activeMix?.copy(
                layers = it.activeMix.layers.filter { l -> l.position != position }
            ))
        }
    }

    override suspend fun swapLayer(position: Int, layer: Domain.SoundLayer) {
        playerLayerManager.stop(position)
        layerVolumes[position] = layer.volume
        if (layer.isMuted) mutedLayers += position else mutedLayers -= position
        playerLayerManager.load(layer)
        playerLayerManager.play(position)
        _audioState.update {
            val replaced = it.activeMix?.layers
                ?.filter { l -> l.position != position }
                .orEmpty() + layer
            it.copy(activeMix = it.activeMix?.copy(layers = replaced.sortedBy { l -> l.position }))
        }
    }

    override suspend fun crossfadeTo(mix: Domain.SoundMix, durationMs: Long) {
        crossfadeJob?.cancel()
        crossfadeJob = serviceScope.launch {
            runCatching {
                fadeOut(durationMs / 2)
                play(mix)
            }.onFailure {
                _audioState.update { s -> s.copy(playbackStatus = Domain.PlaybackStatus.ERROR) }
            }
        }
    }

    override suspend fun fadeOut(durationMs: Long) {
        fadeJob?.cancel()
        val master = _audioState.value.activeMix?.masterVolume ?: 1f
        val steps  = (durationMs / FADE_STEP).toInt().coerceAtLeast(1)

        _audioState.update { it.copy(playbackStatus = Domain.PlaybackStatus.FADING_OUT) }

        fadeJob = serviceScope.launch {
            for (step in steps downTo 0) {
                val fraction = step.toFloat() / steps
                playerLayerManager.setMasterVolume(master * fraction, layerVolumes)
                delay(FADE_STEP)
            }
            playerLayerManager.pauseAll()
            // Restore true master so next play() starts at correct volume
            playerLayerManager.setMasterVolume(master, layerVolumes)
            _audioState.update { it.copy(playbackStatus = Domain.PlaybackStatus.PAUSED, fadeInProgress = null) }
        }
        fadeJob?.join()
    }

    override suspend fun fadeIn(durationMs: Long) {
        fadeJob?.cancel()
        val master = _audioState.value.activeMix?.masterVolume ?: 1f
        val steps  = (durationMs / FADE_STEP).toInt().coerceAtLeast(1)

        playerLayerManager.setMasterVolume(0f, layerVolumes)
        _audioState.update { it.copy(playbackStatus = Domain.PlaybackStatus.FADING_IN, fadeInProgress = 0f) }

        fadeJob = serviceScope.launch {
            for (step in 0..steps) {
                val fraction = step.toFloat() / steps
                playerLayerManager.setMasterVolume(master * fraction, layerVolumes)
                _audioState.update { it.copy(fadeInProgress = fraction) }
                delay(FADE_STEP)
            }
            playerLayerManager.setMasterVolume(master, layerVolumes)
            _audioState.update {
                it.copy(
                    playbackStatus        = Domain.PlaybackStatus.PLAYING,
                    isPlayingInBackground = true,
                    fadeInProgress        = null,
                )
            }
        }
        fadeJob?.join()
    }

    override suspend fun enableNightLock() {
        _audioState.update { it.copy(isNightLockEnabled = true) }
    }

    override suspend fun disableNightLock() {
        _audioState.update { it.copy(isNightLockEnabled = false) }
    }

    override fun release() = onDestroy()

    //  AudioFocus

    private fun registerFocusCallbacks() {
        audioFocusHandler.registerCallbacks(
            onGained = {
                serviceScope.launch {
                    if (pausedByTransientLoss) {
                        pausedByTransientLoss = false
                        resume()
                    }
                    val master = _audioState.value.activeMix?.masterVolume ?: 1f
                    playerLayerManager.unduck(layerVolumes, master)
                    _audioState.update { it.copy(audioFocusStatus = Domain.AudioFocusStatus.GAINED) }
                }
            },
            onLost = {
                serviceScope.launch {
                    pause()
                    audioFocusHandler.abandon()
                    _audioState.update { it.copy(audioFocusStatus = Domain.AudioFocusStatus.LOST) }
                }
            },
            onLostTransient = {
                serviceScope.launch {
                    pausedByTransientLoss = _audioState.value.playbackStatus == Domain.PlaybackStatus.PLAYING
                    pause()
                    _audioState.update { it.copy(audioFocusStatus = Domain.AudioFocusStatus.LOST_TRANSIENT) }
                }
            },
            onDuck = {
                val master = _audioState.value.activeMix?.masterVolume ?: 1f
                playerLayerManager.duck(master, DUCK_FACTOR)
                _audioState.update { it.copy(audioFocusStatus = Domain.AudioFocusStatus.DUCK) }
            },
            onUnduck = {
                val master = _audioState.value.activeMix?.masterVolume ?: 1f
                playerLayerManager.unduck(layerVolumes, master)
                _audioState.update { it.copy(audioFocusStatus = Domain.AudioFocusStatus.GAINED) }
            },
        )
    }

    //  Bluetooth

    private fun registerBluetoothReceiver() {
        val filter = BluetoothReceiver.intentFilter()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(bluetoothReceiver, filter)
        }
        bluetoothReceiver.isConnected
            .onEach { connected ->
                _audioState.update { it.copy(isBluetoothConnected = connected) }
                // Headset disconnected mid-playback — pause gracefully
                if (!connected && _audioState.value.playbackStatus == Domain.PlaybackStatus.PLAYING) {
                    pause()
                }
            }
            .launchIn(serviceScope)
    }

    // Player errors

    private fun observePlayerErrors() {
        playerLayerManager.errors
            .onEach { errors ->
                if (errors.isNotEmpty()) {
                    _audioState.update { it.copy(playbackStatus = Domain.PlaybackStatus.ERROR) }
                }
            }
            .launchIn(serviceScope)
    }

    //  MediaSession

    private fun buildMediaSession() {
        // Use player0 as the session player — it represents the primary layer
        mediaSession = MediaSession.Builder(this, playerLayerManager.primaryPlayer)
            .build()
    }

    // Notification

    private fun startForegroundSafely() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SleepSlide")
            .setContentText("Playing in background")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Playback",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "SleepSlide background playback"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }



    private fun cancelFades() {
        fadeJob?.cancel();      fadeJob      = null
        crossfadeJob?.cancel(); crossfadeJob = null
    }

    private fun Domain.SoundMix.updateLayerVolume(position: Int, volume: Float) =
        copy(layers = layers.map { if (it.position == position) it.copy(volume = volume) else it })

    private fun Domain.SoundMix.updateLayerMuted(position: Int, muted: Boolean) =
        copy(layers = layers.map { if (it.position == position) it.copy(isMuted = muted) else it })
}