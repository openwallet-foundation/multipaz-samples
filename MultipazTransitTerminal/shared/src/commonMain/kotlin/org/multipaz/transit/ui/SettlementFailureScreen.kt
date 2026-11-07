package org.multipaz.transit.ui

import androidx.compose.runtime.Composable
import org.multipaz.transit.payment.SettlementResult

/**
 * The gate stays shut. The reason the reader gave is shown verbatim, since it is the only thing
 * that tells the rider (or the station attendant) what to do differently on the next tap.
 */
@Composable
fun SettlementFailureScreen(
    failure: SettlementResult.Declined,
    amountCents: Long,
    destination: Station?,
    onNewTransaction: () -> Unit,
) {
    val c = TransitTheme.colors
    OutcomeScreen(
        accent = c.error,
        mark = OutcomeMark.CROSS,
        headline = "Gate closed",
        subline = failure.reason,
        details = buildList {
            destination?.let { add(OutcomeDetail("To ${it.name}", formatCurrency(amountCents))) }
            add(OutcomeDetail("Card", "Not charged", c.ink.copy(alpha = 0.6f)))
        },
        actionLabel = "Try again",
        onAction = onNewTransaction,
    )
}
