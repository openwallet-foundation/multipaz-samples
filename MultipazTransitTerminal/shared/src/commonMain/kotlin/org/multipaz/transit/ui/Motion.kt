package org.multipaz.transit.ui

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Runs 0 → 1 once, the frame after this composable enters. Screens stagger their parts by
 * passing a [delayMillis], which is what gives the outcome screens their settle-in feel.
 */
@Composable
fun rememberAppearance(delayMillis: Int = 0, durationMillis: Int = 360): Float {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis, delayMillis, EaseOutCubic),
        label = "appearance",
    )
    return progress
}

/** Fades in while lifting the last few dp into place. */
fun Modifier.riseIn(progress: Float): Modifier = graphicsLayer {
    alpha = progress
    translationY = (1f - progress) * 9.dp.toPx()
}

/** Fades in while scaling up from slightly small, for the outcome badges. */
fun Modifier.popIn(progress: Float): Modifier = graphicsLayer {
    alpha = progress
    val scale = 0.86f + 0.14f * progress
    scaleX = scale
    scaleY = scale
}
