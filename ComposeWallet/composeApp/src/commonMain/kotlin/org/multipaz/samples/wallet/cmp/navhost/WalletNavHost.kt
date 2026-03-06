package org.multipaz.samples.wallet.cmp.navhost

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import org.multipaz.compose.document.DocumentModel
import org.multipaz.document.DocumentStore
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.prompt.PromptModel
import org.multipaz.samples.wallet.cmp.SettingsModel
import org.multipaz.samples.wallet.cmp.WalletRoute
import org.multipaz.samples.wallet.cmp.ui.DocumentDetailsScreen
import org.multipaz.samples.wallet.cmp.ui.DocumentClaimsScreen
import org.multipaz.samples.wallet.cmp.ui.DocumentViewerScreen
import org.multipaz.samples.wallet.cmp.ui.WalletScreen

@Composable
fun WalletNavHost(
    documentModel: DocumentModel,
    settingsModel: SettingsModel,
    promptModel: PromptModel,
    presentmentSource: PresentmentSource,
    documentStore: DocumentStore,
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
                settingsModel = settingsModel,
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
                promptModel = promptModel,
                presentmentSource = presentmentSource,
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
                documentTypeRepository = presentmentSource.documentTypeRepository,
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
