package com.opxl.sleepslide.presentation.widget

import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.opxl.sleepslide.R
import com.opxl.sleepslide.data.AudioServiceHolder
import com.opxl.sleepslide.di.ApplicationScope
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.service.QuickStartService
import com.opxl.sleepslide.domain.service.TimerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

private const val TAG = "SleepTile"
private const val CLICK_BUDGET_MS = 8_000L
private const val MESSAGE_MS = 3_000L

/**
 * Quick Settings tile. Inactive: "Sleep now", one tap starts the user's usual night. Active:
 * shows "Playing" with the time left, and a tap stops playback. Never opens the app.
 *
 * Shares [QuickStartService] with the reminder button, so the mix, entitlement gate and timer
 * rules are identical. Problems (nothing to play, premium-only mix, audio failure) are shown
 * briefly on the tile itself, since a tile cannot reliably open a screen after async work.
 */
@AndroidEntryPoint
class SleepTileService : TileService() {

    @Inject lateinit var quickStart: QuickStartService
    @Inject lateinit var audioServiceHolder: AudioServiceHolder
    @Inject lateinit var timerService: TimerService
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    // Lives while the tile is visible; the click work runs on appScope so it outlives this.
    private var uiScope: CoroutineScope? = null
    private var listenJob: Job? = null
    private var messageJob: Job? = null

    private sealed interface View {
        data object Idle : View
        data class Playing(val minutesLeft: Int?) : View
        data object Starting : View
        data class Message(val resId: Int) : View
    }

    // What the tile is really doing, kept current by observe(); a transient message or the
    // "Starting…" state is drawn over it and then falls back to it.
    @Volatile private var live: View = View.Idle
    @Volatile private var transient: View? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onStartListening() {
        super.onStartListening()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { uiScope = it }
        listenJob = scope.launch { observe() }
    }

    override fun onStopListening() {
        listenJob?.cancel()
        uiScope?.cancel()
        uiScope = null
        super.onStopListening()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observe() {
        val audio = audioServiceHolder.service.flatMapLatest { it?.audioState ?: flowOf(null) }
        // Whole minutes only, so the tile redraws once a minute rather than every tick.
        val minutes = timerService.timerState.map { t ->
            if (t.status == Domain.TimerStatus.RUNNING || t.status == Domain.TimerStatus.FADING) {
                ((t.remainingMs + 59_999L) / 60_000L).toInt()
            } else null
        }.distinctUntilChanged()

        combine(audio, minutes) { a, m ->
            val sounding = a?.playbackStatus.let {
                it == Domain.PlaybackStatus.PLAYING ||
                    it == Domain.PlaybackStatus.FADING_IN ||
                    it == Domain.PlaybackStatus.LOADING
            }
            if (sounding) View.Playing(m) else View.Idle
        }.distinctUntilChanged().collect { live = it; render(transient ?: it) }
    }

    override fun onClick() {
        super.onClick()
        val playing = qsTile?.state == Tile.STATE_ACTIVE
        if (isSecure && isLocked) {
            unlockAndRun { handleClick(playing) }
        } else {
            handleClick(playing)
        }
    }

    private fun handleClick(wasPlaying: Boolean) {
        if (wasPlaying) {
            appScope.launch { quickStart.stop() }
            return
        }
        showTransient(View.Starting, autoClear = false)
        appScope.launch {
            val outcome = runCatching {
                withTimeout(CLICK_BUDGET_MS) {
                    quickStart.start(QuickStartService.Options(maxAgeMs = null, applyTimer = true))
                }
            }.getOrElse {
                Log.e(TAG, "Quick start failed", it)
                QuickStartService.Outcome.Failed(it.message ?: "failed")
            }
            Log.d(TAG, "Outcome: $outcome")
            when (outcome) {
                is QuickStartService.Outcome.Started,
                is QuickStartService.Outcome.AlreadyPlaying -> clearTransient()
                is QuickStartService.Outcome.NeedsUpgrade ->
                    showTransient(View.Message(R.string.tile_needs_upgrade))
                is QuickStartService.Outcome.NothingToPlay ->
                    showTransient(View.Message(R.string.tile_nothing_to_play))
                is QuickStartService.Outcome.Failed ->
                    showTransient(View.Message(R.string.tile_failed))
            }
        }
    }

    private fun showTransient(view: View, autoClear: Boolean = true) {
        transient = view
        messageJob?.cancel()
        render(view)
        if (autoClear) {
            messageJob = appScope.launch {
                delay(MESSAGE_MS)
                clearTransient()
            }
        }
    }

    private fun clearTransient() {
        transient = null
        render(live)
    }

    /** Tile updates must come from the main thread; click work arrives on a background scope. */
    private fun render(view: View) {
        if (Looper.myLooper() == Looper.getMainLooper()) renderNow(view) else mainHandler.post { renderNow(view) }
    }

    private fun renderNow(view: View) {
        val tile = qsTile ?: return
        val (state, subtitle) = when (view) {
            is View.Idle     -> Tile.STATE_INACTIVE to null
            is View.Starting -> Tile.STATE_INACTIVE to getString(R.string.tile_starting)
            is View.Message  -> Tile.STATE_INACTIVE to getString(view.resId)
            is View.Playing  -> Tile.STATE_ACTIVE to
                (view.minutesLeft?.let { getString(R.string.tile_playing_minutes_left, it) }
                    ?: getString(R.string.tile_playing))
        }
        tile.state = state
        tile.label = getString(R.string.tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_moon)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = subtitle
        tile.updateTile()
    }
}
