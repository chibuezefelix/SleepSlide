package com.opxl.sleepslide.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.opxl.sleepslide.ui.theme.PaleYellow
import com.opxl.sleepslide.ui.theme.PaleYellowText

/** "Premium" tag for content the free tier can't play — Library sound rows, Presets cards. */
@Composable
fun PremiumPill(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(PaleYellow)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text("Premium", style = MaterialTheme.typography.labelSmall, color = PaleYellowText)
    }
}
