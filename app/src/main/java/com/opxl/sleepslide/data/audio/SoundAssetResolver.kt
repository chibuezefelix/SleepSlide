package com.opxl.sleepslide.data.audio


internal object SoundAssetResolver {

    private val assetPaths = mapOf(
        "white_noise"  to "sounds/tinnitus/white.ogg",
        "pink_noise"   to "sounds/tinnitus/pink.ogg",
        "brown_noise"  to "sounds/tinnitus/brown.ogg",
        "grey_noise"   to "sounds/tinnitus/grey.ogg",
        "rain_light"   to "sounds/nature/rain_light.ogg",
        "rain_heavy"   to "sounds/nature/rain_heavy.ogg",
        "ocean_waves"  to "sounds/nature/ocean.ogg",
        "forest"       to "sounds/nature/forest.ogg",
        "stream"       to "sounds/nature/stream.ogg",
        "fireplace"    to "sounds/ambient/fireplace.ogg",
        "fan"          to "sounds/ambient/fan.ogg",
        "train"        to "sounds/ambient/train.ogg",
        "cafe"         to "sounds/ambient/cafe.ogg",
    )

    fun resolve(soundId: String): String =
        assetPaths[soundId] ?: error("No asset registered for sound id: $soundId")

    fun resolveOrNull(soundId: String): String? = assetPaths[soundId]
}