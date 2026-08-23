package com.opxl.sleepslide.data.local

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.opxl.sleepslide.domain.model.Domain
import org.json.JSONArray


object PrefKeys {
    val HAS_COMPLETED_ONBOARDING    = booleanPreferencesKey("has_completed_onboarding")
    val ONBOARDING_PATH             = stringPreferencesKey("onboarding_path")
    val DEFAULT_FADE_IN_MS          = longPreferencesKey("default_fade_in_ms")
    val DEFAULT_FADE_OUT_MS         = longPreferencesKey("default_fade_out_ms")
    val LAST_TIMER_DURATION_MS      = longPreferencesKey("last_timer_duration_ms")
    val THEME_MODE                  = stringPreferencesKey("theme_mode")
    val DARK_MODE_START_HOUR        = intPreferencesKey("dark_mode_start_hour")
    val DARK_MODE_END_HOUR          = intPreferencesKey("dark_mode_end_hour")
    val IS_NIGHT_LOCK_DEFAULT       = booleanPreferencesKey("is_night_lock_default")
    val HAS_DISMISSED_BATTERY_PROMPT = booleanPreferencesKey("has_dismissed_battery_prompt")
    val BATTERY_PROMPT_SHOWN_COUNT  = intPreferencesKey("battery_prompt_shown_count")
    val IS_HIGH_CONTRAST            = booleanPreferencesKey("is_high_contrast")
    val LAST_PLAYED_PRESET_ID       = longPreferencesKey("last_played_preset_id")
    val LAST_PLAYED_MIX_JSON        = stringPreferencesKey("last_played_mix_json")
}
object Mapper {

    fun Local.SoundEntity.toDomain(): Domain.Sound = Domain.Sound(
        id = id,
        title = title,
        category = Domain.SoundCategory.valueOf(category),
        assetPath = assetPath,
        isPremium = isPremium,
        isBundled = isBundled,
        downloadedPath = downloadedPath,
        durationMs = durationMs,
        tags = tags.toTagList(),
        frequencyHz = frequencyHz,
    )

    fun Domain.Sound.toEntity(): Local.SoundEntity = Local.SoundEntity(
        id = id,
        title = title,
        category = category.name,
        assetPath = assetPath,
        isPremium = isPremium,
        isBundled = isBundled,
        downloadedPath = downloadedPath,
        durationMs = durationMs,
        tags = tags.toTagJson(),
        frequencyHz = frequencyHz,
    )


    private fun String.toTagList(): List<String> = buildList {
        val arr = JSONArray(this@toTagList)
        repeat(arr.length()) { add(arr.getString(it)) }
    }

    private fun List<String>.toTagJson(): String =
        JSONArray(this).toString()


    fun Local.PresetWithLayers.toDomain(soundMap: Map<String, Domain.Sound>): Domain.Preset =
        Domain.Preset(
            id = preset.id,
            name = preset.name,
            sortOrder = preset.sortOrder,
            isPinned = preset.isPinned,
            createdAt = preset.createdAt,
            lastUsedAt = preset.lastUsedAt,
            emoji = preset.emoji,
            mix = Domain.SoundMix(
                layers = layers
                    .sortedBy { it.position }
                    .mapNotNull { it.toDomain(soundMap) },
                masterVolume = preset.masterVolume,
                fadeInDurationMs = preset.fadeInDurationMs,
            ),
        )

    private fun Local.PresetLayerEntity.toDomain(soundMap: Map<String, Domain.Sound>): Domain.SoundLayer? {
        val sound = soundMap[soundId] ?: return null
        return Domain.SoundLayer(
            sound = sound,
            volume = volume,
            isMuted = isMuted,
            position = position,
        )
    }

    fun Domain.Preset.toEntity(): Local.PresetEntity = Local.PresetEntity(
        id = id,
        name = name,
        sortOrder = sortOrder,
        isPinned = isPinned,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        emoji = emoji,
        masterVolume = mix.masterVolume,
        fadeInDurationMs = mix.fadeInDurationMs,
    )

    fun Domain.Preset.toLayerEntities(): List<Local.PresetLayerEntity> =
        mix.layers.map { layer ->
            Local.PresetLayerEntity(
                presetId = id,
                soundId  = layer.sound.id,
                position = layer.position,
                volume   = layer.volume,
                isMuted  = layer.isMuted,
            )
        }

    fun Preferences.toUserPreferences(): Domain.UserPreferences {
        val defaults = Domain.UserPreferences()
        return Domain.UserPreferences(
            hasCompletedOnboarding      = this[PrefKeys.HAS_COMPLETED_ONBOARDING]
                ?: defaults.hasCompletedOnboarding,
            onboardingPath              = this[PrefKeys.ONBOARDING_PATH]
                ?.let { runCatching { Domain.OnboardingPath.valueOf(it) }.getOrNull() }
                ?: defaults.onboardingPath,
            defaultFadeInMs             = this[PrefKeys.DEFAULT_FADE_IN_MS]
                ?: defaults.defaultFadeInMs,
            defaultFadeOutMs            = this[PrefKeys.DEFAULT_FADE_OUT_MS]
                ?: defaults.defaultFadeOutMs,
            lastTimerDurationMs         = this[PrefKeys.LAST_TIMER_DURATION_MS]
                ?: defaults.lastTimerDurationMs,
            themeMode                   = this[PrefKeys.THEME_MODE]
                ?.let { runCatching { Domain.ThemeMode.valueOf(it) }.getOrNull() }
                ?: defaults.themeMode,
            darkModeStartHour           = this[PrefKeys.DARK_MODE_START_HOUR]
                ?: defaults.darkModeStartHour,
            darkModeEndHour             = this[PrefKeys.DARK_MODE_END_HOUR]
                ?: defaults.darkModeEndHour,
            isNightLockEnabledByDefault = this[PrefKeys.IS_NIGHT_LOCK_DEFAULT]
                ?: defaults.isNightLockEnabledByDefault,
            hasDismissedBatteryOptPrompt= this[PrefKeys.HAS_DISMISSED_BATTERY_PROMPT]
                ?: defaults.hasDismissedBatteryOptPrompt,
            batteryOptPromptShownCount  = this[PrefKeys.BATTERY_PROMPT_SHOWN_COUNT]
                ?: defaults.batteryOptPromptShownCount,
            isHighContrastEnabled       = this[PrefKeys.IS_HIGH_CONTRAST]
                ?: defaults.isHighContrastEnabled,
            lastPlayedPresetId          = this[PrefKeys.LAST_PLAYED_PRESET_ID],
            lastPlayedMixJson           = this[PrefKeys.LAST_PLAYED_MIX_JSON],
        )
    }

    fun Domain.UserPreferences.toPreferencesMap(): Map<Preferences.Key<*>, Any?> = mapOf(
//        PrefKeys.HAS_COMPLETED_ONBOARDING     to hasCompletedOnboarding,
//        PrefKeys.ONBOARDING_PATH              to onboardingPath.name,
//        PrefKeys.DEFAULT_FADE_IN_MS           to defaultFadeInMs,
//        PrefKeys.DEFAULT_FADE_OUT_MS          to defaultFadeOutMs,
//        PrefKeys.LAST_TIMER_DURATION_MS       to lastTimerDurationMs,
//        PrefKeys.THEME_MODE                   to themeMode.name,
//        PrefKeys.DARK_MODE_START_HOUR         to darkModeStartHour,
//        PrefKeys.DARK_MODE_END_HOUR           to darkModeEndHour,
//        PrefKeys.IS_NIGHT_LOCK_DEFAULT        to isNightLockEnabledByDefault,
//        PrefKeys.HAS_DISMISSED_BATTERY_PROMPT to hasDismissedBatteryOptPrompt,
//        PrefKeys.BATTERY_PROMPT_SHOWN_COUNT   to batteryOptPromptShownCount,
//        PrefKeys.IS_HIGH_CONTRAST             to isHighContrastEnabled,
//        PrefKeys.LAST_PLAYED_PRESET_ID        to lastPlayedPresetId,
//        PrefKeys.LAST_PLAYED_MIX_JSON         to lastPlayedMixJson,
    )

    fun MutablePreferences.applyFrom(prefs: Domain.UserPreferences) {
        this[PrefKeys.HAS_COMPLETED_ONBOARDING]      = prefs.hasCompletedOnboarding
        this[PrefKeys.ONBOARDING_PATH]               = prefs.onboardingPath.name
        this[PrefKeys.DEFAULT_FADE_IN_MS]            = prefs.defaultFadeInMs
        this[PrefKeys.DEFAULT_FADE_OUT_MS]           = prefs.defaultFadeOutMs
        this[PrefKeys.LAST_TIMER_DURATION_MS]        = prefs.lastTimerDurationMs
        this[PrefKeys.THEME_MODE]                    = prefs.themeMode.name
        this[PrefKeys.DARK_MODE_START_HOUR]          = prefs.darkModeStartHour
        this[PrefKeys.DARK_MODE_END_HOUR]            = prefs.darkModeEndHour
        this[PrefKeys.IS_NIGHT_LOCK_DEFAULT]         = prefs.isNightLockEnabledByDefault
        this[PrefKeys.HAS_DISMISSED_BATTERY_PROMPT]  = prefs.hasDismissedBatteryOptPrompt
        this[PrefKeys.BATTERY_PROMPT_SHOWN_COUNT]    = prefs.batteryOptPromptShownCount
        this[PrefKeys.IS_HIGH_CONTRAST]              = prefs.isHighContrastEnabled
        prefs.lastPlayedPresetId?.let { this[PrefKeys.LAST_PLAYED_PRESET_ID] = it }
        prefs.lastPlayedMixJson?.let  { this[PrefKeys.LAST_PLAYED_MIX_JSON]  = it }
    }

    fun Local.PlayHistoryEntity.toDomain(): Domain.PlaySession = Domain.PlaySession(
        id = id,
        presetId = presetId,
        mixSnapshot = mixSnapshot,
        startedAt = startedAt,
        endedAt = endedAt,
        durationPlayedMs = durationPlayedMs,
        stoppedBy = stoppedBy.toDomain(),
    )

    private fun Local.StopReason.toDomain(): Domain.StopReason =
        Domain.StopReason.valueOf(name)

    fun Domain.PlaySession.toEntity(): Local.PlayHistoryEntity = Local.PlayHistoryEntity(
        id = id,
        presetId = presetId,
        mixSnapshot = mixSnapshot,
        startedAt = startedAt,
        endedAt = endedAt,
        durationPlayedMs = durationPlayedMs,
        stoppedBy = Local.StopReason.valueOf(stoppedBy.name),
    )
}