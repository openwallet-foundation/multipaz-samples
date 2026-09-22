package org.multipaz.transit.payment

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import org.multipaz.claim.MdocClaim
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.documenttype.knowntypes.EUPersonalID
import org.multipaz.documenttype.knowntypes.PhotoID
import org.multipaz.documenttype.knowntypes.addKnownTypes
import org.multipaz.util.Logger
import org.multipaz.utopia.knowntypes.DigitalPaymentCredential
import org.multipaz.utopia.knowntypes.addUtopiaTypes
import org.multipaz.verification.Iso18013PresentmentRecord
import org.multipaz.verification.MdocVerifiedPresentation
import kotlin.time.Clock

private val AGE_DOCTYPES = listOf(
    DrivingLicense.MDL_DOCTYPE,
    PhotoID.PHOTO_ID_DOCTYPE,
    EUPersonalID.EUPID_DOCTYPE,
)

/**
 * Extracts everything the receipt needs from a settled proximity presentment: the customer's
 * [PaymentCardDetails] and the [AgeDiscount] that came back alongside it.
 *
 * The records server is the authority: it already verified the issuer + device signatures and the
 * card-bound amount when [RpcPaymentSettler.commit] succeeded. Here we re-run
 * [Iso18013PresentmentRecord.verify] only to pull the issuer-signed claims back out in a structured
 * form.
 *
 * The [DocumentTypeRepository] must be populated. The request carries four doc requests (mdL /
 * PhotoID / EUPID for age, plus the DPC), and with more than one doc request
 * `DeviceResponse.verify` resolves each document back to its request by looking up the registered
 * transaction types — an empty repository would silently skip that step. Registering
 * [addUtopiaTypes] therefore also re-runs the SCA amount binding locally, which the server has
 * already checked; a disagreement surfaces here as missing receipt details rather than a failed
 * sale. [addKnownTypes] must come first: [addUtopiaTypes] resolves the EUPID document type
 * eagerly.
 *
 * Best-effort: these details are cosmetic and the payment is already committed by the time this
 * runs, so any verification hiccup returns [PresentedClaims.NONE] rather than failing a completed
 * sale.
 */
suspend fun extractPresentedClaims(record: Iso18013PresentmentRecord): PresentedClaims =
    runCatching {
        val verifiedPresentations = record.verify(
            atTime = Clock.System.now(),
            documentTypeRepository = DocumentTypeRepository().apply {
                addKnownTypes()
                addUtopiaTypes()
            },
            zkSystemRepository = null,
        ).filterIsInstance<MdocVerifiedPresentation>()

        PresentedClaims(
            card = verifiedPresentations
                .firstOrNull { it.docType == DigitalPaymentCredential.CARD_DOCTYPE }
                ?.let { paymentCardOf(it) },
            discount = verifiedPresentations
                .filter { it.docType in AGE_DOCTYPES }
                .let { discountOf(it) },
        )
    }.getOrElse { e ->
        Logger.w("PaymentCardClaims", "Could not extract presented claims for receipt", e)
        PresentedClaims.NONE
    }

private fun MdocVerifiedPresentation.claim(name: String): MdocClaim? =
    issuerSignedClaims.firstOrNull { it.dataElementName == name }

private fun paymentCardOf(presentation: MdocVerifiedPresentation): PaymentCardDetails {
    fun text(name: String): String? = presentation.claim(name)
        ?.value
        ?.let { runCatching { it.asTstr }.getOrNull() }

    return PaymentCardDetails(
        issuerName = text("issuer_name"),
        holderName = text("holder_name"),
        maskedAccountReference = text("masked_account_reference"),
        paymentInstrumentId = text("payment_instrument_id"),
        expiryDate = text("expiry_date"),
    )
}

/**
 * Reads the age attestation out of whichever identity credential the wallet chose to satisfy the
 * "mdL or PhotoID or PID" use case with.
 *
 * `mandatory = true` on that use case is a hint to the wallet, not something the terminal can
 * enforce, so a response carrying only the payment card is a real possibility and maps to
 * [AgeDiscount.None]. EUPID has no `age_over_65`, so 65+ stays null there; it does carry
 * `age_in_years`, which settles both thresholds when the boolean is absent.
 */
private fun discountOf(presentations: List<MdocVerifiedPresentation>): AgeDiscount {
    val presentation = AGE_DOCTYPES
        .firstNotNullOfOrNull { type -> presentations.firstOrNull { it.docType == type } }
        ?: return AgeDiscount.None

    fun bool(name: String): Boolean? = presentation.claim(name)
        ?.value
        ?.let {
            runCatching { it.asBoolean }.onFailure {
                Logger.w(
                    "Unexpected claim value for '$name'",
                    it.stackTraceToString()
                )
            }.getOrNull()
        }

    // Check senior first to avoid being masked by age_over_18 = false on contradictory documents
    if (bool("age_over_65") == true)
        return AgeDiscount.SeniorCitizen

    if (bool("age_over_18") == false)
        return AgeDiscount.Child

    val ageInYears = presentation.claim("age_in_years")
        ?.value
        ?.let {
            runCatching { it.asNumber }.onFailure {
                Logger.w(
                    "Unexpected claim value for 'age_in_years'",
                    it.stackTraceToString()
                )
            }.getOrNull()
        }

    val timeZone = TimeZone.currentSystemDefault()
    val now = Clock.System.now()

    val dateOfBirthInstant = presentation.claim("birth_date")
        ?.value
        ?.let {
            runCatching { it.asDateString.atStartOfDayIn(timeZone) }.onFailure {
                Logger.w(
                    "Unexpected claim value for 'birth_date'",
                    it.stackTraceToString()
                )
            }.getOrNull()
        }

    // Prefer age_in_years when available; fall back to birth_date calculation
    return when {
        ageInYears != null -> when {
            ageInYears >= 65 -> AgeDiscount.SeniorCitizen
            ageInYears < 18 -> AgeDiscount.Child
            else -> AgeDiscount.None
        }

        dateOfBirthInstant != null -> when {
            // calculated purely based on calendar date (not based on the birth time zone)
            now < dateOfBirthInstant.plus(
                18,
                DateTimeUnit.YEAR,
                timeZone
            ) -> AgeDiscount.Child

            now >= dateOfBirthInstant.plus(
                65,
                DateTimeUnit.YEAR,
                timeZone
            ) -> AgeDiscount.SeniorCitizen

            else -> AgeDiscount.None
        }

        else -> AgeDiscount.None
    }
}
