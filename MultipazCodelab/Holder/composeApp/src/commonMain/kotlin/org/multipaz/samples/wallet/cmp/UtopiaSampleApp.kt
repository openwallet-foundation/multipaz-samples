package org.multipaz.samples.wallet.cmp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.channels.Channel
import org.koin.compose.koinInject
import org.multipaz.models.provisioning.ProvisioningModel
import org.multipaz.samples.wallet.cmp.ui.HomeScreen
import org.multipaz.samples.wallet.cmp.ui.ProvisioningTestScreen
import org.multipaz.samples.wallet.cmp.util.ProvisioningSupport
import org.multipaz.util.Logger


private const val TAG = "Navigation"

@Composable
fun UtopiaSampleApp(
    credentialOffers: Channel<String>,
    provisioningModel: ProvisioningModel = koinInject(),
    provisioningSupport: ProvisioningSupport = koinInject()
) {
    MaterialTheme {

        val stableProvisioningModel = remember(provisioningModel) { provisioningModel }
        val stableProvisioningSupport = remember(provisioningSupport) { provisioningSupport }
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "main"
        ) {
            composable("main") {
                Logger.i(TAG, "NavHost: Rendering 'main' route")
                HomeScreen()
            }
            composable("provisioning") {
                Logger.i(TAG, "NavHost: Rendering 'provisioning' route")
                ProvisioningTestScreen(
                    onNavigateToMain = { navController.navigate("main") }
                )
            }
        }

        // Use the working pattern from identity-credential project
        LaunchedEffect(true) {
            Logger.i(TAG, "LaunchedEffect: Credential processing loop started")
            while (true) {
                Logger.i(TAG, "LaunchedEffect: Waiting for credential offer...")
                val credentialOffer = credentialOffers.receive()
                Logger.i(TAG, "LaunchedEffect: Received credential offer: $credentialOffer")
                Logger.i(TAG, "LaunchedEffect: Launching OpenID4VCI provisioning...")
                stableProvisioningModel.launchOpenID4VCIProvisioning(
                    offerUri = credentialOffer,
                    clientPreferences = ProvisioningSupport.OPENID4VCI_CLIENT_PREFERENCES,
                    backend = stableProvisioningSupport
                )
                Logger.i(
                    TAG,
                    "LaunchedEffect: Provisioning launched, navigating to provisioning"
                )
                navController.navigate("provisioning")
                Logger.i(TAG, "LaunchedEffect: Navigation completed")
            }
        }
    }
}
