package com.opxl.sleepslide.di

import com.opxl.sleepslide.data.audio.AudioServiceImpl
import com.opxl.sleepslide.data.purchase.PurchaseServiceImpl
import com.opxl.sleepslide.data.timer.TimerServiceImpl
import com.opxl.sleepslide.domain.service.AudioService
import com.opxl.sleepslide.domain.service.PurchaseService
import com.opxl.sleepslide.domain.service.TimerService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds //since we are binding an interface to its implementation, we use @Binds instead of @Provides
    @Singleton
    abstract fun bindAudioService(audioServiceImpl: AudioServiceImpl): AudioService

    @Binds
    @Singleton
    abstract  fun bindTimerService(timerServiceImpl: TimerServiceImpl): TimerService

    @Binds
    @Singleton
    abstract fun bindPurchaseService(purchaseServiceImpl: PurchaseServiceImpl): PurchaseService
}