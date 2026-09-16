package org.multipaz.pos.payment

import kotlinx.serialization.Serializable

@Serializable
enum class ProximityScanMode {
    QR,
    NFC
}


enum class PaymentMethod {
    NFC,
    QR_CODE
}

/**
 * Details read from a customer's Utopia Wholesale Digital Payment Credential (DPC)
 *
 * An instance only exists once the mdoc's issuer and device signatures have validated
 */
data class PaymentCardDetails(
    val issuerName: String? = null,
    val holderName: String? = null,
    val maskedAccountReference: String? = null,
    val paymentInstrumentId: String? = null,
    val expiryDate: String? = null,
) {
    val displayName: String?
        get() = holderName?.ifBlank { null }
}

/** Outcome of a settlement attempt. */
sealed interface SettlementResult {
    data class Approved(
        val transactionId: String,
        val amountCents: Long,
        val method: PaymentMethod,
        val timestampEpochMillis: Long,
        /** Payment card read from the presented DPC. */
        val card: PaymentCardDetails? = null,
    ) : SettlementResult

    data class Declined(val reason: String) : SettlementResult
}
