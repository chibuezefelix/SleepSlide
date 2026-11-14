package com.opxl.sleepslide.domain.service

/**
 * Starts the user's usual night without any UI: last mix, gated by entitlement, optional timer.
 * The one place that knows how. Entry points (Home resume, widget, Quick Settings tile,
 * reminder action) call this and map the [Outcome] to their own surface.
 */
interface QuickStartService {

    data class Options(
        /** Skip presets last used longer ago than this. Null = no limit (bedtime surfaces). */
        val maxAgeMs: Long? = null,
        /** Start the sleep timer with the user's last duration and default fade-out. */
        val applyTimer: Boolean = false,
        /** Start this saved preset instead of the last mix (a widget's pinned-preset chip). */
        val presetId: Long? = null,
    )

    sealed interface Outcome {
        /** [presetName] is null for an unsaved mix. [timerMs] is null when no timer was started. */
        data class Started(val presetName: String?, val timerMs: Long?) : Outcome

        /** Sound was already playing; nothing restarted. [timerStarted] if a timer was added. */
        data class AlreadyPlaying(val timerStarted: Boolean) : Outcome

        /** Every layer of the last mix is premium and the user is on the free tier. */
        data object NeedsUpgrade : Outcome

        /** No mix was ever played, or its sounds no longer exist. Send the user to the Library. */
        data object NothingToPlay : Outcome

        /** The audio service did not come up, or audio focus was refused. */
        data class Failed(val reason: String) : Outcome
    }

    suspend fun start(options: Options = Options()): Outcome

    /** Stop playback and cancel any running timer. No-op when nothing is playing. */
    suspend fun stop()
}
