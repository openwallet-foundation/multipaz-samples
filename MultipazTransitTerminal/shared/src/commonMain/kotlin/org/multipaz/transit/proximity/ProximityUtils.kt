package org.multipaz.transit.proximity

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.multipaz.cbor.Cbor
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodNfcV2
import org.multipaz.mdoc.nfc.MdocHandoverType
import org.multipaz.mdoc.nfc.ScanMdocReaderResult
import org.multipaz.mdoc.transport.MdocTransportClosedException
import org.multipaz.mdoc.transport.MdocTransportException
import org.multipaz.nfc.NfcTagLostException
import org.multipaz.util.Logger

private fun Throwable.isTagLostOrTransportClosed(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is NfcTagLostException ||
            current is MdocTransportClosedException ||
            current is MdocTransportException
        ) {
            return true
        }
        current = current.cause
    }
    return false
}

suspend fun handleNfcHandover(
    scanResult: ScanMdocReaderResult,
    proximityReaderModel: ProximityReaderModel,
): Boolean {
    if (proximityReaderModel.state.value != ProximityReaderModel.State.IDLE) {
        Logger.i(
            TAG,
            "Ignoring NFC handover, state is already ${proximityReaderModel.state.value}"
        )
        return false
    }
    require(scanResult.type == MdocHandoverType.V2_HANDOVER) {
        "This terminal accepts NFCv2 handover only."
    }
    require(scanResult.transport.connectionMethod is MdocConnectionMethodNfcV2) {
        "This terminal accepts NFCv2 NFC-only transfer only."
    }
    try {
        proximityReaderModel.apply {
            reset()
            setConnectionEndpoint(
                deviceEngagement = Cbor.decode(scanResult.encodedDeviceEngagement.toByteArray()),
                handover = scanResult.handover,
                existingTransport = scanResult.transport,
                nfcHandoverType = scanResult.type,
                durationNfcTapToEngagement = scanResult.processingDuration
            )
        }

        proximityReaderModel.state.first { it == ProximityReaderModel.State.COMPLETED || it == ProximityReaderModel.State.IDLE }
        if (proximityReaderModel.state.value == ProximityReaderModel.State.COMPLETED) {
            // Only a thrown reader flow failure aborts the tap; a session which completed with
            // an unusable response is left for the caller to report.
            val err = (proximityReaderModel.outcome as? ProximityReaderOutcome.Failed)?.error
            err?.let {
                if (err.isTagLostOrTransportClosed()) {
                    Logger.i(
                        TAG,
                        "Tag lost during NFC-only transfer, resetting model for re-tap",
                        err
                    )
                    proximityReaderModel.reset()
                }
                throw err
            }
        }
        return true
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Logger.w(TAG, "Error handling NFC handover endpoint setup", e)
        proximityReaderModel.reset()
        throw e
    }
}
