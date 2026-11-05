package org.multipaz.transit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** The one solid, unmissable action on a screen. */
@Composable
fun PrimaryButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = TransitTheme.colors
    val type = TransitTheme.type
    Box(
        modifier
            .clip(RoundedCornerShape(13.dp))
            .background(c.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = type.title, color = Color.White)
    }
}

/** A quiet secondary action: outline only, so it never competes with the tap target. */
@Composable
fun GhostButton(
    label: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    onClick: () -> Unit,
) {
    val c = TransitTheme.colors
    val type = TransitTheme.type
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, c.ink.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = type.label, color = color ?: c.ink.copy(alpha = 0.6f))
    }
}
