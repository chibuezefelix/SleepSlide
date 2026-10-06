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
    // Set per build type in app/build.gradle.kts — always false in release, opt-in for
    // debug via TESTING_MODE=true in local.properties. Grants PREMIUM without a purchase.

    @Provides
    @Singleton
    @RevenueCatApiKey
    fun provideRevenueCatApiKey(): String = if (BuildConfig.TESTING_MODE) BuildConfig.TEST_REVENUECAT_KEY else BuildConfig.REVENUECAT_KEY


    @Provides
    @Singleton
    @IsTestingMode
    fun provideIsTestingMode(): Boolean = BuildConfig.TESTING_MODE
}