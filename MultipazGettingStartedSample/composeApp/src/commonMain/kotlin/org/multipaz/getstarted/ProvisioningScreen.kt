package org.multipaz.getstarted

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.multipaz.compose.provisioning.Provisioning
import org.multipaz.provisioning.ProvisioningModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ProvisioningScreen(
    provisioningModel: ProvisioningModel,
    provisioningSupport: ProvisioningSupport,
    provisioningState: ProvisioningModel.State,
    goBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Provisioning",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.W700

                    )
                },
                navigationIcon = {
                    IconButton(onClick = goBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Show progress indicator only when provisioning is active
                if (provisioningState !is ProvisioningModel.CredentialsIssued &&
                    provisioningState !is ProvisioningModel.Error
                ) {
                    Provisioning(
                        provisioningModel = provisioningModel,
                        waitForRedirectLinkInvocation = { state ->
                            provisioningSupport.waitForAppLinkInvocation(state)
                        }
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp),
                        strokeWidth = 4.dp,
                    )
                }

                when (provisioningState) {
                    is ProvisioningModel.CredentialsIssued -> {
                        OnCredentialIssued(goBack = goBack)
                    }

                    is ProvisioningModel.Error -> {
                        OnError(goBack = {
                            goBack()
                        })
                    }

                    else -> Unit
                }
            }
        }
    )
}

@Composable
private fun OnCredentialIssued(
    goBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(100.dp),
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Success",
            tint = Color.Green
        )
        Text(
            modifier = Modifier.padding(16.dp),
            text = "Credentials Issued Successfully",
            style = MaterialTheme.typography.headlineMedium.copy(
                textAlign = TextAlign.Center
            )
        )

        Button(onClick = {
            goBack()
        }) {
            Text(text = "Go Back")
        }
    }
}

@Composable
private fun OnError(
    goBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = "Error",
            tint = Color.Red,
            modifier = Modifier.size(100.dp)
        )
        Text(
            modifier = Modifier.padding(16.dp),
            text = "An error occurred",
            style = MaterialTheme.typography.headlineMedium.copy(
                textAlign = TextAlign.Center
            )
        )

        Button(onClick = {
            goBack()
        }) {
            Text(text = "Try Again Later")
        }
    }
}
