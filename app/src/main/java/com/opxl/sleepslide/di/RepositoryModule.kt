package com.opxl.sleepslide.di

import com.opxl.sleepslide.data.purchase.PurchaseRepositoryImpl
import com.opxl.sleepslide.data.repository.PlayHistoryRepositoryImpl
import com.opxl.sleepslide.data.repository.PresetRepositoryImpl
import com.opxl.sleepslide.data.repository.SoundRepositoryImpl
import com.opxl.sleepslide.data.repository.UserPreferencesRepositoryImpl
import com.opxl.sleepslide.data.repository.VolumeMemoryRepositoryImpl
import com.opxl.sleepslide.domain.repository.PlayHistoryRepository
import com.opxl.sleepslide.domain.repository.PresetRepository
import com.opxl.sleepslide.domain.repository.PurchaseRepository
import com.opxl.sleepslide.domain.repository.SoundRepository
import com.opxl.sleepslide.domain.repository.UserPreferencesRepository
import com.opxl.sleepslide.domain.repository.VolumeMemoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSoundRepository(impl: SoundRepositoryImpl): SoundRepository

    @Binds
    @Singleton
    abstract fun bindPresetRepository(impl: PresetRepositoryImpl): PresetRepository

    @Binds
    @Singleton
    abstract fun bindPurchaseRepository(impl: PurchaseRepositoryImpl): PurchaseRepository

    @Binds
    @Singleton
    abstract fun bindPlayHistoryRepository(impl: PlayHistoryRepositoryImpl): PlayHistoryRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindVolumeMemoryRepository(impl: VolumeMemoryRepositoryImpl): VolumeMemoryRepository
}