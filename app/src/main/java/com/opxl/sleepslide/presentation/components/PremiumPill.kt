package com.opxl.sleepslide.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.opxl.sleepslide.R
import com.opxl.sleepslide.ui.theme.PaleYellow
import com.opxl.sleepslide.ui.theme.PaleYellowText

/** "Premium" tag for content the free tier can't play — Library sound rows, Presets cards. */
@Composable
fun PremiumPill(modifier: Modifier = Modifier) {
    Tag(
        text         = stringResource(R.string.premium_pill),
        background   = PaleYellow,
        contentColor = PaleYellowText,
        modifier     = modifier,
    )
}
