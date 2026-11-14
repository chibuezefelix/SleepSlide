package com.opxl.sleepslide.data.timer


import android.os.SystemClock
import com.opxl.sleepslide.di.ApplicationScope
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.model.Domain.TimerState
import com.opxl.sleepslide.domain.service.TimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sleep-timer clock. It only keeps time — [SleepTimerController] inside the audio
 * service turns FADING / [finished] into a fade and a stop.
 *
 * Every timer also arms a [TimerBackstop] alarm for end + [BACKSTOP_GRACE_MS]. It is
 * disarmed once the service confirms the stop ([acknowledgeFinished]); if that never
 * happens, the alarm stops playback itself. Each timer gets a token so an old alarm
 * can never stop a later session.
 *
 * Times are [SystemClock.elapsedRealtime] — wall-clock changes overnight (time zone,
 * network time) must not move the timer.
 */
@Singleton
class TimerServiceImpl internal constructor(
    private val backstop: TimerBackstop,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
) : TimerService {

    @Inject constructor(
        backstop: TimerBackstop,
        @ApplicationScope scope: CoroutineScope,
    ) : this(backstop, scope, SystemClock::elapsedRealtime)

    companion object {
        private const val TICK_INTERVAL_MS = 1_000L
        val BACKSTOP_GRACE_MS = TimeUnit.MINUTES.toMillis(2)
    }

    private val _timerState = MutableStateFlow(idleState())
    override val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _finished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val finished: SharedFlow<Unit> = _finished.asSharedFlow()

    private var tickJob: Job? = null

    private val lock = Any()
    /** Token of the current timer; re-armed with the same token when time changes. */
    private var currentToken = 0L
    /** Token of the armed backstop alarm, null when none is armed. */
    private var armedToken: Long? = null

    override suspend fun start(durationMs: Long, fadeBeforeEndMs: Long) {
        cancel()

        val startedAt = clock()
        currentToken = startedAt
        _timerState.value = TimerState(
            status = Domain.TimerStatus.RUNNING,
            durationMs = durationMs,
            remainingMs = durationMs,
            fadeBeforeEndMs = fadeBeforeEndMs,
            startedAt = startedAt,
        )

        armBackstop()
        startTicking()
    }

    override suspend fun cancel() {
        tickJob?.cancel()
        tickJob = null
        disarmBackstop()
        _timerState.value = idleState()
    }

    override suspend fun addTime(extraMs: Long) {
        if (!_timerState.value.isActive()) return

        _timerState.update { it.copy(durationMs = it.durationMs + extraMs) }
        refresh()
        armBackstop()
    }

    override suspend fun reduceTime(reduceMs: Long) {
        if (!_timerState.value.isActive()) return

        // Shift the start back instead of shrinking durationMs so the progress ring
        // jumps forward rather than rescaling.
        _timerState.update { it.copy(startedAt = it.startedAt?.minus(reduceMs)) }
        refresh()
        if (_timerState.value.isActive()) armBackstop()
    }

    /** The audio service stopped playback for this timer — the backstop is not needed. */
    fun acknowledgeFinished() = disarmBackstop()

    /**
     * New playback started. A finished timer goes back to IDLE, and an armed backstop
     * from it is dropped so it cannot cut the new session. An active timer is untouched.
     */
    fun onPlaybackStarted() {
        val state = _timerState.value
        if (state.isActive()) return
        disarmBackstop()
        if (state.status == Domain.TimerStatus.FINISHED) _timerState.value = idleState()
    }

    /**
     * The backstop alarm fired. Returns true only for the armed token — the caller then
     * stops whatever is still playing. A timer still marked active is finished here.
     */
    fun onBackstopFired(token: Long): Boolean {
        val matched = synchronized(lock) {
            if (armedToken != token) return@synchronized false
            armedToken = null
            true
        }
        if (matched && _timerState.value.isActive()) finish()
        return matched
    }

    // Internal

    private fun startTicking() {
        tickJob = scope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                if (!_timerState.value.isActive()) break
                refresh()
            }
        }
    }

    private fun refresh() {
        val state = _timerState.value
        if (!state.isActive()) return

        val remaining = (state.endsAt() - clock()).coerceAtLeast(0L)
        if (remaining <= 0L) {
            finish()
            return
        }
        val status = if (remaining <= state.fadeBeforeEndMs) Domain.TimerStatus.FADING else Domain.TimerStatus.RUNNING
        _timerState.update { it.copy(remainingMs = remaining, status = status) }
    }

    private fun finish() {
        tickJob?.cancel()
        tickJob = null
        _timerState.update { it.copy(status = Domain.TimerStatus.FINISHED, remainingMs = 0L) }
        _finished.tryEmit(Unit)
    }

    private fun armBackstop() {
        val token = currentToken
        synchronized(lock) { armedToken = token }
        backstop.schedule(token, _timerState.value.endsAt() + BACKSTOP_GRACE_MS)
    }

    private fun disarmBackstop() {
        val wasArmed = synchronized(lock) {
            (armedToken != null).also { armedToken = null }
        }
        if (wasArmed) backstop.cancel()
    }

    private fun TimerState.isActive() =
        status == Domain.TimerStatus.RUNNING || status == Domain.TimerStatus.FADING

    private fun TimerState.endsAt() = (startedAt ?: clock()) + durationMs

    private fun idleState() = TimerState(
        status      = Domain.TimerStatus.IDLE,
        durationMs  = 0L,
        remainingMs = 0L,
    )
}
