package com.opxl.sleepslide.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.opxl.sleepslide.data.local.Mapper.applyFrom
import com.opxl.sleepslide.data.local.Mapper.toUserPreferences
import com.opxl.sleepslide.domain.model.Domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPrefsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val userPreferences: Flow<Domain.UserPreferences> = dataStore.data
        .map { it.toUserPreferences() }

    suspend fun save(prefs: Domain.UserPreferences) {
        dataStore.edit { it.applyFrom(prefs) }
    }

    suspend fun setOnboardingComplete(path: Domain.OnboardingPath) {
        dataStore.edit {
            it[PrefKeys.HAS_COMPLETED_ONBOARDING] = true
            it[PrefKeys.ONBOARDING_PATH] = path.name
        }
    }

    suspend fun setThemeMode(mode: Domain.ThemeMode) {
        dataStore.edit { it[PrefKeys.THEME_MODE] = mode.name }
    }

    suspend fun setDarkModeWindow(startHour: Int, endHour: Int) {
        dataStore.edit {
            it[PrefKeys.DARK_MODE_START_HOUR] = startHour
            it[PrefKeys.DARK_MODE_END_HOUR] = endHour
        }
    }

    suspend fun setDefaultFadeIn(durationMs: Long) {
        dataStore.edit { it[PrefKeys.DEFAULT_FADE_IN_MS] = durationMs }
    }

    suspend fun setDefaultFadeOut(durationMs: Long) {
        dataStore.edit { it[PrefKeys.DEFAULT_FADE_OUT_MS] = durationMs }
    }

    suspend fun setLastTimerDuration(durationMs: Long) {
        dataStore.edit { it[PrefKeys.LAST_TIMER_DURATION_MS] = durationMs }
    }

    suspend fun setNightLockDefault(enabled: Boolean) {
        dataStore.edit { it[PrefKeys.IS_NIGHT_LOCK_DEFAULT] = enabled }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        dataStore.edit { it[PrefKeys.IS_HIGH_CONTRAST] = enabled }
    }

    suspend fun markBatteryOptPromptShown() {
        dataStore.edit {
            val current = it[PrefKeys.BATTERY_PROMPT_SHOWN_COUNT] ?: 0
            it[PrefKeys.BATTERY_PROMPT_SHOWN_COUNT] = current + 1
        }
    }

    suspend fun dismissBatteryOptPrompt() {
        dataStore.edit { it[PrefKeys.HAS_DISMISSED_BATTERY_PROMPT] = true }
    }

    suspend fun setLastPlayedPreset(presetId: Long) {
        dataStore.edit {
            it[PrefKeys.LAST_PLAYED_PRESET_ID] = presetId
            it.remove(PrefKeys.LAST_PLAYED_MIX_JSON)
        }
    }

    suspend fun setLastPlayedEphemeralMix(mixJson: String) {
        dataStore.edit {
            it[PrefKeys.LAST_PLAYED_MIX_JSON] = mixJson
            it.remove(PrefKeys.LAST_PLAYED_PRESET_ID)
        }
    }

    suspend fun clearLastPlayed() {
        dataStore.edit {
            it.remove(PrefKeys.LAST_PLAYED_PRESET_ID)
            it.remove(PrefKeys.LAST_PLAYED_MIX_JSON)
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}