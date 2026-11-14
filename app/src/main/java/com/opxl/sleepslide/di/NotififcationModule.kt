package com.opxl.sleepslide.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import  com.opxl.sleepslide.data.notification.WindDownNotificationServiceImpl
import com.opxl.sleepslide.data.repository.WindDownRepositoryImpl
import com.opxl.sleepslide.domain.repository.WindDownRepository
import  com.opxl.sleepslide.domain.service.WindDownNotificationService
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindWindDownNotificationService(
        impl: WindDownNotificationServiceImpl,
    ): WindDownNotificationService

    @Binds
    @Singleton
    abstract fun bindWindDownRepository(
        impl: WindDownRepositoryImpl,
    ): WindDownRepository
}