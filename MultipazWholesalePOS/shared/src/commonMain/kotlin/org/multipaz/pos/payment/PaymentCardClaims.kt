package org.multipaz.pos.payment

import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.util.Logger
import org.multipaz.utopia.knowntypes.DigitalPaymentCredential
import org.multipaz.verification.Iso18013PresentmentRecord
import org.multipaz.verification.MdocVerifiedPresentation
import kotlin.time.Clock

/**
 * Extracts the customer's [PaymentCardDetails] from a settled proximity presentment, for display on
 * the receipt.
 *
 * The records server is the authority: it already verified the issuer + device signatures and the
 * card-bound amount when [RpcPaymentSettler.commit] succeeded. Here we re-run
 * [Iso18013PresentmentRecord.verify] only to pull the issuer-signed claims (holder, masked account,
 * expiry, etc.) back out in a structured form. An empty [DocumentTypeRepository] is deliberate: the
 * issuer-signed claims are read straight from the response namespaces regardless of registered
 * types, and leaving the SCA transaction type unregistered avoids re-enforcing the amount binding
 * locally (the server already checked it).
 *
 * Best-effort: card details are cosmetic and the payment is already committed by the time this runs,
 * so any verification hiccup returns null rather than failing a completed sale.
 */
suspend fun extractPaymentCard(record: Iso18013PresentmentRecord): PaymentCardDetails? = runCatching {
    val presentation = record.verify(
        atTime = Clock.System.now(),
        documentTypeRepository = DocumentTypeRepository(),
        zkSystemRepository = null,
    ).filterIsInstance<MdocVerifiedPresentation>()
        .firstOrNull { it.docType == DigitalPaymentCredential.CARD_DOCTYPE }
        ?: return@runCatching null

    fun claim(name: String): String? = presentation.issuerSignedClaims
        .firstOrNull { it.dataElementName == name }
        ?.value
        ?.let { runCatching { it.asTstr }.getOrNull() }

    PaymentCardDetails(
        issuerName = claim("issuer_name"),
        holderName = claim("holder_name"),
        maskedAccountReference = claim("masked_account_reference"),
        paymentInstrumentId = claim("payment_instrument_id"),
        expiryDate = claim("expiry_date"),
    )
}.getOrElse { e ->
    Logger.w("PaymentCardClaims", "Could not extract card details for receipt", e)
    null
}
