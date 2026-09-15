package org.multipaz.pos.proximity

import org.multipaz.cbor.Cbor
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethod
import org.multipaz.mdoc.nfc.MdocHandoverType
import org.multipaz.mdoc.request.DeviceRequest
import org.multipaz.mdoc.response.DeviceResponse
import org.multipaz.mdoc.role.MdocRole
import org.multipaz.mdoc.sessionencryption.SessionEncryption
import org.multipaz.mdoc.transport.MdocTransport
import org.multipaz.mdoc.transport.MdocTransportFactory
import org.multipaz.util.Constants
import org.multipaz.util.Logger

/**
 * Runs the ISO/IEC 18013-5 reader exchange for [session]: opens a transport to the holder, sends
 * [deviceRequest] over an encrypted session, and waits for the response.
 *
 * The transport is closed before returning, whether or not the exchange succeeded. Throwing means
 * the exchange itself failed; returning means the session ended, which is not the same as it having
 * produced a usable response - see the returned [ProximityReaderOutcome].
 */
internal suspend fun runReaderFlow(
    session: ProximityReaderSession,
    deviceRequest: DeviceRequest,
): ProximityReaderOutcome {
    val transport = session.existingTransport ?: session.createTransport()

    Logger.dCbor(TAG, "handover", Cbor.encode(session.handover))

    val sessionEncryption = SessionEncryption(
        role = MdocRole.MDOC_READER,
        eSelfKey = session.eReaderKey,
        remotePublicKey = session.deviceEngagement.eDeviceKey,
        encodedSessionTranscript = Cbor.encode(session.sessionTranscript),
        insertSequenceNumbers = session.nfcHandoverType == MdocHandoverType.V2_HANDOVER
    )

    try {
        transport.open(session.deviceEngagement.eDeviceKey)
        Logger.dCbor(TAG, "DeviceRequest", deviceRequest.toDataItem())
        transport.sendMessage(
            sessionEncryption.encryptMessage(
                messagePlaintext = Cbor.encode(deviceRequest.toDataItem()),
                statusCode = null
            )
        )

        val deviceResponse = transport.receiveResponse(sessionEncryption)
            ?: return ProximityReaderOutcome.NoResponse
        if (deviceResponse.status != 0) {
            return ProximityReaderOutcome.InvalidStatus(deviceResponse.status)
        }
        return ProximityReaderOutcome.Success(
            deviceRequest = deviceRequest,
            deviceResponse = deviceResponse,
            sessionTranscript = session.sessionTranscript,
            eReaderKey = session.eReaderKey,
        )
    } finally {
        transport.close()
    }
}

/**
 * Creates a transport for one of the connection methods the holder offered in its engagement.
 */
private fun ProximityReaderSession.createTransport(): MdocTransport {
    val connectionMethods = MdocConnectionMethod.disambiguate(
        deviceEngagement.connectionMethods,
        MdocRole.MDOC_READER
    )
    return MdocTransportFactory.Default.createTransport(
        connectionMethod = connectionMethods[0], // TODO: maybe selectConnectionMethod(connectionMethods)
        role = MdocRole.MDOC_READER,
        options = transportOptions
    )
}

/**
 * Waits for the holder's response and, unless the holder said it is hanging up, tells it the session
 * is over - auto-close is enabled, so the reader terminates the session itself.
 *
 * Returns `null` if the session ended without the holder sending a DeviceResponse.
 */
private suspend fun MdocTransport.receiveResponse(
    sessionEncryption: SessionEncryption
): DeviceResponse? {
    val sessionData = waitForMessage()
    if (sessionData.isEmpty()) {
        Logger.i(TAG, "Holder closed the connection without sending a DeviceResponse")
        return null
    }

    val (message, status) = sessionEncryption.decryptMessage(sessionData)
    Logger.i(TAG, "Holder sent ${message?.size} bytes status $status")
    if (status == Constants.SESSION_DATA_STATUS_SESSION_TERMINATION) {
        Logger.i(
            TAG, "Holder indicated they closed the connection. " +
                    "Closing and ending reader loop"
        )
    } else {
        Logger.i(
            TAG, "Holder did not indicate they are closing the connection. " +
                    "Auto-close is enabled, so sending termination message, closing, and " +
                    "ending reader loop"
        )
        sendMessage(SessionEncryption.encodeStatus(Constants.SESSION_DATA_STATUS_SESSION_TERMINATION))
    }
    return message?.let { DeviceResponse.fromDataItem(Cbor.decode(it)) }
}
