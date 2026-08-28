package com.opxl.sleepslide.di

import androidx.work.WorkManager
import com.opxl.sleepslide.data.audio.AudioServiceImpl
import com.opxl.sleepslide.data.purchase.PurchaseServiceImpl
import com.opxl.sleepslide.data.timer.TimerServiceImpl
import com.opxl.sleepslide.domain.service.AudioService
import com.opxl.sleepslide.domain.service.PurchaseService
import com.opxl.sleepslide.domain.service.TimerService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
//
//    @Binds //since we are binding an interface to its implementation, we use @Binds instead of @Provides
//    @Singleton
//    abstract fun bindAudioService(audioServiceImpl: AudioServiceImpl): AudioService
//
@Provides
@Singleton
fun provideTimerService(
    workManager: WorkManager,
    @ApplicationScope scope: CoroutineScope,
): TimerService = TimerServiceImpl(workManager, scope)

    @Provides
    @Singleton
    fun providePurchaseService(purchaseServiceImpl: PurchaseServiceImpl): PurchaseService = purchaseServiceImpl
}

