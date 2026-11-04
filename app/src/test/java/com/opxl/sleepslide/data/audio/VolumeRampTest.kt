package com.opxl.sleepslide.data.audio

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The fade must end on time even when every step runs late — on device each 50ms step
 * took ~55ms, so a step-counted 60s fade was still at ~11% volume when the timer stopped.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VolumeRampTest {

    private val step = 50L

    /** Clock that also moves [lagPerStepMs] on every progress callback — a slow volume set. */
    private class LaggyClock(private val scope: TestScope, private val lagPerStepMs: Long) {
        var lag = 0L
        fun now() = scope.testScheduler.currentTime + lag
        fun onStep() { lag += lagPerStepMs }
    }

    private suspend fun TestScope.runRamp(durationMs: Long, lagPerStepMs: Long): Pair<List<Float>, Long> {
        val clock = LaggyClock(this, lagPerStepMs)
        val progress = mutableListOf<Float>()
        val start = clock.now()
        rampVolume(durationMs, step, clock::now) {
            progress += it
            clock.onStep()
        }
        return progress to clock.now() - start
    }

    @Test
    fun `slow steps do not stretch the fade`() = runTest {
        val duration = 60_000L
        val (progress, endedAt) = runRamp(duration, lagPerStepMs = 5L)

        assertEquals(1f, progress.last())
        // Ends within one laggy step of the requested duration. Counting steps instead
        // would end at 1200 × 55ms = 66s.
        assertTrue("ended at $endedAt", endedAt in duration..duration + step + 2 * 5L)
    }

    @Test
    fun `progress never goes backwards`() = runTest {
        val (progress, _) = runRamp(10_000L, lagPerStepMs = 7L)
        assertEquals(progress.sorted(), progress)
        assertEquals(0f, progress.first())
    }

    @Test
    fun `zero duration jumps straight to the end`() = runTest {
        val (progress, _) = runRamp(0L, lagPerStepMs = 0L)
        assertEquals(listOf(1f), progress)
    }
}
