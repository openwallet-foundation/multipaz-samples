package org.multipaz.transit.journey

import org.multipaz.cbor.annotation.CborSerializable
import org.multipaz.rpc.annotation.RpcInterface
import org.multipaz.rpc.annotation.RpcMethod

/**
 * Client-side wire representation of a journey returned by the terminal backend.
 *
 * The payment instrument identifier is intentionally omitted: it is only used as the backend
 * lookup key and must not be retained in the journey record.
 */
@CborSerializable
data class Journey(
    /** Station where the rider checked in. */
    val entryStation: String,
    /** Selected destination, present after the backend moves the journey beyond `OPEN`. */
    val exitStation: String? = null,
    /** Fare in minor currency units, once a destination has been selected. */
    val fareCents: Long? = null,
    /** Fare entitlement snapshot, for example `STANDARD`, `CHILD`, or `SENIOR`. */
    val fareClass: String,
    /** Current backend lifecycle state, for example `OPEN` or `SETTLED`. */
    val state: String,
) {
    companion object
}

/** RPC contract used by the terminal application to persist and advance a journey. */
@RpcInterface
interface JourneyService {
    /** Creates an `OPEN` journey or returns an existing resumable journey for the instrument. */
    @RpcMethod(endpoint = "checkIn")
    suspend fun checkIn(paymentInstrumentId: String, entryStation: String, fareClass: String): Journey

    /** Stores the selected exit station and fare, then advances the journey to checkout-ready. */
    @RpcMethod(endpoint = "selectExit")
    suspend fun selectExit(paymentInstrumentId: String, exitStation: String, fareCents: Long): Journey

    /** Marks a checkout-ready journey as `PAYMENT_PENDING`. */
    @RpcMethod(endpoint = "beginCheckout")
    suspend fun beginCheckout(paymentInstrumentId: String): Journey

    /** Marks a payment-pending journey as `SETTLED`. */
    @RpcMethod(endpoint = "settle")
    suspend fun settle(paymentInstrumentId: String): Journey
}
