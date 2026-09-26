package com.opxl.sleepslide.testutil

import com.opxl.sleepslide.data.repository.BundledSoundCatalogue
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.observer.EntitlementObserver
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/** Real catalogue rows, so the tests break if the shipped flags change. */
object Fixtures {
    fun sound(id: String): Domain.Sound = BundledSoundCatalogue.all.first { it.id == id }

    val delta: Domain.Sound get() = sound("binaural_delta")      // premium
    val lightRain: Domain.Sound get() = sound("rain_light")      // free

    /** Delta Beats + Light Rain — the mix from the spec. */
    fun deltaAndRain() = Domain.SoundMix(
        layers = listOf(
            Domain.SoundLayer(sound = delta, volume = 0.4f, position = 0),
            Domain.SoundLayer(sound = lightRain, volume = 0.8f, position = 1),
        ),
    )

    fun entitlementObserver(tier: Domain.EntitlementTier): EntitlementObserver = mockk {
        every { entitlement } returns MutableStateFlow(Domain.Entitlement(tier, revenueCatUserId = "test"))
        every { this@mockk.tier } returns flowOf(tier)
        every { isPremium } returns flowOf(tier == Domain.EntitlementTier.PREMIUM)
    }
}
