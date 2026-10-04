package com.opxl.sleepslide.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.opxl.sleepslide.R
import com.opxl.sleepslide.ui.theme.BrandYellow
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.MutedGray
import com.opxl.sleepslide.ui.theme.WarmWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAGLINE = "Calm sounds, mixed your way,\nfor deeper, longer sleep."
private const val FOOTER = "SleepSlide"

private val TileSize = 48.dp
// The adaptive-icon foreground is a 108dp canvas whose visible area is the middle 72dp.
private val TileArtSize = TileSize * 108f / 72f
private val RippleReach = 44.dp

/**
 * Branded intro shown over the app on a cold start, after the system splash: the icon
 * tile with sound-wave ripples, the wordmark sliding out beside it, a tagline and a
 * footer. Fades away by itself and calls [onFinished]; taps underneath are blocked.
 */
@Composable
fun BrandSplash(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val currentOnFinished by rememberUpdatedState(onFinished)

    val tileScale = remember { Animatable(0.6f) }
    val tileAlpha = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    var showWordmark by remember { mutableStateOf(false) }
    var showTagline by remember { mutableStateOf(false) }
    var showFooter by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { tileAlpha.animateTo(1f, tween(300)) }
        launch {
            tileScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            )
        }
        delay(250)
        showWordmark = true
        delay(350)
        showTagline = true
        delay(150)
        showFooter = true
        delay(1_100)
        exit.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        currentOnFinished()
    }

    val ripple = rememberInfiniteTransition(label = "ripple")
    val ripplePhase by ripple.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_800, easing = LinearEasing), RepeatMode.Restart),
        label = "ripplePhase",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - exit.value
                val s = 1f + 0.04f * exit.value
                scaleX = s
                scaleY = s
            }
            .background(WarmWhite)
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(
                    ripplePhase = ripplePhase,
                    modifier = Modifier.graphicsLayer {
                        alpha = tileAlpha.value
                        scaleX = tileScale.value
                        scaleY = tileScale.value
                    },
                )
                AnimatedVisibility(
                    visible = showWordmark,
                    enter = expandHorizontally(tween(450, easing = FastOutSlowInEasing), expandFrom = Alignment.Start) +
                        fadeIn(tween(450, delayMillis = 120)),
                ) {
                    Row {
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "SleepSlide",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Charcoal,
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(Modifier.size(20.dp))

            AnimatedVisibility(
                visible = showTagline,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 },
            ) {
                Text(
                    text = TAGLINE,
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                    color = MutedGray,
                    textAlign = TextAlign.Center,
                )
            }
        }

        AnimatedVisibility(
            visible = showFooter,
            enter = fadeIn(tween(600)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = FOOTER,
                style = MaterialTheme.typography.labelLarge,
                color = MutedGray,
            )
        }
    }
}

/** The launcher icon as a rounded yellow tile, with three rings pulsing out like sound. */
@Composable
private fun IconTile(ripplePhase: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(TileSize)
            .drawBehind {
                val base = size.minDimension / 2f
                val reach = RippleReach.toPx()
                val stroke = Stroke(width = 1.5.dp.toPx())
                repeat(3) { i ->
                    val p = (ripplePhase + i / 3f) % 1f
                    drawCircle(
                        color = BrandYellow,
                        radius = base + p * reach,
                        alpha = (1f - p) * 0.9f,
                        style = stroke,
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(TileSize)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandYellow),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.requiredSize(TileArtSize),
            )
        }
    }
}
