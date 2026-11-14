package com.opxl.sleepslide.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.opxl.sleepslide.R
import com.opxl.sleepslide.domain.model.Domain

/**
 * Shared icon glyphs backed by the vector drawables in res/drawable. One stroke weight,
 * one grid, so a glyph never looks heavier than its neighbour.
 *
 * Decorative by default (null contentDescription) — the enclosing button carries the label.
 */
enum class Glyph(@DrawableRes val res: Int) {
    Plus(R.drawable.ic_plus),
    Minus(R.drawable.ic_minus),
    Close(R.drawable.ic_close),
    Check(R.drawable.ic_check),
    ChevronLeft(R.drawable.ic_chevron_left),
    ChevronRight(R.drawable.ic_chevron_right),
    ChevronDown(R.drawable.ic_chevron_down),
    ChevronUp(R.drawable.ic_chevron_up),
    Search(R.drawable.ic_search),
    Play(R.drawable.ic_play),
    Pause(R.drawable.ic_pause),
    Stop(R.drawable.ic_stop),
    More(R.drawable.ic_dots),
    Pin(R.drawable.ic_pin),
    Trash(R.drawable.ic_trash),
    Lock(R.drawable.ic_lock),
    Bell(R.drawable.ic_bell),
    Moon(R.drawable.ic_moon),
    Sun(R.drawable.ic_sun),
    Info(R.drawable.ic_info),
    Compass(R.drawable.ic_compass),
    Sliders(R.drawable.ic_sliders),
    Sparkle(R.drawable.ic_sparkle),
    Bluetooth(R.drawable.ic_bluetooth),
    Volume(R.drawable.ic_volume),
    VolumeOff(R.drawable.ic_volume_off),
    Drag(R.drawable.ic_drag),
    Save(R.drawable.ic_save),
    Refresh(R.drawable.ic_refresh),
    Eye(R.drawable.ic_eye),
    Battery(R.drawable.ic_battery),
    Clock(R.drawable.ic_clock),
    Timer(R.drawable.ic_timer),
    Headphones(R.drawable.ic_headphones),
    WaveBars(R.drawable.ic_wave_bars),
    Lightning(R.drawable.ic_lightning),

    // Sound categories
    Leaf(R.drawable.ic_leaf),
    Waves(R.drawable.ic_waves),
    Ear(R.drawable.ic_ear),
    Taps(R.drawable.ic_taps),
    Book(R.drawable.ic_book),
}

/** The glyph that stands for a sound category wherever one is shown. */
val Domain.SoundCategory.glyph: Glyph
    get() = when (this) {
        Domain.SoundCategory.TINNITUS -> Glyph.Ear
        Domain.SoundCategory.NATURE   -> Glyph.Leaf
        Domain.SoundCategory.AMBIENT  -> Glyph.Waves
        Domain.SoundCategory.BINAURAL -> Glyph.Headphones
        Domain.SoundCategory.ASMR     -> Glyph.Taps
        Domain.SoundCategory.GUIDED   -> Glyph.Book
    }

@Composable
fun GlyphIcon(
    glyph: Glyph,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    contentDescription: String? = null,
) {
    Icon(
        painter            = painterResource(glyph.res),
        contentDescription = contentDescription,
        tint               = tint,
        modifier           = modifier.size(size),
    )
}

// Named shorthands kept for existing call sites.

@Composable
fun ChevronLeft(tint: Color, modifier: Modifier = Modifier) =
    GlyphIcon(Glyph.ChevronLeft, tint, modifier, size = 16.dp)

@Composable
fun ChevronRight(tint: Color, modifier: Modifier = Modifier) =
    GlyphIcon(Glyph.ChevronRight, tint, modifier, size = 16.dp)

@Composable
fun CheckIcon(tint: Color, size: Dp = 12.dp, modifier: Modifier = Modifier) =
    GlyphIcon(Glyph.Check, tint, modifier, size = size)

@Composable
fun ClockIcon(tint: Color, size: Dp = 16.dp, modifier: Modifier = Modifier) =
    GlyphIcon(Glyph.Clock, tint, modifier, size = size)

@Composable
fun HeadphonesIcon(tint: Color, size: Dp = 12.dp, modifier: Modifier = Modifier) =
    GlyphIcon(Glyph.Headphones, tint, modifier, size = size)

/** Tinted circle (background, foreground) for a sound category — one palette app-wide. */
@Composable
@androidx.compose.runtime.ReadOnlyComposable
fun categoryColors(category: Domain.SoundCategory): Pair<Color, Color> = when (category) {
    Domain.SoundCategory.TINNITUS -> com.opxl.sleepslide.ui.theme.PaleBlue   to com.opxl.sleepslide.ui.theme.PaleBlueText
    Domain.SoundCategory.NATURE   -> com.opxl.sleepslide.ui.theme.PaleGreen  to com.opxl.sleepslide.ui.theme.PaleGreenText
    Domain.SoundCategory.ASMR     -> com.opxl.sleepslide.ui.theme.PaleYellow to com.opxl.sleepslide.ui.theme.PaleYellowText
    else                          -> com.opxl.sleepslide.ui.theme.SurfaceMuted to com.opxl.sleepslide.ui.theme.Charcoal
}
