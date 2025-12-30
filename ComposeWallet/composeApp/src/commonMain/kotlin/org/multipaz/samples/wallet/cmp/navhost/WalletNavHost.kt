package org.multipaz.samples.wallet.cmp.navhost

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import coil3.ImageLoader
import org.multipaz.compose.document.DocumentModel
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.presentment.model.PresentmentModel
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.samples.wallet.cmp.WalletRoute
import org.multipaz.samples.wallet.cmp.ui.WalletDetailScreen
import org.multipaz.samples.wallet.cmp.ui.WalletScreen

@Composable
fun WalletNavHost(
    documentModel: DocumentModel,
    presentmentModel: PresentmentModel,
    presentmentSource: PresentmentSource,
    documentTypeRepository: DocumentTypeRepository,
    imageLoader: ImageLoader,
) {
    val navState = remember { mutableStateOf<WalletRoute>(WalletRoute.Wallet) }

    when (val route = navState.value) {
        WalletRoute.Wallet -> {
            WalletScreen(
                documentModel = documentModel,
                onDocumentSelected = { documentInfo ->
                    navState.value = WalletRoute.WalletDetails(documentInfo)
                }
            )
        }

        is WalletRoute.WalletDetails -> {
            WalletDetailScreen(
                documentInfo = route.documentInfo,
                documentModel = documentModel,
                presentmentModel = presentmentModel,
                presentmentSource = presentmentSource,
                documentTypeRepository = documentTypeRepository,
                imageLoader = imageLoader,
                onBack = { navState.value = WalletRoute.Wallet }
            )
        }
    }
}
