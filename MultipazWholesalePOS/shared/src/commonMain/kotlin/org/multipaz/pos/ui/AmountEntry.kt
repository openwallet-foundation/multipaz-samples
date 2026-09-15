package org.multipaz.pos.ui

import androidx.compose.runtime.Immutable

/**
 * Pure, immutable model of what has been typed on the amount numpad.
 *
 * Every mutation returns a new instance, so the entry logic is fully unit-testable
 * without a Compose runtime. [raw] holds the literal keystrokes, e.g. "1450.7";
 * an empty string represents 0.00.
 */
@Immutable
data class AmountEntry(val raw: String = "") {

    /** The entered amount in integer cents, e.g. "14.5" -> 1450. */
    val cents: Long
        get() {
            if (raw.isEmpty()) return 0
            val dot = raw.indexOf('.')
            val intPart = (if (dot >= 0) raw.substring(0, dot) else raw).ifEmpty { "0" }
            val frac = (if (dot >= 0) raw.substring(dot + 1) else "").padEnd(2, '0').take(2)
            return intPart.toLong() * 100 + frac.toLong()
        }

    /** The amount formatted for display, e.g. "1,450.70". */
    val display: String get() = formatAmountEntry(raw)

    /** Appends a single digit ('0'..'9'), enforcing max 2 decimals and a safety cap. */
    fun append(digit: Char): AmountEntry {
        require(digit in '0'..'9') { "Not a digit: $digit" }
        val dot = raw.indexOf('.')
        if (dot >= 0 && raw.length - dot > 2) return this           // already 2 decimals
        if (raw.replace(".", "").length >= MAX_DIGITS) return this  // safety cap
        return AmountEntry(raw + digit)
    }

    /** Appends the decimal point; a no-op if one is already present. */
    fun appendDot(): AmountEntry =
        if (raw.contains('.')) this
        else AmountEntry(if (raw.isEmpty()) "0." else "$raw.")

    /** Clears back to empty (0.00). */
    fun cleared(): AmountEntry = AmountEntry()

    companion object {
        /** Maximum number of entered digits (excluding the decimal point). */
        const val MAX_DIGITS = 9
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

/** Formats a raw dollars-and-cents string typed on the numpad for display, e.g. "1450.7" -> "1,450.70". */
fun formatAmountEntry(raw: String): String {
    if (raw.isEmpty()) return "0.00"
    val dot = raw.indexOf('.')
    val intPart = if (dot >= 0) raw.substring(0, dot) else raw
    val fracPart = if (dot >= 0) raw.substring(dot + 1) else ""
    val intGrouped = groupThousands(intPart.ifEmpty { "0" }.trimStart('0').ifEmpty { "0" })
    return if (dot >= 0) "$intGrouped.${fracPart.padEnd(2, '0').take(2)}" else "$intGrouped.00"
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
