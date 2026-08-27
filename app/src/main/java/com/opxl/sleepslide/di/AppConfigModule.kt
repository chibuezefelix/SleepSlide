package com.opxl.sleepslide.di


import com.opxl.sleepslide.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton


@Qualifier @Retention(AnnotationRetention.BINARY) annotation class RevenueCatApiKey

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {

    @Provides
    @Singleton
    @RevenueCatApiKey
    fun provideRevenueCatApiKey(): String = BuildConfig.REVENUECAT_KEY
}