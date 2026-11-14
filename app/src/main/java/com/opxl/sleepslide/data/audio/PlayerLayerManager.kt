package com.opxl.sleepslide.data.audio

import android.content.Context
import android.net.Uri
import java.io.File
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.opxl.sleepslide.domain.model.Domain
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PlayerLayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("layer0") private val player0: ExoPlayer,
    @Named("layer1") private val player1: ExoPlayer,
    @Named("layer2") private val player2: ExoPlayer,


) {
    private val players = arrayOf<ExoPlayer>(player0, player1, player2)
    val primaryPlayer : ExoPlayer = player0
    private val _errors = MutableStateFlow<Map<Int, String>>(emptyMap())
    val errors: StateFlow<Map<Int, String>> = _errors.asStateFlow()

    init {
        players.forEachIndexed { index, player ->
            player.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    _errors.value = _errors.value + (index to (error.message ?: "Unknown error"))
                }
                override fun onPlayerErrorChanged(error: PlaybackException?) {
                    if (error == null) {
                        _errors.value = _errors.value - index
                    }
                }
            })
        }
    }

    fun load(layer: Domain.SoundLayer) {
        val player = players.getOrNull(layer.position) ?: return
        // The Sound row is the source of truth for where the audio lives: a downloaded
        // file if present, else the bundled asset (BundledSoundCatalogue / DB assetPath).
        val uri = layer.sound.downloadedPath
            ?.takeIf { it.isNotBlank() }
            ?.let { Uri.fromFile(File(it)) }
            ?: Uri.parse("asset:///${layer.sound.assetPath}")
        player.apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = if (layer.isMuted) 0f else layer.volume.coerceIn(0f, 1f)
            prepare()
        }
    }

    fun play(position: Int) {
        players.getOrNull(position)?.play()
    }

    fun playAll() = players.forEach { it.play() }

    fun pause(position: Int) {
        players.getOrNull(position)?.pause()
    }

    fun pauseAll() = players.forEach { it.pause() }

    fun stop(position: Int) {
        players.getOrNull(position)?.apply {
            stop()
            clearMediaItems()
        }
    }

    fun stopAll() = players.forEachIndexed { index, _ -> stop(index) }

    fun setVolume(position: Int, volume: Float, masterVolume: Float = 1f) {
        players.getOrNull(position)?.volume = (volume * masterVolume).coerceIn(0f, 1f)
    }

    fun setMasterVolume(masterVolume: Float, layerVolumes: Map<Int, Float>) {
        players.forEachIndexed { index, player ->
            val layerVol = layerVolumes[index] ?: 1f
            player.volume = (layerVol * masterVolume).coerceIn(0f, 1f)
        }
    }

    fun mute(position: Int) {
        players.getOrNull(position)?.volume = 0f
    }

    fun unmute(position: Int, volume: Float, masterVolume: Float = 1f) {
        players.getOrNull(position)?.volume = (volume * masterVolume).coerceIn(0f, 1f)
    }

    fun duck(factor: Float = 0.2f, DUCK_FACTOR: Float) {
        players.forEach { it.volume = (it.volume * factor).coerceIn(0f, 1f) }
    }

    fun unduck(layerVolumes: Map<Int, Float>, masterVolume: Float) {
        players.forEachIndexed { index, player ->
            val vol = layerVolumes[index] ?: 1f
            player.volume = (vol * masterVolume).coerceIn(0f, 1f)
        }
    }

    fun isPlaying(position: Int): Boolean =
        players.getOrNull(position)?.isPlaying ?: false

    fun isAnyPlaying(): Boolean = players.any { it.isPlaying }

    fun clearError(position: Int) {
        _errors.value -= position
    }

    /**
     * Only for process teardown. The players are @Singleton and outlive the service;
     * AudioServiceImpl.onDestroy() must call [stopAll], not this — a released player
     * throws "Handler on a dead thread" the next time the service binds.
     */
    fun releaseAll() {
        players.forEach { it.release() }
    }
}