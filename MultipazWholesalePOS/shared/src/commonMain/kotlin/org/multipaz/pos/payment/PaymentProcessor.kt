package org.multipaz.pos.payment

import kotlinx.io.bytestring.ByteString
import org.multipaz.cbor.annotation.CborSerializable
import org.multipaz.rpc.annotation.RpcInterface
import org.multipaz.rpc.annotation.RpcMethod
import org.multipaz.verification.PresentmentRecord

/**
 * RPC interface for payment transaction processing.
 *
 * A payment flow consists of two steps:
 * 1. [createTransaction] — the verifier creates a pending transaction and receives a nonce
 *    to be included in the credential presentation.
 * 2. [commitTransaction] — after the holder presents a credential, the verifier submits
 *    the [PresentmentRecord] to finalize the transaction.
 */
@RpcInterface
interface PaymentProcessor {
    /**
     * Creates a new pending payment transaction.
     *
     * @param request details of the payment (payee, amount, currency).
     * @return transaction data including the transaction ID and a nonce that the payment
     *  server uses to request credential presentation.
     */
    @RpcMethod(endpoint = "create")
    suspend fun createTransaction(request: PaymentTransactionRequest): PaymentTransactionData

    /**
     * Commits a previously created transaction by providing a verified credential presentation.
     *
     * @param presentmentRecord self-contained result of the credential presentation.
     * @return transaction confirmation id
     */
    @RpcMethod(endpoint = "commit")
    suspend fun commitTransaction(presentmentRecord: PresentmentRecord): String
}

/**
 * Request to create a payment transaction via [PaymentProcessor.createTransaction].
 *
 * @property payeeAccount identifier of the payee's account.
 * @property amount payment amount.
 * @property currency ISO 4217 currency code (e.g. "USD", "EUR").
 * @property description optional human-readable description of the payment.
 */
@CborSerializable
data class PaymentTransactionRequest(
    val payeeAccount: String,
    val amount: Double,
    val currency: String,
    val description: String?
) {
    companion object
}

/**
 * Data returned when a payment transaction is created via [PaymentProcessor.createTransaction].
 *
 * @property transactionId unique identifier for the pending transaction.
 * @property payeeName display name of the payee, suitable for showing to the payer.
 * @property nonce a nonce to use in the presentment request.
 */
@CborSerializable
data class PaymentTransactionData(
    val transactionId: String,
    val payeeName: String,
    val nonce: ByteString
) {
    companion object
}