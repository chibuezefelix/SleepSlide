package com.opxl.sleepslide.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.opxl.sleepslide.R

/**
 * SleepSlide typography system — Exo 2 throughout.
 *
 * Uses the *static* weight files. The variable file (exo_variable_font_wght) renders
 * at its default axis value unless FontVariation settings are supplied, which made
 * every style look thin regardless of the FontWeight requested.
 */

private val Exo2 = FontFamily(
    Font(R.font.exo_regular,    FontWeight.Normal),
    Font(R.font.exo_medium,     FontWeight.Medium),
    Font(R.font.exo_semi_bold,  FontWeight.SemiBold),
    Font(R.font.exo_bold,       FontWeight.Bold),
    Font(R.font.exo_extra_bold, FontWeight.ExtraBold),
)

private val UiFont = Exo2
private val EditorialFont = Exo2
private val MonoFont = Exo2

val SleepSlideTypography = Typography(

    // Large editorial screen titles.
    displayLarge = TextStyle(
        fontFamily = EditorialFont,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.2).sp
    ),

    displayMedium = TextStyle(
        fontFamily = EditorialFont,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.9).sp
    ),

    displaySmall = TextStyle(
        fontFamily = EditorialFont,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.7).sp
    ),

    headlineLarge = TextStyle(
        fontFamily = EditorialFont,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.6).sp
    ),

    headlineMedium = TextStyle(
        fontFamily = EditorialFont,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp
    ),

    headlineSmall = TextStyle(
        fontFamily = EditorialFont,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),

    titleLarge = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),

    titleMedium = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),

    titleSmall = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),

    bodySmall = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),

    labelLarge = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    labelMedium = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    labelSmall = TextStyle(
        fontFamily = MonoFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp
    )
)