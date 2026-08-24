package com.opxl.sleepslide.domain.repository


import com.opxl.sleepslide.domain.model.Domain
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {

    fun observe(): Flow<Domain.UserPreferences>

    suspend fun get(): Domain.UserPreferences

    suspend fun completeOnboarding(path: Domain.OnboardingPath)

    suspend fun setThemeMode(mode: Domain.ThemeMode)

    suspend fun setDarkModeWindow(startHour: Int, endHour: Int)

    suspend fun setDefaultFadeIn(durationMs: Long)

    suspend fun setDefaultFadeOut(durationMs: Long)

    suspend fun setLastTimerDuration(durationMs: Long)

    suspend fun setNightLockDefault(enabled: Boolean)

    suspend fun setHighContrast(enabled: Boolean)

    suspend fun recordBatteryOptPromptShown()

    suspend fun dismissBatteryOptPrompt()

    suspend fun setLastPlayedPreset(presetId: Long)

    suspend fun setLastPlayedEphemeralMix(mixJson: String)

    suspend fun clearLastPlayed()

    suspend fun reset()
}