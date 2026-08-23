package com.opxl.sleepslide.domain.model

object Domain {

    data class Sound(
        val id: String,
        val title: String,
        val category: SoundCategory,
        val assetPath: String,
        val isPremium: Boolean = false,
        val isBundled: Boolean = true,
        val downloadedPath: String? = null,
        val durationMs: Long? = null,
        val tags: List<String> = emptyList(),
        val frequencyHz: Int? = null,
    )

    enum class SoundCategory {
        TINNITUS,
        NATURE,
        AMBIENT,
        BINAURAL,
        GUIDED,
    }
// One active layer inside a [SoundMix].
// * Up to 3 layers play simultaneously, each with independent volume.
    data class SoundLayer(
        val sound: Sound,
        val volume: Float = 1.0f,           // 0.0 – 1.0
        val isMuted: Boolean = false,
        val position: Int = 0,              // slot index 0..2
    )

    data class SoundMix(
        val layers: List<SoundLayer>,           // max 3
        val masterVolume: Float = 1.0f,         // 0.0 – 1.0 global master
        val fadeInDurationMs: Long = 3_000L,    // US-004: default 3 s
        val lastPlayedAt: Long? = null,         // epoch ms — drives "Resume" card
    ) {
        init {
            require(layers.size <= 3) { "A mix supports at most 3 layers" }
        }

        val isEmpty: Boolean get() = layers.isEmpty()
    }

    data class Preset(
        val id: Long = 0L,
        val name: String,
        val mix: SoundMix,
        val sortOrder: Int = 0,
        val isPinned: Boolean = false,
        val createdAt: Long = System.currentTimeMillis(),
        val lastUsedAt: Long? = null,
        val emoji: String? = null,
    )

    data class PlaySession(
        val id: Long = 0L,
        val presetId: Long?,
        val mixSnapshot: String,
        val startedAt: Long,
        val endedAt: Long?,
        val durationPlayedMs: Long,
        val stoppedBy: StopReason,
    )

    enum class StopReason { USER, TIMER, AUDIO_FOCUS, BLUETOOTH, ERROR }

    data class TimerState(
        val status: TimerStatus,
        val durationMs: Long,
        val remainingMs: Long,
        val fadeBeforeEndMs: Long = 60_000L,
        val startedAt: Long? = null,
    )

    enum class TimerStatus {
        IDLE,
        RUNNING,
        FADING,     // inside the fade-out window
        FINISHED,
    }

    data class AudioState(
        val playbackStatus: PlaybackStatus,
        val activeMix: SoundMix? = null,
        val activePresetId: Long? = null,
        val audioFocusStatus: AudioFocusStatus,
        val isPlayingInBackground: Boolean = false,
        val isBluetoothConnected: Boolean = false,
        val isNightLockEnabled: Boolean = false,
        val fadeInProgress: Float? = null,
    )

    enum class PlaybackStatus {
        IDLE,
        LOADING,
        PLAYING,
        PAUSED,
        FADING_IN,
        FADING_OUT,
        ERROR,
    }

    enum class AudioFocusStatus {
        GAINED,
        LOST,
        LOST_TRANSIENT,
        DUCK,
    }

    data class Entitlement(
        val tier: EntitlementTier,
        val purchasedAt: Long? = null,
        val revenueCatUserId: String,
        val isRestored: Boolean = false,
    )

    enum class EntitlementTier {
        FREE,
        PREMIUM,
    }


    data class UserPreferences(
        // Onboarding
        val hasCompletedOnboarding: Boolean = false,
        val onboardingPath: OnboardingPath = OnboardingPath.NONE,

        // Playback defaults
        val defaultFadeInMs: Long = 3_000L,         // 1_000 – 10_000
        val defaultFadeOutMs: Long = 60_000L,
        val lastTimerDurationMs: Long = 30 * 60 * 1000L,   // 30 min default

        // Theme
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val darkModeStartHour: Int = 21,            // 9 PM
        val darkModeEndHour: Int = 7,               // 7 AM

        // Night lock
        val isNightLockEnabledByDefault: Boolean = false,

        // Battery optimisation
        val hasDismissedBatteryOptPrompt: Boolean = false,
        val batteryOptPromptShownCount: Int = 0,

        // Accessibility
        val isHighContrastEnabled: Boolean = false,

        // Last played
        val lastPlayedPresetId: Long? = null,
        val lastPlayedMixJson: String? = null,       
    )

    enum class ThemeMode { LIGHT, DARK, SYSTEM, SCHEDULED }

    enum class OnboardingPath {
        NONE,
        TINNITUS,
        GENERAL,
    }
}