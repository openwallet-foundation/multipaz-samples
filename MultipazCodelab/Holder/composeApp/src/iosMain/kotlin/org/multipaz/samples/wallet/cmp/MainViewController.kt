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
import org.multipaz.presentment.model.PresentmentModel
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.provisioning.ProvisioningModel
import org.multipaz.samples.wallet.cmp.di.initKoin
import org.multipaz.samples.wallet.cmp.util.ProvisioningSupport
import org.multipaz.trustmanagement.TrustManager
import org.multipaz.util.Logger

private const val TAG = "MainViewController"

// Store credentialOffers channel globally so HandleUrl can access it
private var globalCredentialOffers: Channel<String>? = null

@Suppress("FunctionName") // iOS interop: follows UIKit factory method naming convention
fun MainViewController() =
    ComposeUIViewController(
        configure = {
            initKoin()
        },
    ) {
        var isInitialized by remember { mutableStateOf(false) }
        val credentialOffers: Channel<String> = Channel()

        // Store the channel globally for HandleUrl to access
        LaunchedEffect(credentialOffers) {
            globalCredentialOffers = credentialOffers
        }

        // Initialize Koin dependencies eagerly (similar to old App.init())
        LaunchedEffect(Unit) {
            Logger.i(TAG, "iOS: Starting eager initialization of Koin dependencies")
            try {
                // Trigger initialization of all singletons that use runBlocking
                val koinHelper = object : KoinComponent { }
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
            }
        }

        if (!isInitialized) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "Initializing...")
            }
            return@ComposeUIViewController
        }

        UtopiaSampleApp(
            credentialOffers = credentialOffers,
        )
    }

/**
 * Handle a link (either an app link, universal link, or custom URL scheme link).
 * Called from SwiftUI's .onOpenURL modifier.
 */
@Suppress("FunctionName") // Swift interop: follows Swift naming convention for exported functions
fun HandleUrl(url: String) {
    val credentialOffers = globalCredentialOffers
    if (credentialOffers == null) {
        Logger.w(TAG, "HandleUrl: credentialOffers channel not yet initialized, URL will be ignored: $url")
        return
    }

    try {
        val koinHelper = object : KoinComponent { }
        val provisioningModel = koinHelper.get<ProvisioningModel>()
        val provisioningSupport = koinHelper.get<ProvisioningSupport>()

        org.multipaz.samples.wallet.cmp.util.handleUrl(
            url = url,
            credentialOffers = credentialOffers,
            provisioningModel = provisioningModel,
            provisioningSupport = provisioningSupport,
        )
    } catch (e: Exception) {
        Logger.e(TAG, "Error in HandleUrl: ${e.message}", e)
    }
}
