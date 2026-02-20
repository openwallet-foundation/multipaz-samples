package org.multipaz.samples.wallet.cmp

import org.multipaz.compose.mdoc.MdocNdefService
import org.multipaz.compose.prompt.PresentmentActivity
import org.multipaz.crypto.EcCurve
import org.multipaz.mdoc.transport.MdocTransportOptions

class NdefService: MdocNdefService() {

    override suspend fun getSettings(): Settings {
        // TODO: optimize initialization of App so we can just get settingsModel and presentmentSource() out
        val app = App.getInstance()
        app.init()

        val source = app.presentmentSource
        PresentmentActivity.presentmentModel.reset(
            documentStore = source.documentStore,
            documentTypeRepository = source.documentTypeRepository,
            // TODO: if user is currently selecting a document, pass it here
            preselectedDocuments = emptyList()
        )

        return Settings(
            source = source,
            promptModel = PresentmentActivity.promptModel,
            presentmentModel = PresentmentActivity.presentmentModel,
            activityClass = PresentmentActivity::class.java,
            sessionEncryptionCurve = EcCurve.P256,
            useNegotiatedHandover = true,
            negotiatedHandoverPreferredOrder = listOf(
                "ble:central_client_mode:",
                "ble:peripheral_server_mode:",
            ),
            staticHandoverBleCentralClientModeEnabled = false,
            staticHandoverBlePeripheralServerModeEnabled = false,
            staticHandoverNfcDataTransferEnabled = false,
            transportOptions = MdocTransportOptions(bleUseL2CAPInEngagement = true),
        )
    }
}