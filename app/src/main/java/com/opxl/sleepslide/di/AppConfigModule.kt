package com.opxl.sleepslide.di


import com.opxl.sleepslide.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton




@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {
    private const val IS_TESTING_MODE = true

    @Provides
    @Singleton
    @RevenueCatApiKey
    fun provideRevenueCatApiKey(): String = if (IS_TESTING_MODE) BuildConfig.TEST_REVENUECAT_KEY else BuildConfig.REVENUECAT_KEY


    @Provides
    @Singleton
    @IsTestingMode
    fun provideIsTestingMode(): Boolean = IS_TESTING_MODE
}