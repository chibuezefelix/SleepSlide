package com.opxl.sleepslide.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Layout constants shared by every screen. A screen that needs a different value is
 * usually a screen that has drifted — change the token, not the call site.
 */
object Dimens {
    /** Horizontal screen gutter. */
    val gutter = 20.dp

    /** Cards and list groups. */
    val cardRadius = 20.dp

    /** Hero panels, sheets and the photo backdrop. */
    val panelRadius = 24.dp

    /** Inputs and square icon buttons. */
    val controlRadius = 14.dp

    /** Tags and small badges. */
    val tagRadius = 8.dp

    /** Hairline borders and dividers. */
    val hairline = 1.dp

    /** Circular icon buttons (back, add, more). */
    val iconButton = 40.dp

    /** Leading icon circles in list rows. */
    val leadingCircle = 40.dp

    /** Minimum height of a tappable list row. */
    val rowMinHeight = 60.dp

    /** Vertical rhythm between a section label and the content above it. */
    val sectionGap = 28.dp
}
