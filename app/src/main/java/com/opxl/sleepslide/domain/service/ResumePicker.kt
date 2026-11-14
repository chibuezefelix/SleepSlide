package com.opxl.sleepslide.domain.service

import com.opxl.sleepslide.domain.model.Domain

/**
 * Chooses "the mix to resume". Shared by the Home resume card and [QuickStartService] so the
 * two can never disagree about what the user's last night was.
 *
 * Order: last-played preset, else the most recently used preset, else the last unsaved mix.
 *
 * @param maxAgeMs presets last used longer ago than this are skipped. Home passes 24 h; Quick
 *   Start passes null, because at bedtime the last mix is almost always the right one.
 */
object ResumePicker {

    sealed interface Pick {
        data class FromPreset(val preset: Domain.Preset, val lastUsedAt: Long) : Pick
        data class FromMix(val mix: Domain.SoundMix) : Pick
    }

    fun pick(
        prefs: Domain.UserPreferences,
        presets: List<Domain.Preset>,
        now: Long,
        maxAgeMs: Long?,
        deserializeMix: (String) -> Domain.SoundMix?,
    ): Pick? {
        fun fresh(usedAt: Long?): Boolean =
            usedAt != null && (maxAgeMs == null || now - usedAt < maxAgeMs)

        // 1. The preset the user last launched
        prefs.lastPlayedPresetId?.let { id ->
            val preset = presets.find { it.id == id }
            val usedAt = preset?.lastUsedAt
            if (preset != null && usedAt != null && fresh(usedAt)) {
                return Pick.FromPreset(preset, usedAt)
            }
        }

        // 2. The most recently used preset, even without an explicit last-played pref
        presets.filter { fresh(it.lastUsedAt) }
            .maxByOrNull { it.lastUsedAt!! }
            ?.let { return Pick.FromPreset(it, it.lastUsedAt!!) }

        // 3. The last unsaved mix
        prefs.lastPlayedMixJson?.let { json ->
            val mix = runCatching { deserializeMix(json) }.getOrNull()
            if (mix != null && !mix.isEmpty) return Pick.FromMix(mix)
        }

        return null
    }
}
