package org.multipaz.pos

object Constants {
    // For local development with `adb reverse tcp:8110 tcp:8110`, use:
    // const val DEFAULT_TERMINAL_URL = "http://localhost:8110/rpc"
    const val DEFAULT_TERMINAL_URL = "https://utopia.multipaz.org/pos-terminal/rpc"

    const val DEFAULT_PAYEE_ACCOUNT = "20000001"

    const val TERMINAL_PAYEE_NAME = "Utopia Wholesale POS"
    const val TERMINAL_PAYEE_ID = "utopia-wholesale-pos-terminal-01"
    const val TERMINAL_CURRENCY = "USD"
}
