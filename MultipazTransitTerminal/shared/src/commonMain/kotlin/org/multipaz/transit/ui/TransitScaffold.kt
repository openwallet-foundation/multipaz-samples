package org.multipaz.transit.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.multipaz.transit.Constants
import org.multipaz.transit.payment.AgeDiscount

/** How far along the two credentials are, which is what tints the document chips. */
enum class DocPhase { IDLE, READING, DONE, FAILED }

/**
 * The kiosk chrome that every screen sits inside: the faregate identity at the top, the
 * destination the rider picked plus the two documents this gate needs, and the gate bar along
 * the bottom that slides open once the fare settles.
 */
@Composable
fun TransitScaffold(
    destination: Station?,
    discount: AgeDiscount,
    docPhase: DocPhase,
    journeyResumeNotice: String? = null,
    onChangeDestination: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = TransitTheme.colors
    Column(modifier.fillMaxSize().background(c.surface)) {
        Header()
        destination?.let {
            DestinationSummary(destination, discount)
            DocumentChips(docPhase, discount)
        }
        journeyResumeNotice?.let {
            JourneyResumeNotice(journeyResumeNotice)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            content = content,
        )
        GateBar(open = docPhase == DocPhase.DONE)
    }
}

/** Briefly explains why an already selected destination has been restored instead of reselected. */
@Composable
private fun JourneyResumeNotice(message: String) {
    val c = TransitTheme.colors
    val type = TransitTheme.type
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.tint)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        style = type.bodySmall,
        color = c.primaryInk,
    )
}

@Composable
private fun Header() {
    val c = TransitTheme.colors
    val type = TransitTheme.type
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(22.dp).border(2.5.dp, c.primary, CircleShape)
            )
            Spacer(Modifier.width(9.dp))
            Column {
                Text(
                    Constants.TRANSIT_AUTHORITY,
                    style = type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = c.ink,
                )
                Text(
                    "${Constants.TERMINAL_STATION} · ${Constants.TERMINAL_GATE}",
                    style = type.caption,
                    color = c.ink.copy(alpha = 0.45f),
                )
            }
        }
    }
}

/** "To Harbor Quay — $3.40": what the rider is about to be charged, kept in view the whole way. */
@Composable
private fun DestinationSummary(destination: Station, discount: AgeDiscount) {
    val c = TransitTheme.colors
    val type = TransitTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.tint)
            .padding(horizontal = 15.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "Exit at ${destination.name}",
            style = type.bodySmall,
            color = c.ink.copy(alpha = 0.6f)
        )
        Text(destination.fare, style = type.fare, color = c.primaryInk)
    }
}

/**
 * The two credentials this gate reads in a single tap. They light up together because that is
 * exactly what happens: one presentment carries both the age attestation and the payment card.
 */
@Composable
private fun DocumentChips(phase: DocPhase, discount: AgeDiscount) {
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DocumentChip(Modifier.weight(1f), phase, fareClassLabel(discount), round = true)
        DocumentChip(Modifier.weight(1f), phase, "Payment card", round = false)
    }
}

private fun fareClassLabel(discount: AgeDiscount): String = when (discount) {
    AgeDiscount.Child -> "Child fare · 33% discount"
    AgeDiscount.SeniorCitizen -> "Senior fare · 50% discount"
    AgeDiscount.None -> "Normal fare"
}

@Composable
private fun DocumentChip(
    modifier: Modifier,
    phase: DocPhase,
    label: String,
    round: Boolean,
) {
    val c = TransitTheme.colors
    val type = TransitTheme.type
    val spec = tween<Color>(durationMillis = 250)
    val border by animateColorAsState(
        when (phase) {
            DocPhase.DONE -> c.successBorder
            DocPhase.READING -> c.readingBorder
            DocPhase.FAILED -> c.errorBorder
            DocPhase.IDLE -> c.border
        },
        spec,
    )
    val background by animateColorAsState(
        when (phase) {
            DocPhase.DONE -> c.successBackground
            DocPhase.READING -> c.readingBackground
            DocPhase.FAILED -> c.errorBackground
            DocPhase.IDLE -> c.card
        },
        spec,
    )
    val mark by animateColorAsState(
        when (phase) {
            DocPhase.DONE -> c.success
            DocPhase.READING -> c.primary
            DocPhase.FAILED -> c.error
            DocPhase.IDLE -> c.dotIdle
        },
        spec,
    )
    val ink by animateColorAsState(
        when (phase) {
            DocPhase.DONE -> c.successInkDeep
            DocPhase.READING -> c.primaryInk
            DocPhase.FAILED -> c.errorInk
            DocPhase.IDLE -> c.ink.copy(alpha = 0.45f)
        },
        spec,
    )
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // An age attestation reads as a seal; a payment card reads as a card.
        Box(
            Modifier
                .size(width = 14.dp, height = if (round) 14.dp else 10.dp)
                .clip(if (round) CircleShape else RoundedCornerShape(2.dp))
                .background(mark)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = type.label, color = ink)
    }
}

/**
 * The gate itself, drawn as a bar across the foot of the screen: solid while the gate is shut,
 * sliding out of the way once the fare has settled.
 */
@Composable
private fun GateBar(open: Boolean) {
    val c = TransitTheme.colors
    val closed by animateFloatAsState(
        targetValue = if (open) 0f else 1f,
        animationSpec = tween(durationMillis = if (open) 900 else 0),
        label = "gate",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 22.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(c.track),
    ) {
        if (closed > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(closed)
                    .background(c.success)
            )
        }
    }
}
