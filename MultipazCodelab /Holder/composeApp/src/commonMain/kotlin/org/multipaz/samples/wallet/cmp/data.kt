package org.multipaz.samples.wallet.cmp

data class UtopiaMemberInfo(
    val utopia_card_number: String = "12345678901",
    val ssn: String = "1102"
) {
    /**
     * Serialize to JSON-like string format
     */
    fun toJsonString(): String {
        return """{"utopia_card_number":"$utopia_card_number","ssn":"$ssn"}"""
    }
}

