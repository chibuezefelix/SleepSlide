package com.opxl.sleepslide.domain.service

import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResumePickerTest {

    private val now = 1_000_000_000L
    private val hour = 60 * 60 * 1000L
    private val mix = Fixtures.deltaAndRain()

    private fun preset(id: Long, usedAgoMs: Long?) = Domain.Preset(
        id = id, name = "P$id", mix = mix,
        lastUsedAt = usedAgoMs?.let { now - it },
    )

    private fun pick(
        prefs: Domain.UserPreferences = Domain.UserPreferences(),
        presets: List<Domain.Preset> = emptyList(),
        maxAgeMs: Long? = null,
        deserialize: (String) -> Domain.SoundMix? = { null },
    ) = ResumePicker.pick(prefs, presets, now, maxAgeMs, deserialize)

    @Test
    fun `last played preset wins over a more recent one`() {
        val last = preset(1, usedAgoMs = 5 * hour)
        val newer = preset(2, usedAgoMs = 1 * hour)

        val result = pick(Domain.UserPreferences(lastPlayedPresetId = 1), listOf(newer, last))

        assertEquals(1L, (result as ResumePicker.Pick.FromPreset).preset.id)
    }

    @Test
    fun `without a last-played pref the most recently used preset is chosen`() {
        val result = pick(presets = listOf(preset(1, 5 * hour), preset(2, 1 * hour), preset(3, null)))

        assertEquals(2L, (result as ResumePicker.Pick.FromPreset).preset.id)
    }

    @Test
    fun `staleness limit skips old presets, no limit keeps them`() {
        val old = preset(1, usedAgoMs = 48 * hour)

        assertNull(pick(presets = listOf(old), maxAgeMs = 24 * hour))
        assertEquals(1L, (pick(presets = listOf(old), maxAgeMs = null) as ResumePicker.Pick.FromPreset).preset.id)
    }

    @Test
    fun `falls back to the last unsaved mix`() {
        val result = pick(Domain.UserPreferences(lastPlayedMixJson = "json"), deserialize = { mix })

        assertEquals(mix, (result as ResumePicker.Pick.FromMix).mix)
    }

    @Test
    fun `an unreadable or empty unsaved mix is ignored`() {
        val prefs = Domain.UserPreferences(lastPlayedMixJson = "json")

        assertNull(pick(prefs, deserialize = { null }))
        assertNull(pick(prefs, deserialize = { throw IllegalStateException("bad json") }))
        assertNull(pick(prefs, deserialize = { Domain.SoundMix(emptyList()) }))
    }

    @Test
    fun `nothing played yet gives nothing`() {
        assertNull(pick())
    }
}
