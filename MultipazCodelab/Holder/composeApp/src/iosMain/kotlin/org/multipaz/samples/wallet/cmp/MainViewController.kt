package org.multipaz.samples.wallet.cmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.multipaz.document.DocumentStore
import org.multipaz.models.presentment.PresentmentModel
import org.multipaz.models.presentment.PresentmentSource
import org.multipaz.models.provisioning.ProvisioningModel
import org.multipaz.samples.wallet.cmp.di.initKoin
import org.multipaz.samples.wallet.cmp.util.ProvisioningSupport
import org.multipaz.trustmanagement.TrustManager
import org.multipaz.util.Logger

private const val TAG = "MainViewController"

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) {
    var isInitialized by remember { mutableStateOf(false) }

    // Initialize Koin dependencies eagerly (similar to old App.init())
    LaunchedEffect(Unit) {
        Logger.i(TAG, "iOS: Starting eager initialization of Koin dependencies")
        try {
            // Trigger initialization of all singletons that use runBlocking
            val koinHelper = object : KoinComponent {}
            koinHelper.get<TrustManager>() // This loads certificates with runBlocking
            koinHelper.get<DocumentStore>()
            koinHelper.get<ProvisioningModel>()
            koinHelper.get<ProvisioningSupport>()
            koinHelper.get<PresentmentModel>()
            koinHelper.get<PresentmentSource>()
            Logger.i(TAG, "iOS: All Koin dependencies initialized successfully")
            isInitialized = true
        } catch (e: Exception) {
            Logger.e(TAG, "iOS: Error during initialization: ${e.message}", e)
            throw e
        }
    }

    if (!isInitialized) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Initializing...")
        }
        return@ComposeUIViewController
    }

    // Create a KoinComponent to access dependencies
    val koinHelper = object : KoinComponent {
        val provisioningModel: ProvisioningModel = get()
        val provisioningSupport: ProvisioningSupport = get()
        val credentialOffers: Channel<String> = Channel()
    }

    UtopiaSampleApp(
        credentialOffers = koinHelper.credentialOffers,
        provisioningModel = koinHelper.provisioningModel,
        provisioningSupport = koinHelper.provisioningSupport
    )
}