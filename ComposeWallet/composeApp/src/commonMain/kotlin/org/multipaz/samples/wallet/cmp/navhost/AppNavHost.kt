package org.multipaz.samples.wallet.cmp.navhost

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import org.multipaz.compose.provisioning.ProvisioningBottomSheet
import org.multipaz.provisioning.ProvisioningModel
import org.multipaz.samples.wallet.cmp.App
import org.multipaz.samples.wallet.cmp.AppRoute

@Composable
fun AppNavHost(
    app: App,
    imageLoader: ImageLoader,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppRoute.Wallet,
    ) {
        composable<AppRoute.Wallet> {
            WalletNavHost(
                documentModel = app.documentModel,
                settingsModel = app.settingsModel,
                promptModel = App.promptModel,
                presentmentSource = app.presentmentSource,
                documentStore = app.documentStore,
            )
        }
    }
}
