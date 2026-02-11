package org.multipaz.samples.wallet.cmp.navhost

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import org.multipaz.provisioning.ProvisioningModel
import org.multipaz.samples.wallet.cmp.App
import org.multipaz.samples.wallet.cmp.AppRoute
import org.multipaz.samples.wallet.cmp.ui.ProvisioningScreen

@Composable
fun AppNavHost(
    app: App,
    imageLoader: ImageLoader,
) {
    val navController = rememberNavController()
    val provisioningState by app.provisioningModel.state.collectAsState()

    LaunchedEffect(provisioningState) {
        val provisioningActive =
            provisioningState != ProvisioningModel.Idle &&
                provisioningState != ProvisioningModel.CredentialsIssued

        val targetRoute: AppRoute = if (provisioningActive) {
            AppRoute.Provisioning
        } else {
            AppRoute.Wallet
        }

        navController.navigate(targetRoute) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Wallet,
    ) {
        composable<AppRoute.Wallet> {
            WalletNavHost(
                documentModel = app.documentModel,
                presentmentModel = app.presentmentModel,
                presentmentSource = app.presentmentSource,
                documentTypeRepository = app.documentTypeRepository,
                documentStore = app.documentStore,
                imageLoader = imageLoader,
            )
        }

        composable<AppRoute.Provisioning> {
            ProvisioningScreen(
                provisioningModel = app.provisioningModel,
                provisioningSupport = app.provisioningSupport,
                onCancel = { app.provisioningModel.cancel() }
            )
        }
    }
}
