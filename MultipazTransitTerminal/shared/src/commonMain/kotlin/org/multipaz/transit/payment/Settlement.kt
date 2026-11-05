package org.multipaz.transit.payment

import kotlinx.serialization.Serializable

enum class AgeDiscount(val fareClass: String) {
    None("STANDARD"),
    Child("CHILD"),
    SeniorCitizen("SENIOR");

    companion object {
        fun fromFareClass(fareClass: String): AgeDiscount =
            entries.first { it.fareClass == fareClass }
    }
}

/** The claims read back off a settled presentment, for the receipt. */
data class PresentedClaims(
    val card: PaymentCardDetails? = null,
    val discount: AgeDiscount = AgeDiscount.None,
) {
    companion object {
        val NONE = PresentedClaims()
    }
}
