package com.opxl.sleepslide.ui.theme


import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * SleepSlide color system.
 *
 * The palette intentionally uses warm neutrals with restrained semantic accents.
 * Avoid introducing arbitrary colors directly inside composables.
 */

// Raw light palette — read only by Theme.kt. Screens use the theme-aware tokens below.

internal object LightPalette {
    val Bone = Color(0xFFF7F6F3)
    val WarmWhite = Color(0xFFEEEEEB)   // scaffold ground — light warm grey so white cards and text lift off it
    val White = Color(0xFFFFFFFF)

    val Charcoal = Color(0xFF111111)
    val DarkGray = Color(0xFF2F3437)
    val MutedGray = Color(0xFF5C5B57)   // secondary text — darkened for contrast on the grey ground

    val Border = Color(0xFFDCDCD8)
    val SurfaceMuted = Color(0xFFFAFAF9)   // unselected chips/tabs — must lift off the grey ground, bordered inside cards

    // Pastel semantic accents
    val PaleRed = Color(0xFFFDEBEC)
    val PaleRedText = Color(0xFF9F2F2D)

    val PaleBlue = Color(0xFFE1F3FE)
    val PaleBlueText = Color(0xFF1F6C9F)

    val PaleGreen = Color(0xFFEDF3EC)
    val PaleGreenText = Color(0xFF346538)

    val PaleYellow = Color(0xFFFBF3DB)
    val PaleYellowText = Color(0xFF956400)
}

// Theme-aware tokens. Names describe the LIGHT look; in dark mode each flips to its
// counterpart (Charcoal = ink, White = card surface, WarmWhite = ground), so a Charcoal
// button with White text inverts cleanly. Composable-only — inside draw lambdas, read
// the token into a local first.

val Bone: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.bone
val WarmWhite: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.ground
val White: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.surface

val Charcoal: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.ink
val DarkGray: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.inkSoft
val MutedGray: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.muted

val Border: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.border
val SurfaceMuted: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.mutedSurface

// Emphasis panels — the now-playing bar, mix sheet, active cards. Charcoal on light, but a
// raised grey on dark rather than inverting to a bright slab at bedtime.
val Panel: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.panel
val OnPanel: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.onPanel

val PaleRed: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.paleRed
val PaleRedText: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.paleRedText

val PaleBlue: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.paleBlue
val PaleBlueText: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.paleBlueText

val PaleGreen: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.paleGreen
val PaleGreenText: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.paleGreenText

val PaleYellow: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.paleYellow
val PaleYellowText: Color @Composable @ReadOnlyComposable get() = LocalSleepSlideColors.current.paleYellowText

// Fixed in both themes — text and scrims laid over photos or a dimmed screen.

val OnImage = Color(0xFFFFFFFF)
val ImageScrim = Color(0xFF111111)
val OverlayMark = Color(0xFFEEEEEB)

// Brand — the launcher icon's yellow tile and black mark. Fixed in both themes.

val BrandYellow = Color(0xFFFBED34)
val BrandInk = Color(0xFF111111)

// Dark theme neutrals

val DarkBackground = Color(0xFF151514)
val DarkSurface = Color(0xFF1C1C1A)
val DarkSurfaceVariant = Color(0xFF242422)
val DarkPanel = Color(0xFF2C2C29)

val DarkText = Color(0xFFF5F4F0)
val DarkTextSecondary = Color(0xFFB5B3AE)

val DarkBorder = Color(0xFF333330)