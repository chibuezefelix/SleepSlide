package com.opxl.sleepslide.presentation.widget

import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.service.ResumePicker

/** One-tap chip for a pinned preset on the wide widget. */
data class PinnedChip(val id: Long, val name: String)

/**
 * Everything the home-screen widget draws, decided in one pure place so it can be unit-tested.
 *
 * @property mixName name of the preset "Sleep now" would play; null for an unsaved mix.
 * @property hasMix false when there is nothing to resume yet (the widget then invites a first pick).
 * @property timerLabel e.g. "30m"; null when quick start does not start a timer.
 */
data class WidgetModel(
    val playing: Boolean,
    val hasMix: Boolean,
    val mixName: String?,
    val timerLabel: String?,
    val pinned: List<PinnedChip>,
) {
    companion object {
        const val MAX_PINNED = 3

        fun build(
            prefs: Domain.UserPreferences,
            presets: List<Domain.Preset>,
            playing: Boolean,
            activePresetId: Long?,
            now: Long,
            deserializeMix: (String) -> Domain.SoundMix?,
        ): WidgetModel {
            // Same rule as Sleep now itself, with no staleness limit.
            val pick = ResumePicker.pick(prefs, presets, now, maxAgeMs = null, deserializeMix)
            val resumeName = (pick as? ResumePicker.Pick.FromPreset)?.preset?.name

            val activeName = if (playing) presets.find { it.id == activePresetId }?.name else null

            return WidgetModel(
                playing    = playing,
                hasMix     = playing || pick != null,
                mixName    = activeName ?: resumeName,
                timerLabel = if (prefs.isQuickStartTimerEnabled) formatTimerChip(prefs.lastTimerDurationMs) else null,
                pinned     = presets.filter { it.isPinned }
                    .sortedBy { it.sortOrder }
                    .take(MAX_PINNED)
                    .map { PinnedChip(it.id, it.name) },
            )
        }
    }
}

/** 90 s -> "2m" (rounded up), 30 min -> "30m", 60 min -> "1h", 90 min -> "1h 30m". */
fun formatTimerChip(ms: Long): String {
    val minutes = ((ms + 59_999L) / 60_000L).coerceAtLeast(1L)
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "${m}m"
        m == 0L -> "${h}h"
        else    -> "${h}h ${m}m"
    }
}
