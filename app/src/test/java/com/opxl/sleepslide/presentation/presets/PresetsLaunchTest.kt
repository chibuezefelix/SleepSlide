package com.opxl.sleepslide.presentation.presets

import com.opxl.sleepslide.data.AudioServiceHolder
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.observer.AudioStateObserver
import com.opxl.sleepslide.domain.repository.PresetRepository
import com.opxl.sleepslide.domain.repository.SoundRepository
import com.opxl.sleepslide.domain.service.AudioService
import com.opxl.sleepslide.domain.service.LockedLayersNotice
import com.opxl.sleepslide.domain.service.PlaybackGate
import com.opxl.sleepslide.testutil.Fixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** The Presets launch path end to end, minus the real audio service. */
@OptIn(ExperimentalCoroutinesApi::class)
class PresetsLaunchTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `free user launching Delta + Light Rain plays only Light Rain and gets the upgrade notice`() =
        runTest(dispatcher) {
            val audioService = mockk<AudioService>(relaxed = true)
            val holder = AudioServiceHolder().apply { attach(audioService) }
            val soundRepository = mockk<SoundRepository> {
                coEvery { resolve(any()) } answers { firstArg() }
            }
            val gate = PlaybackGate(soundRepository, Fixtures.entitlementObserver(Domain.EntitlementTier.FREE))
            val presetRepository = mockk<PresetRepository>(relaxed = true) {
                every { observeAll() } returns flowOf(emptyList())
            }
            val audioStateObserver = mockk<AudioStateObserver> {
                every { audioState } returns MutableStateFlow(
                    Domain.AudioState(Domain.PlaybackStatus.IDLE, audioFocusStatus = Domain.AudioFocusStatus.NONE)
                )
            }
            val viewModel = PresetsViewModel(
                presetRepository          = presetRepository,
                playHistoryRepository     = mockk(relaxed = true),
                userPreferencesRepository = mockk(relaxed = true),
                audioServiceHolder        = holder,
                audioStateObserver        = audioStateObserver,
                entitlementObserver       = Fixtures.entitlementObserver(Domain.EntitlementTier.FREE),
                playbackGate              = gate,
            )
            val preset = Domain.Preset(id = 7L, name = "Deep sleep", mix = Fixtures.deltaAndRain())

            viewModel.launchPreset(preset)
            advanceUntilIdle()

            val played = slot<Domain.SoundMix>()
            coVerify(exactly = 1) { audioService.play(capture(played)) }
            assertEquals(listOf("rain_light"), played.captured.layers.map { it.sound.id })
            assertEquals(
                LockedLayersNotice(listOf("Delta Beats · 2 Hz"), playingRest = true),
                gate.notices.first(),
            )
            // The saved preset is untouched — Delta comes back if they resubscribe
            coVerify(exactly = 0) { presetRepository.updateMix(any(), any()) }
            coVerify(exactly = 0) { presetRepository.update(any()) }
            assertEquals(2, preset.mix.layers.size)
        }
}
