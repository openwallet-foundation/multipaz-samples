package org.multipaz.getstarted

import org.multipaz.compose.mdoc.MdocNdefService
import org.multipaz.crypto.EcCurve
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.util.Platform.promptModel

class NdefService : MdocNdefService() {

    override suspend fun getSettings(): Settings {
        return Settings(
            sessionEncryptionCurve = EcCurve.P256,
            allowMultipleRequests = false,
            useNegotiatedHandover = true,
            negotiatedHandoverPreferredOrder = listOf(
                "ble:central_client_mode:",
                "ble:peripheral_server_mode:",
            ),
            staticHandoverBleCentralClientModeEnabled = false,
            staticHandoverBlePeripheralServerModeEnabled = false,
            staticHandoverNfcDataTransferEnabled = false,
            transportOptions = MdocTransportOptions(bleUseL2CAP = true),
            promptModel = promptModel,
            presentmentActivityClass = NfcActivity::class.java,
        )
    }
}