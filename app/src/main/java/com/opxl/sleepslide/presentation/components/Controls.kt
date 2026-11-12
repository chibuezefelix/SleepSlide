package com.opxl.sleepslide.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.opxl.sleepslide.ui.theme.Border
import com.opxl.sleepslide.ui.theme.Charcoal
import com.opxl.sleepslide.ui.theme.Dimens
import com.opxl.sleepslide.ui.theme.LocalSleepSlideColors
import com.opxl.sleepslide.ui.theme.MutedGray
import com.opxl.sleepslide.ui.theme.SurfaceMuted
import com.opxl.sleepslide.ui.theme.White

/*
 * Controls: buttons, chips, switch, search field. All pills, all one height per role.
 */

/** Round icon button. Outlined on the surface by default; `filled` inverts to ink. */
@Composable
fun CircleIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.iconButton,
    filled: Boolean = false,
    enabled: Boolean = true,
    background: Color = if (filled) Charcoal else White,
    border: Color = if (filled) Charcoal else Border,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (enabled) background else background.copy(alpha = background.alpha * 0.6f))
            .border(Dimens.hairline, if (enabled) border else border.copy(alpha = border.alpha * 0.6f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick, role = Role.Button)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.alpha(if (enabled) 1f else 0.4f), contentAlignment = Alignment.Center, content = content)
    }
}

/** Borderless round icon button for secondary actions inside rows (mute, remove, more). */
@Composable
fun GhostIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    background: Color = Color.Transparent,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick, role = Role.Button)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Primary pill: ink fill, on-ink text. */
@Composable
fun PrimaryPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    background: Color = Charcoal,
    contentColor: Color = White,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick, role = Role.Button)
            .alpha(if (enabled) 1f else 0.5f)
            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        leading?.invoke()
        Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}

/** Secondary pill: surface fill with a hairline. */
@Composable
fun SecondaryPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = Charcoal,
    background: Color = White,
    border: Color = Border,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .border(Dimens.hairline, border, CircleShape)
            .clickable(enabled = enabled, onClick = onClick, role = Role.Button)
            .alpha(if (enabled) 1f else 0.5f)
            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        leading?.invoke()
        Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}

/** A selectable pill chip for filters and option steps. */
@Composable
fun SelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable (tint: Color) -> Unit)? = null,
) {
    val bg by animateColorAsState(if (selected) Charcoal else SurfaceMuted, tween(180), label = "chip_bg")
    val fg by animateColorAsState(if (selected) White else MutedGray, tween(180), label = "chip_fg")
    val stroke by animateColorAsState(if (selected) Charcoal else Border, tween(180), label = "chip_border")
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .border(Dimens.hairline, stroke, CircleShape)
            .clickable(enabled = enabled, onClick = onClick, role = Role.Button)
            .alpha(if (enabled) 1f else 0.5f)
            .defaultMinSize(minHeight = 36.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        leading?.invoke(fg)
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

/** Small rounded tag ("Premium", a count). */
@Composable
fun Tag(
    text: String,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.tagRadius))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}

/**
 * The app's switch: a pill track with a sliding thumb. State is hoisted; `onToggle = null`
 * makes it a passive indicator inside a row that handles the tap itself.
 */
@Composable
fun SleepSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onToggle: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
) {
    val colors = LocalSleepSlideColors.current
    val trackWidth = 44.dp
    val trackHeight = 26.dp
    val thumb = 20.dp
    val inset = 3.dp
    val track by animateColorAsState(if (checked) colors.ink else colors.border, tween(200), label = "sw_track")
    val thumbColor = when {
        checked      -> colors.surface
        colors.isDark -> colors.muted
        else         -> colors.surface
    }
    val x by animateDpAsState(if (checked) trackWidth - thumb - inset else inset, tween(200), label = "sw_x")
    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(CircleShape)
            .background(track)
            .then(
                if (onToggle != null)
                    Modifier.clickable(enabled = enabled, role = Role.Switch) { onToggle(!checked) }
                else Modifier
            )
            .alpha(if (enabled) 1f else 0.5f),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = x)
                .size(thumb)
                .clip(CircleShape)
                .background(thumbColor),
        )
    }
}

/** Pill search field with a leading glyph and a clear button once there is text. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    clearDescription: String = "",
    enabled: Boolean = true,
    active: Boolean = value.isNotEmpty(),
    focusRequester: FocusRequester? = null,
) {
    val stroke by animateColorAsState(if (active) Charcoal else Border, tween(180), label = "search_border")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(SurfaceMuted)
            .border(Dimens.hairline, stroke, CircleShape)
            .height(46.dp)
            .padding(start = 16.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GlyphIcon(Glyph.Search, MutedGray, size = 18.dp)
        BasicTextField(
            value         = value,
            onValueChange = { if (enabled) onValueChange(it) },
            enabled       = enabled,
            singleLine    = true,
            textStyle     = MaterialTheme.typography.bodyMedium.copy(color = Charcoal),
            cursorBrush   = SolidColor(Charcoal),
            modifier      = Modifier
                .weight(1f)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(hint, style = MaterialTheme.typography.bodyMedium, color = MutedGray)
                    }
                    inner()
                }
            },
        )
        if (value.isNotEmpty() && onClear != null) {
            GhostIconButton(
                onClick            = onClear,
                contentDescription = clearDescription,
                size               = 28.dp,
                background         = Border,
            ) { GlyphIcon(Glyph.Close, Charcoal, size = 12.dp) }
        }
    }
}

/** Bordered text input in the app's style, for dialogs. */
@Composable
fun SleepTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    active: Boolean = value.isNotBlank(),
) {
    val shape = RoundedCornerShape(Dimens.controlRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceMuted)
            .border(Dimens.hairline, if (active) Charcoal else Border, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value         = value,
            onValueChange = onValueChange,
            singleLine    = true,
            textStyle     = MaterialTheme.typography.bodyMedium.copy(color = Charcoal),
            cursorBrush   = SolidColor(Charcoal),
            modifier      = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyMedium, color = MutedGray)
                    inner()
                }
            },
        )
    }
}

/** Three pulsing dots used while something loads inline. */
@Composable
fun LoadingDots(tint: Color, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { Box(Modifier.size(5.dp).clip(CircleShape).background(tint)) }
    }
}

/** Fixed-width spacer for aligning a title between two icon buttons. */
@Composable
fun IconButtonSpacer() = Box(Modifier.width(Dimens.iconButton))
