package org.multipaz.pos.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AmountEntryScreen(onCheckout: (Long) -> Unit) {
    val c = PosTheme.colors
    var entry by remember { mutableStateOf(AmountEntry()) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(c.surfaceContainerLow)
            .border(1.dp, c.surfaceVariant, RoundedCornerShape(4.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        AmountDisplay(entry.display)
        NumpadGrid(
            Modifier.weight(1f),
            onDigit = { entry = entry.append(it) },
            onDot = { entry = entry.appendDot() },
            onClear = { entry = entry.cleared() },
        )
        CheckoutButton(onClick = { onCheckout(entry.cents) })
    }
}

@Composable
private fun AmountDisplay(text: String) {
    val c = PosTheme.colors
    val type = PosTheme.type
    val blink = rememberInfiniteTransition(label = "caret")
    val caretAlpha by blink.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "caretAlpha",
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(c.surfaceContainerLowest)
            .border(1.dp, c.primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("TOTAL AMOUNT", style = type.labelMd, color = c.primary, letterSpacing = 2.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$", style = type.headlineLg, color = c.primary.copy(alpha = 0.4f))
            Spacer(Modifier.width(8.dp))
            Text(text, style = type.amountDisplay, color = c.primary)
            Box(
                Modifier
                    .padding(start = 4.dp)
                    .width(4.dp)
                    .height(44.dp)
                    .alpha(caretAlpha)
                    .background(c.primary),
            )
        }
    }
}

@Composable
private fun NumpadGrid(
    modifier: Modifier = Modifier,
    onDigit: (Char) -> Unit,
    onDot: () -> Unit,
    onClear: () -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { key -> NumKey(key, Modifier.weight(1f)) { onDigit(key[0]) } }
            }
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ClearKey(Modifier.weight(1f), onClear)
            NumKey("0", Modifier.weight(1f)) { onDigit('0') }
            NumKey(".", Modifier.weight(1f)) { onDot() }
        }
    }
}

@Composable
private fun NumKey(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = PosTheme.colors
    val type = PosTheme.type
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(c.surfaceContainerHigh)
            .border(1.dp, c.surfaceVariant, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = type.numpadKey, color = c.onSurface)
    }
}

@Composable
private fun ClearKey(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = PosTheme.colors
    val type = PosTheme.type
    Column(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(c.errorContainer)
            .border(1.dp, c.error.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Clear", tint = c.onErrorContainer, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text("CLEAR", style = type.labelSm, color = c.onErrorContainer)
    }
}

@Composable
private fun CheckoutButton(onClick: () -> Unit) {
    val c = PosTheme.colors
    val type = PosTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(c.primaryContainer)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "CHECKOUT NOW",
            style = type.headlineMd.copy(fontWeight = FontWeight.Bold),
            color = c.onPrimaryContainer,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.onPrimaryContainer, modifier = Modifier.size(32.dp))
    }
}
