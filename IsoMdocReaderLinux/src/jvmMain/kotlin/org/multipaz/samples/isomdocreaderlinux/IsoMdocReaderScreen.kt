package org.multipaz.samples.isomdocreaderlinux

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.multipaz.compose.qrcode.generateQrCode

private sealed interface ReaderUiState {
    data class Scanning(
        val statusMessage: String,
        val qrCodeUri: String? = null,
    ) : ReaderUiState
    data class Result(val verificationResult: IsoMdocVerificationResult) : ReaderUiState
    data class Error(val message: String) : ReaderUiState
}

private const val AUTO_RESET_SECONDS = 10
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

    LaunchedEffect(session, scanCycle) {
        if (uiState is ReaderUiState.Scanning) {
            try {
                var currentStatus = INITIAL_SCANNING_MESSAGE
                var currentQrUri: String? = null
                val result = session.readAndVerifyIsoMdoc(
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
                    text = "BLE HCI: $hciDevice  •  USB NFC Reader: $usbStatusLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (val state = uiState) {
                    is ReaderUiState.Scanning -> {
                        ScanningCard(
                            statusMessage = state.statusMessage,
                            qrCodeUri = state.qrCodeUri,
                            hasUsbNfcReader = connectedUsbReader != null,
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
    hasUsbNfcReader: Boolean,
    onCancel: () -> Unit,
) {
    val qrCodeBitmap = remember(qrCodeUri) {
        qrCodeUri?.let { generateQrCode(it) }
    }
    val isConnecting = qrCodeBitmap == null || !statusMessage.startsWith("Waiting for")
    val showCancelButton = isConnecting && statusMessage != INITIAL_SCANNING_MESSAGE

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
            } else {
                Icon(
                    imageVector = Icons.Default.Contactless,
                    contentDescription = "NFC Tap",
                    modifier = Modifier.size(88.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Verifying ISO mdoc",
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
                    Text("Cancel")
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
            // Portrait
            if (result.portrait != null) {
                Image(
                    bitmap = result.portrait,
                    contentDescription = "Holder Portrait",
                    modifier = Modifier
                        .size(220.dp)
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
                        .size(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "No Portrait",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Statement with green or red border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        border = BorderStroke(4.dp, borderColor),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 18.dp, horizontal = 24.dp),
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

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = onDone) {
                Text("Done ($secondsRemaining s)")
            }
        }
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
