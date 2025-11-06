package org.multipaz.samples.wallet.cmp

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.multipaz.models.provisioning.ProvisioningModel
import org.multipaz.samples.wallet.cmp.di.initKoin
import org.multipaz.samples.wallet.cmp.util.ProvisioningSupport

val credentialOffers: Channel<String> = Channel()
fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) {
    // Create a KoinComponent to access dependencies
    val koinHelper = object : KoinComponent {
        val provisioningModel: ProvisioningModel = get()
        val provisioningSupport: ProvisioningSupport = get()

    }

    UtopiaSampleApp(
        credentialOffers = credentialOffers,
        provisioningModel = koinHelper.provisioningModel,
        provisioningSupport = koinHelper.provisioningSupport
    )
}