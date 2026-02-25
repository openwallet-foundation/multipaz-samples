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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.multipaz.compose.permissions.rememberBluetoothEnabledState
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import org.multipaz.compose.presentment.MdocProximityQrPresentment
import org.multipaz.compose.presentment.MdocProximityQrSettings
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.compose.qrcode.generateQrCode
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethod
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodNfc
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.presentment.model.PresentmentModel
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.prompt.PromptModel
import org.multipaz.samples.wallet.cmp.util.AppSettingsModel
import org.multipaz.samples.wallet.cmp.util.Constants.APP_NAME
import org.multipaz.util.Logger
import org.multipaz.util.UUID
import kotlin.time.Duration.Companion.seconds

private const val TAG = "AccountScreen"

@Composable
fun AccountScreen(
    promptModel: PromptModel = koinInject(),
    presentmentModel: PresentmentModel = koinInject(),
    presentmentSource: PresentmentSource = koinInject(),
    settingsModel: AppSettingsModel = koinInject(),
    hasCredentials: Boolean?,
) {
    val coroutineScope = rememberCoroutineScope { promptModel }
    val blePermissionState = rememberBluetoothPermissionState()
    val bleEnabledState = rememberBluetoothEnabledState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PromptDialogs(promptModel)
        Spacer(modifier = Modifier.height(30.dp))

        MembershipCard()

        Spacer(modifier = Modifier.height(16.dp))
        CredentialStatusIndicator(hasCredentials)
        Spacer(modifier = Modifier.height(16.dp))

        if (!blePermissionState.isGranted) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        blePermissionState.launchPermissionRequest()
                    }
                },
            ) {
                Text("Request BLE permissions")
            }
        } else if (!bleEnabledState.isEnabled) {
            Button(onClick = { coroutineScope.launch { bleEnabledState.enable() } }) {
                Text("Enable BLE")
            }
        } else {
            val noCredentialDialog = remember { mutableStateOf(false) }

            // Collect settings as state
            val bleCentralClientEnabled = settingsModel.presentmentBleCentralClientModeEnabled.collectAsState().value
            val blePeripheralServerEnabled = settingsModel.presentmentBlePeripheralServerModeEnabled.collectAsState().value
            val nfcDataTransferEnabled = settingsModel.presentmentNfcDataTransferEnabled.collectAsState().value
            val bleL2CapEnabled = settingsModel.presentmentBleL2CapEnabled.collectAsState().value
            val bleL2CapInEngagementEnabled = settingsModel.presentmentBleL2CapInEngagementEnabled.collectAsState().value
            val sessionEncryptionCurve = settingsModel.presentmentSessionEncryptionCurve.collectAsState().value

            MdocProximityQrPresentment(
                modifier = Modifier,
                source = presentmentSource,
                promptModel = promptModel,
                prepareSettings = { generateQrCode ->
                    ShowQrButton(
                        hasCredentials = hasCredentials,
                        bleCentralClientEnabled = bleCentralClientEnabled,
                        blePeripheralServerEnabled = blePeripheralServerEnabled,
                        nfcDataTransferEnabled = nfcDataTransferEnabled,
                        bleL2CapEnabled = bleL2CapEnabled,
                        bleL2CapInEngagementEnabled = bleL2CapInEngagementEnabled,
                        onGenerateQrCode = generateQrCode,
                    )
                },
                showQrCode = { uri, reset ->
                    ShowQrCode(
                        uri = uri,
                        onReset = reset,
                    )
                },
                showTransacting = { reset ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("Transacting")
                        Button(onClick = { reset() }) {
                            Text("Cancel")
                        }
                    }
                },
                showCompleted = { error, reset ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (error != null) {
                            Text("Something went wrong: $error")
                        } else {
                            Text("The data was shared")
                        }
                    }
                    LaunchedEffect(Unit) {
                        delay(1.5.seconds)
                        reset()
                    }
                },
                eDeviceKeyCurve = sessionEncryptionCurve,
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
                    },
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
        horizontalArrangement = Arrangement.Center,
    ) {
        when (hasCredentials) {
            true -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Credential Available",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Credential is available",
                    color = Color(0xFF4CAF50),
                    fontSize = 14.sp,
                )
            }

            false -> {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "No Credential",
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Credential not available",
                    color = Color(0xFFF44336),
                    fontSize = 14.sp,
                )
            }

            null -> {
                Text(
                    text = "Checking credential status...",
                    color = Color(0xFF666666),
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun ShowQrButton(
    hasCredentials: Boolean?,
    bleCentralClientEnabled: Boolean,
    blePeripheralServerEnabled: Boolean,
    nfcDataTransferEnabled: Boolean,
    bleL2CapEnabled: Boolean,
    bleL2CapInEngagementEnabled: Boolean,
    onGenerateQrCode: (MdocProximityQrSettings) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (hasCredentials) {
            true -> {
                Button(onClick = {
                    val connectionMethods = mutableListOf<MdocConnectionMethod>()
                    val bleUuid = UUID.randomUUID()
                    if (bleCentralClientEnabled) {
                        connectionMethods.add(
                            MdocConnectionMethodBle(
                                supportsPeripheralServerMode = false,
                                supportsCentralClientMode = true,
                                peripheralServerModeUuid = null,
                                centralClientModeUuid = bleUuid,
                            )
                        )
                    }
                    if (blePeripheralServerEnabled) {
                        connectionMethods.add(
                            MdocConnectionMethodBle(
                                supportsPeripheralServerMode = true,
                                supportsCentralClientMode = false,
                                peripheralServerModeUuid = bleUuid,
                                centralClientModeUuid = null,
                            )
                        )
                    }
                    if (nfcDataTransferEnabled) {
                        connectionMethods.add(
                            MdocConnectionMethodNfc(
                                commandDataFieldMaxLength = 0xffff,
                                responseDataFieldMaxLength = 0x10000
                            )
                        )
                    }
                    onGenerateQrCode(
                        MdocProximityQrSettings(
                            availableConnectionMethods = connectionMethods,
                            createTransportOptions = MdocTransportOptions(
                                bleUseL2CAP = bleL2CapEnabled,
                                bleUseL2CAPInEngagement = bleL2CapInEngagementEnabled
                            )
                        )
                    )
                }) {
                    Text("Present mDL via QR")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text =
                        "The mDL is also available\n" +
                            "via NFC engagement and W3C DC API\n" +
                            "(Android-only right now)",
                    textAlign = TextAlign.Center,
                )
            }

            false -> {
                Text(
                    text = "No usable credentials available.\nPlease add a credential first.",
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        Logger.i(TAG, "Opening issuer website: https://issuer.multipaz.org")
                        uriHandler.openUri("https://issuer.multipaz.org")
                    },
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
    onReset: () -> Unit,
) {
    val qrCodeBitmap = remember { generateQrCode(uri) }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Present QR code to mdoc reader")
        Image(
            modifier = Modifier.fillMaxWidth(),
            bitmap = qrCodeBitmap,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
        )
        Button(onClick = { onReset() }) {
            Text("Cancel")
        }
    }
}
