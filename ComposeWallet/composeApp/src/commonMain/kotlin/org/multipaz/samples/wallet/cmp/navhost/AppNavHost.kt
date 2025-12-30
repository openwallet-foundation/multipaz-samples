package org.multipaz.samples.wallet.cmp.navhost

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var appRoute by remember { mutableStateOf<AppRoute>(AppRoute.Wallet) }
    val provisioningState = app.provisioningModel.state.collectAsState().value

    LaunchedEffect(provisioningState) {
        val provisioningActive =
            provisioningState != ProvisioningModel.Idle &&
                    provisioningState != ProvisioningModel.CredentialsIssued

        appRoute = if (provisioningActive) AppRoute.Provisioning else AppRoute.Wallet
    }

    when (appRoute) {
        AppRoute.Wallet -> WalletNavHost(
            documentModel = app.documentModel,
            presentmentModel = app.presentmentModel,
            presentmentSource = app.presentmentSource,
            documentTypeRepository = app.documentTypeRepository,
            imageLoader = imageLoader
        )

        AppRoute.Provisioning -> ProvisioningScreen(
            provisioningModel = app.provisioningModel,
            provisioningSupport = app.provisioningSupport,
            onCancel = { app.provisioningModel.cancel() }
        )
    }
}