package org.multipaz.transit.journey

import kotlinx.coroutines.withContext
import org.multipaz.rpc.client.RpcAuthorizedDeviceClient
import org.multipaz.rpc.handler.RpcAuthClientSession
import org.multipaz.rpc.handler.RpcExceptionMap
import org.multipaz.rpc.handler.RpcNotifier
import org.multipaz.rpc.transport.HttpTransport
import org.multipaz.transit.Constants.DEFAULT_TERMINAL_URL
import org.multipaz.transit.Platform

/**
 * Attested RPC client for the terminal backend's persistent journey table.
 *
 * The authenticated connection is established lazily and retained for this app instance. Expected
 * backend lifecycle errors are surfaced as [org.multipaz.rpc.handler.InvalidRequestException].
 */
class RpcJourneyStore(
    private val terminalBackendUrl: String = DEFAULT_TERMINAL_URL
) {
    private var journeyService: JourneyService? = null

    /** Returns the cached journey RPC stub, creating an attested device connection if needed. */
    private suspend fun service(): JourneyService {
        journeyService?.let { return it }
        val connection = try {
            RpcAuthorizedDeviceClient.connect(
                exceptionMap = RpcExceptionMap.Builder().build(),
                httpClientEngine = Platform.httpClientEngineFactory,
                url = terminalBackendUrl,
                secureArea = org.multipaz.util.Platform.getSecureArea(),
                storage = org.multipaz.util.Platform.storage,
            )
        } catch (e: HttpTransport.ConnectionException) {
            throw HttpTransport.ConnectionException(
                "Could not connect to terminal backend - is the backend running/accessible?",
                e,
            )
        }
        return JourneyServiceStub("journeys", connection.dispatcher, connection.notifier).also {
            journeyService = it
        }
    }

    /** Persists a new open journey for the presented payment instrument. */
    suspend fun checkIn(paymentInstrumentId: String, entryStation: String, fareClass: String): Journey =
        withContext(RpcAuthClientSession()) { service().checkIn(paymentInstrumentId, entryStation, fareClass) }

    /** Persists the selected destination and its fare. */
    suspend fun selectExit(paymentInstrumentId: String, exitStation: String, fareCents: Long): Journey =
        withContext(RpcAuthClientSession()) { service().selectExit(paymentInstrumentId, exitStation, fareCents) }

    /** Advances a stored journey to the payment-pending state. */
    suspend fun beginCheckout(paymentInstrumentId: String): Journey =
        withContext(RpcAuthClientSession()) { service().beginCheckout(paymentInstrumentId) }

    /** Records successful payment settlement for the journey. */
    suspend fun settle(paymentInstrumentId: String): Journey =
        withContext(RpcAuthClientSession()) { service().settle(paymentInstrumentId) }
}
