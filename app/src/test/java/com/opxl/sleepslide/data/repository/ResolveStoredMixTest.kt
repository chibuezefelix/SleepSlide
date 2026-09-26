package com.opxl.sleepslide.data.repository

import com.opxl.sleepslide.data.local.Mapper.toEntity
import com.opxl.sleepslide.data.local.SoundDao
import com.opxl.sleepslide.testutil.Fixtures
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveStoredMixTest {

    private val soundDao = mockk<SoundDao>()
    private val repository = SoundRepositoryImpl(soundDao, UnconfinedTestDispatcher())
    private val serializer = MixSerializer()

    /** JSON as written by the previous app version, when Delta Beats was still free. */
    private val legacyJson = """
        {"masterVolume":0.9,"fadeInDurationMs":3000,"layers":[
          {"position":0,"volume":0.4,"isMuted":false,"sound":{"id":"binaural_delta",
            "title":"Delta (old title)","category":"BINAURAL","assetPath":"old/delta.wav",
            "isPremium":false,"isBundled":true}},
          {"position":1,"volume":0.8,"isMuted":false,"sound":{"id":"gone_sound",
            "title":"Removed","category":"NATURE","assetPath":"old/gone.ogg",
            "isPremium":false,"isBundled":true}}
        ]}
    """.trimIndent()

    @Test
    fun `stored isPremium=false is replaced by the catalogue's current true`() = runTest {
        coEvery { soundDao.getByIds(any()) } returns listOf(Fixtures.delta.toEntity())

        val stored = serializer.deserialize(legacyJson)
        assertEquals(false, stored.layers.first().sound.isPremium)

        val resolved = repository.resolve(stored)
        val delta = resolved.layers.single { it.sound.id == "binaural_delta" }
        assertTrue(delta.sound.isPremium)
        assertEquals(Fixtures.delta.title, delta.sound.title)
        assertEquals(Fixtures.delta.assetPath, delta.sound.assetPath)
        assertEquals(0.4f, delta.volume)   // layer settings survive
    }

    @Test
    fun `layers whose sound no longer exists are dropped`() = runTest {
        coEvery { soundDao.getByIds(any()) } returns listOf(Fixtures.delta.toEntity())

        val resolved = repository.resolve(serializer.deserialize(legacyJson))
        assertEquals(listOf("binaural_delta"), resolved.layers.map { it.sound.id })
    }

    @Test
    fun `serialized mixes no longer carry isPremium and still round-trip`() {
        val json = serializer.serialize(Fixtures.deltaAndRain())
        assertTrue("isPremium" !in json)
        assertEquals(
            listOf("binaural_delta", "rain_light"),
            serializer.deserialize(json).layers.map { it.sound.id },
        )
    }
}
