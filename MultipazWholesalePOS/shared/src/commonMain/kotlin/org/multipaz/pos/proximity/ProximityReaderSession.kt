package org.multipaz.pos.proximity

import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.buildCborArray
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.mdoc.engagement.DeviceEngagement
import org.multipaz.mdoc.nfc.MdocHandoverType
import org.multipaz.mdoc.transport.MdocTransport
import org.multipaz.mdoc.transport.MdocTransportOptions
import kotlin.time.Duration

/**
 * The parameters of a single reader session, fixed once the connection endpoint is known.
 *
 * Sessions are made with [create], which generates the ephemeral reader key and the session
 * transcript binding the exchange to this engagement, so [ProximityReaderModel] can hold one
 * nullable session instead of a nullable field per parameter.
 *
 * @property handover the handover as defined in ISO/IEC 18013-5, or [org.multipaz.cbor.Simple.NULL]
 *   for QR engagement.
 * @property existingTransport the transport the engagement was received on, if it can be reused, or
 *   `null` to create one from the connection methods in the engagement.
 * @property nfcHandoverType the kind of NFC handover which produced the engagement, or `null` if it
 *   didn't come from NFC.
 * @property durationNfcTapToEngagement how long the NFC tap took, for the session's timings.
 * @property transportOptions the options to create a transport with, when one isn't being reused.
 * @property eReaderKey the ephemeral reader key, on the same curve as the holder's `EDeviceKey`.
 * @property sessionTranscript the `SessionTranscript` as defined in ISO/IEC 18013-5, which both
 *   sides derive their session keys from and which the holder signs over.
 */
internal class ProximityReaderSession private constructor(
    val deviceEngagement: DeviceEngagement,
    val handover: DataItem,
    val existingTransport: MdocTransport?,
    val nfcHandoverType: MdocHandoverType?,
    val durationNfcTapToEngagement: Duration?,
    val transportOptions: MdocTransportOptions,
    val eReaderKey: EcPrivateKey,
    val sessionTranscript: DataItem,
) {
    companion object {
        /**
         * Creates a session for the holder described by [encodedDeviceEngagement], which was either
         * received over NFC or scanned from a QR code.
         */
        suspend fun create(
            encodedDeviceEngagement: DataItem,
            handover: DataItem,
            existingTransport: MdocTransport?,
            nfcHandoverType: MdocHandoverType?,
            durationNfcTapToEngagement: Duration?,
            transportOptions: MdocTransportOptions,
        ): ProximityReaderSession {
            val deviceEngagement = DeviceEngagement.fromDataItem(encodedDeviceEngagement)
            val eReaderKey = Crypto.createEcPrivateKey(deviceEngagement.eDeviceKey.curve)
            return ProximityReaderSession(
                deviceEngagement = deviceEngagement,
                handover = handover,
                existingTransport = existingTransport,
                nfcHandoverType = nfcHandoverType,
                durationNfcTapToEngagement = durationNfcTapToEngagement,
                transportOptions = transportOptions,
                eReaderKey = eReaderKey,
                sessionTranscript = buildCborArray {
                    add(Tagged(24, Bstr(Cbor.encode(encodedDeviceEngagement))))
                    add(Tagged(24, Bstr(Cbor.encode(eReaderKey.publicKey.toCoseKey().toDataItem()))))
                    add(handover)
                },
            )
        }
    }
}
