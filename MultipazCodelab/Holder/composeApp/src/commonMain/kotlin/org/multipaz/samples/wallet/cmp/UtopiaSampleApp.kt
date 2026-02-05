package org.multipaz.samples.wallet.cmp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.channels.Channel
import org.koin.compose.koinInject
import org.multipaz.provisioning.ProvisioningModel
import org.multipaz.samples.wallet.cmp.ui.HomeScreen
import org.multipaz.samples.wallet.cmp.ui.ProvisioningTestScreen
import org.multipaz.samples.wallet.cmp.util.ProvisioningSupport
import org.multipaz.util.Logger

private const val TAG = "Navigation"

@Composable
fun UtopiaSampleApp(
    credentialOffers: Channel<String>,
    provisioningModel: ProvisioningModel = koinInject(),
    provisioningSupport: ProvisioningSupport = koinInject(),
) {
    MaterialTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "main",
        ) {
            composable("main") {
                Logger.i(TAG, "NavHost: Rendering 'main' route")
                HomeScreen()
            }
            composable("provisioning") {
                Logger.i(TAG, "NavHost: Rendering 'provisioning' route")
                ProvisioningTestScreen(
                    onNavigateToMain = { navController.navigate("main") },
                )
            }
        }

        // Use the working pattern from identity-credential project
        LaunchedEffect(true) {
            while (true) {
                val credentialOffer = credentialOffers.receive()
                provisioningModel.launchOpenID4VCIProvisioning(
                    offerUri = credentialOffer,
                    clientPreferences = provisioningSupport.getOpenID4VCIClientPreferences(),
                    backend = provisioningSupport.getOpenID4VCIBackend(),
                )
                navController.navigate("provisioning")
            }
        }
    }
}
