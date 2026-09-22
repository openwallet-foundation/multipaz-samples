package org.multipaz.transit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Which mark the outcome badge carries. */
enum class OutcomeMark { CHECK, CROSS }

/** A label/value line in the outcome's detail stack. */
data class OutcomeDetail(
    val label: String,
    val value: String,
    val valueColor: Color? = null,
)

/**
 * The shape both endings share: a badge, a verdict, and the receipt of what was actually read.
 * Only the accent colour and the mark change between "the gate opened" and "it did not".
 */
@Composable
fun OutcomeScreen(
    accent: Color,
    mark: OutcomeMark,
    headline: String,
    subline: String,
    details: List<OutcomeDetail>,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val c = TransitTheme.colors
    val type = TransitTheme.type
    val badge = rememberAppearance(durationMillis = 420)
    val text = rememberAppearance(delayMillis = 60)
    val stack = rememberAppearance(delayMillis = 140)

    Column(Modifier.fillMaxWidth().padding(horizontal = 26.dp)) {
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(100.dp).popIn(badge).clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center,
            ) {
                OutcomeMarkGlyph(mark)
            }
            Spacer(Modifier.height(26.dp))
            Column(
                Modifier.riseIn(text),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(headline, style = type.headline, color = c.ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(7.dp))
                Text(
                    subline,
                    style = type.body,
                    color = c.ink.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                )
            }
            if (details.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                DetailStack(details, Modifier.riseIn(stack))
            }
        }
        GhostButton(
            label = actionLabel,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            onClick = onAction,
        )
    }
}

/** Hairline-separated rows: the receipt of what the gate actually read. */
@Composable
private fun DetailStack(details: List<OutcomeDetail>, modifier: Modifier = Modifier) {
    val c = TransitTheme.colors
    val type = TransitTheme.type
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.border),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        for (detail in details) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(c.card)
                    .padding(horizontal = 15.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(detail.label, style = type.bodySmall, color = c.ink.copy(alpha = 0.6f))
                Text(
                    detail.value,
                    style = type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = detail.valueColor ?: c.ink,
                )
            }
        }
    }
}

@Composable
private fun OutcomeMarkGlyph(mark: OutcomeMark) {
    Canvas(Modifier.size(42.dp)) {
        val stroke = 5.dp.toPx()
        when (mark) {
            OutcomeMark.CHECK -> {
                // A tick drawn as two strokes: down to the elbow, then up and out.
                val elbow = Offset(size.width * 0.38f, size.height * 0.74f)
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.1f, size.height * 0.5f),
                    end = elbow,
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White,
                    start = elbow,
                    end = Offset(size.width * 0.9f, size.height * 0.24f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            OutcomeMark.CROSS -> {
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.16f, size.height * 0.16f),
                    end = Offset(size.width * 0.84f, size.height * 0.84f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.84f, size.height * 0.16f),
                    end = Offset(size.width * 0.16f, size.height * 0.84f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
