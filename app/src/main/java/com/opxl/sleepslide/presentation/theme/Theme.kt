package com.opxl.sleepslide.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Light color scheme

private val LightColors = lightColorScheme(
    primary = Charcoal,
    onPrimary = White,

    primaryContainer = Charcoal,
    onPrimaryContainer = White,

    secondary = DarkGray,
    onSecondary = White,

    secondaryContainer = Bone,
    onSecondaryContainer = Charcoal,

    tertiary = PaleBlueText,
    onTertiary = White,

    tertiaryContainer = PaleBlue,
    onTertiaryContainer = PaleBlueText,

    background = WarmWhite,
    onBackground = Charcoal,

    surface = White,
    onSurface = Charcoal,

    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = MutedGray,

    outline = Border,
    outlineVariant = Border,

    error = PaleRedText,
    onError = White,

    errorContainer = PaleRed,
    onErrorContainer = PaleRedText
)

// Dark color scheme

private val DarkColors = darkColorScheme(
    primary = DarkText,
    onPrimary = Charcoal,

    primaryContainer = DarkText,
    onPrimaryContainer = Charcoal,

    secondary = DarkTextSecondary,
    onSecondary = Charcoal,

    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkText,

    tertiary = PaleBlue,
    onTertiary = PaleBlueText,

    tertiaryContainer = Color(0xFF173044),
    onTertiaryContainer = PaleBlue,

    background = DarkBackground,
    onBackground = DarkText,

    surface = DarkSurface,
    onSurface = DarkText,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,

    outline = DarkBorder,
    outlineVariant = DarkBorder,

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),

    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// Shapes

/**
 * SleepSlide deliberately avoids excessive rounding.
 *
 * 4dp  -> buttons / compact controls
 * 8dp  -> cards
 * 12dp -> larger surfaces
 */
val SleepSlideShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
)

// Extended SleepSlide design tokens

@Immutable
data class SleepSlideExtendedColors(
    val border: Color,
    val mutedSurface: Color,

    val paleRed: Color,
    val paleRedText: Color,

    val paleBlue: Color,
    val paleBlueText: Color,

    val paleGreen: Color,
    val paleGreenText: Color,

    val paleYellow: Color,
    val paleYellowText: Color
)

private val LightExtendedColors = SleepSlideExtendedColors(
    border = Border,
    mutedSurface = SurfaceMuted,

    paleRed = PaleRed,
    paleRedText = PaleRedText,

    paleBlue = PaleBlue,
    paleBlueText = PaleBlueText,

    paleGreen = PaleGreen,
    paleGreenText = PaleGreenText,

    paleYellow = PaleYellow,
    paleYellowText = PaleYellowText
)

private val DarkExtendedColors = SleepSlideExtendedColors(
    border = DarkBorder,
    mutedSurface = DarkSurfaceVariant,

    paleRed = Color(0xFF442022),
    paleRedText = Color(0xFFFFB4B0),

    paleBlue = Color(0xFF173044),
    paleBlueText = Color(0xFF9DD7FF),

    paleGreen = Color(0xFF203623),
    paleGreenText = Color(0xFFB6DDB5),

    paleYellow = Color(0xFF403716),
    paleYellowText = Color(0xFFE8D18A)
)

val LocalSleepSlideColors = staticCompositionLocalOf {
    LightExtendedColors
}

// -----------------------------------------------------------------------------
// Theme
// -----------------------------------------------------------------------------

@Composable
fun SleepSlideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    val extendedColors = if (darkTheme) {
        DarkExtendedColors
    } else {
        LightExtendedColors
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalSleepSlideColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SleepSlideTypography,
            shapes = SleepSlideShapes,
            content = content
        )
    }
}