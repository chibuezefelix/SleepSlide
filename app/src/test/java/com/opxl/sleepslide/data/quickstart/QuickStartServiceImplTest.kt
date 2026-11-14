package com.opxl.sleepslide.data.quickstart

import android.content.Context
import com.opxl.sleepslide.data.AudioServiceHolder
import com.opxl.sleepslide.data.repository.MixSerializer
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.repository.PresetRepository
import com.opxl.sleepslide.domain.repository.SoundRepository
import com.opxl.sleepslide.domain.repository.UserPreferencesRepository
import com.opxl.sleepslide.domain.service.AudioService
import com.opxl.sleepslide.domain.service.PlaybackGate
import com.opxl.sleepslide.domain.service.QuickStartService.Options
import com.opxl.sleepslide.domain.service.QuickStartService.Outcome
import com.opxl.sleepslide.domain.service.TimerService
import com.opxl.sleepslide.testutil.Fixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickStartServiceImplTest {

    private val dayMs = 24 * 60 * 60 * 1000L

    private fun stateOf(status: Domain.PlaybackStatus) =
        Domain.AudioState(playbackStatus = status, audioFocusStatus = Domain.AudioFocusStatus.NONE)

    private val audioState = MutableStateFlow(stateOf(Domain.PlaybackStatus.IDLE))
    private val audio = mockk<AudioService>(relaxed = true) {
        every { audioState } returns this@QuickStartServiceImplTest.audioState
    }
    private val holder = AudioServiceHolder().also { it.attach(audio) }
    private val emptyHolder = AudioServiceHolder()   // service not running yet
    private val context = mockk<Context>(relaxed = true)

    private val timerState = MutableStateFlow(
        Domain.TimerState(Domain.TimerStatus.IDLE, durationMs = 0, remainingMs = 0)
    )
    private val timer = mockk<TimerService>(relaxed = true) {
        every { timerState } returns this@QuickStartServiceImplTest.timerState
    }

    private val presets = mockk<PresetRepository>(relaxed = true)
    private val prefs = mockk<UserPreferencesRepository>(relaxed = true)
    private val serializer = mockk<MixSerializer>(relaxed = true)
    private val soundRepository = mockk<SoundRepository> {
        coEvery { resolve(any()) } answers { firstArg() }
    }

    private fun service(
        tier: Domain.EntitlementTier = Domain.EntitlementTier.PREMIUM,
        holder: AudioServiceHolder = this.holder,
    ) =
        QuickStartServiceImpl(
            context = context,
            presetRepository = presets,
            userPreferencesRepository = prefs,
            mixSerializer = serializer,
            playbackGate = PlaybackGate(soundRepository, Fixtures.entitlementObserver(tier)),
            audioServiceHolder = holder,
            timerService = timer,
            main = Dispatchers.Unconfined,
        )

    private fun givenPreset(usedAgoMs: Long = 1_000L, mix: Domain.SoundMix = Fixtures.deltaAndRain()):
        Domain.Preset {
        val preset = Domain.Preset(
            id = 7, name = "Night", mix = mix,
            lastUsedAt = System.currentTimeMillis() - usedAgoMs,
        )
        every { presets.observeAll() } returns flowOf(listOf(preset))
        coEvery { prefs.get() } returns Domain.UserPreferences(
            lastPlayedPresetId = 7,
            lastTimerDurationMs = 45 * 60 * 1000L,
            defaultFadeOutMs = 30_000L,
        )
        return preset
    }

    @Test
    fun `starts the last preset, records it and starts the timer`() = runTest {
        givenPreset()

        val outcome = service().start(Options(applyTimer = true))

        assertEquals(Outcome.Started("Night", 45 * 60 * 1000L), outcome)
        coVerify { audio.play(any(), any()) }
        coVerify { presets.recordUsed(7) }
        coVerify { prefs.setLastPlayedPreset(7) }
        coVerify { timer.start(45 * 60 * 1000L, 30_000L) }
    }

    @Test
    fun `headless start plays even when audio focus is denied (Android 15 hardening)`() = runTest {
        givenPreset()

        service().start()

        coVerify { audio.play(any(), true) }
    }

    @Test
    fun `no timer when not asked`() = runTest {
        givenPreset()

        assertEquals(Outcome.Started("Night", null), service().start(Options(applyTimer = false)))
        coVerify(exactly = 0) { timer.start(any(), any()) }
    }

    @Test
    fun `quick start timer switched off in Settings means no timer even when asked`() = runTest {
        val preset = givenPreset()
        coEvery { prefs.get() } returns Domain.UserPreferences(
            lastPlayedPresetId = preset.id,
            isQuickStartTimerEnabled = false,
        )

        assertEquals(Outcome.Started("Night", null), service().start(Options(applyTimer = true)))
        coVerify(exactly = 0) { timer.start(any(), any()) }
    }

    @Test
    fun `a chosen preset is played instead of the last mix`() = runTest {
        val last = Domain.Preset(id = 7, name = "Night", mix = Fixtures.deltaAndRain(),
            lastUsedAt = System.currentTimeMillis())
        val other = Domain.Preset(id = 9, name = "Storm", mix = Fixtures.deltaAndRain())
        every { presets.observeAll() } returns flowOf(listOf(last, other))
        coEvery { prefs.get() } returns Domain.UserPreferences(lastPlayedPresetId = 7)

        val outcome = service().start(Options(presetId = 9))

        assertEquals(Outcome.Started("Storm", null), outcome)
        coVerify { presets.recordUsed(9) }
        coVerify { prefs.setLastPlayedPreset(9) }
    }

    @Test
    fun `a chosen preset that no longer exists gives NothingToPlay`() = runTest {
        givenPreset()

        assertEquals(Outcome.NothingToPlay, service().start(Options(presetId = 404)))
        coVerify(exactly = 0) { audio.play(any(), any()) }
    }

    @Test
    fun `bedtime surfaces ignore staleness, Home's 24 h limit applies when passed`() = runTest {
        givenPreset(usedAgoMs = 3 * dayMs)

        assertTrue(service().start(Options(maxAgeMs = null)) is Outcome.Started)
        assertEquals(Outcome.NothingToPlay, service().start(Options(maxAgeMs = dayMs)))
    }

    @Test
    fun `free user with an all-premium mix is sent to upgrade, nothing plays`() = runTest {
        givenPreset(mix = Domain.SoundMix(listOf(Domain.SoundLayer(sound = Fixtures.delta))))

        val outcome = service(Domain.EntitlementTier.FREE).start()

        assertEquals(Outcome.NeedsUpgrade, outcome)
        coVerify(exactly = 0) { audio.play(any(), any()) }
    }

    @Test
    fun `free user with a mixed mix plays only the free layers`() = runTest {
        givenPreset()

        service(Domain.EntitlementTier.FREE).start()

        coVerify {
            audio.play(match { m -> m.layers.map { it.sound.id } == listOf("rain_light") }, any())
        }
    }

    @Test
    fun `already playing does not restart, but adds a timer when none is running`() = runTest {
        givenPreset()
        audioState.value = stateOf(Domain.PlaybackStatus.PLAYING)

        val outcome = service().start(Options(applyTimer = true))

        assertEquals(Outcome.AlreadyPlaying(timerStarted = true), outcome)
        coVerify(exactly = 0) { audio.play(any(), any()) }
        coVerify { timer.start(any(), any()) }
    }

    @Test
    fun `already playing with a running timer leaves the timer alone`() = runTest {
        givenPreset()
        audioState.value = stateOf(Domain.PlaybackStatus.PLAYING)
        timerState.value = Domain.TimerState(Domain.TimerStatus.RUNNING, 1_000, 500)

        val outcome = service().start(Options(applyTimer = true))

        assertEquals(Outcome.AlreadyPlaying(timerStarted = false), outcome)
        coVerify(exactly = 0) { timer.start(any(), any()) }
    }

    @Test
    fun `nothing to play never starts the audio service, so no idle Playing notice appears`() = runTest {
        every { presets.observeAll() } returns flowOf(emptyList())
        coEvery { prefs.get() } returns Domain.UserPreferences()

        assertEquals(Outcome.NothingToPlay, service(holder = emptyHolder).start())

        verify(exactly = 0) { context.startService(any()) }
        verify(exactly = 0) { context.startForegroundService(any()) }
    }

    @Test
    fun `an all-premium mix on the free tier never starts the audio service either`() = runTest {
        givenPreset(mix = Domain.SoundMix(listOf(Domain.SoundLayer(sound = Fixtures.delta))))

        assertEquals(
            Outcome.NeedsUpgrade,
            service(Domain.EntitlementTier.FREE, holder = emptyHolder).start(),
        )

        verify(exactly = 0) { context.startService(any()) }
        verify(exactly = 0) { context.startForegroundService(any()) }
    }

    /** Cold start: starting the service attaches it and, like the real one, promotes it to foreground. */
    private fun givenColdStartService(becomesForeground: Boolean = true) {
        every { context.startService(any()) } answers {
            emptyHolder.attach(audio)
            if (becomesForeground) emptyHolder.markForeground(true)
            null
        }
    }

    @Test
    fun `cold start - plays once the service we started is foreground`() = runTest {
        givenPreset()
        givenColdStartService()

        val outcome = service(holder = emptyHolder).start()

        assertEquals(Outcome.Started("Night", null), outcome)
        coVerify { audio.play(any(), any()) }
        verify(exactly = 0) { context.stopService(any()) }
    }

    @Test
    fun `cold start - a failed play stops the service we started, only after it is foreground`() = runTest {
        givenPreset()
        givenColdStartService()
        coEvery { audio.play(any(), any()) } coAnswers {
            audioState.value = stateOf(Domain.PlaybackStatus.ERROR)
        }

        val outcome = service(holder = emptyHolder).start()

        assertTrue(outcome is Outcome.Failed)
        verify(exactly = 1) { context.stopService(any()) }
    }

    @Test
    fun `a failed play never stops a service that was already running`() = runTest {
        givenPreset()
        coEvery { audio.play(any(), any()) } coAnswers {
            audioState.value = stateOf(Domain.PlaybackStatus.ERROR)
        }

        assertTrue(service().start() is Outcome.Failed)   // default holder: already attached
        verify(exactly = 0) { context.stopService(any()) }
    }

    @Test
    fun `nothing ever played gives NothingToPlay`() = runTest {
        every { presets.observeAll() } returns flowOf(emptyList())
        coEvery { prefs.get() } returns Domain.UserPreferences()

        assertEquals(Outcome.NothingToPlay, service().start())
    }

    @Test
    fun `audio focus refused is reported as a failure and nothing is recorded`() = runTest {
        givenPreset()
        coEvery { audio.play(any(), any()) } coAnswers {
            audioState.value = stateOf(Domain.PlaybackStatus.ERROR)
        }

        assertTrue(service().start() is Outcome.Failed)
        coVerify(exactly = 0) { presets.recordUsed(any()) }
    }
}
