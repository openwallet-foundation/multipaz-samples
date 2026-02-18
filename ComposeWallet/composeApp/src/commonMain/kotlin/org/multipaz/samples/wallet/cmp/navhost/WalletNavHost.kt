package org.multipaz.samples.wallet.cmp.navhost

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import kotlinx.coroutines.launch
import org.multipaz.compose.document.DocumentModel
import org.multipaz.document.DocumentStore
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.presentment.model.PresentmentModel
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.samples.wallet.cmp.WalletRoute
import org.multipaz.samples.wallet.cmp.ui.DocumentDetailsScreen
import org.multipaz.samples.wallet.cmp.ui.DocumentClaimsScreen
import org.multipaz.samples.wallet.cmp.ui.DocumentViewerScreen
import org.multipaz.samples.wallet.cmp.ui.WalletScreen

@Composable
fun WalletNavHost(
    documentModel: DocumentModel,
    presentmentModel: PresentmentModel,
    presentmentSource: PresentmentSource,
    documentTypeRepository: DocumentTypeRepository,
    documentStore: DocumentStore,
    imageLoader: ImageLoader,
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = WalletRoute.WalletList,
    ) {
        composable<WalletRoute.WalletList> {
            WalletScreen(
                documentModel = documentModel,
                onDocumentSelected = { documentInfo ->
                    navController.navigate(
                        WalletRoute.WalletDetails(
                            documentId = documentInfo.document.identifier
                        )
                    )
                }
            )
        }

        composable<WalletRoute.WalletDetails> { backStackEntry ->
            val route = backStackEntry.toRoute<WalletRoute.WalletDetails>()
            DocumentViewerScreen(
                documentId = route.documentId,
                documentModel = documentModel,
                presentmentModel = presentmentModel,
                presentmentSource = presentmentSource,
                documentTypeRepository = documentTypeRepository,
                imageLoader = imageLoader,
                onBack = { navController.popBackStack() },
                onMenuClick = {
                    navController.navigate(
                        WalletRoute.DocumentDetails(documentId = route.documentId)
                    )
                }
            )
        }

        composable<WalletRoute.PersonalIdInfo> { backStackEntry ->
            val route = backStackEntry.toRoute<WalletRoute.PersonalIdInfo>()
            DocumentClaimsScreen(
                documentId = route.documentId,
                documentModel = documentModel,
                documentTypeRepository = documentTypeRepository,
                onBack = { navController.popBackStack() }
            )
        }

        composable<WalletRoute.DocumentDetails> { backStackEntry ->
            val route = backStackEntry.toRoute<WalletRoute.DocumentDetails>()
            DocumentDetailsScreen(
                documentId = route.documentId,
                documentModel = documentModel,
                onBack = { navController.popBackStack() },
                onPersonalIdInfoClick = {
                    navController.navigate(
                        WalletRoute.PersonalIdInfo(documentId = route.documentId)
                    )
                },
                onRemove = {
                    coroutineScope.launch {
                        documentStore.deleteDocument(route.documentId)
                        navController.popBackStack(
                            route = WalletRoute.WalletList,
                            inclusive = false
                        )
                    }
                }
            )
        }
    }
}
