package com.opxl.sleepslide.domain.service

import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.repository.SoundRepository
import com.opxl.sleepslide.testutil.Fixtures
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackGateTest {

    /** Resolve is identity here — ResolveStoredMixTest covers the catalogue lookup. */
    private val soundRepository = mockk<SoundRepository> {
        coEvery { resolve(any()) } answers { firstArg() }
    }

    private fun gate(tier: Domain.EntitlementTier) =
        PlaybackGate(soundRepository, Fixtures.entitlementObserver(tier))

    @Test
    fun `free user - premium layers are skipped and the user is told`() = runTest {
        val gate = gate(Domain.EntitlementTier.FREE)

        val decision = gate.prepare(Fixtures.deltaAndRain())

        decision as PlaybackGate.Decision.Play
        assertEquals(listOf("rain_light"), decision.mix.layers.map { it.sound.id })
        assertEquals(
            LockedLayersNotice(lockedTitles = listOf("Delta Beats · 2 Hz"), playingRest = true),
            gate.notices.first(),
        )
    }

    @Test
    fun `free user - an all-premium mix plays nothing`() = runTest {
        val gate = gate(Domain.EntitlementTier.FREE)
        val allPremium = Domain.SoundMix(listOf(Domain.SoundLayer(sound = Fixtures.delta)))

        assertEquals(PlaybackGate.Decision.AllLocked, gate.prepare(allPremium))
        assertEquals(false, gate.notices.first().playingRest)
    }

    @Test
    fun `premium user - whole mix plays`() = runTest {
        val decision = gate(Domain.EntitlementTier.PREMIUM).prepare(Fixtures.deltaAndRain())

        decision as PlaybackGate.Decision.Play
        assertEquals(2, decision.mix.layers.size)
    }

    @Test
    fun `mix with no surviving sounds is NothingToPlay`() = runTest {
        coEvery { soundRepository.resolve(any()) } returns Domain.SoundMix(emptyList())
        val decision = gate(Domain.EntitlementTier.FREE).prepare(Fixtures.deltaAndRain())
        assertTrue(decision is PlaybackGate.Decision.NothingToPlay)
    }
}
