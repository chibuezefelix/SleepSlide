package com.opxl.sleepslide.data.repository


import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.BuildConfig


internal object BundledSoundCatalogue {
    val all: List<Domain.Sound> = listOf(
        Domain.Sound(id = "white_noise",  title = "White Noise",  category = Domain.SoundCategory.TINNITUS, assetPath = BuildConfig.WHITE_NOISE,  frequencyHz = 0),
        Domain.Sound(id = "pink_noise",   title = "Pink Noise",   category = Domain.SoundCategory.TINNITUS, assetPath = "sounds/tinnitus/pink.ogg",   frequencyHz = 500),
        Domain.Sound(id = "brown_noise",  title = "Brown Noise",  category = Domain.SoundCategory.TINNITUS, assetPath = "sounds/tinnitus/brown.ogg",  frequencyHz = 200),
        Domain.Sound(id = "grey_noise",   title = "Grey Noise",   category = Domain.SoundCategory.TINNITUS, assetPath = "sounds/tinnitus/grey.ogg",   frequencyHz = 1000),
        Domain.Sound(id = "rain_light",   title = "Light Rain",   category = Domain.SoundCategory.NATURE,   assetPath = "sounds/nature/rain_light.ogg"),
        Domain.Sound(id = "rain_heavy",   title = "Heavy Rain",   category = Domain.SoundCategory.NATURE,   assetPath = "sounds/nature/rain_heavy.ogg"),
        Domain.Sound(id = "ocean_waves",  title = "Ocean Waves",  category = Domain.SoundCategory.NATURE,   assetPath = "sounds/nature/ocean.ogg"),
        Domain.Sound(id = "forest",       title = "Forest",       category = Domain.SoundCategory.NATURE,   assetPath = "sounds/nature/forest.ogg"),
        Domain.Sound(id = "stream",       title = "Stream",       category = Domain.SoundCategory.NATURE,   assetPath = "sounds/nature/stream.ogg"),
        Domain.Sound(id = "fireplace",    title = "Fireplace",    category = Domain.SoundCategory.AMBIENT,  assetPath = "sounds/ambient/fireplace.ogg"),
        Domain.Sound(id = "fan",          title = "Electric Fan", category = Domain.SoundCategory.AMBIENT,  assetPath = "sounds/ambient/fan.ogg"),
        Domain.Sound(id = "train",        title = "Train",        category = Domain.SoundCategory.AMBIENT,  assetPath = "sounds/ambient/train.ogg",   isPremium = true),
        Domain.Sound(id = "cafe",         title = "Café",         category = Domain.SoundCategory.AMBIENT,  assetPath = "sounds/ambient/cafe.ogg",    isPremium = true),
    )
}