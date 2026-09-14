package com.opxl.sleepslide.ui.theme


import androidx.compose.ui.graphics.Color

/**
 * SleepSlide color system.
 *
 * The palette intentionally uses warm neutrals with restrained semantic accents.
 * Avoid introducing arbitrary colors directly inside composables.
 */

// Base / Neutral

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

// Dark theme neutrals

val DarkBackground = Color(0xFF151514)
val DarkSurface = Color(0xFF1C1C1A)
val DarkSurfaceVariant = Color(0xFF242422)

val DarkText = Color(0xFFF5F4F0)
val DarkTextSecondary = Color(0xFFB5B3AE)

val DarkBorder = Color(0xFF333330)