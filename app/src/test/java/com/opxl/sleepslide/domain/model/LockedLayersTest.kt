package com.opxl.sleepslide.domain.model

import com.opxl.sleepslide.domain.model.Domain.lockedLayers
import com.opxl.sleepslide.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LockedLayersTest {

    @Test
    fun `free tier - premium layers are locked`() {
        val locked = Fixtures.deltaAndRain().lockedLayers(Domain.EntitlementTier.FREE)
        assertEquals(listOf("binaural_delta"), locked.map { it.sound.id })
    }

    @Test
    fun `premium tier - nothing is locked`() {
        val locked = Fixtures.deltaAndRain().lockedLayers(Domain.EntitlementTier.PREMIUM)
        assertTrue(locked.isEmpty())
    }

    @Test
    fun `newly premium catalogue sounds are flagged, green noise stays free`() {
        val premium = listOf(
            "notched_3000", "notched_4000", "notched_6000", "notched_8000",
            "binaural_alpha", "binaural_theta", "binaural_delta",
        )
        premium.forEach { assertTrue(it, Fixtures.sound(it).isPremium) }
        assertEquals(false, Fixtures.sound("green_noise").isPremium)
    }
}
