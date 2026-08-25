package com.opxl.sleepslide.di

import com.opxl.sleepslide.data.audio.AudioStateObserverImpl
import com.opxl.sleepslide.data.observer.EntitlementObserverImpl
import com.opxl.sleepslide.data.timer.TimerStateObserverImpl
import com.opxl.sleepslide.domain.observer.AudioStateObserver
import com.opxl.sleepslide.domain.observer.EntitlementObserver
import com.opxl.sleepslide.domain.observer.TimerStateObserver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ObserverModule {

    @Binds
    @Singleton

    abstract fun bindAudioStateObserver(impl: AudioStateObserverImpl): AudioStateObserver

    @Binds
    @Singleton
    abstract fun bindTimerStateObserver(impl: TimerStateObserverImpl): TimerStateObserver

    @Binds
    @Singleton
    abstract fun bindEntitlementObserver(impl: EntitlementObserverImpl): EntitlementObserver
}