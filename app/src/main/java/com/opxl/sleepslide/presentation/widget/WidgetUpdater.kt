package com.opxl.sleepslide.presentation.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.opxl.sleepslide.data.AudioServiceHolder
import com.opxl.sleepslide.di.ApplicationScope
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.repository.PresetRepository
import com.opxl.sleepslide.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WidgetUpdater"

/**
 * Redraws the home-screen widget when something it shows changes: playing or not (and which
 * preset), the last mix, the quick-start timer setting, or the preset list. State-driven, not
 * on a timer, so it costs no battery while nothing changes. `updateAll` does nothing when no
 * widget is placed.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioServiceHolder: AudioServiceHolder,
    private val presetRepository: PresetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun start() {
        val playback = audioServiceHolder.service
            .flatMapLatest { it?.audioState ?: flowOf(null) }
            .map { a ->
                val sounding = a?.playbackStatus.let {
                    it == Domain.PlaybackStatus.PLAYING || it == Domain.PlaybackStatus.FADING_IN ||
                        it == Domain.PlaybackStatus.LOADING
                }
                sounding to a?.activePresetId
            }
            .distinctUntilChanged()

        val prefs = userPreferencesRepository.observe()
            .map { listOf(it.lastPlayedPresetId, it.lastPlayedMixJson, it.isQuickStartTimerEnabled, it.lastTimerDurationMs) }
            .distinctUntilChanged()

        val presets = presetRepository.observeAll()
            .map { list -> list.map { listOf(it.id, it.name, it.isPinned, it.sortOrder, it.lastUsedAt) } }
            .distinctUntilChanged()

        scope.launch {
            combine(playback, prefs, presets) { _, _, _ -> Unit }
                .drop(1)            // the first emission is just the current state
                .debounce(300)      // a play() changes several of these at once
                .collect {
                    runCatching { SleepWidget().updateAll(context) }
                        .onFailure { Log.w(TAG, "Widget update failed", it) }
                }
        }
    }
}
