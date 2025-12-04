package org.multipaz.samples.wallet.cmp

import org.koin.android.ext.android.inject
import org.multipaz.compose.mdoc.MdocNdefService

import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.prompt.PromptModel
import org.multipaz.samples.wallet.cmp.util.AppSettingsModel
import org.multipaz.util.Platform

class NdefService: MdocNdefService() {
    private lateinit var settingsModel: AppSettingsModel
    private val promptModel: PromptModel by inject()

    override suspend fun getSettings(): Settings {

        settingsModel = AppSettingsModel.create(
            storage = Platform.storage,
            readOnly = true
        )

        return Settings(
            sessionEncryptionCurve = settingsModel.presentmentSessionEncryptionCurve.value,
            allowMultipleRequests = settingsModel.presentmentAllowMultipleRequests.value,
            useNegotiatedHandover = settingsModel.presentmentUseNegotiatedHandover.value,
            negotiatedHandoverPreferredOrder = settingsModel.presentmentNegotiatedHandoverPreferredOrder.value,
            staticHandoverBleCentralClientModeEnabled = settingsModel.presentmentBleCentralClientModeEnabled.value,
            staticHandoverBlePeripheralServerModeEnabled = settingsModel.presentmentBlePeripheralServerModeEnabled.value,
            staticHandoverNfcDataTransferEnabled = settingsModel.presentmentNfcDataTransferEnabled.value,
            transportOptions = MdocTransportOptions(
                bleUseL2CAP = settingsModel.presentmentBleL2CapEnabled.value,
                bleUseL2CAPInEngagement = settingsModel.presentmentBleL2CapInEngagementEnabled.value
            ),
            promptModel = promptModel,
            presentmentActivityClass = NfcActivity::class.java
        )
    }
}