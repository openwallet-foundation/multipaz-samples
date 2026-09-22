package org.multipaz.transit.ui

import androidx.compose.runtime.Immutable
import org.multipaz.transit.payment.AgeDiscount

/**
 * A destination offered on the faregate. Fares are held in cents so the amount handed to the
 * payment settler is the same value the rider saw on the row they tapped.
 */
@Immutable
data class Station(
    val name: String,
    val line: String,
    val zone: String,
    val minutes: Int,
    val platform: String,
    var fareCents: Long,
) {
    /** The supporting line under the station name, e.g. "Line 2 · Zone 1 · 6 min". */
    val detail: String get() = "$line · $zone · $minutes min"

    val fare: String get() = formatCurrency(fareCents)
}

/** The destinations reachable from this faregate. */
public fun getStationList(discount: AgeDiscount): List<Station> {
    val multiplier = when (discount) {
        AgeDiscount.Child -> 0.67 // 33% discount
        AgeDiscount.SeniorCitizen -> 0.5 // 50% discount
        AgeDiscount.None -> 1.0
    }

    return listOf(
        Station("Central Exchange", "Line 2", "Zone 1", 6, "Platform 2", 275),
        Station("Grove Street", "Line 2", "Zone 1", 11, "Platform 2", 275),
        Station("Harbor Quay", "Line 4", "Zone 2", 18, "Platform 1", 340),
        Station("Utopia North", "Line 2", "Zone 2", 24, "Platform 2", 340),
        Station("Airfield Terminal", "Line 7", "Zone 3", 41, "Platform 4", 560),
    ).map {
        it.copy(
            fareCents = (it.fareCents * multiplier).toLong()
        )
    }
}

/** Formats an amount in cents as US currency, e.g. 1234567 -> "$12,345.67". */
fun formatCurrency(cents: Long): String {
    val negative = cents < 0
    val abs = if (negative) -cents else cents
    val dollars = abs / 100
    val rem = (abs % 100).toString().padStart(2, '0')
    val grouped = groupThousands(dollars.toString())
    return (if (negative) "-$" else "$") + "$grouped.$rem"
}

private fun groupThousands(digits: String): String {
    val sb = StringBuilder()
    val n = digits.length
    for (i in digits.indices) {
        if (i > 0 && (n - i) % 3 == 0) sb.append(',')
        sb.append(digits[i])
    }
    return sb.toString()
}
