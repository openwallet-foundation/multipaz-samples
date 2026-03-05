package org.multipaz.getstarted

import org.multipaz.compose.mdoc.MdocNdefService
import org.multipaz.compose.prompt.PresentmentActivity
import org.multipaz.crypto.EcCurve
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.util.Platform.promptModel

class NdefService : MdocNdefService() {
    override suspend fun getSettings(): Settings {
        val app = App.getInstance()
        app.init()

        val source = app.presentmentSource
        PresentmentActivity.presentmentModel.reset(
            documentStore = source.documentStore,
            documentTypeRepository = source.documentTypeRepository,
            preselectedDocuments = emptyList()
        )

        return Settings(
            source = app.presentmentSource,
            promptModel = PresentmentActivity.promptModel,
            presentmentModel = PresentmentActivity.presentmentModel,
            activityClass = PresentmentActivity::class.java,
            transportOptions = MdocTransportOptions(bleUseL2CAP = true),
            sessionEncryptionCurve = EcCurve.P256,
            useNegotiatedHandover = true,
            negotiatedHandoverPreferredOrder = listOf(
                "ble:central_client_mode:",
                "ble:peripheral_server_mode:",
            ),
            staticHandoverBleCentralClientModeEnabled = false,
            staticHandoverBlePeripheralServerModeEnabled = false,
            staticHandoverNfcDataTransferEnabled = false,
        )
    }
}