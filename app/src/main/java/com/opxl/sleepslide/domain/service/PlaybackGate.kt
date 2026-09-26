package com.opxl.sleepslide.domain.service

import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.model.Domain.isLockedFor
import com.opxl.sleepslide.domain.model.Domain.lockedLayers
import com.opxl.sleepslide.domain.observer.EntitlementObserver
import com.opxl.sleepslide.domain.repository.SoundRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single entitlement checkpoint in front of [AudioService.play] / [AudioService.crossfadeTo].
 * Every path that starts a stored or constructed mix calls [prepare] first.
 *
 * FREE users launching a mix with premium layers get the free layers and a [LockedLayersNotice];
 * the stored mix itself is never modified, so the layers return if they (re)subscribe.
 *
 * Notices are app-wide: launches usually navigate to the Player, so the screen that started
 * playback is gone before a snackbar could show. The NavGraph collects [notices].
 */
@Singleton
class PlaybackGate @Inject constructor(
    private val soundRepository: SoundRepository,
    private val entitlementObserver: EntitlementObserver,
) {

    private val _notices = Channel<LockedLayersNotice>(Channel.BUFFERED)
    val notices: Flow<LockedLayersNotice> = _notices.receiveAsFlow()

    sealed interface Decision {
        data class Play(val mix: Domain.SoundMix) : Decision
        /** Every layer is premium on the free tier. The user has already been told. */
        data object AllLocked : Decision
        /** None of the mix's sounds exist any more. */
        data object NothingToPlay : Decision
    }

    /**
     * Resolves [mix] against the catalogue and strips layers the current tier may not play.
     *
     * Enforced at launch only. If the subscription lapses while a premium layer is already
     * playing, that session runs to the end and the rule applies at the next launch — nothing
     * observes the tier to cut audio, because the listener is probably asleep. Pause/resume of
     * the running session is not a launch and is not gated.
     */
    suspend fun prepare(mix: Domain.SoundMix): Decision {
        val resolved = soundRepository.resolve(mix)
        if (resolved.isEmpty) return Decision.NothingToPlay

        val locked = resolved.lockedLayers(entitlementObserver.entitlement.value.tier)
        if (locked.isEmpty()) return Decision.Play(resolved)

        val playable = resolved.copy(layers = resolved.layers - locked.toSet())
        _notices.trySend(
            LockedLayersNotice(
                lockedTitles = locked.map { it.sound.title },
                playingRest  = !playable.isEmpty,
            )
        )
        return if (playable.isEmpty) Decision.AllLocked else Decision.Play(playable)
    }

    /** Single-sound variant for add/swap into a running mix. Posts a notice when locked. */
    fun isLocked(sound: Domain.Sound): Boolean {
        val locked = sound.isLockedFor(entitlementObserver.entitlement.value.tier)
        if (locked) _notices.trySend(LockedLayersNotice(listOf(sound.title), playingRest = false))
        return locked
    }
}

/** A launch skipped premium layers. [playingRest] is false when nothing could be played. */
data class LockedLayersNotice(
    val lockedTitles: List<String>,
    val playingRest: Boolean,
)
