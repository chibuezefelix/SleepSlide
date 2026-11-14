package com.opxl.sleepslide.presentation.widget

import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetModelTest {

    private val now = 1_000_000_000L
    private val mix = Fixtures.deltaAndRain()

    private fun preset(id: Long, name: String, usedAgo: Long? = null, pinned: Boolean = false, order: Int = 0) =
        Domain.Preset(id = id, name = name, mix = mix, isPinned = pinned, sortOrder = order,
            lastUsedAt = usedAgo?.let { now - it })

    private fun build(
        prefs: Domain.UserPreferences = Domain.UserPreferences(),
        presets: List<Domain.Preset> = emptyList(),
        playing: Boolean = false,
        activePresetId: Long? = null,
    ) = WidgetModel.build(prefs, presets, playing, activePresetId, now) { null }

    @Test
    fun `nothing played yet gives the empty state`() {
        val m = build()
        assertFalse(m.hasMix)
        assertNull(m.mixName)
    }

    @Test
    fun `shows the preset Sleep now would play, however old`() {
        val m = build(presets = listOf(preset(1, "Rain", usedAgo = 90L * 24 * 60 * 60 * 1000)))
        assertTrue(m.hasMix)
        assertEquals("Rain", m.mixName)
    }

    @Test
    fun `an unsaved last mix counts as a mix but has no name`() {
        val prefs = Domain.UserPreferences(lastPlayedMixJson = "json")
        val m = WidgetModel.build(prefs, emptyList(), false, null, now) { mix }
        assertTrue(m.hasMix)
        assertNull(m.mixName)
    }

    @Test
    fun `while playing the active preset name wins and the widget has a mix`() {
        val m = build(
            presets = listOf(preset(1, "Rain", usedAgo = 10), preset(2, "Storm")),
            playing = true, activePresetId = 2,
        )
        assertTrue(m.playing)
        assertEquals("Storm", m.mixName)
    }

    @Test
    fun `playing something unsaved still counts as having a mix`() {
        assertTrue(build(playing = true).hasMix)
    }

    @Test
    fun `timer chip follows the quick start timer setting`() {
        val on = build(Domain.UserPreferences(lastTimerDurationMs = 30 * 60_000L))
        val off = build(Domain.UserPreferences(isQuickStartTimerEnabled = false))
        assertEquals("30m", on.timerLabel)
        assertNull(off.timerLabel)
    }

    @Test
    fun `at most three pinned presets, in pin order`() {
        val presets = listOf(
            preset(1, "A", pinned = true, order = 3), preset(2, "B", pinned = true, order = 1),
            preset(3, "C", pinned = true, order = 2), preset(4, "D", pinned = true, order = 4),
            preset(5, "E", pinned = false),
        )
        assertEquals(listOf("B", "C", "A"), build(presets = presets).pinned.map { it.name })
    }

    @Test
    fun `timer chip formatting`() {
        assertEquals("1m", formatTimerChip(1_000))
        assertEquals("2m", formatTimerChip(90_000))
        assertEquals("30m", formatTimerChip(30 * 60_000L))
        assertEquals("1h", formatTimerChip(60 * 60_000L))
        assertEquals("1h 30m", formatTimerChip(90 * 60_000L))
        assertEquals("2h", formatTimerChip(120 * 60_000L))
    }
}
