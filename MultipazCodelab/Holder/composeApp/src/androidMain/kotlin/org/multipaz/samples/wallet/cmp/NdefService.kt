package org.multipaz.samples.wallet.cmp

import org.koin.android.ext.android.inject
import org.multipaz.compose.mdoc.MdocNdefService
import org.multipaz.compose.prompt.PresentmentActivity
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.samples.wallet.cmp.util.AppSettingsModel

class NdefService : MdocNdefService() {
    private val presentmentSource: PresentmentSource by inject()
    private val settingsModel: AppSettingsModel by inject()

    override suspend fun getSettings(): Settings {
        // Reset the presentment model with the source's document store and repository
        PresentmentActivity.presentmentModel.reset(
            documentStore = presentmentSource.documentStore,
            documentTypeRepository = presentmentSource.documentTypeRepository,
            preselectedDocuments = emptyList(),
        )

        return Settings(
            source = presentmentSource,
            promptModel = PresentmentActivity.promptModel,
            presentmentModel = PresentmentActivity.presentmentModel,
            activityClass = PresentmentActivity::class.java,
            sessionEncryptionCurve = settingsModel.presentmentSessionEncryptionCurve.value,
            useNegotiatedHandover = settingsModel.presentmentUseNegotiatedHandover.value,
            negotiatedHandoverPreferredOrder = settingsModel.presentmentNegotiatedHandoverPreferredOrder.value,
            staticHandoverBleCentralClientModeEnabled = settingsModel.presentmentBleCentralClientModeEnabled.value,
            staticHandoverBlePeripheralServerModeEnabled =
                settingsModel.presentmentBlePeripheralServerModeEnabled.value,
            staticHandoverNfcDataTransferEnabled = settingsModel.presentmentNfcDataTransferEnabled.value,
            transportOptions =
                MdocTransportOptions(
                    bleUseL2CAP = settingsModel.presentmentBleL2CapEnabled.value,
                    bleUseL2CAPInEngagement = settingsModel.presentmentBleL2CapInEngagementEnabled.value,
                ),
        )
    }
}
