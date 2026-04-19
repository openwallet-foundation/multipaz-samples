package org.multipaz.getstarted

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.compose.provisioning.ProvisioningBottomSheet
import org.multipaz.document.Document
import org.multipaz.getstarted.core.AppContainer
import org.multipaz.getstarted.core.httpClientEngineFactory
import org.multipaz.getstarted.provisioning.ProvisioningSupport
import org.multipaz.getstarted.verification.ShowResponseDestination
import org.multipaz.getstarted.verification.ShowResponseScreen
import org.multipaz.provisioning.DocumentProvisioningHandler
import org.multipaz.provisioning.ProvisioningModel

class App {

    private val container = AppContainer.getInstance()
    private val credentialOffers = Channel<String>()

    lateinit var provisioningModel: ProvisioningModel
    lateinit var provisioningSupport: ProvisioningSupport
    var isInitialized = false

    suspend fun init() {
        if (isInitialized) return

        container.init()

        provisioningModel = ProvisioningModel(
            documentProvisioningHandler = DocumentProvisioningHandler(
                documentStore = container.documentStore,
                secureArea = container.secureArea
            ),
            httpClient = HttpClient(httpClientEngineFactory) {
                followRedirects = false
            },
            promptModel = AppContainer.promptModel,
            authorizationSecureArea = container.secureArea
        )
        provisioningSupport = ProvisioningSupport(
            storage = container.storage,
            secureArea = container.secureArea,
        )
        provisioningSupport.init()

        isInitialized = true
    }

    fun handleUrl(url: String) {
        if (url.startsWith(OID4VCI_CREDENTIAL_OFFER_URL_SCHEME)
            || url.startsWith(HAIP_URL_SCHEME)
        ) {
            val queryIndex = url.indexOf('?')
            if (queryIndex >= 0) {
                CoroutineScope(Dispatchers.Default).launch {
                    credentialOffers.send(url)
                }
            }
        } else if (url.startsWith(ProvisioningSupport.APP_LINK_BASE_URL)) {
            CoroutineScope(Dispatchers.Default).launch {
                provisioningSupport.processAppLinkInvocation(url)
            }
        }
    }

    @Composable
    fun Content() {
        val navController = rememberNavController()
        val identityIssuer = "Multipaz Getting Started Sample"

        val isInitialized = remember { mutableStateOf(false) }

        if (!isInitialized.value) {
            CoroutineScope(Dispatchers.Main).launch {
                init()
                isInitialized.value = true
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Initializing...")
            }
            return
        }

        val provisioningState = provisioningModel.state.collectAsState().value

        val documents = remember { mutableStateListOf<Document>() }

        LaunchedEffect(
            navController.currentDestination,
            provisioningState
        ) {
            val shouldRefresh =
                provisioningState is ProvisioningModel.CredentialsIssued ||
                        navController.currentDestination != null

            if (shouldRefresh) {
                val currentDocuments = container.listDocuments()
                if (currentDocuments.size != documents.size) {
                    documents.clear()
                    documents.addAll(currentDocuments)
                }
            }
        }

        MaterialTheme {
            PromptDialogs(AppContainer.promptModel)

            LaunchedEffect(true) {
                if (!provisioningModel.isActive) {
                    while (true) {
                        val credentialOffer = credentialOffers.receive()
                        provisioningModel.launchOpenID4VCIProvisioning(
                            offerUri = credentialOffer,
                            clientPreferences = provisioningSupport.getOpenID4VCIClientPreferences(),
                            backend = provisioningSupport.getOpenID4VCIBackend()
                        )
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = Destination.HomeDestination,
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            ) {
                composable<Destination.HomeDestination> {
                    HomeScreen(
                        container = container,
                        navController = navController,
                        identityIssuer = identityIssuer,
                        documents = documents,
                        onDeleteDocument = {
                            documents.remove(it)
                        }
                    )
                }

                composable<ShowResponseDestination> { backStackEntry ->
                    val response =
                        backStackEntry.toRoute<ShowResponseDestination>()

                    ShowResponseScreen(
                        response = response,
                        documentTypeRepository = container.documentTypeRepository,
                        goBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            ProvisioningBottomSheet(
                provisioningModel = provisioningModel,
                waitForRedirectLinkInvocation = { state ->
                    provisioningSupport.waitForAppLinkInvocation(state)
                }
            )
        }
    }

    companion object {
        private const val OID4VCI_CREDENTIAL_OFFER_URL_SCHEME = "openid-credential-offer://"
        private const val HAIP_URL_SCHEME = "haip://"

        private var app: App? = null
        fun getInstance(): App {
            if (app == null) {
                app = App()
            }
            return app!!
        }
    }
}
