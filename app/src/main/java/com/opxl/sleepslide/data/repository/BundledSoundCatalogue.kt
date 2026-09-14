package com.opxl.sleepslide.data.repository


import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.model.Domain.SoundCategory.AMBIENT
import com.opxl.sleepslide.domain.model.Domain.SoundCategory.NATURE
import com.opxl.sleepslide.domain.model.Domain.SoundCategory.TINNITUS
import com.opxl.sleepslide.BuildConfig

/**
 * Every file under `assets/sounds/`. Kept in sync with the DB by
 * [SoundRepositoryImpl.seedBundledSounds] on each cold start, so adding a row here
 * (or fixing a path) reaches devices that have already seeded.
 *
 * Tags drive two things: search/filters in the Library, and scene selection for the
 * player backdrop (see presentation/scene). Use the shared vocabulary below.
 *   mood:   calm · cozy · dramatic · deep
 *   time:   night · day · evening
 *   scene:  water · rain · storm · forest · garden · fire · indoor · city
 *   who:    baby (steady, event-free — safe at low level) · hearing (low-frequency masker)
 *
 * Noise colours are synthesised by tools/gen_noise.py — loop-perfect, no licence.
 * Field recordings were cleaned with tools/oggtool.py; provenance in docs/SOUND_CREDITS.md.
 */
internal object BundledSoundCatalogue {
    val all: List<Domain.Sound> = listOf(
        // ── Tinnitus / hearing (synthesised, 15 s seamless loops) ─────────────────
        Domain.Sound(id = "white_noise",  title = "White Noise",  category = TINNITUS, assetPath = BuildConfig.WHITE_NOISE,          frequencyHz = 0,    tags = listOf("noise", "masking", "baby")),
        Domain.Sound(id = "pink_noise",   title = "Pink Noise",   category = TINNITUS, assetPath = "sounds/tinnitus/pink.wav",       frequencyHz = 500,  tags = listOf("noise", "masking", "baby", "calm")),
        Domain.Sound(id = "brown_noise",  title = "Brown Noise",  category = TINNITUS, assetPath = "sounds/tinnitus/brown.wav",      frequencyHz = 200,  tags = listOf("noise", "masking", "baby", "hearing", "deep")),
        Domain.Sound(id = "grey_noise",   title = "Grey Noise",   category = TINNITUS, assetPath = "sounds/tinnitus/grey.wav",       frequencyHz = 1000, tags = listOf("noise", "masking")),

        // ── Rain ───────────────────────────────────────────────────────────────────
        Domain.Sound(id = "rain_light",   title = "Light Rain",          category = NATURE, assetPath = "sounds/nature/rain_light.ogg",   tags = listOf("rain", "water", "calm", "baby")),
        Domain.Sound(id = "rain_window",  title = "Rain on the Window",  category = NATURE, assetPath = "sounds/nature/rain_window.ogg",  tags = listOf("rain", "indoor", "cozy", "night", "baby")),
        Domain.Sound(id = "rain_heavy",   title = "Heavy Rain",          category = NATURE, assetPath = "sounds/nature/rain_heavy.ogg",   tags = listOf("rain", "water", "deep")),
        Domain.Sound(id = "rain_street",  title = "Rain on a Street",    category = NATURE, assetPath = "sounds/nature/rain_street.ogg",  tags = listOf("rain", "city", "evening")),
        Domain.Sound(id = "thunderstorm", title = "Thunderstorm",        category = NATURE, assetPath = "sounds/nature/thunderstorm.ogg", tags = listOf("rain", "storm", "dramatic", "night")),
        Domain.Sound(id = "summer_storm", title = "Summer Storm",        category = NATURE, assetPath = "sounds/nature/summer_storm.ogg", tags = listOf("rain", "storm", "dramatic", "evening")),
        Domain.Sound(id = "rain_walk",    title = "Rainy Walk",          category = NATURE, assetPath = "sounds/nature/rain_walk.ogg",    tags = listOf("rain", "storm", "city"), isPremium = true),

        // ── Water & forest ─────────────────────────────────────────────────────────
        Domain.Sound(id = "stream",         title = "Forest Stream",  category = NATURE, assetPath = "sounds/nature/stream.ogg",         tags = listOf("water", "forest", "calm", "day", "baby")),
        Domain.Sound(id = "creek",          title = "Mountain Creek", category = NATURE, assetPath = "sounds/nature/creek.ogg",          tags = listOf("water", "forest", "calm", "day"), isPremium = true),
        Domain.Sound(id = "pond",           title = "Pond at Dawn",   category = NATURE, assetPath = "sounds/nature/pond.ogg",           tags = listOf("water", "garden", "birds", "day", "calm")),
        Domain.Sound(id = "night_crickets", title = "Night Crickets", category = NATURE, assetPath = "sounds/nature/night_crickets.ogg", tags = listOf("forest", "garden", "night", "calm")),

        // ── Ambient ────────────────────────────────────────────────────────────────
        Domain.Sound(id = "fireplace",   title = "Fireplace",       category = AMBIENT, assetPath = "sounds/ambient/fireplace.ogg",   tags = listOf("fire", "cozy", "indoor", "evening")),
        Domain.Sound(id = "deep_rumble", title = "Deep Ocean Vent", category = AMBIENT, assetPath = "sounds/ambient/deep_rumble.ogg", tags = listOf("deep", "water", "hearing", "masking", "baby"), frequencyHz = 100),
        Domain.Sound(id = "womb_heart",  title = "Womb & Heartbeat", category = AMBIENT, assetPath = "sounds/nature/womb_heart_1.ogg", tags = listOf("baby", "deep", "calm", "masking", "night")),
    )
}
