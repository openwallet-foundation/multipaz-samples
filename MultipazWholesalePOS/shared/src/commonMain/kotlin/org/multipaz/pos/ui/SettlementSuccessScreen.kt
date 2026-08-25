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
import androidx.compose.material.icons.filled.CheckCircle
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
import org.multipaz.pos.payment.PaymentCardDetails

@Composable
fun SettlementSuccessScreen(
    amountCents: Long,
    onNewTransaction: () -> Unit,
    card: PaymentCardDetails? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SettledPanel(
            Modifier.weight(1f).padding(16.dp)
        )
        DetailsPanel(
            Modifier.weight(1f).padding(16.dp),
            amountCents,
            onNewTransaction,
            card,
        )
    }
}

@Composable
private fun SettledPanel(modifier: Modifier = Modifier) {
    val c = PosTheme.colors
    val type = PosTheme.type
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
            Modifier.size(120.dp).clip(CircleShape).background(c.success),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(96.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("SETTLED", style = type.displayLg, color = c.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            "TRANSACTION VERIFIED",
            style = type.labelMd,
            color = c.primary,
            letterSpacing = 4.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DetailsPanel(
    modifier: Modifier = Modifier,
    amountCents: Long,
    onNewTransaction: () -> Unit,
    card: PaymentCardDetails? = null,
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
                Text("AMOUNT SETTLED", style = type.labelSm, color = c.onSurfaceVariant)
                Text(formatCurrency(amountCents), style = type.headlineLg, color = c.primary)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.surfaceVariant))
            if (card != null) {
                CardholderRow("AMOUNT", "AUTHORIZED BY CARD", valueColor = c.success)
                CardholderRow("CARDHOLDER", card.displayName ?: "—")
                card.issuerName?.let { CardholderRow("ISSUER", it) }
                card.maskedAccountReference?.let { CardholderRow("CARD", it) }
                card.expiryDate?.let { CardholderRow("EXPIRES", it) }
            }
        }
        PrimaryAction(
            Icons.Filled.AddShoppingCart,
            "NEW TRANSACTION",
            filled = false,
            onClick = onNewTransaction
        )
    }
}

@Composable
private fun CardholderRow(label: String, value: String, valueColor: Color? = null) {
    val c = PosTheme.colors
    val type = PosTheme.type
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = type.labelSm, color = c.onSurfaceVariant)
        Text(value, style = type.labelMd, color = valueColor ?: c.onSurface)
    }
}

@Composable
private fun PrimaryAction(
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
                if (filled) Modifier else Modifier.border(
                    1.dp,
                    c.primary,
                    RoundedCornerShape(4.dp)
                )
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