package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.multipaz.models.provisioning.ProvisioningModel
import org.multipaz.util.Logger
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import org.koin.compose.koinInject
import org.multipaz.provisioning.AuthorizationChallenge
import org.multipaz.provisioning.AuthorizationResponse
import org.multipaz.samples.wallet.cmp.util.ProvisioningSupport

@Composable
fun ProvisioningTestScreen(
    provisioningModel: ProvisioningModel = koinInject(),
    provisioningSupport: ProvisioningSupport = koinInject(),
    onNavigateToMain: () -> Unit
) {
    Logger.i(
        EvidenceRequestWebView,
        "ProvisioningTestScreen rendered with state: ${provisioningModel.state.value}"
    )

    val provisioningState = provisioningModel.state.collectAsState(ProvisioningModel.Idle).value
    Logger.i(
        EvidenceRequestWebView,
        "ProvisioningTestScreen: collected state is: $provisioningState"
    )

    Column {
        Spacer(modifier = Modifier.height(100.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {

            Text(
                modifier = Modifier
                    .padding(16.dp)
                    .clickable { onNavigateToMain() },
                text = "← Back",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        when (provisioningState) {
            is ProvisioningModel.Authorizing -> {
                Logger.i(
                    EvidenceRequestWebView,
                    "ProvisioningTestScreen: Rendering Authorize with challenges: ${provisioningState.authorizationChallenges}"
                )
                Authorize(
                    provisioningModel,
                    provisioningState.authorizationChallenges,
                    provisioningSupport
                )
            }

            is ProvisioningModel.Error -> {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    text = "Error: ${provisioningState.err.message}"
                )
                Text(
                    modifier = Modifier.padding(4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    text = "For details: adb logcat -s ProvisioningModel"
                )
            }

            else -> {
                val text = when (provisioningState) {
                    ProvisioningModel.Idle -> "Initializing..."
                    ProvisioningModel.Initial -> "Starting provisioning..."
                    ProvisioningModel.Connected -> "Connected to the back-end"
                    ProvisioningModel.ProcessingAuthorization -> "Processing authorization..."
                    ProvisioningModel.Authorized -> "Authorized"
                    ProvisioningModel.RequestingCredentials -> "Requesting credentials..."
                    ProvisioningModel.CredentialsIssued -> "Credentials issued"
                    is ProvisioningModel.Error -> throw IllegalStateException()
                    is ProvisioningModel.Authorizing -> throw IllegalStateException()
                }
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    text = text
                )
            }
        }
    }
}

@Composable
private fun Authorize(
    provisioningModel: ProvisioningModel,
    challenges: List<AuthorizationChallenge>,
    provisioningSupport: ProvisioningSupport
) {
    Logger.i(EvidenceRequestWebView, "Authorize function called with ${challenges.size} challenges")
    when (val challenge = challenges.first()) {
        is AuthorizationChallenge.OAuth -> {
            Logger.i(
                EvidenceRequestWebView,
                "Authorize: Rendering EvidenceRequestWebView for OAuth challenge"
            )
            EvidenceRequestWebView(
                evidenceRequest = challenge,
                provisioningModel = provisioningModel,
                provisioningSupport = provisioningSupport
            )
        }

        is AuthorizationChallenge.SecretText -> TODO()
    }
}

const val EvidenceRequestWebView = "PRO:EvidenceRequestWebView"

@Composable
fun EvidenceRequestWebView(
    evidenceRequest: AuthorizationChallenge.OAuth,
    provisioningModel: ProvisioningModel,
    provisioningSupport: ProvisioningSupport
) {
    // Add this logging to see when the component is created/re-created
    Logger.i(EvidenceRequestWebView, "EvidenceRequestWebView Composable created/re-created")
    Logger.i(
        EvidenceRequestWebView,
        "EvidenceRequestWebView: evidenceRequest.url = ${evidenceRequest.url}"
    )
    Logger.i(
        EvidenceRequestWebView,
        "EvidenceRequestWebView: evidenceRequest.state = ${evidenceRequest.state}"
    )

    // Stabilize the evidenceRequest to prevent unnecessary re-compositions
    val stableEvidenceRequest = remember(evidenceRequest.url, evidenceRequest.state) {
        evidenceRequest
    }

    // NB: these scopes will be cancelled when navigating outside of this screen.
    LaunchedEffect(stableEvidenceRequest.url) {
        Logger.i(EvidenceRequestWebView, "EvidenceRequestWebView LaunchedEffect START")
        Logger.i(
            EvidenceRequestWebView,
            "EvidenceRequestWebView: Waiting for app link invocation with state: ${stableEvidenceRequest.state}"
        )
        val invokedUrl = provisioningSupport.waitForAppLinkInvocation(stableEvidenceRequest.state)
        Logger.i(
            EvidenceRequestWebView,
            "EvidenceRequestWebView LaunchedEffect invokedUrl: $invokedUrl"
        )
        provisioningModel.provideAuthorizationResponse(
            AuthorizationResponse.OAuth(stableEvidenceRequest.id, invokedUrl)
        )
    }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(stableEvidenceRequest.url) {
        // Launch the browser
        Logger.i(
            EvidenceRequestWebView,
            "EvidenceRequestWebView: About to open browser with URL: ${stableEvidenceRequest.url}"
        )
        uriHandler.openUri(stableEvidenceRequest.url)
        Logger.i(EvidenceRequestWebView, "EvidenceRequestWebView: Browser opened successfully")
        // Poll as a fallback
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Launching browser, continue there",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

