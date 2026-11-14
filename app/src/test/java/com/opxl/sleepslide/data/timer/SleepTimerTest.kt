package com.opxl.sleepslide.data.timer

import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.service.AudioService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sleep timer end to end, with no Activity or ViewModel: TimerServiceImpl keeps time,
 * SleepTimerController (as the audio service runs it) fades and stops, and the
 * backstop only matters when that stop did not happen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerTest {

    private class FakeBackstop : TimerBackstop {
        var scheduledToken: Long? = null
        var triggerAt: Long? = null
        override fun schedule(token: Long, triggerAtElapsedMs: Long) {
            scheduledToken = token
            triggerAt = triggerAtElapsedMs
        }
        override fun cancel() {
            scheduledToken = null
            triggerAt = null
        }
    }

    private val backstop = FakeBackstop()
    private val audioState = MutableStateFlow(
        Domain.AudioState(
            playbackStatus   = Domain.PlaybackStatus.PLAYING,
            audioFocusStatus = Domain.AudioFocusStatus.GAINED,
        )
    )
    private val audio = mockk<AudioService>(relaxed = true) {
        every { audioState } returns this@SleepTimerTest.audioState
    }

    private fun TestScope.timer(scope: CoroutineScope = backgroundScope) =
        TimerServiceImpl(backstop, scope) { testScheduler.currentTime }

    private fun TestScope.timerWithController(): TimerServiceImpl =
        timer().also {
            SleepTimerController(it, audio, backgroundScope).start()
            runCurrent()
        }

    private val thirtyMin = 30 * 60_000L
    private val fade = 60_000L

    @Test
    fun `timer running out with no UI stops playback and disarms the backstop`() = runTest {
        val timer = timerWithController()
        timer.start(thirtyMin, fade)
        val token = backstop.scheduledToken!!

        advanceTimeBy(thirtyMin + 1_000L)
        runCurrent()

        coVerify(exactly = 1) { audio.stop() }
        assertEquals(Domain.TimerStatus.FINISHED, timer.timerState.value.status)
        assertEquals(null, backstop.scheduledToken)
        assertFalse(timer.onBackstopFired(token))
    }

    @Test
    fun `entering the fade window fades out over the time left`() = runTest {
        val timer = timerWithController()
        timer.start(thirtyMin, fade)

        advanceTimeBy(thirtyMin - fade + 1_000L)
        runCurrent()

        assertEquals(Domain.TimerStatus.FADING, timer.timerState.value.status)
        coVerify(exactly = 1) { audio.fadeOut(match { it in 1L..fade }) }
        coVerify(exactly = 0) { audio.stop() }
    }

    @Test
    fun `adding time during the fade restores volume and adds exactly that time`() = runTest {
        val timer = timerWithController()
        timer.start(thirtyMin, fade)
        advanceTimeBy(thirtyMin - 30_000L + 1)
        runCurrent()
        val before = timer.timerState.value.remainingMs

        timer.addTime(5 * 60_000L)
        runCurrent()

        assertEquals(Domain.TimerStatus.RUNNING, timer.timerState.value.status)
        // Ticks are 1 s apart, so `before` may be up to a second stale.
        val expected = before + 5 * 60_000L
        assertEquals(expected.toDouble(), timer.timerState.value.remainingMs.toDouble(), 1_000.0)
        coVerify(exactly = 1) { audio.cancelFadeOut() }
        assertEquals(
            (testScheduler.currentTime + expected + TimerServiceImpl.BACKSTOP_GRACE_MS).toDouble(),
            backstop.triggerAt!!.toDouble(),
            1_000.0,
        )
    }

    @Test
    fun `failed stop leaves the backstop armed`() = runTest {
        coEvery { audio.stop() } throws IllegalStateException("player gone")
        val timer = timerWithController()
        timer.start(thirtyMin, fade)
        val token = backstop.scheduledToken!!

        advanceTimeBy(thirtyMin + 1_000L)
        runCurrent()

        assertEquals(token, backstop.scheduledToken)
        assertTrue(timer.onBackstopFired(token))
    }

    @Test
    fun `backstop finishes a timer whose ticker died`() = runTest {
        val tickerScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val timer = timer(tickerScope)
        timer.start(thirtyMin, fade)
        val token = backstop.scheduledToken!!
        tickerScope.cancel()

        advanceTimeBy(thirtyMin + TimerServiceImpl.BACKSTOP_GRACE_MS)
        assertEquals(Domain.TimerStatus.RUNNING, timer.timerState.value.status)

        assertTrue(timer.onBackstopFired(token))
        assertEquals(Domain.TimerStatus.FINISHED, timer.timerState.value.status)
    }

    @Test
    fun `an old alarm never stops a later session`() = runTest {
        val timer = timer()

        timer.start(thirtyMin, fade)
        val first = backstop.scheduledToken!!
        advanceTimeBy(1_000L)
        timer.start(thirtyMin, fade)
        assertFalse("restarted timer", timer.onBackstopFired(first))

        val second = backstop.scheduledToken!!
        timer.cancel()
        assertFalse("cancelled timer", timer.onBackstopFired(second))

        timer.start(60_000L, fade)
        val third = backstop.scheduledToken!!
        advanceTimeBy(61_000L)
        timer.onPlaybackStarted() // new mix, no timer, nothing acknowledged the old one
        assertFalse("new playback", timer.onBackstopFired(third))
        assertEquals(Domain.TimerStatus.IDLE, timer.timerState.value.status)
    }

    @Test
    fun `playback started during an active timer keeps it`() = runTest {
        val timer = timer()
        timer.start(thirtyMin, fade)
        val token = backstop.scheduledToken!!

        timer.onPlaybackStarted()

        assertEquals(Domain.TimerStatus.RUNNING, timer.timerState.value.status)
        assertEquals(token, backstop.scheduledToken)
    }

    @Test
    fun `reducing past zero finishes the timer`() = runTest {
        val timer = timerWithController()
        timer.start(5 * 60_000L, fade)

        timer.reduceTime(10 * 60_000L)
        runCurrent()

        assertEquals(Domain.TimerStatus.FINISHED, timer.timerState.value.status)
        coVerify(exactly = 1) { audio.stop() }
    }
}
