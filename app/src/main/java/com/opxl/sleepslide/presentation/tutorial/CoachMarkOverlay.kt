package com.opxl.sleepslide.presentation.tutorial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.WarmWhite
import com.opxl.sleepslide.ui.theme.White

/** Where a coach mark points ([anchor] in root coordinates) and the one-line label it shows. */
data class CoachMark(val anchor: Rect, val label: String)

/** Bounds of the elements a screen wants pointed at, keyed by the ids its label list uses. */
class CoachMarkAnchors {
    val bounds = mutableStateMapOf<String, Rect>()
}

val LocalCoachMarkAnchors = staticCompositionLocalOf<CoachMarkAnchors?> { null }

/**
 * Registers this element as a coach mark target. A no-op outside a [CoachMarkHost], so
 * private composables can carry it without knowing whether the screen shows coach marks.
 */
@Composable
fun Modifier.coachMarkAnchor(key: String): Modifier {
    val anchors = LocalCoachMarkAnchors.current ?: return this
    DisposableEffect(anchors, key) { onDispose { anchors.bounds.remove(key) } }
    return onGloballyPositioned { coords ->
        val rect = coords.boundsInRoot()
        if (anchors.bounds[key] != rect) anchors.bounds[key] = rect
    }
}

/**
 * Wraps a screen: provides the anchor registry to [content] and lays the overlay over it
 * on the screen's first visit. Only marks whose target is currently laid out are drawn, so
 * [labels] can list elements that appear in some states and not others (empty vs ready).
 *
 * @param labels anchor key → label, in the order the marks should be resolved.
 */
@Composable
fun CoachMarkHost(
    viewModel: TutorialViewModel,
    labels: List<Pair<String, String>>,
    content: @Composable () -> Unit,
) {
    val show by viewModel.showCoachMarks.collectAsStateWithLifecycle()
    val anchors = remember { CoachMarkAnchors() }

    CompositionLocalProvider(LocalCoachMarkAnchors provides anchors) {
        Box(Modifier.fillMaxSize()) {
            content()

            // Wait for at least one target to exist so a loading screen isn't dimmed for nothing.
            AnimatedVisibility(
                visible = show && labels.any { (key, _) -> anchors.bounds.containsKey(key) },
                enter   = fadeIn(tween(250)),
                exit    = fadeOut(tween(200)),
            ) {
                // Resolved inside so the arrows stay put while the overlay fades out.
                val marks = labels.mapNotNull { (key, label) ->
                    anchors.bounds[key]?.let { CoachMark(anchor = it, label = label) }
                }
                CoachMarkOverlay(marks = marks, onDismiss = viewModel::dismiss)
            }
        }
    }
}

/**
 * Translucent scrim with a spotlight cut around each anchor, an arrow from a label to the
 * spotlight, and the label itself — all drawn in one pass. Any tap dismisses; the gesture is
 * swallowed so nothing underneath scrolls or activates.
 */
@Composable
fun CoachMarkOverlay(
    marks: List<CoachMark>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "Tap anywhere to continue",
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle   = MaterialTheme.typography.labelLarge.copy(color = Charcoal)
    val hintStyle    = MaterialTheme.typography.labelMedium.copy(color = WarmWhite.copy(alpha = 0.7f))
    var origin by remember { mutableStateOf(Offset.Zero) }
    val description = remember(marks) { marks.joinToString(". ") { it.label } }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(onDismiss) {
                awaitEachGesture {
                    awaitFirstDown().consume()
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    onDismiss()
                }
            }
            .semantics(mergeDescendants = true) {
                contentDescription = "$description. $hint"
                onClick { onDismiss(); true }
            }
            // Offscreen so BlendMode.Clear punches holes in the scrim rather than the window
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        val w = size.width
        val h = size.height
        val margin   = 16.dp.toPx()
        val pad      = 12.dp.toPx()
        val gap      = 40.dp.toPx()
        val nudge    = 8.dp.toPx()
        val headLen  = 7.dp.toPx()
        val spotPad  = 6.dp.toPx()
        val spotCorner  = CornerRadius(10.dp.toPx())
        val labelCorner = CornerRadius(8.dp.toPx())
        val stroke   = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val maxTextWidth = (minOf(250.dp.toPx(), w - 2 * margin) - 2 * pad).toInt()

        drawRect(Charcoal.copy(alpha = 0.78f))

        // Targets that are actually inside this overlay; each becomes a spotlight and an obstacle.
        val spots = marks
            .map { it to it.anchor.translate(-origin) }
            .filter { (_, a) -> a.left >= 0f && a.top >= 0f && a.right <= w && a.bottom <= h }
            .map { (mark, a) -> mark to a.inflate(spotPad) }
        val obstacles = spots.mapTo(mutableListOf()) { it.second }

        spots.forEach { (mark, spot) ->
            drawRoundRect(Color.Black, spot.topLeft, spot.size, spotCorner, blendMode = BlendMode.Clear)
            drawRoundRect(WarmWhite, spot.topLeft, spot.size, spotCorner, style = stroke)

            // Label goes on whichever side of the target has more room, then slides away
            // from anything already drawn so labels never cover each other or a spotlight.
            val text  = textMeasurer.measure(mark.label, labelStyle, constraints = Constraints(maxWidth = maxTextWidth))
            val boxW  = text.size.width + 2 * pad
            val boxH  = text.size.height + 1.5f * pad
            val above = spot.center.y > h / 2f
            val left  = (spot.center.x - boxW / 2f).coerceIn(margin, w - margin - boxW)
            var top   = if (above) spot.top - gap - boxH else spot.bottom + gap
            var box   = Rect(left, top, left + boxW, top + boxH)
            repeat(4) {
                obstacles.firstOrNull { it.overlaps(box) }?.let { hit ->
                    top = if (above) hit.top - boxH - nudge else hit.bottom + nudge
                    box = Rect(left, top, left + boxW, top + boxH)
                }
            }
            top = top.coerceIn(margin, h - margin - boxH)
            box = Rect(left, top, left + boxW, top + boxH)
            obstacles += box

            // Arrow: leaves the label vertically, arrives at the spotlight vertically.
            val start = Offset(box.center.x, if (above) box.bottom else box.top)
            val end   = Offset(spot.center.x, if (above) spot.top - nudge / 2 else spot.bottom + nudge / 2)
            val bend  = (end.y - start.y) / 2f
            val dir   = if (above) 1f else -1f      // +1 when the head points down
            drawPath(
                Path().apply {
                    moveTo(start.x, start.y)
                    cubicTo(start.x, start.y + bend, end.x, end.y - bend, end.x, end.y)
                    moveTo(end.x - headLen * 0.5f, end.y - dir * headLen * 0.87f)
                    lineTo(end.x, end.y)
                    lineTo(end.x + headLen * 0.5f, end.y - dir * headLen * 0.87f)
                },
                color = WarmWhite,
                style = stroke,
            )

            drawRoundRect(White, box.topLeft, box.size, labelCorner)
            drawText(text, topLeft = Offset(box.left + pad, box.top + 0.75f * pad))
        }

        val hintLayout = textMeasurer.measure(hint, hintStyle)
        drawText(
            hintLayout,
            topLeft = Offset((w - hintLayout.size.width) / 2f, h - 2 * margin - hintLayout.size.height),
        )
    }
}
