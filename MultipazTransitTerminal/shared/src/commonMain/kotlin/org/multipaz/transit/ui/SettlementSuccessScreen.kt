package org.multipaz.transit.ui

import androidx.compose.runtime.Composable
import org.multipaz.transit.payment.AgeDiscount
import org.multipaz.transit.payment.PaymentCardDetails

/**
 * The gate opens. The rider gets the verdict first and the receipt second: what was proven,
 * which card paid, and how much came off it.
 */
@Composable
fun SettlementSuccessScreen(
    amountCents: Long,
    destination: Station?,
    onNewTransaction: () -> Unit,
    card: PaymentCardDetails? = null,
    age: AgeDiscount = AgeDiscount.None,
) {
    val c = TransitTheme.colors
    val details = buildList {
        // The fare settles on the payment credential alone, so the age row reports what the
        // wallet actually returned rather than asserting the threshold the gate asked for.
        add(
            when {
                age == AgeDiscount.Child ->
                    OutcomeDetail("Child Ticket", "Verified", c.successInk)
                age == AgeDiscount.SeniorCitizen ->
                    OutcomeDetail("Senior Citizen Ticket", "Verified", c.successInk)
                else -> OutcomeDetail("Normal Ticket", "Verified", c.ink)
            }
        )
        add(
            OutcomeDetail(
                label = card?.maskedAccountReference?.let { "Card $it" } ?: "Payment card",
                value = formatCurrency(amountCents),
            )
        )
        card?.displayName?.let { add(OutcomeDetail("Rider", it)) }
        card?.issuerName?.let { add(OutcomeDetail("Issuer", it)) }
    }
    OutcomeScreen(
        accent = c.success,
        mark = OutcomeMark.CHECK,
        headline = "Go ahead",
        subline = destination
            ?.let { "Faregate opening · ${it.platform}" }
            ?: "Faregate opening",
        details = details,
        actionLabel = "Next rider",
        onAction = onNewTransaction,
    )
}
