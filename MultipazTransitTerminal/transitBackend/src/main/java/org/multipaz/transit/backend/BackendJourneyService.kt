package org.multipaz.transit.backend

import org.multipaz.cbor.annotation.CborSerializable
import org.multipaz.rpc.annotation.RpcInterface
import org.multipaz.rpc.annotation.RpcMethod

/**
 * Backend-local RPC contract and storage record.
 *
 * Its CBOR field names intentionally match the client [org.multipaz.transit.journey.Journey]
 * contract, allowing the Android/iOS app to decode the response without the backend depending on
 * the multiplatform UI module.
 */
@CborSerializable
data class StoredJourney(
    val entryStation: String,
    val exitStation: String? = null,
    val fareCents: Long? = null,
    val fareClass: String,
    val state: String,
) {
    companion object
}

/** Backend-local counterpart of the shared journey RPC contract. */
@RpcInterface
interface BackendJourneyService {
    /** Creates an open journey or returns an existing resumable journey for the presentation. */
    @RpcMethod(endpoint = "checkIn")
    suspend fun checkIn(
        paymentInstrumentId: String,
        entryStation: String,
        fareClass: String
    ): StoredJourney

    /** Stores the selected destination and calculated fare. */
    @RpcMethod(endpoint = "selectExit")
    suspend fun selectExit(
        paymentInstrumentId: String,
        exitStation: String,
        fareCents: Long
    ): StoredJourney

    /** Records that exit payment processing has started. */
    @RpcMethod(endpoint = "beginCheckout")
    suspend fun beginCheckout(paymentInstrumentId: String): StoredJourney

    /** Records that exit payment processing completed. */
    @RpcMethod(endpoint = "settle")
    suspend fun settle(paymentInstrumentId: String): StoredJourney
}