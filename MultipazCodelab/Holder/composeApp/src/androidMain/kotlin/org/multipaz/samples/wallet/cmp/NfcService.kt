package org.multipaz.samples.wallet.cmp

import org.koin.android.ext.android.inject
import org.multipaz.compose.mdoc.MdocNdefService
import org.multipaz.compose.prompt.PresentmentActivity
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.presentment.model.PresentmentSource

class NfcService : MdocNdefService() {
    private val presentmentSource: PresentmentSource by inject()

    override suspend fun getSettings(): Settings {
        // Reset the presentment model with the source's document store and repository
        PresentmentActivity.presentmentModel.reset(
            documentStore = presentmentSource.documentStore,
            documentTypeRepository = presentmentSource.documentTypeRepository,
            preselectedDocuments = emptyList()
        )

        return Settings(
            source = presentmentSource,
            promptModel = PresentmentActivity.promptModel,
            presentmentModel = PresentmentActivity.presentmentModel,
            activityClass = PresentmentActivity::class.java,
            sessionEncryptionCurve = org.multipaz.crypto.EcCurve.P256,
            useNegotiatedHandover = true,
            negotiatedHandoverPreferredOrder = listOf(
                "ble:central_client_mode:",
                "ble:peripheral_server_mode:",
                "nfc:"
            ),
            staticHandoverBleCentralClientModeEnabled = false,
            staticHandoverBlePeripheralServerModeEnabled = true,
            staticHandoverNfcDataTransferEnabled = false,
            transportOptions = MdocTransportOptions(
                bleUseL2CAP = false,
                bleUseL2CAPInEngagement = true
            )
        )
    }
}
