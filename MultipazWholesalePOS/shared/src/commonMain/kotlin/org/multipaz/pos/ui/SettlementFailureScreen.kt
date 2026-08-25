package org.multipaz.pos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.multipaz.pos.payment.SettlementResult

/**
 * Terminal state for a sale that did not complete.
 */
@Composable
fun SettlementFailureScreen(
    failure: SettlementResult.Declined,
    amountCents: Long,
    onNewTransaction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FailurePanel(Modifier.weight(1f).padding(16.dp), failure.reason)
        FailureDetailsPanel(
            Modifier.weight(1f).padding(16.dp),
            failure.reason,
            amountCents,
            onNewTransaction
        )
    }
}

@Composable
private fun FailurePanel(modifier: Modifier = Modifier, reason: String) {
    val c = PosTheme.colors
    val type = PosTheme.type
    val accent = c.error
    Column(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(c.surfaceContainerLow)
            .border(1.dp, c.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(120.dp).clip(CircleShape).background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.ReportProblem,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(96.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = reason,
            style = type.labelMd,
            color = c.error,
            letterSpacing = 4.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FailureDetailsPanel(
    modifier: Modifier = Modifier,
    reason: String,
    amountCents: Long,
    onNewTransaction: () -> Unit,
) {
    val c = PosTheme.colors
    val type = PosTheme.type
    Column(modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(c.surfaceContainerHigh)
                .border(1.dp, c.outlineVariant, RoundedCornerShape(8.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("AMOUNT", style = type.labelSm, color = c.onSurfaceVariant)
                Text(formatCurrency(amountCents), style = type.headlineLg, color = c.onSurface)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.surfaceVariant))
        }

        FailureAction(
            Icons.Filled.AddShoppingCart,
            "NEW TRANSACTION",
            filled = false,
            onClick = onNewTransaction
        )
    }
}

@Composable
private fun FailureAction(
    icon: ImageVector,
    label: String,
    filled: Boolean,
    onClick: () -> Unit
) {
    val c = PosTheme.colors
    val type = PosTheme.type
    val bg = if (filled) c.primary else Color.Transparent
    val fg = if (filled) c.onPrimary else c.primary
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .then(
                if (filled) Modifier
                else Modifier.border(1.dp, c.primary, RoundedCornerShape(4.dp))
            )
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = type.headlineMd, color = fg)
    }
}
