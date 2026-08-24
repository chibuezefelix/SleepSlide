package com.opxl.sleepslide.di

import com.opxl.sleepslide.data.local.AppDatabase
import com.opxl.sleepslide.data.local.PlayHistoryDao
import com.opxl.sleepslide.data.local.PresetDao
import com.opxl.sleepslide.data.local.SoundDao
import com.opxl.sleepslide.data.local.SoundVolumeMemoryDao
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences_ds"
)

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides fun provideSoundDao(db: AppDatabase): SoundDao = db.soundDao()
    @Provides fun providePresetDao(db: AppDatabase): PresetDao = db.presetDao()
    @Provides fun providePlayHistoryDao(db: AppDatabase): PlayHistoryDao = db.playHistoryDao()
    @Provides fun provideSoundVolumeMemoryDao(db: AppDatabase): SoundVolumeMemoryDao = db.soundVolumeMemoryDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}

