package org.multipaz.getstarted.verification

import kotlinx.serialization.Serializable

@Serializable
data class ShowResponseDestination(
    val vpResponse: String?,
    val deviceResponse: String?,
    val sessionTranscript: String?,
    val nonce: String?,
    val eReaderKey: String?,
    val metadata: String?
)
