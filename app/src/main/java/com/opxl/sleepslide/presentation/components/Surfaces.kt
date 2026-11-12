package com.opxl.sleepslide.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.Dimens
import com.opxl.sleepslide.ui.theme.MutedGray
import com.opxl.sleepslide.ui.theme.White

/*
 * Surfaces: the few containers every screen is built from.
 *
 *  SurfaceCard   — a bordered card on the ground (hero content, grids, banners)
 *  GroupCard     — a card that holds ListRows separated by inset dividers
 *  groupedRow()  — the same look for rows that must live in a LazyColumn
 *  ListRow       — leading circle · title/subtitle · trailing
 *  SectionLabel  — small-caps section heading
 *  ScreenTitle   — page title for tab screens
 *  DashedCard    — "add something here" affordance
 */

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Dimens.cardRadius),
    background: Color = White,
    border: Color = Border,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    glow: Glow? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .glow(glow)
            .border(Dimens.hairline, border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/** A card of rows. Put [RowDivider] between rows, or use [ListRow]s with `divider = true`. */
@Composable
fun GroupCard(
    modifier: Modifier = Modifier,
    background: Color = White,
    border: Color = Border,
    glow: Glow? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Dimens.cardRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .glow(glow)
            .border(Dimens.hairline, border, shape),
        content = content,
    )
}

/** Inset hairline between rows of a [GroupCard]. [inset] is the leading edge; the trailing edge stays at 16dp. */
@Composable
fun RowDivider(inset: Dp = 16.dp, color: Color = Border, endInset: Dp = 16.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = inset, end = endInset)
            .height(Dimens.hairline)
            .background(color),
    )
}

/** Where a row sits inside its group — decides which corners round and where dividers go. */
enum class GroupPosition { Single, First, Middle, Last;
    val isFirst get() = this == Single || this == First
    val isLast  get() = this == Single || this == Last
}

fun groupPosition(index: Int, count: Int): GroupPosition = when {
    count <= 1  -> GroupPosition.Single
    index == 0  -> GroupPosition.First
    index == count - 1 -> GroupPosition.Last
    else        -> GroupPosition.Middle
}

/**
 * Makes a lazy-list item look like one row of a [GroupCard]: shared background, the group's
 * outer border on the outside edges only, and an inset divider under every row but the last.
 * Items must be laid out with no spacing between them.
 */
@Composable
fun Modifier.groupedRow(
    position: GroupPosition,
    background: Color = White,
    border: Color = Border,
    dividerInset: Dp = 16.dp,
): Modifier {
    val r = Dimens.cardRadius
    val shape = RoundedCornerShape(
        topStart    = if (position.isFirst) r else 0.dp,
        topEnd      = if (position.isFirst) r else 0.dp,
        bottomStart = if (position.isLast) r else 0.dp,
        bottomEnd   = if (position.isLast) r else 0.dp,
    )
    val stroke = Dimens.hairline
    return this
        .clip(shape)
        .background(background)
        .drawBehind {
            val w = size.width
            val h = size.height
            val s = stroke.toPx()
            val i = s / 2f
            val rr = r.toPx()
            val p = Path()
            // Left edge, bottom to top
            p.moveTo(i, if (position.isLast) h - rr else h)
            if (position.isFirst) {
                p.lineTo(i, rr)
                p.arcTo(Rect(i, i, i + 2 * rr, i + 2 * rr), 180f, 90f, false)
                p.lineTo(w - rr, i)
                p.arcTo(Rect(w - i - 2 * rr, i, w - i, i + 2 * rr), 270f, 90f, false)
            } else {
                p.lineTo(i, 0f)
                p.moveTo(w - i, 0f)
            }
            // Right edge, top to bottom
            p.lineTo(w - i, if (position.isLast) h - rr else h)
            if (position.isLast) {
                p.arcTo(Rect(w - i - 2 * rr, h - i - 2 * rr, w - i, h - i), 0f, 90f, false)
                p.lineTo(rr, h - i)
                p.arcTo(Rect(i, h - i - 2 * rr, i + 2 * rr, h - i), 90f, 90f, false)
            }
            drawPath(p, border, style = Stroke(s))
            if (!position.isLast) {
                val inset = dividerInset.toPx()
                drawLine(border, Offset(inset, h - i), Offset(w - inset, h - i), s)
            }
        }
}

/**
 * One row: a leading slot (usually [IconCircle]), a title with optional subtitle, and a
 * trailing slot. Tappable when [onClick] is given.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    titleColor: Color = Charcoal,
    subtitleColor: Color = MutedGray,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    titleMaxLines: Int = 1,
    subtitleMaxLines: Int = 2,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = Dimens.rowMinHeight)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = title,
                style    = MaterialTheme.typography.titleSmall,
                color    = titleColor,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = subtitle,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = subtitleColor,
                    maxLines = subtitleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/** A tinted circle holding a glyph — the leading element of list rows. */
@Composable
fun IconCircle(
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.leadingCircle,
    border: Color? = null,
    glow: Color? = null,          // faint highlight in this hue — list rows' only glow
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .circleGlow(glow)
            .then(if (border != null) Modifier.border(Dimens.hairline, border, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Small-caps heading above a section. [trailing] is for a count or a "See all" action. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    topPadding: Dp = Dimens.sectionGap,
    bottomPadding: Dp = 10.dp,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text  = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MutedGray,
        )
        trailing?.invoke()
    }
}

/** Page title for tab screens — a headline with an optional muted line under it. */
@Composable
fun ScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier) {
        Text(
            text  = title,
            style = MaterialTheme.typography.headlineLarge,
            color = Charcoal,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )
        }
    }
}

/** A dashed-outline card: the place where something can be added. */
@Composable
fun DashedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = Border,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Dimens.cardRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .drawBehind {
                val s = Dimens.hairline.toPx()
                val dash = 6.dp.toPx()
                drawRoundRect(
                    color        = color,
                    topLeft      = Offset(s / 2f, s / 2f),
                    size         = Size(size.width - s, size.height - s),
                    cornerRadius = CornerRadius(Dimens.cardRadius.toPx()),
                    style        = Stroke(s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
                )
            }
            .padding(contentPadding),
        content = content,
    )
}
