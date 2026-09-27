package com.opxl.sleepslide.presentation.scene

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.ui.theme.ImageScrim
import com.opxl.sleepslide.ui.theme.OnImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Photo hero for the player. Picks a [Scene] for the current mix (see [Scene.pick]),
 * crossfades when the mix changes, and "breathes" — a very slow Ken Burns drift —
 * only while audio is playing, so a paused player visibly rests.
 *
 * The user can tap the photo to cycle scenes; that choice sticks until the mix
 * changes, at which point the picker takes over again.
 */
@Composable
fun SceneBackdrop(
    sounds: List<Domain.Sound>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onTap: (() -> Unit)? = null,   // null = tap cycles scenes; otherwise tap is delegated
) {
    // Re-pick only when the set of sounds changes, not on every volume tick
    val mixKey = remember(sounds) { sounds.map { it.id }.sorted().joinToString(",") }
    val autoScene = remember(mixKey) { Scene.pick(sounds) }

    var userScene by remember(mixKey) { mutableStateOf<Scene?>(null) }
    val scene = userScene ?: autoScene

    val height by animateFloatAsState(
        targetValue   = when {
            compact   -> if (isPlaying) 150f else 130f
            else      -> if (isPlaying) 250f else 200f
        },
        animationSpec = tween(700),
        label         = "sceneHeight",
    )

    // Ken Burns: `motion` eases the drift in while playing and back to rest when paused — no
    // snap in either direction. The infinite drift itself only exists while motion is non-zero:
    // left running, it ticks a frame every vsync for as long as the screen is up, paused or not,
    // and competes with nav transitions for frame time.
    val motion = animateFloatAsState(
        targetValue   = if (isPlaying) 1f else 0f,
        animationSpec = tween(1_500),
        label         = "sceneMotion",
    )
    val drifting by remember { derivedStateOf { motion.value > 0f } }
    val kenBurns = if (drifting) rememberKenBurns() else null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(height.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ImageScrim)
            .clickable { onTap?.invoke() ?: run { userScene = scene.next() } }
            .semantics {
                contentDescription = if (onTap != null) "Scene: ${scene.title}. Tap to open player."
                                     else "Scene: ${scene.title}. Tap to change."
            },
    ) {
        AnimatedContent(
            targetState    = scene,
            transitionSpec = { fadeIn(tween(900)) togetherWith fadeOut(tween(900)) },
            label          = "sceneCrossfade",
        ) { target ->
            val bitmap by rememberSceneBitmap(target)
            bitmap?.let {
                Image(
                    bitmap             = it,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Read here (draw phase) so the drift never recomposes the screen
                            if (kenBurns != null) {
                                val m = motion.value
                                val s = 1f + (kenBurns.scale.value - 1f) * m
                                scaleX = s
                                scaleY = s
                                translationX = size.width * kenBurns.shift.value * m
                            }
                        },
                )
            }
        }

        // Legibility scrim + caption
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1.0f  to ImageScrim.copy(alpha = 0.72f),
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text  = scene.title,
                style = MaterialTheme.typography.titleMedium,
                color = OnImage,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = scene.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnImage.copy(alpha = 0.78f),
                )
                if (userScene != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text  = "· your pick",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnImage.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }
}

// ── Asset decoding ────────────────────────────────────────────────────────────

private class KenBurns(val scale: State<Float>, val shift: State<Float>)

@Composable
private fun rememberKenBurns(): KenBurns {
    val drift = rememberInfiniteTransition(label = "kenBurns")
    val scale = drift.animateFloat(
        initialValue  = 1.0f,
        targetValue   = 1.10f,
        animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing), RepeatMode.Reverse),
        label         = "kbScale",
    )
    val shift = drift.animateFloat(
        initialValue  = -0.02f,
        targetValue   = 0.02f,
        animationSpec = infiniteRepeatable(tween(36_000, easing = LinearEasing), RepeatMode.Reverse),
        label         = "kbShift",
    )
    return remember(scale, shift) { KenBurns(scale, shift) }
}

// ── Asset decoding ────────────────────────────────────────────────────────────

// Holds ImageBitmaps (not Bitmaps) so re-entering a screen doesn't re-wrap on every recomposition.
private val sceneCache = HashMap<Scene, ImageBitmap>()

private fun cachedScene(scene: Scene): ImageBitmap? = synchronized(sceneCache) { sceneCache[scene] }

@Composable
private fun rememberSceneBitmap(scene: Scene): State<ImageBitmap?> {
    val context = LocalContext.current
    return produceState(initialValue = cachedScene(scene), scene) {
        if (value == null) value = withContext(Dispatchers.IO) { decodeScene(context, scene) }
    }
}

private fun decodeScene(context: Context, scene: Scene): ImageBitmap? = runCatching {
    cachedScene(scene)?.let { return it }
    val opts = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.RGB_565   // photos, no alpha — halves memory
    }
    context.assets.open(scene.assetPath).use { BitmapFactory.decodeStream(it, null, opts) }
        ?.asImageBitmap()
        ?.also { it.prepareToDraw(); synchronized(sceneCache) { sceneCache[scene] = it } }
}.getOrNull()

/** Convenience for screens that have no mix yet — a scene for the time of day. */
@Composable
fun rememberTimeOfDayScene(): Scene = remember { Scene.pick(emptyList(), Calendar.getInstance()) }
