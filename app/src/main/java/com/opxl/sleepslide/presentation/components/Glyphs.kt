package com.opxl.sleepslide.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.opxl.sleepslide.R

/**
 * Shared icon glyphs backed by the vector drawables in res/drawable.
 * Decorative by default (null contentDescription) — the enclosing button carries the label.
 */

@Composable
fun ChevronLeft(tint: Color, modifier: Modifier = Modifier) {
    Icon(
        painter            = painterResource(R.drawable.ic_chevron_left),
        contentDescription = null,
        tint               = tint,
        modifier           = modifier.size(16.dp),
    )
}

@Composable
fun ChevronRight(tint: Color, modifier: Modifier = Modifier) {
    Icon(
        painter            = painterResource(R.drawable.ic_chevron_right),
        contentDescription = null,
        tint               = tint,
        modifier           = modifier.size(16.dp),
    )
}

@Composable
fun CheckIcon(tint: Color, size: Dp = 12.dp, modifier: Modifier = Modifier) {
    Icon(
        painter            = painterResource(R.drawable.ic_check),
        contentDescription = null,
        tint               = tint,
        modifier           = modifier.size(size),
    )
}

@Composable
fun ClockIcon(tint: Color, size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Icon(
        painter            = painterResource(R.drawable.ic_clock),
        contentDescription = null,
        tint               = tint,
        modifier           = modifier.size(size),
    )
}
