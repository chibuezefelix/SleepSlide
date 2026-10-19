package com.opxl.sleepslide.presentation.scene

import com.opxl.sleepslide.domain.model.Domain
import java.util.Calendar
import androidx.annotation.StringRes
import com.opxl.sleepslide.R

/**
 * A backdrop photo shipped in `assets/images/`, with the vocabulary it "answers to".
 *
 * [tags] use the same words as [com.opxl.sleepslide.data.repository.BundledSoundCatalogue]
 * so a mix can be matched to a picture; [hours] is the time-of-day window in which the
 * photo feels right on its own (e.g. a sunset at 20:00, not at 07:00).
 */
enum class Scene(
    val assetPath: String,
    @StringRes val title: Int,
    @StringRes val caption: Int,
    val tags: Set<String>,
    val hours: IntRange,
) {
    GARDEN(
        assetPath = "images/garden.jpg",
        title     = R.string.scene_garden_title,
        caption   = R.string.scene_garden_caption,
        tags      = setOf("garden", "day", "calm", "birds", "forest", "baby"),
        hours     = 6..16,
    ),
    MOUNTAIN_LAKE(
        assetPath = "images/mountain_lake.jpg",
        title     = R.string.scene_mountain_lake_title,
        caption   = R.string.scene_mountain_lake_caption,
        tags      = setOf("water", "rain", "forest", "calm", "night", "deep", "hearing", "noise", "masking"),
        hours     = 21..23,
    ),
    SUNSET_VALLEY(
        assetPath = "images/sunset_valley.jpg",
        title     = R.string.scene_sunset_valley_title,
        caption   = R.string.scene_sunset_valley_caption,
        tags      = setOf("evening", "fire", "cozy", "storm", "dramatic", "indoor", "city"),
        hours     = 17..20,
    );

    fun next(): Scene = entries[(ordinal + 1) % entries.size]

    companion object {
        /**
         * Picks the scene that best fits what is playing right now.
         *
         * Score = tag overlap with the mix (weight 2, dominant) + time-of-day fit (1)
         * + a tiny day-of-year rotation so equally-good candidates don't always
         * resolve the same way — a rain mix shows the lake one evening and the valley
         * the next, but never the garden at midnight.
         */
        fun pick(
            sounds: List<Domain.Sound>,
            now: Calendar = Calendar.getInstance(),
        ): Scene {
            val hour = now.get(Calendar.HOUR_OF_DAY)
            val rotation = now.get(Calendar.DAY_OF_YEAR)
            val mixTags = sounds.flatMap { it.tags }.toSet()

            return entries.maxBy { scene ->
                val overlap = scene.tags.count { it in mixTags } * 2
                val timeFit = if (hour in scene.hours) 1 else 0
                val tieBreak = ((scene.ordinal + rotation) % entries.size) * 0.01
                overlap + timeFit + tieBreak
            }
        }
    }
}
