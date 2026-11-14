package com.opxl.sleepslide.presentation.widget

import com.opxl.sleepslide.data.AudioServiceHolder
import com.opxl.sleepslide.data.repository.MixSerializer
import com.opxl.sleepslide.domain.repository.PresetRepository
import com.opxl.sleepslide.domain.repository.UserPreferencesRepository
import com.opxl.sleepslide.domain.service.QuickStartService
import com.opxl.sleepslide.domain.service.WindDownNotificationService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Widgets and their action callbacks are not Hilt components; they reach the graph through this. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun quickStart(): QuickStartService
    fun presets(): PresetRepository
    fun userPreferences(): UserPreferencesRepository
    fun audioServiceHolder(): AudioServiceHolder
    fun mixSerializer(): MixSerializer
    fun windDown(): WindDownNotificationService
}
