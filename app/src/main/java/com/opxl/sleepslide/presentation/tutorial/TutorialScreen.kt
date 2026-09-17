package com.opxl.sleepslide.presentation.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.MutedGray
import com.opxl.sleepslide.ui.theme.PaleBlueText
import com.opxl.sleepslide.ui.theme.SurfaceMuted
import com.opxl.sleepslide.ui.theme.WarmWhite
import com.opxl.sleepslide.ui.theme.White
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * One card per feature. Titles are at most four words; [what] and [how] are one sentence each.
 * Sleep timer and fade in/out share a card — the timer *is* the fade-out, so splitting them
 * would explain the same ramp twice.
 */
enum class TutorialCard(val title: String, val what: String, val how: String) {
    MIX_BUILDER(
        title = "Mix builder",
        what  = "Layer up to three sounds and balance each one to taste.",
        how   = "Tap sounds in the Library to add them, then drag their sliders in the mix panel.",
    ),
    TIMER_AND_FADES(
        title = "Timer & fades",
        what  = "The sleep timer fades your mix out at the end, and fade-in eases it on at the start.",
        how   = "Tap the timer button in the player to pick a duration; set default fade lengths in Settings.",
    ),
    NIGHT_LOCK(
        title = "Night lock",
        what  = "Blocks accidental taps so a stray hand can't stop your sound.",
        how   = "Turn it on at the bottom of the player, then long-press the screen to unlock.",
    ),
    PRESETS(
        title = "Presets",
        what  = "Saves a finished mix so you can start it again with one tap.",
        how   = "Tap the save button in the player, then find, pin or reorder it on the Presets tab.",
    ),
    WIND_DOWN(
        title = "Wind-down reminder",
        what  = "A nightly notification nudges you to start winding down.",
        how   = "Enable it in Settings and choose the hour and minute you want the nudge.",
    ),
    TINNITUS(
        title = "Tinnitus sounds",
        what  = "Noise tuned to a frequency helps mask ringing in the ears.",
        how   = "Open the Tinnitus category in the Library and pick the pitch closest to your ring.",
    ),
    BLUETOOTH(
        title = "Bluetooth behaviour",
        what  = "Playback pauses by itself when your headphones disconnect.",
        how   = "Reconnect and press play; a BT badge on Home shows when headphones are linked.",
    ),
}

@Composable
fun TutorialScreen(onClose: () -> Unit) {
    val cards = TutorialCard.entries
    val pagerState = rememberPagerState { cards.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmWhite)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Feature tour", style = MaterialTheme.typography.titleMedium, color = Charcoal)
                Text(
                    "${pagerState.currentPage + 1} of ${cards.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(White)
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .clickable(onClick = onClose)
                    .semantics { contentDescription = "Close" },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(12.dp)) {
                    val s = 1.5.dp.toPx()
                    drawLine(Charcoal, Offset(0f, 0f), Offset(size.width, size.height), s, StrokeCap.Round)
                    drawLine(Charcoal, Offset(size.width, 0f), Offset(0f, size.height), s, StrokeCap.Round)
                }
            }
        }

        HorizontalPager(
            state          = pagerState,
            modifier       = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing    = 12.dp,
            beyondViewportPageCount = 1,
        ) { page -> FeatureCard(cards[page]) }

        // Dot indicator — the pill slides with the swipe rather than jumping page to page.
        val dotStep = 14.dp
        val pillWidth = 16.dp
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 20.dp)
                .size(width = dotStep * (cards.size - 1) + pillWidth, height = 8.dp)
                .semantics { contentDescription = "Page ${pagerState.currentPage + 1} of ${cards.size}" },
        ) {
            val step = dotStep.toPx()
            val pill = pillWidth.toPx()
            repeat(cards.size) { i ->
                drawCircle(Border, radius = size.height / 2f, center = Offset(i * step + pill / 2f, size.height / 2f))
            }
            val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
            drawRoundRect(
                color        = Charcoal,
                topLeft      = Offset(position * step, 0f),
                size         = Size(pill, size.height),
                cornerRadius = CornerRadius(size.height / 2f),
            )
        }

        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun FeatureCard(card: TutorialCard) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(White)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .semantics { contentDescription = "${card.title} illustration" },
        ) {
            drawRoundRect(SurfaceMuted, cornerRadius = CornerRadius(10.dp.toPx()))
            drawRoundRect(Border, cornerRadius = CornerRadius(10.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawIllustration(card)
        }

        Spacer(Modifier.height(24.dp))
        Text(card.title, style = MaterialTheme.typography.headlineSmall, color = Charcoal)
        Spacer(Modifier.height(8.dp))
        Text(card.what, style = MaterialTheme.typography.bodyMedium, color = Charcoal)
        Spacer(Modifier.height(16.dp))
        Text("HOW TO USE IT", style = MaterialTheme.typography.labelSmall, color = MutedGray)
        Spacer(Modifier.height(4.dp))
        Text(card.how, style = MaterialTheme.typography.bodyMedium, color = MutedGray)
    }
}

/** Stroked-path glyphs in the same hand as the app's tab icons. `s` is the glyph's working size. */
private fun DrawScope.drawIllustration(card: TutorialCard) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val s  = minOf(size.width, size.height) * 0.72f
    val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val thin   = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

    when (card) {
        TutorialCard.MIX_BUILDER -> {
            // Three layer sliders at different levels
            val knobs = listOf(0.72f, 0.40f, 0.58f)
            val x0 = cx - s * 0.5f; val x1 = cx + s * 0.5f
            knobs.forEachIndexed { i, k ->
                val y = cy + (i - 1) * s * 0.26f
                val kx = x0 + (x1 - x0) * k
                drawLine(Border,   Offset(x0, y), Offset(x1, y), stroke.width, StrokeCap.Round)
                drawLine(Charcoal, Offset(x0, y), Offset(kx, y), stroke.width, StrokeCap.Round)
                drawCircle(Charcoal, radius = 5.dp.toPx(), center = Offset(kx, y))
            }
        }

        TutorialCard.TIMER_AND_FADES -> {
            // Timer ring with the fade envelope (rise, hold, fall) inside it
            val r = s * 0.44f
            val arcRect = Rect(cx - r, cy - r, cx + r, cy + r)
            drawArc(Border,    135f, 270f, false, arcRect.topLeft, arcRect.size, style = stroke)
            drawArc(Charcoal,  135f, 190f, false, arcRect.topLeft, arcRect.size, style = stroke)
            drawPath(
                Path().apply {
                    moveTo(cx - r * 0.55f, cy + r * 0.32f)
                    lineTo(cx - r * 0.22f, cy - r * 0.22f)
                    lineTo(cx + r * 0.10f, cy - r * 0.22f)
                    lineTo(cx + r * 0.55f, cy + r * 0.32f)
                },
                PaleBlueText,
                style = thin,
            )
        }

        TutorialCard.NIGHT_LOCK -> {
            // Padlock, plus a fingertip with hold rings for the long-press unlock
            val bodyW = s * 0.42f; val bodyH = s * 0.32f
            val bodyTop = cy - s * 0.02f
            drawRoundRect(
                Charcoal,
                topLeft = Offset(cx - bodyW / 2f, bodyTop),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(6.dp.toPx()),
                style = stroke,
            )
            val shackleR = s * 0.14f
            drawArc(
                Charcoal, 180f, 180f, false,
                topLeft = Offset(cx - shackleR, bodyTop - s * 0.14f - shackleR),
                size = Size(shackleR * 2, shackleR * 2),
                style = stroke,
            )
            drawLine(Charcoal, Offset(cx - shackleR, bodyTop - s * 0.14f), Offset(cx - shackleR, bodyTop), stroke.width, StrokeCap.Round)
            drawLine(Charcoal, Offset(cx + shackleR, bodyTop - s * 0.14f), Offset(cx + shackleR, bodyTop), stroke.width, StrokeCap.Round)
            drawCircle(Charcoal, radius = 3.dp.toPx(), center = Offset(cx, bodyTop + bodyH * 0.5f))

            val tip = Offset(cx + s * 0.40f, cy + s * 0.30f)
            drawCircle(PaleBlueText, radius = 4.dp.toPx(), center = tip)
            drawCircle(PaleBlueText.copy(alpha = 0.55f), radius = 10.dp.toPx(), center = tip, style = thin)
            drawCircle(PaleBlueText.copy(alpha = 0.25f), radius = 16.dp.toPx(), center = tip, style = thin)
        }

        TutorialCard.PRESETS -> {
            // Two stacked cards; the front one carries a star and a title line
            val cardW = s * 0.62f; val cardH = s * 0.46f
            val back  = Offset(cx - cardW / 2f - s * 0.04f, cy - cardH / 2f - s * 0.06f)
            val front = Offset(cx - cardW / 2f + s * 0.04f, cy - cardH / 2f + s * 0.06f)
            val corner = CornerRadius(8.dp.toPx())
            drawRoundRect(White,  back,  Size(cardW, cardH), corner)
            drawRoundRect(Border, back,  Size(cardW, cardH), corner, style = thin)
            drawRoundRect(White,    front, Size(cardW, cardH), corner)
            drawRoundRect(Charcoal, front, Size(cardW, cardH), corner, style = stroke)

            val starC = Offset(front.x + s * 0.10f, front.y + s * 0.11f)
            val outer = s * 0.055f; val inner = outer * 0.45f
            drawPath(
                Path().apply {
                    for (i in 0 until 10) {
                        val rr = if (i % 2 == 0) outer else inner
                        val a = Math.toRadians((-90 + i * 36).toDouble())
                        val x = starC.x + (rr * cos(a)).toFloat(); val y = starC.y + (rr * sin(a)).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                },
                Charcoal,
            )
            val lineY = front.y + cardH - s * 0.12f
            drawLine(MutedGray, Offset(front.x + s * 0.06f, lineY),               Offset(front.x + cardW * 0.62f, lineY),               thin.width, StrokeCap.Round)
            drawLine(Border,    Offset(front.x + s * 0.06f, lineY + s * 0.06f),  Offset(front.x + cardW * 0.42f, lineY + s * 0.06f),  thin.width, StrokeCap.Round)
        }

        TutorialCard.WIND_DOWN -> {
            // Bell with clapper; crescent moon carved out of a disc with the panel colour
            val domeR = s * 0.16f
            val base  = cy + s * 0.14f
            drawPath(
                Path().apply {
                    moveTo(cx - s * 0.21f, base)
                    lineTo(cx - domeR, cy - s * 0.14f)
                    arcTo(Rect(cx - domeR, cy - s * 0.14f - domeR, cx + domeR, cy - s * 0.14f + domeR), 180f, 180f, false)
                    lineTo(cx + s * 0.21f, base)
                    close()
                },
                Charcoal,
                style = stroke,
            )
            drawArc(Charcoal, 0f, 180f, false, Offset(cx - s * 0.06f, base - s * 0.04f), Size(s * 0.12f, s * 0.12f), style = stroke)

            val moon = Offset(cx + s * 0.34f, cy - s * 0.30f)
            drawCircle(PaleBlueText, radius = s * 0.09f, center = moon)
            drawCircle(SurfaceMuted, radius = s * 0.08f, center = moon + Offset(s * 0.045f, -s * 0.03f))
        }

        TutorialCard.TINNITUS -> {
            // Noise wave with a notch at the tuned frequency, marked by a dashed line
            val x0 = cx - s * 0.5f; val x1 = cx + s * 0.5f
            val steps = 80
            drawPath(
                Path().apply {
                    for (i in 0..steps) {
                        val t = i / steps.toFloat()
                        val x = x0 + (x1 - x0) * t
                        val notch = 1f - 0.85f * exp(-((x - cx) / (s * 0.12f)).let { it * it })
                        val y = cy + s * 0.14f * notch * sin(t * 2f * Math.PI.toFloat() * 5f)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                },
                Charcoal,
                style = stroke,
            )
            drawLine(
                PaleBlueText,
                Offset(cx, cy - s * 0.30f), Offset(cx, cy + s * 0.30f),
                thin.width, StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
            )
        }

        TutorialCard.BLUETOOTH -> {
            // Headphones with the Bluetooth rune beside them
            val bandR = s * 0.30f
            val bandC = Offset(cx, cy + s * 0.04f)
            drawArc(Charcoal, 180f, 180f, false, Offset(bandC.x - bandR, bandC.y - bandR), Size(bandR * 2, bandR * 2), style = stroke)
            val cupW = s * 0.12f; val cupH = s * 0.20f
            val corner = CornerRadius(4.dp.toPx())
            drawRoundRect(Charcoal, Offset(bandC.x - bandR - cupW * 0.5f, bandC.y - s * 0.02f), Size(cupW, cupH), corner)
            drawRoundRect(Charcoal, Offset(bandC.x + bandR - cupW * 0.5f, bandC.y - s * 0.02f), Size(cupW, cupH), corner)

            val rx = cx + s * 0.42f; val ry = cy - s * 0.40f
            val rw = s * 0.14f;      val rh = s * 0.26f
            drawPath(
                Path().apply {
                    moveTo(rx + rw * 0.5f, ry);             lineTo(rx + rw * 0.5f, ry + rh)
                    lineTo(rx + rw * 0.85f, ry + rh * 0.75f); lineTo(rx + rw * 0.15f, ry + rh * 0.25f)
                    moveTo(rx + rw * 0.5f, ry);             lineTo(rx + rw * 0.85f, ry + rh * 0.25f)
                    lineTo(rx + rw * 0.15f, ry + rh * 0.75f)
                },
                PaleBlueText,
                style = thin,
            )
        }
    }
}
