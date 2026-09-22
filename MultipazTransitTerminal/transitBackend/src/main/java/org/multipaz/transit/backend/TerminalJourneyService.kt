package org.multipaz.transit.backend

import kotlinx.io.bytestring.ByteString
import org.multipaz.cbor.annotation.CborSerializable
import org.multipaz.rpc.annotation.RpcState
import org.multipaz.rpc.backend.BackendEnvironment
import org.multipaz.rpc.backend.RpcAuthBackendDelegate
import org.multipaz.rpc.handler.InvalidRequestException
import org.multipaz.rpc.handler.RpcAuthInspector
import org.multipaz.storage.KeyExistsStorageException
import org.multipaz.storage.Storage
import org.multipaz.storage.StorageTable
import org.multipaz.storage.StorageTableSpec
import org.multipaz.util.toBase64Url
import java.security.MessageDigest
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/** Multipaz table definition. Expiration removes abandoned journeys without a separate cleanup job. */
private val JourneyTableSpec = StorageTableSpec(
    name = "TransitJourneys",
    supportPartitions = false,
    supportExpiration = true,
    schemaVersion = 1,
)

/** Maximum time a rider may retain an uncompleted journey. */
private val OPEN_JOURNEY_LIFETIME = 4.hours

/** Retention period for a settled journey, allowing a receipt to be recovered briefly. */
private val SETTLED_JOURNEY_LIFETIME = 24.hours.times(7)

/** States persisted in [StoredJourney.state]. */
enum class JourneyStates {
    OPEN,
    READY_FOR_CHECKOUT,
    PAYMENT_PENDING,
    SETTLED,
}

/**
 * Persistent journey mapping keyed by a one-way digest of the payment instrument id.
 *
 * Each method is guarded by device attestation. Expected domain failures use
 * [InvalidRequestException], which Multipaz serializes back to the app
 */
@RpcState(endpoint = "journeys", creatable = true)
@CborSerializable
class TerminalJourneyService : BackendJourneyService, RpcAuthInspector by RpcAuthBackendDelegate {

    /** Resolves the backend-owned table; the server environment supplies JDBC-backed storage. */
    private suspend fun storageTable(): StorageTable =
        BackendEnvironment.getInterface(Storage::class)!!
            .getTable(JourneyTableSpec)

    /** Derives the storage key without persisting the raw payment instrument identifier. */
    private fun parseReaderKey(paymentInstrumentId: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(paymentInstrumentId.encodeToByteArray())
            .toBase64Url()

    override suspend fun checkIn(
        paymentInstrumentId: String,
        entryStation: String,
        fareClass: String,
    ): StoredJourney {
        if (paymentInstrumentId.isBlank()) {
            throw InvalidRequestException("Payment instrument id is required")
        }

        val journeys = storageTable()
        val key = parseReaderKey(paymentInstrumentId)
        val existingJourney = journeys.get(key)?.let { StoredJourney.fromCbor(it.toByteArray()) }

        if (existingJourney != null) {
            return when (existingJourney.state) {
                JourneyStates.OPEN.name,
                JourneyStates.READY_FOR_CHECKOUT.name -> existingJourney

                JourneyStates.PAYMENT_PENDING.name -> throw InvalidRequestException(
                    "Journey payment is already in progress; complete or recover that payment first"
                )

                JourneyStates.SETTLED.name -> StoredJourney(
                    entryStation = entryStation,
                    fareClass = fareClass,
                    state = JourneyStates.OPEN.name,
                ).also { journey ->
                    journeys.update(
                        key = key,
                        data = ByteString(journey.toCbor()),
                        expiration = Clock.System.now() + OPEN_JOURNEY_LIFETIME,
                    )
                }

                else -> throw InvalidRequestException(
                    "Journey has an unsupported state: ${existingJourney.state}"
                )
            }
        }

        val journey = StoredJourney(
            entryStation = entryStation,
            fareClass = fareClass,
            state = JourneyStates.OPEN.name,
        )

        try {
            journeys.insert(
                key = key,
                data = ByteString(journey.toCbor()),
                expiration = Clock.System.now() + OPEN_JOURNEY_LIFETIME,
            )
        } catch (e: KeyExistsStorageException) {
            // A concurrent gate created it after the read above. Return its current record,
            // giving the caller the same resume behavior as the normal existing-journey path.
            return journeys.get(key)?.let { StoredJourney.fromCbor(it.toByteArray()) }
                ?: throw InvalidRequestException(e.message ?: "Could not create or resume journey")
        }
        return journey
    }

    override suspend fun selectExit(
        paymentInstrumentId: String,
        exitStation: String,
        fareCents: Long,
    ): StoredJourney = updateJourney(
        paymentInstrumentId,
        JourneyStates.OPEN.name,
    ) { journey ->
        if (fareCents < 0) {
            throw InvalidRequestException("Fare cannot be negative")
        }
        journey.copy(
            exitStation = exitStation,
            fareCents = fareCents,
            state = JourneyStates.READY_FOR_CHECKOUT.name,
        )
    }

    override suspend fun beginCheckout(paymentInstrumentId: String): StoredJourney =
        updateJourney(
            paymentInstrumentId,
            JourneyStates.READY_FOR_CHECKOUT.name
        ) { journey ->
            journey.copy(
                state = JourneyStates.PAYMENT_PENDING.name
            )
        }

    override suspend fun settle(paymentInstrumentId: String): StoredJourney =
        updateJourney(
            paymentInstrumentId,
            JourneyStates.PAYMENT_PENDING.name,
            SETTLED_JOURNEY_LIFETIME
        ) { journey ->
            journey.copy(state = JourneyStates.SETTLED.name)
        }

    /**
     * Loads a journey, verifies its expected lifecycle state, transforms it, and refreshes its
     * retention deadline. Invalid state transitions are returned to the client as RPC errors.
     */
    private suspend fun updateJourney(
        paymentInstrumentId: String,
        expectedState: String,
        expiration: kotlin.time.Duration = OPEN_JOURNEY_LIFETIME,
        transform: (StoredJourney) -> StoredJourney,
    ): StoredJourney {
        val key = parseReaderKey(paymentInstrumentId)
        val journeys = storageTable()
        val current = journeys.get(key)?.let { StoredJourney.fromCbor(it.toByteArray()) }
            ?: throw InvalidRequestException("No active journey for this payment instrument")
        if (current.state != expectedState) {
            throw InvalidRequestException(
                "Journey is ${current.state}; expected $expectedState"
            )
        }
        return transform(current).also { updated ->
            journeys.update(
                key,
                ByteString(updated.toCbor()),
                expiration = Clock.System.now() + expiration
            )
        }
    }

    companion object
}
