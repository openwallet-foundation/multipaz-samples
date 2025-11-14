package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.multipaz.compose.permissions.rememberBluetoothEnabledState
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import org.multipaz.compose.presentment.MdocProximityQrPresentment
import org.multipaz.compose.presentment.MdocProximityQrSettings
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.compose.qrcode.generateQrCode
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.models.presentment.PresentmentModel
import org.multipaz.models.presentment.PresentmentSource
import org.multipaz.prompt.PromptModel
import org.multipaz.samples.wallet.cmp.util.Constants.APP_NAME
import org.multipaz.samples.wallet.cmp.util.Constants.appIcon
import org.multipaz.util.Logger
import org.multipaz.util.UUID

private const val TAG = "AccountScreen"

@Composable
fun AccountScreen(
    promptModel: PromptModel = koinInject(),
    presentmentModel: PresentmentModel = koinInject(),
    presentmentSource: PresentmentSource = koinInject(),
    documentTypeRepository: DocumentTypeRepository = koinInject(),
    hasCredentials: Boolean?,
) {
    val coroutineScope = rememberCoroutineScope { promptModel }
    val blePermissionState = rememberBluetoothPermissionState()
    val bleEnabledState = rememberBluetoothEnabledState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PromptDialogs(promptModel)
        Spacer(modifier = Modifier.height(30.dp))

        MembershipCard()

        Spacer(modifier = Modifier.height(16.dp))
        CredentialStatusIndicator(hasCredentials)

        if (!blePermissionState.isGranted) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        blePermissionState.launchPermissionRequest()
                    }
                }
            ) {
                Text("Request BLE permissions")
            }
        } else if (!bleEnabledState.isEnabled) {
            Button(onClick = { coroutineScope.launch { bleEnabledState.enable() } }) {
                Text("Enable BLE")
            }
        } else {
            val context = LocalPlatformContext.current
            val imageLoader = remember {
                ImageLoader.Builder(context).components { /* network loader omitted */ }.build()
            }

            val noCredentialDialog = remember { mutableStateOf(false) }
            MdocProximityQrPresentment(
                appName = APP_NAME,
                appIconPainter = painterResource(appIcon),
                presentmentModel = presentmentModel,
                presentmentSource = presentmentSource,
                promptModel = promptModel,
                documentTypeRepository = documentTypeRepository,
                imageLoader = imageLoader,
                allowMultipleRequests = false,
                showQrButton = { onQrButtonClicked ->
                    ShowQrButton(
                        hasCredentials,
                        onQrButtonClicked
                    )
                },
                showQrCode = { uri ->
                    ShowQrCode(
                        uri = uri,
                        presentmentModel = presentmentModel
                    )
                }
            )
            if (noCredentialDialog.value) {
                AlertDialog(
                    onDismissRequest = { noCredentialDialog.value = false },
                    title = { Text("No credential available") },
                    text = { Text("Please add a credential before presenting.") },
                    confirmButton = {
                        TextButton(onClick = { noCredentialDialog.value = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CredentialStatusIndicator(hasCredentials: Boolean?) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        when (hasCredentials) {
            true -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Credential Available",
                    tint = Color(0xFF4CAF50), // Green color
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Credential is available",
                    color = Color(0xFF4CAF50), // Green color
                    fontSize = 14.sp
                )
            }

            false -> {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "No Credential",
                    tint = Color(0xFFF44336), // Red color
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Credential not available",
                    color = Color(0xFFF44336), // Red color
                    fontSize = 14.sp
                )
            }

            null -> {
                Text(
                    text = "Checking credential status...",
                    color = Color(0xFF666666), // Gray color
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ShowQrButton(
    hasCredentials: Boolean?,
    onQrButtonClicked: (settings: MdocProximityQrSettings) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (hasCredentials) {
            true -> {
                Button(onClick = {
                    val connectionMethods = listOf(
                        MdocConnectionMethodBle(
                            supportsPeripheralServerMode = false,
                            supportsCentralClientMode = true,
                            peripheralServerModeUuid = null,
                            centralClientModeUuid = UUID.randomUUID(),
                        )
                    )
                    onQrButtonClicked(
                        MdocProximityQrSettings(
                            availableConnectionMethods = connectionMethods,
                            createTransportOptions = MdocTransportOptions(bleUseL2CAP = true)
                        )
                    )
                }) {
                    Text("Present mDL via QR")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "The mDL is also available\n" +
                            "via NFC engagement and W3C DC API\n" +
                            "(Android-only right now)",
                    textAlign = TextAlign.Center
                )
            }

            false -> {
                Text(
                    text = "No usable credentials available.\nPlease add a credential first.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        Logger.i(TAG, "Opening issuer website: https://issuer.multipaz.org")
                        uriHandler.openUri("https://issuer.multipaz.org")
                    }
                ) {
                    Text("Get Credentials from Issuer")
                }
            }

            else -> {
                Text("Checking credentials...")
            }
        }
    }
}

@Composable
private fun ShowQrCode(
    uri: String,
    presentmentModel: PresentmentModel
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val qrCodeBitmap = remember { generateQrCode(uri) }
        Spacer(modifier = Modifier.height(330.dp))
        Text(text = "Present QR code to mdoc reader")
        Image(
            modifier = Modifier.fillMaxWidth(),
            bitmap = qrCodeBitmap,
            contentDescription = null,
            contentScale = ContentScale.FillWidth
        )
        Button(
            onClick = {
                presentmentModel.reset()
            }
        ) {
            Text("Cancel")
        }
    }
}
