package org.multipaz.samples.wallet.cmp

import android.content.Context
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent.inject
import org.multipaz.compose.mdoc.MdocNdefService
import org.multipaz.compose.prompt.PresentmentActivity
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.presentment.PresentmentSource
import org.multipaz.samples.wallet.cmp.util.AppSettingsModel

class NdefService(
    applicationContext: Context,
    sendResponse: (ByteArray) -> Unit,
) : MdocNdefService(
    applicationContext,
    sendResponse,
) {
    private val presentmentSource: PresentmentSource by inject(PresentmentSource::class.java)
    private val settingsModel: AppSettingsModel by inject(AppSettingsModel::class.java)

    override suspend fun getSettings(): Settings {
        // Reset the presentment model with the source's document store and repository
        PresentmentActivity.presentmentModel.reset(
            source = presentmentSource,
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
