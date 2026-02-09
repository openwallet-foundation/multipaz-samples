package org.multipaz.getstarted

import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination {

    @Serializable
    data object HomeDestination : Destination
    @Serializable
    data object ProvisioningDestination : Destination

    @Serializable
    data class ShowResponseDestination(
        val vpResponse: String?,
        val deviceResponse: String?,
        val sessionTranscript: String?,
        val nonce: String?,
        val eReaderKey: String?,
        val metadata: String?
    ) : Destination
}
