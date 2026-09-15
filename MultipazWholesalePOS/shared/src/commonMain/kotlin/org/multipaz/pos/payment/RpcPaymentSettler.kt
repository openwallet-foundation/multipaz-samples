package org.multipaz.pos.payment

import kotlinx.coroutines.withContext
import org.multipaz.pos.Constants
import org.multipaz.pos.Constants.DEFAULT_PAYEE_ACCOUNT
import org.multipaz.pos.Constants.DEFAULT_TERMINAL_URL
import org.multipaz.pos.Platform
import org.multipaz.rpc.client.RpcAuthorizedDeviceClient
import org.multipaz.rpc.handler.RpcAuthClientSession
import org.multipaz.rpc.handler.RpcExceptionMap
import org.multipaz.rpc.transport.HttpTransport
import org.multipaz.verification.Iso18013PresentmentRecord

const val TAG = "RpcPaymentSettler"

class RpcPaymentSettler(
    private val terminalBackendUrl: String = DEFAULT_TERMINAL_URL,
    private val payeeAccount: String = DEFAULT_PAYEE_ACCOUNT
) {
    private var paymentProcessor: PaymentProcessor? = null

    suspend fun init() {
        try {
            RpcAuthorizedDeviceClient.connect(
                exceptionMap = RpcExceptionMap.Builder().build(),
                httpClientEngine = Platform.httpClientEngineFactory,
                url = terminalBackendUrl,
                secureArea = org.multipaz.util.Platform.getSecureArea(),
                storage = org.multipaz.util.Platform.storage
            )
        } catch (e: HttpTransport.ConnectionException) {
            throw HttpTransport.ConnectionException(
                "Could not connect to terminal backend - is the backend running/accessible?",
                e
            )
        }.also {
            paymentProcessor = PaymentProcessorStub(
                endpoint = "pos",
                dispatcher = it.dispatcher,
                notifier = it.notifier
            )
        }
    }

    suspend fun createTransaction(amountCents: Long): PaymentTransactionData =
        withContext(RpcAuthClientSession()) {
            if (paymentProcessor == null) {
                init()
            }
            paymentProcessor?.createTransaction(
                PaymentTransactionRequest(
                    payeeAccount = payeeAccount,
                    amount = amountCents / 100.0,
                    currency = Constants.TERMINAL_CURRENCY,
                    description = null
                )
            ) ?: throw IllegalStateException("Payment processor failed to initialize - pls check the connection and try again")
        }

    suspend fun commit(record: Iso18013PresentmentRecord): String =
        withContext(RpcAuthClientSession()) {
            if (paymentProcessor == null) {
                init()
            }
            paymentProcessor?.commitTransaction(record)
                ?: throw IllegalStateException("Payment processor failed to initialize - pls check the connection and try again")
        }
}