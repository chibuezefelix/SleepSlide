package com.opxl.sleepslide.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.MutedGray
import com.opxl.sleepslide.ui.theme.White

/**
 * The app's one slider: a thin line with a solid run up to the value, a muted
 * remainder, and a small round thumb sitting on the line (see docs reference img).
 *
 * Wraps Material 3 [Slider] so drag/keyboard/a11y behaviour is unchanged — only the
 * thumb and track are drawn here. Use [LineSliderColors.light] on cards and
 * [LineSliderColors.dark] on charcoal surfaces; pass `muted = true` for a silenced layer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    colors: LineSliderColors = LineSliderColors.light(),
    trackThickness: Dp = 3.dp,
    thumbRadius: Dp = 6.dp,
) {
    Slider(
        value                 = value,
        onValueChange         = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange            = valueRange,
        enabled               = enabled,
        modifier              = modifier,
        thumb = {
            Box(
                Modifier
                    .size(thumbRadius * 2)
                    .clip(CircleShape)
                    .background(colors.thumb)
            )
        },
        track = { state ->
            LineTrack(
                state     = state,
                colors    = colors,
                thickness = trackThickness,
                // Match the thumb's height so the line is centred on it and the
                // hit area is the same as the thumb's.
                height    = thumbRadius * 2,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineTrack(
    state: SliderState,
    colors: LineSliderColors,
    thickness: Dp,
    height: Dp,
) {
    val span = state.valueRange.endInclusive - state.valueRange.start
    val fraction = if (span > 0f) ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f) else 0f

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val y = size.height / 2f
        val stroke = thickness.toPx()
        val inset = stroke / 2f                       // keep round caps inside the bounds
        val start = Offset(inset, y)
        val end   = Offset(size.width - inset, y)
        val split = Offset(inset + (size.width - stroke) * fraction, y)

        drawLine(colors.inactive, start, end,   stroke, StrokeCap.Round)
        if (fraction > 0f) drawLine(colors.active, start, split, stroke, StrokeCap.Round)
    }
}

@Immutable
data class LineSliderColors(
    val active: Color,
    val inactive: Color,
    val thumb: Color,
) {
    companion object {
        /** On white cards / the grey ground. */
        fun light(muted: Boolean = false) = LineSliderColors(
            active   = if (muted) MutedGray else Charcoal,
            inactive = Border,
            thumb    = if (muted) MutedGray else Charcoal,
        )

        /** On charcoal surfaces such as the Library mix sheet. */
        fun dark(muted: Boolean = false) = LineSliderColors(
            active   = if (muted) White.copy(alpha = 0.25f) else White,
            inactive = White.copy(alpha = 0.18f),
            thumb    = if (muted) White.copy(alpha = 0.25f) else White,
        )
    }
}
