package org.multipaz.transit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.multipaz.transit.payment.AgeDiscount
import kotlin.time.Duration.Companion.milliseconds

/**
 * The journey simulation between entry and exit. A station can be selected manually; when no
 * selection is made within seven seconds, the terminal selects a random destination for the demo.
 */
@Composable
fun StationSelectionScreen(
    discount: AgeDiscount,
    onStationSelected: (Station) -> Unit,
) {
    val stations = getStationList(discount = discount)
    var secondsRemaining by remember(discount) { mutableIntStateOf(7) }
    var selected by remember(discount) { mutableStateOf(false) }

    fun select(station: Station) {
        if (!selected) {
            selected = true
            onStationSelected(station)
        }
    }

    LaunchedEffect(discount) {
        repeat(7) {
            delay(1_000.milliseconds)
            secondsRemaining -= 1
        }
        select(stations.random())
    }

    val c = TransitTheme.colors
    val type = TransitTheme.type
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 22.dp)) {
        Text("Where is the rider travelling?", style = type.display, color = c.ink)
        Spacer(Modifier.height(5.dp))
        Text(
            "Select a destination, or one will be simulated automatically in $secondsRemaining s.",
            style = type.body,
            color = c.ink.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 6.dp),
        ) {
            items(stations, key = { it.name }) { station ->
                StationRow(station) { select(station) }
            }
        }
    }
}

@Composable
private fun StationRow(station: Station, onClick: () -> Unit) {
    val c = TransitTheme.colors
    val type = TransitTheme.type
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(if (pressed) c.tintLow else c.card)
            .border(
                width = 1.dp,
                color = if (pressed) c.primaryBorder else c.border,
                shape = RoundedCornerShape(13.dp),
            )
            .clickable(interactionSource = interactions, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(station.name, style = type.title, color = c.ink)
            Spacer(Modifier.height(3.dp))
            Text(station.detail, style = type.caption, color = c.ink.copy(alpha = 0.45f))
        }
        Spacer(Modifier.width(12.dp))
        Text(station.fare, style = type.fare, color = c.primaryInk)
    }
}
