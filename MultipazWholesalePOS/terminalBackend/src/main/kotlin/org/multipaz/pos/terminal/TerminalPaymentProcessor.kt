package org.multipaz.pos.terminal

import kotlinx.coroutines.withContext
import org.multipaz.cbor.annotation.CborSerializable
import org.multipaz.rpc.annotation.RpcState
import org.multipaz.rpc.backend.BackendEnvironment
import org.multipaz.rpc.backend.Configuration
import org.multipaz.rpc.backend.RpcAuthBackendDelegate
import org.multipaz.rpc.client.RpcAuthorizedServerClient
import org.multipaz.rpc.handler.RpcAuthClientSession
import org.multipaz.rpc.handler.RpcAuthInspector
import org.multipaz.rpc.handler.RpcExceptionMap
import org.multipaz.rpc.handler.RpcNotifier
import org.multipaz.server.common.getBaseUrl
import org.multipaz.server.enrollment.ServerIdentity
import org.multipaz.server.enrollment.getServerIdentity
import org.multipaz.server.payment.PaymentProcessor
import org.multipaz.server.payment.PaymentProcessorStub
import org.multipaz.server.payment.PaymentTransactionData
import org.multipaz.server.payment.PaymentTransactionRequest
import org.multipaz.verification.PresentmentRecord

/**
 * The merchant terminal's backend front door. It implements the same [PaymentProcessor] RPC the
 * records server (SoR) exposes, but guards it with **device attestation** — `RpcAuthInspector by
 * RpcAuthBackendDelegate` resolves to the environment's `RpcAuthInspectorAssertion`, so every call
 * must come from an app instance that registered a valid [org.multipaz.device.DeviceAttestation]
 * matching this server's `client_requirements` (the POS app's signing-cert digest + package).
 *
 * On a valid call it forwards to the SoR's `PaymentProcessor`, signing with the terminal's
 * `PAYMENT_PROCESSOR` key ([getServerIdentity]) — the key that used to live in the app now lives
 * only here. This is the wallet's app→backend→server split: the app proves it's genuine, the backend
 * holds the money-moving key.
 */
@RpcState(
    endpoint = "pos",
    creatable = true
)
@CborSerializable
class TerminalPaymentProcessor: PaymentProcessor, RpcAuthInspector by RpcAuthBackendDelegate {
    override suspend fun createTransaction(
        request: PaymentTransactionRequest
    ): PaymentTransactionData = withRecordsProcessor { it.createTransaction(request) }

    override suspend fun commitTransaction(
        presentmentRecord: PresentmentRecord
    ): String = withRecordsProcessor { it.commitTransaction(presentmentRecord) }

    private suspend fun <T> withRecordsProcessor(block: suspend (PaymentProcessor) -> T): T {
        val configuration = BackendEnvironment.getInterface(Configuration::class)!!
        val recordsUrl = configuration.getValue("records_server_url")
            ?: throw IllegalStateException("'records_server_url' is not configured")
        val dispatcher = RpcAuthorizedServerClient.connect(
            exceptionMap = RpcExceptionMap.Builder().build(),
            rpcEndpointUrl = "$recordsUrl/rpc",
            callingServerUrl = BackendEnvironment.getBaseUrl(),
            signingKey = getServerIdentity(ServerIdentity.PAYMENT_PROCESSOR),
        )
        return withContext(RpcAuthClientSession()) {
            block(PaymentProcessorStub("payment", dispatcher, RpcNotifier.SILENT))
        }
    }

    companion object
}
