package org.multipaz.getstarted

import org.multipaz.compose.mdoc.MdocNdefService
import org.multipaz.compose.prompt.PresentmentActivity
import org.multipaz.crypto.EcCurve
import org.multipaz.getstarted.core.AppContainer
import org.multipaz.mdoc.transport.MdocTransportOptions

class NdefService : MdocNdefService() {
    override suspend fun getSettings(): Settings {
        val container = AppContainer.getInstance()
        container.init()

        val source = container.presentmentSource
        PresentmentActivity.presentmentModel.reset(
            documentStore = source.documentStore,
            documentTypeRepository = source.documentTypeRepository,
            preselectedDocuments = emptyList()
        )

        return Settings(
            source = container.presentmentSource,
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
