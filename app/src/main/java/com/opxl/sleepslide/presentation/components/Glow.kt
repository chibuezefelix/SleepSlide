package com.opxl.sleepslide.presentation.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.opxl.sleepslide.ui.theme.GlowStrength
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * A soft radial wash in one corner of a card — light caught on a surface, not a decoration.
 * Off by default everywhere; only feature cards (hero, playing mix, unlock, preset tiles) opt in.
 *
 * [tint] is the hue at full strength (a category colour, or the ink for neutral cards); its peak
 * alpha comes from the theme's [GlowStrength] token so light and dark grounds each get a level
 * that reads as light rather than dirt. [animated] lets the centre drift through one slow loop —
 * reserved for the hero and whatever is actively playing.
 */
@Immutable
data class Glow(
    val tint: Color,
    val animated: Boolean = false,
    val corner: GlowCorner = GlowCorner.TopStart,
)

enum class GlowCorner { TopStart, TopEnd, BottomStart, BottomEnd }

/** One drift cycle. Slow enough to be felt rather than watched. */
private const val DRIFT_PERIOD_MS = 10_000

/** Draws [glow] behind this node's content (place it right after the card's background). */
@Composable
fun Modifier.glow(glow: Glow?): Modifier {
    if (glow == null) return this
    val color = glow.tint.copy(alpha = glow.tint.alpha * GlowStrength)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val phase = rememberDriftPhase(glow.animated)
    return drawBehind {
        val w = size.width
        val h = size.height
        val start = if (rtl) w else 0f
        val end   = if (rtl) 0f else w
        val anchor = when (glow.corner) {
            GlowCorner.TopStart    -> Offset(start, 0f)
            GlowCorner.TopEnd      -> Offset(end, 0f)
            GlowCorner.BottomStart -> Offset(start, h)
            GlowCorner.BottomEnd   -> Offset(end, h)
        }
        // Read in the draw phase only: the drift redraws, it never recomposes.
        val a = phase.value * 2f * PI.toFloat()
        val drift = min(w, h) * 0.12f
        val center = anchor + Offset(cos(a) * drift, sin(a) * drift)
        // About a third of the card: capped by the short side so tall cards don't flood.
        val radius = min(max(w, h) * 0.6f, min(w, h) * 0.95f).coerceAtLeast(1f)
        drawRect(
            Brush.radialGradient(
                0f    to color,
                0.45f to color.copy(alpha = color.alpha * 0.45f),
                1f    to color.copy(alpha = 0f),
                center = center,
                radius = radius,
            ),
        )
    }
}

/**
 * Faint highlight inside a leading circle — the list-row version of [glow]. Rows themselves stay
 * flat so lists read calmly; only the small circle carries the hue.
 */
@Composable
fun Modifier.circleGlow(tint: Color?): Modifier {
    if (tint == null) return this
    // Below the card strength: a small circle at full strength reads as a glossy bead, not light.
    val color = tint.copy(alpha = tint.alpha * GlowStrength * 0.7f)
    return drawBehind {
        drawRect(
            Brush.radialGradient(
                0f to color,
                1f to color.copy(alpha = 0f),
                center = Offset(size.width * 0.28f, size.height * 0.22f),
                radius = size.minDimension * 1.05f,
            ),
        )
    }
}

/**
 * 0→1 phase of the drift loop. Runs only while [animated], the screen is resumed (so it stops
 * with the screen off during overnight playback) and the system hasn't removed animations.
 */
@Composable
private fun rememberDriftPhase(animated: Boolean): State<Float> {
    val phase = remember { Animatable(0f) }
    if (!animated) return phase.asState()

    val context = LocalContext.current
    val motionAllowed = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val running = motionAllowed && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        while (true) {
            val from = phase.value % 1f
            phase.snapTo(from)
            phase.animateTo(from + 1f, tween(DRIFT_PERIOD_MS, easing = LinearEasing))
        }
    }
    return phase.asState()
}
