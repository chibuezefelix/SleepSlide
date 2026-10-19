package com.opxl.sleepslide.presentation.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.opxl.sleepslide.R
import com.opxl.sleepslide.domain.model.Domain
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Display titles for bundled sounds, keyed by [Domain.Sound.id]. The English `title` stored in
 * Room and in saved preset JSON is only a fallback for ids this build doesn't know.
 */
private val SOUND_TITLES: Map<String, Int> = mapOf(
    "white_noise" to R.string.sound_title_white_noise,
    "pink_noise" to R.string.sound_title_pink_noise,
    "brown_noise" to R.string.sound_title_brown_noise,
    "grey_noise" to R.string.sound_title_grey_noise,
    "green_noise" to R.string.sound_title_green_noise,
    "notched_3000" to R.string.sound_title_notched_3000,
    "notched_4000" to R.string.sound_title_notched_4000,
    "notched_6000" to R.string.sound_title_notched_6000,
    "notched_8000" to R.string.sound_title_notched_8000,
    "rain_light" to R.string.sound_title_rain_light,
    "rain_window" to R.string.sound_title_rain_window,
    "rain_heavy" to R.string.sound_title_rain_heavy,
    "rain_street" to R.string.sound_title_rain_street,
    "thunderstorm" to R.string.sound_title_thunderstorm,
    "summer_storm" to R.string.sound_title_summer_storm,
    "rain_walk" to R.string.sound_title_rain_walk,
    "stream" to R.string.sound_title_stream,
    "creek" to R.string.sound_title_creek,
    "pond" to R.string.sound_title_pond,
    "night_crickets" to R.string.sound_title_night_crickets,
    "fireplace" to R.string.sound_title_fireplace,
    "deep_rumble" to R.string.sound_title_deep_rumble,
    "womb_heart" to R.string.sound_title_womb_heart,
    "binaural_alpha" to R.string.sound_title_binaural_alpha,
    "binaural_theta" to R.string.sound_title_binaural_theta,
    "binaural_delta" to R.string.sound_title_binaural_delta,
)

@Composable
fun Domain.Sound.displayTitle(): String =
    SOUND_TITLES[id]?.let { stringResource(it) } ?: title

fun Domain.Sound.displayTitle(context: Context): String =
    SOUND_TITLES[id]?.let(context::getString) ?: title

fun Domain.Sound.titleText(): UiText =
    SOUND_TITLES[id]?.let { UiText.Res(it) } ?: UiText.Verbatim(title)

@get:StringRes
val Domain.SoundCategory.labelRes: Int
    get() = when (this) {
        Domain.SoundCategory.TINNITUS -> R.string.sound_category_tinnitus
        Domain.SoundCategory.NATURE   -> R.string.sound_category_nature
        Domain.SoundCategory.AMBIENT  -> R.string.sound_category_ambient
        Domain.SoundCategory.BINAURAL -> R.string.sound_category_binaural
        Domain.SoundCategory.GUIDED   -> R.string.sound_category_guided
    }

/** Lets ViewModels match search queries against the titles the user actually sees. */
@Singleton
class SoundTextResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun title(sound: Domain.Sound): String = sound.displayTitle(context)
    fun category(category: Domain.SoundCategory): String = context.getString(category.labelRes)
}
