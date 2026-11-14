package com.opxl.sleepslide.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository that exposes wind-down notification preferences as a reactive
 * stream and provides write methods for toggling and time selection.
 *
 * Delegates all persistence to [UserPreferencesRepository] — wind-down state
 * lives in the same DataStore as all other user preferences.
 * This interface exists so the feature can be tested and injected independently
 * of the broader preferences surface.
 */
interface WindDownRepository {

    /** Emits the latest wind-down enabled state whenever it changes. */
    fun observeEnabled(): Flow<Boolean>

    /** Emits the latest scheduled hour whenever it changes. */
    fun observeHour(): Flow<Int>

    /** Emits the latest scheduled minute whenever it changes. */
    fun observeMinute(): Flow<Int>

    /** Emits the full (enabled, hour, minute) triple for callers that need all three. */
    fun observe(): Flow<WindDownConfig>

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setTime(hour: Int, minute: Int)

    /** Reads the current config synchronously — used for one-off reads. */
    suspend fun get(): WindDownConfig
}

data class WindDownConfig(
    val isEnabled: Boolean  = false,
    val hour: Int           = 21,
    val minute: Int         = 30,
)
