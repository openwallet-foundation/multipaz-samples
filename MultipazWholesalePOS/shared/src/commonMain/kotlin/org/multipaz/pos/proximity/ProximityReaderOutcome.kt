package org.multipaz.pos.proximity

import org.multipaz.cbor.DataItem
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.mdoc.request.DeviceRequest
import org.multipaz.mdoc.response.DeviceResponse

/**
 * The outcome of a reader session, available from [ProximityReaderModel.outcome] once the model
 * reaches [ProximityReaderModel.State.COMPLETED].
 */
sealed interface ProximityReaderOutcome {

    /**
     * The holder returned a [DeviceResponse] with an OK status.
     *
     * [sessionTranscript] and [eReaderKey] are carried along because verifying the response means
     * checking it against what was asked and which session it was asked in.
     */
    data class Success(
        val deviceRequest: DeviceRequest,
        val deviceResponse: DeviceResponse,
        val sessionTranscript: DataItem,
        val eReaderKey: EcPrivateKey,
    ) : ProximityReaderOutcome

    /**
     * A session which didn't yield a usable response.
     *
     * [error] describes the failure and is what should be surfaced to the caller.
     */
    sealed interface Failure : ProximityReaderOutcome {
        val error: Throwable
    }

    /**
     * The reader flow threw before a response was obtained, for example because the transport went
     * away mid-session.
     */
    data class Failed(override val error: Throwable) : Failure

    /**
     * The session ended without the holder sending a DeviceResponse message.
     */
    data object NoResponse : Failure {
        override val error: Throwable
            get() = IllegalStateException("No DeviceResponse message")
    }

    /**
     * The holder sent a DeviceResponse carrying a non-zero status.
     */
    data class InvalidStatus(val status: Int) : Failure {
        override val error: Throwable
            get() = IllegalStateException("DeviceResponse has non-zero status $status")
    }
}
