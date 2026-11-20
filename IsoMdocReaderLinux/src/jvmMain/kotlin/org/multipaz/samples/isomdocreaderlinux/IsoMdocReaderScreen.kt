package org.multipaz.samples.isomdocreaderlinux

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.multipaz.compose.qrcode.generateQrCode
import org.multipaz.mdoc.transport.MdocTransportType
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

private sealed interface ReaderUiState {
    data class Scanning(
        val statusMessage: String,
        val qrCodeUri: String? = null,
    ) : ReaderUiState
    data class Result(val verificationResult: IsoMdocVerificationResult) : ReaderUiState
    data class Error(val message: String) : ReaderUiState
}

private const val AUTO_RESET_SECONDS = 10
private const val DEFAULT_RESPONSE_TIMEOUT_SECONDS = 30
private const val INITIAL_SCANNING_MESSAGE = "Preparing Reader QR..."

@Composable
fun IsoMdocReaderScreen(
    usbVendorId: Int?,
    usbProductId: Int?,
    hciDevice: String
) {
    val session = remember(usbVendorId, usbProductId) {
        IsoMdocReaderSession(usbVendorId = usbVendorId, usbProductId = usbProductId)
    }
    var uiState by remember {
        mutableStateOf<ReaderUiState>(
            ReaderUiState.Scanning(INITIAL_SCANNING_MESSAGE)
        )
    }
    var connectedUsbReader by remember {
        mutableStateOf<ConnectedUsbNfcReader?>(null)
    }
    var scanCycle by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var useBle by remember { mutableStateOf(true) }
    var useL2capInEngagement by remember { mutableStateOf(true) }
    var usePeripheralMacAddressInEngagement by remember { mutableStateOf(true) }
    var useReaderAuthentication by remember { mutableStateOf(true) }
    var responseTimeoutSeconds by remember { mutableIntStateOf(DEFAULT_RESPONSE_TIMEOUT_SECONDS) }

    val hasNonDefaultSettings = !useBle ||
        !useL2capInEngagement ||
        !usePeripheralMacAddressInEngagement ||
        !useReaderAuthentication ||
        responseTimeoutSeconds != DEFAULT_RESPONSE_TIMEOUT_SECONDS

    LaunchedEffect(
        session,
        scanCycle,
        showSettings,
        useBle,
        useL2capInEngagement,
        usePeripheralMacAddressInEngagement,
        useReaderAuthentication,
        responseTimeoutSeconds,
    ) {
        if (!showSettings && uiState is ReaderUiState.Scanning) {
            val initialMsg = if (useBle) {
                INITIAL_SCANNING_MESSAGE
            } else {
                "Waiting for USB NFC reader (BLE disabled)..."
            }
            uiState = ReaderUiState.Scanning(initialMsg)
            try {
                var currentStatus = initialMsg
                var currentQrUri: String? = null
                val result = session.readAndVerifyIsoMdoc(
                    useBle = useBle,
                    useL2capInEngagement = useL2capInEngagement,
                    usePeripheralMacAddressInEngagement = usePeripheralMacAddressInEngagement,
                    useReaderAuthentication = useReaderAuthentication,
                    responseTimeout = responseTimeoutSeconds.seconds,
                    onStatusChanged = { status ->
                        currentStatus = status
                        uiState = ReaderUiState.Scanning(
                            statusMessage = currentStatus,
                            qrCodeUri = currentQrUri,
                        )
                    },
                    onQrCodeChanged = { qrUri ->
                        currentQrUri = qrUri
                        uiState = ReaderUiState.Scanning(
                            statusMessage = currentStatus,
                            qrCodeUri = currentQrUri,
                        )
                    },
                    onUsbReaderChanged = { reader ->
                        connectedUsbReader = reader
                    }
                )
                uiState = ReaderUiState.Result(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.printStackTrace()
                uiState = ReaderUiState.Error(e.message ?: e::class.simpleName ?: "Unknown error")
            }
        }
    }

    val usbStatusLabel = when (val reader = connectedUsbReader) {
        null -> if (usbVendorId != null && usbProductId != null) {
            "%04x:%04x (not connected)".format(usbVendorId, usbProductId)
        } else {
            "Not connected"
        }
        else -> reader.displayLabel
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            if (showSettings) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { showSettings = false },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Multipaz mDL / PhotoID Verifier",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildAnnotatedString {
                            if (useBle) {
                                append("BLE HCI: $hciDevice")
                            } else {
                                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                    append("BLE HCI: $hciDevice")
                                }
                            }
                            append("  •  USB NFC Reader: $usbStatusLabel")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showSettings) {
                    SettingsCard(
                        useBle = useBle,
                        onUseBleChanged = { useBle = it },
                        useL2capInEngagement = useL2capInEngagement,
                        onUseL2capInEngagementChanged = { useL2capInEngagement = it },
                        usePeripheralMacAddressInEngagement = usePeripheralMacAddressInEngagement,
                        onUsePeripheralMacAddressInEngagementChanged = {
                            usePeripheralMacAddressInEngagement = it
                        },
                        useReaderAuthentication = useReaderAuthentication,
                        onUseReaderAuthenticationChanged = { useReaderAuthentication = it },
                        responseTimeoutSeconds = responseTimeoutSeconds,
                        onResponseTimeoutSecondsChanged = { responseTimeoutSeconds = it },
                        hasNonDefaultSettings = hasNonDefaultSettings,
                        onResetSettings = {
                            useBle = true
                            useL2capInEngagement = true
                            usePeripheralMacAddressInEngagement = true
                            useReaderAuthentication = true
                            responseTimeoutSeconds = DEFAULT_RESPONSE_TIMEOUT_SECONDS
                        },
                    )
                } else {
                    when (val state = uiState) {
                        is ReaderUiState.Scanning -> {
                            ScanningCard(
                                statusMessage = state.statusMessage,
                                qrCodeUri = state.qrCodeUri,
                                useBle = useBle,
                                hasUsbNfcReader = connectedUsbReader != null,
                                responseTimeoutSeconds = responseTimeoutSeconds,
                                onCancel = {
                                    uiState = ReaderUiState.Scanning(INITIAL_SCANNING_MESSAGE)
                                    scanCycle++
                                }
                            )
                        }
                        is ReaderUiState.Result -> {
                            ResultCard(
                                result = state.verificationResult,
                                onDone = {
                                    uiState = ReaderUiState.Scanning(INITIAL_SCANNING_MESSAGE)
                                    scanCycle++
                                }
                            )
                        }
                        is ReaderUiState.Error -> {
                            ErrorCard(
                                errorMessage = state.message,
                                onRetry = {
                                    uiState = ReaderUiState.Scanning(INITIAL_SCANNING_MESSAGE)
                                    scanCycle++
                                }
                            )
                        }
                    }
                }
            }

            if (!showSettings) {
                OutlinedButton(
                    onClick = { showSettings = true },
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        if (hasNonDefaultSettings) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Red, CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settings")
                }
            }

            // Footer Disclaimer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                Text(
                    text = "Disclaimer: This sample does not check the issuer signing certificates against trusted IACAs and is for demo only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ScanningCard(
    statusMessage: String,
    qrCodeUri: String?,
    useBle: Boolean,
    hasUsbNfcReader: Boolean,
    responseTimeoutSeconds: Int,
    onCancel: () -> Unit,
) {
    val qrCodeBitmap = remember(qrCodeUri) {
        qrCodeUri?.let { generateQrCode(it) }
    }
    val isConnecting = if (useBle) {
        qrCodeBitmap == null || !statusMessage.startsWith("Waiting for")
    } else {
        !statusMessage.startsWith("Waiting for")
    }
    val showCancelButton = isConnecting && statusMessage != INITIAL_SCANNING_MESSAGE
    var secondsRemaining by remember(showCancelButton, responseTimeoutSeconds) {
        mutableIntStateOf(responseTimeoutSeconds)
    }

    LaunchedEffect(showCancelButton, responseTimeoutSeconds) {
        if (showCancelButton) {
            secondsRemaining = responseTimeoutSeconds
            while (secondsRemaining > 0) {
                delay(1_000)
                secondsRemaining--
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (qrCodeBitmap != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrCodeBitmap,
                        contentDescription = "Reader QR Code",
                        modifier = Modifier.size(210.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (hasUsbNfcReader) {
                        "Scan Reader QR or NFC tap to check Age over 21"
                    } else {
                        "Scan Reader QR to check Age over 21"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (hasUsbNfcReader) {
                        "Scan the QR code from your wallet app, or hold your device against the USB NFC reader"
                    } else {
                        "Scan the QR code from your wallet app (plug in a USB NFC reader at any time to also enable NFC tap)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else if (!useBle && !isConnecting) {
                Icon(
                    imageVector = Icons.Default.Contactless,
                    contentDescription = "NFC Tap",
                    modifier = Modifier.size(88.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (hasUsbNfcReader) {
                        "NFC tap to check Age over 21"
                    } else {
                        "Connect a USB NFC reader to check Age over 21"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (hasUsbNfcReader) {
                        "Hold your device against the USB NFC reader (BLE is disabled)"
                    } else {
                        "Plug in a USB NFC reader at any time to enable NFC tap (BLE is disabled)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Contactless,
                    contentDescription = "NFC Tap",
                    modifier = Modifier.size(88.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Waiting for response",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (hasUsbNfcReader) {
                            Icons.Default.Contactless
                        } else {
                            Icons.Default.QrCode2
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
            if (showCancelButton) {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel ($secondsRemaining s)")
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    result: IsoMdocVerificationResult,
    onDone: () -> Unit
) {
    var secondsRemaining by remember(result) { mutableIntStateOf(AUTO_RESET_SECONDS) }

    LaunchedEffect(result) {
        while (secondsRemaining > 0) {
            delay(1_000)
            secondsRemaining--
        }
        onDone()
    }

    val borderColor = if (result.isOlderThan21) {
        Color(0xFF2E7D32) // Green
    } else {
        Color(0xFFC62828) // Red
    }
    val statementText = if (result.isOlderThan21) {
        "This person is older than 21"
    } else {
        "This person is not older than 21"
    }

    val stats = result.transportStats
    val requestSentOnNfc = (stats.nfcHybridNumSentViaNfc ?: 0) > 0 ||
        stats.transportType == MdocTransportType.NFC_DATA_TRANSFER_MODE
    val responseOnNfcBeforeBle = if (stats.nfcHybridNumReceivedFirstOnTransport != null) {
        stats.nfcHybridNumReceivedFirstOnTransport == 0
    } else {
        stats.transportType == MdocTransportType.NFC_DATA_TRANSFER_MODE
    }
    val bleScanTimeText = stats.bleScanningTime?.let { "${it.inWholeMilliseconds} ms" } ?: "N/A"
    val bleConnectTimeText = stats.bleConnectingTime?.let { "${it.inWholeMilliseconds} ms" } ?: "N/A"
    val bleTransferTypeText = when (stats.bleUsingL2cap) {
        true -> "L2CAP"
        false -> "GATT"
        null -> "N/A"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Portrait
            if (result.portrait != null) {
                Image(
                    bitmap = result.portrait,
                    contentDescription = "Holder Portrait",
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "No Portrait",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Statement with green or red border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        border = BorderStroke(4.dp, borderColor),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 12.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statementText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = borderColor,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transport stats summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatRow(
                    label = "Total time",
                    value = "${result.totalDuration.inWholeMilliseconds} ms"
                )
                StatRow(
                    label = "DeviceRequest sent on NFC",
                    value = if (requestSentOnNfc) "Yes" else "No"
                )
                StatRow(
                    label = "DeviceResponse on NFC before BLE",
                    value = if (responseOnNfcBeforeBle) "Yes" else "No"
                )
                StatRow(
                    label = "BLE scan / connect time",
                    value = "$bleScanTimeText / $bleConnectTimeText"
                )
                StatRow(
                    label = "Bytes sent / received",
                    value = "${stats.bytesSent} / ${stats.bytesReceived} bytes"
                )
                StatRow(
                    label = "BLE data transfer",
                    value = bleTransferTypeText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(onClick = onDone) {
                Text("Done ($secondsRemaining s)")
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ErrorCard(
    errorMessage: String,
    onRetry: () -> Unit
) {
    var secondsRemaining by remember(errorMessage) { mutableIntStateOf(AUTO_RESET_SECONDS) }

    LaunchedEffect(errorMessage) {
        while (secondsRemaining > 0) {
            delay(1_000)
            secondsRemaining--
        }
        onRetry()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Verification Failed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Done ($secondsRemaining s)")
            }
        }
    }
}

@Composable
private fun SettingsCard(
    useBle: Boolean,
    onUseBleChanged: (Boolean) -> Unit,
    useL2capInEngagement: Boolean,
    onUseL2capInEngagementChanged: (Boolean) -> Unit,
    usePeripheralMacAddressInEngagement: Boolean,
    onUsePeripheralMacAddressInEngagementChanged: (Boolean) -> Unit,
    useReaderAuthentication: Boolean,
    onUseReaderAuthenticationChanged: (Boolean) -> Unit,
    responseTimeoutSeconds: Int,
    onResponseTimeoutSecondsChanged: (Int) -> Unit,
    hasNonDefaultSettings: Boolean,
    onResetSettings: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SettingCheckboxRow(
                label = "Use Bluetooth Low Energy",
                checked = useBle,
                onCheckedChange = onUseBleChanged,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingCheckboxRow(
                label = "Use L2CAP in engagement",
                checked = useL2capInEngagement,
                onCheckedChange = onUseL2capInEngagementChanged,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingCheckboxRow(
                label = "Use peripheral MAC address in engagement",
                checked = usePeripheralMacAddressInEngagement,
                onCheckedChange = onUsePeripheralMacAddressInEngagementChanged,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingCheckboxRow(
                label = "Use reader authentication",
                checked = useReaderAuthentication,
                onCheckedChange = onUseReaderAuthenticationChanged,
            )

            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Response timeout",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "$responseTimeoutSeconds s",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = responseTimeoutSeconds.toFloat(),
                    onValueChange = { value ->
                        val stepped = ((value / 5f).roundToInt() * 5).coerceIn(5, 120)
                        onResponseTimeoutSecondsChanged(stepped)
                    },
                    valueRange = 5f..120f,
                    steps = 22
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
                onClick = onResetSettings,
                enabled = hasNonDefaultSettings
            ) {
                Text("Reset Settings")
            }
        }
    }
}

@Composable
private fun SettingCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Checkbox) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
