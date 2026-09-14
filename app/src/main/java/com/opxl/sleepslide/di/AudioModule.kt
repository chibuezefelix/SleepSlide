package com.opxl.sleepslide.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object  AudioModule{


    private fun buildExoplayer(@ApplicationContext context: Context): ExoPlayer{
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // handleAudioFocus = false: AudioFocusHandler owns focus for the whole mix.
        // With three players each requesting focus, Android grants the newest request
        // and sends AUDIOFOCUS_LOSS to our own handler — the app pauses itself.
        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, false)
            .setHandleAudioBecomingNoisy(true)
            .build()


    }

    @Provides
    @Singleton
    @Named("layer0")
    fun provideExoplayerLayer0(@ApplicationContext context: Context): ExoPlayer = buildExoplayer(context)


    @Provides
    @Singleton
    @Named("layer1")
    fun provideExoplayerLayer1(@ApplicationContext context: Context): ExoPlayer = buildExoplayer(context)

    @Provides
    @Singleton
    @Named("layer2")
    fun provideExoplayerLayer2(@ApplicationContext context: Context): ExoPlayer = buildExoplayer(context)
}