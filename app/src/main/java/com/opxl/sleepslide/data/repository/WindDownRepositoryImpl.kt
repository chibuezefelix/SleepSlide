package com.opxl.sleepslide.data.repository


import com.opxl.sleepslide.data.local.UserPrefsDataStore
import com.opxl.sleepslide.domain.repository.WindDownConfig
import com.opxl.sleepslide.domain.repository.WindDownRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads from and writes to the same [UserPrefsDataStore] as all other
 * preferences. Wind-down config is a subset of [UserPreferences] — this
 * impl projects only the three relevant fields so callers never need to
 * depend on the full preferences object just to read wind-down state.
 */
@Singleton
class WindDownRepositoryImpl @Inject constructor(
    private val dataStore: UserPrefsDataStore,
) : WindDownRepository {

    override fun observeEnabled(): Flow<Boolean> =
        dataStore.userPreferences.map { it.isWindDownEnabled }

    override fun observeHour(): Flow<Int> =
        dataStore.userPreferences.map { it.windDownHour }

    override fun observeMinute(): Flow<Int> =
        dataStore.userPreferences.map { it.windDownMinute }

    override fun observe(): Flow<WindDownConfig> =
        dataStore.userPreferences.map { prefs ->
            WindDownConfig(
                isEnabled = prefs.isWindDownEnabled,
                hour      = prefs.windDownHour,
                minute    = prefs.windDownMinute,
            )
        }

    override suspend fun get(): WindDownConfig {
        val prefs = dataStore.userPreferences.first()
        return WindDownConfig(
            isEnabled = prefs.isWindDownEnabled,
            hour      = prefs.windDownHour,
            minute    = prefs.windDownMinute,
        )
    }

    override suspend fun setEnabled(enabled: Boolean) =
        dataStore.setWindDownEnabled(enabled)

    override suspend fun setTime(hour: Int, minute: Int) =
        dataStore.setWindDownTime(hour, minute)
}