package org.multipaz.getstarted.presentment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.multipaz.compose.permissions.rememberBluetoothEnabledState
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import org.multipaz.compose.presentment.MdocProximityQrPresentment
import org.multipaz.compose.presentment.MdocProximityQrSettings
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethod
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.presentment.PresentmentSource
import org.multipaz.prompt.PromptModel
import org.multipaz.util.UUID
import kotlin.time.Duration.Companion.seconds

@Composable
fun PresentmentHomeSection(
    presentmentSource: PresentmentSource,
    promptModel: PromptModel,
    modifier: Modifier = Modifier
) {
    val blePermissionState = rememberBluetoothPermissionState()
    val bleEnabledState = rememberBluetoothEnabledState()
    val coroutineScope = rememberCoroutineScope { promptModel }

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
        Button(
            onClick = { coroutineScope.launch { bleEnabledState.enable() } }) {
            Text("Enable Bluetooth")
        }
    } else {
        MdocProximityQrPresentment(
            modifier = modifier,
            source = presentmentSource,
            promptModel = promptModel,
            prepareSettings = { generateQrCode ->
                val connectionMethods = mutableListOf<MdocConnectionMethod>()
                val bleUuid = UUID.randomUUID()
                connectionMethods.add(
                    MdocConnectionMethodBle(
                        supportsPeripheralServerMode = true,
                        supportsCentralClientMode = false,
                        peripheralServerModeUuid = bleUuid,
                        centralClientModeUuid = null,
                    )
                )

                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(
                        onClick = {
                            generateQrCode(
                                MdocProximityQrSettings(
                                    availableConnectionMethods = connectionMethods,
                                    createTransportOptions = MdocTransportOptions(
                                        bleUseL2CAPInEngagement = true
                                    )
                                )
                            )
                        }
                    ) @Composable {
                        Text("Present mDoc via QR code")
                    }
                }
            },
            showTransacting = { reset ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Sharing your credential…",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    OutlinedButton(onClick = { reset() }) {
                        Text("Cancel")
                    }
                }
            },
            showQrCode = { uri, reset ->
                ShowQrCode(
                    uri,
                    onCancel = {
                        reset()
                    }
                )
            },
            showCompleted = { error, reset ->
                if (error is CancellationException) {
                    reset()
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (error != null) "Something went wrong" else "Credential shared",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (error != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                        if (error != null) {
                            Text(
                                text = error.message ?: error.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    LaunchedEffect(Unit) {
                        delay(1.5.seconds)
                        reset()
                    }
                }
            },
        )
    }
}
