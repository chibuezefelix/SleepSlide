package com.opxl.sleepslide.data.repository

import com.opxl.sleepslide.data.local.UserPrefsDataStore
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: UserPrefsDataStore,
) : UserPreferencesRepository {

    override fun observe(): Flow<Domain.UserPreferences> = dataStore.userPreferences

    override suspend fun get(): Domain.UserPreferences = dataStore.userPreferences.first()

    override suspend fun completeOnboarding(path: Domain.OnboardingPath) =
        dataStore.setOnboardingComplete(path)

    override suspend fun setThemeMode(mode: Domain.ThemeMode) =
        dataStore.setThemeMode(mode)

    override suspend fun setDarkModeWindow(startHour: Int, endHour: Int) =
        dataStore.setDarkModeWindow(startHour, endHour)

    override suspend fun setDefaultFadeIn(durationMs: Long) =
        dataStore.setDefaultFadeIn(durationMs)

    override suspend fun setDefaultFadeOut(durationMs: Long) =
        dataStore.setDefaultFadeOut(durationMs)

    override suspend fun setLastTimerDuration(durationMs: Long) =
        dataStore.setLastTimerDuration(durationMs)

    override suspend fun setNightLockDefault(enabled: Boolean) =
        dataStore.setNightLockDefault(enabled)

    override suspend fun setHighContrast(enabled: Boolean) =
        dataStore.setHighContrast(enabled)

    override suspend fun recordBatteryOptPromptShown() =
        dataStore.markBatteryOptPromptShown()

    override suspend fun dismissBatteryOptPrompt() =
        dataStore.dismissBatteryOptPrompt()

    override suspend fun setLastPlayedPreset(presetId: Long) =
        dataStore.setLastPlayedPreset(presetId)

    override suspend fun setLastPlayedEphemeralMix(mixJson: String) =
        dataStore.setLastPlayedEphemeralMix(mixJson)

    override suspend fun clearLastPlayed() =
        dataStore.clearLastPlayed()

    override suspend fun reset() =
        dataStore.clear()
}