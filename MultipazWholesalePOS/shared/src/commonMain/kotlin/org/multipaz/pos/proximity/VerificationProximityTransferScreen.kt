package org.multipaz.pos.proximity

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SettingsBluetooth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.multipaz.cbor.DataItem
import org.multipaz.compose.camera.CameraCaptureResolution
import org.multipaz.compose.camera.CameraSelection
import org.multipaz.compose.permissions.rememberBluetoothEnabledState
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import org.multipaz.compose.permissions.rememberCameraPermissionState
import org.multipaz.compose.qrcode.QrCodeScanner
import org.multipaz.crypto.SecurityException
import org.multipaz.documenttype.knowntypes.PaymentTransaction
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.nfc.MdocReaderNfcHandoverOptions
import org.multipaz.mdoc.nfc.ScanMdocReaderResult
import org.multipaz.mdoc.nfc.scanMdocReader
import org.multipaz.mdoc.request.DeviceRequest
import org.multipaz.mdoc.request.DocRequestInfo
import org.multipaz.mdoc.request.TransactionsInfo
import org.multipaz.mdoc.request.buildDeviceRequest
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.nfc.NfcTagReader
import org.multipaz.pos.Constants
import org.multipaz.pos.payment.PaymentMethod
import org.multipaz.pos.payment.ProximityScanMode
import org.multipaz.pos.payment.RpcPaymentSettler
import org.multipaz.pos.ui.PosTheme
import org.multipaz.pos.ui.type
import org.multipaz.prompt.PromptModel
import org.multipaz.util.Logger
import org.multipaz.util.UUID
import org.multipaz.utopia.knowntypes.DigitalPaymentCredential
import org.multipaz.verification.Iso18013PresentmentRecord

@Composable
fun VerificationProximityTransferScreen(
    proximityReaderModel: ProximityReaderModel,
    promptModel: PromptModel,
    paymentSettler: RpcPaymentSettler,
    amountCents: Long,
    onBackClicked: () -> Unit,
    onTransferComplete: suspend (presentmentRecord: Iso18013PresentmentRecord, method: PaymentMethod) -> Unit,
    onTransferError: (error: Throwable) -> Unit,
    onNfcHandover: (suspend (ScanMdocReaderResult) -> Unit)? = null,
    onQrCodeScanned: (suspend (String) -> Unit)? = null,
) {
    val blePermissionState = rememberBluetoothPermissionState()
    val bleEnabledState = rememberBluetoothEnabledState()

    val coroutineScope = rememberCoroutineScope { promptModel }
    val scrollState = rememberScrollState()

    var scanMode by remember { mutableStateOf<ProximityScanMode>(ProximityScanMode.NFC) }

    val proximityReaderModelState = proximityReaderModel.state.collectAsState().value

    LaunchedEffect(proximityReaderModelState) {
        when (proximityReaderModelState) {
            ProximityReaderModel.State.WAITING_FOR_DEVICE_REQUEST -> {
                val sessionTranscript = try {
                    proximityReaderModel.sessionTranscript
                } catch (e: Exception) {
                    Logger.w(TAG, "Session transcript not available", e)
                    return@LaunchedEffect
                }

                try {
                    proximityReaderModel.setDeviceRequest(
                        deviceRequest = createPaymentDeviceRequest(
                            paymentSettler = paymentSettler,
                            amountCents = amountCents,
                            sessionTranscript = sessionTranscript,
                        )
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e(TAG, "Error creating device request", e)
                    onTransferError(e)
                }
            }

            ProximityReaderModel.State.WAITING_FOR_START -> {
                proximityReaderModel.start(coroutineScope)
            }

            ProximityReaderModel.State.COMPLETED -> {
                handleTransferOutcome(
                    outcome = proximityReaderModel.outcome,
                    method = when (scanMode) {
                        ProximityScanMode.NFC -> PaymentMethod.NFC
                        ProximityScanMode.QR -> PaymentMethod.QR_CODE
                    },
                    onTransferComplete = onTransferComplete,
                    onTransferError = onTransferError,
                )
            }

            else -> {}
        }
    }

    val nfcTagReader = NfcTagReader.getReaders().firstOrNull()
    LaunchedEffect(scanMode) {
        if (proximityReaderModel.state.value == ProximityReaderModel.State.IDLE && scanMode == ProximityScanMode.NFC && onNfcHandover != null) {
            if (nfcTagReader != null && !nfcTagReader.dialogAlwaysShown) {
                withContext(promptModel) {
                    while (isActive) {
                        try {
                            val scanResult = nfcTagReader.scanMdocReader(
                                message = null,
                                options = MdocTransportOptions(
                                    bleUseL2CAP = false,               // Doesn't work with Apple Wallet
                                    bleUseL2CAPInEngagement = true
                                ),
                                handoverOptions = MdocReaderNfcHandoverOptions(
                                    useNfcV2 = false
                                ),
                                selectConnectionMethod = { connectionMethods -> connectionMethods.first() },
                                negotiatedHandoverConnectionMethods = listOf(
                                    MdocConnectionMethodBle(
                                        supportsPeripheralServerMode = false,
                                        supportsCentralClientMode = true,
                                        peripheralServerModeUuid = null,
                                        centralClientModeUuid = UUID.randomUUID(),
                                    )
                                ),
                                onHandover = { scanResult ->
                                    onNfcHandover(scanResult)
                                    scanResult
                                })
                            if (scanResult != null) {
                                break
                            }
                        } catch (e: Throwable) {
                            if (!isActive) {
                                Logger.e(
                                    TAG, "Caught exception while scanning and scope isn't active", e
                                )
                                break
                            } else if (e is SecurityException) {
                                Logger.e(TAG, "SecurityException while scanning, stopping scan", e)
                                break
                            } else {
                                Logger.e(TAG, "Caught exception while scanning. Retrying", e)
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Spacer(Modifier.weight(1f))

        if (!blePermissionState.isGranted) {
            PermissionButton(
                text = "Request BLE permissions",
                icon = Icons.Filled.SettingsBluetooth,
                onClick = {
                    coroutineScope.launch {
                        blePermissionState.launchPermissionRequest()
                    }
                })
        } else if (!bleEnabledState.isEnabled) {
            PermissionButton(
                text = "Enable Bluetooth",
                icon = Icons.Filled.BluetoothConnected,
                onClick = {
                    coroutineScope.launch {
                        bleEnabledState.enable()
                    }
                })
        } else if (proximityReaderModelState == ProximityReaderModel.State.IDLE) {
            if (scanMode == ProximityScanMode.NFC) {
                NfcCard()
            } else if (scanMode == ProximityScanMode.QR) {
                QrCard(
                    onQrCodeScanned = { qrCode ->
                        if (qrCode?.startsWith("mdoc:") == true && onQrCodeScanned != null) {
                            if (proximityReaderModel.state.value == ProximityReaderModel.State.IDLE) {
                                coroutineScope.launch {
                                    onQrCodeScanned(qrCode)
                                }
                            }
                        }
                    })
            }

            ToggleButton(scanMode, {
                if (scanMode == ProximityScanMode.NFC)
                    scanMode = ProximityScanMode.QR
                else
                    scanMode = ProximityScanMode.NFC
            })
        } else {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Text(
                text = "Waiting for response",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.weight(1f))

        CheckoutFooter(
            onCancel = onBackClicked
        )
    }
}

/**
 * Registers a transaction for [amountCents] with [paymentSettler] and builds the DeviceRequest which
 * asks the holder for a payment credential bound to it.
 *
 * The transaction is carried in the doc request so the holder signs over the amount and payee it is
 * about to authorize; [sessionTranscript] binds the request to this proximity session.
 */
private suspend fun createPaymentDeviceRequest(
    paymentSettler: RpcPaymentSettler,
    amountCents: Long,
    sessionTranscript: DataItem,
): DeviceRequest {
    val transactionId = paymentSettler.createTransaction(amountCents = amountCents).transactionId

    val payload = PaymentTransaction.Payload(
        transactionId = transactionId,
        currency = Constants.TERMINAL_CURRENCY,
        amount = amountCents / 100.0,
        payee = PaymentTransaction.Payee(
            name = Constants.TERMINAL_PAYEE_NAME,
            id = Constants.TERMINAL_PAYEE_ID,
        ),
    )
    val transactionSerialized: DataItem = PaymentTransaction.serializeCbor(payload)

    return buildDeviceRequest(
        sessionTranscript = sessionTranscript
    ) {
        addDocRequest(
            docType = DigitalPaymentCredential.CARD_DOCTYPE,
            nameSpaces = mapOf(
                DigitalPaymentCredential.CARD_NAMESPACE to listOf(
                    "issuer_name",
                    "holder_name",
                    "masked_account_reference",
                    "payment_instrument_id",
                    "issue_date",
                    "expiry_date",
                ).associateWith { false }),
            docRequestInfo = DocRequestInfo(
                transactions = TransactionsInfo(
                    data = mapOf(
                        PaymentTransaction.mdocRequestInfoIdentifier to transactionSerialized
                    )
                )
            ),
        )
    }
}


/**
 * Delivers the outcome of a completed proximity transfer, reporting the failure to
 * [onTransferError] or handing the resulting presentment record to [onTransferComplete]
 */
private suspend fun handleTransferOutcome(
    outcome: ProximityReaderOutcome?,
    method: PaymentMethod,
    onTransferComplete: suspend (presentmentRecord: Iso18013PresentmentRecord, method: PaymentMethod) -> Unit,
    onTransferError: (error: Throwable) -> Unit,
) {
    when (outcome) {
        // The session was canceled before producing an outcome, so there is nothing to report.
        null -> {}

        is ProximityReaderOutcome.Failure -> onTransferError(outcome.error)

        is ProximityReaderOutcome.Success -> {
            try {
                val presentmentRecord = Iso18013PresentmentRecord(
                    response = outcome.deviceResponse.toDataItem(),
                    sessionTranscript = outcome.sessionTranscript,
                    request = outcome.deviceRequest.toDataItem(),
                    eDeviceKey = outcome.eReaderKey,
                    encryptionInfo = null,
                    origin = null
                )
                onTransferComplete(presentmentRecord, method)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Logger.w(TAG, "Error completing transfer", e)
                onTransferError(e)
            }
        }
    }
}

@Composable
private fun QrCard(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (qrCode: String?) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val cameraPermissionState = rememberCameraPermissionState()

    PaymentCard(modifier) {
        if (!cameraPermissionState.isGranted) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PermissionButton(
                    text = "Request Camera permissions",
                    icon = Icons.Filled.Camera,
                    onClick = {
                        coroutineScope.launch {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    })
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().dropShadow(
                    shape = RoundedCornerShape(16.dp), shadow = Shadow(
                        radius = 10.dp,
                        spread = 7.5.dp,
                        color = Color.Black.copy(alpha = 0.15f),
                        offset = DpOffset(x = 0.dp, 2.dp)
                    )
                ).clip(RoundedCornerShape(16.dp))
            ) {
                QrCodeScanner(
                    modifier = Modifier.fillMaxSize(),
                    cameraSelection = CameraSelection.DEFAULT_BACK_CAMERA,
                    captureResolution = CameraCaptureResolution.HIGH,
                    showCameraPreview = true,
                    onCodeScanned = { qrCode ->
                        onQrCodeScanned(qrCode)
                    })
            }
        }
    }
}

@Composable
private fun NfcCard(modifier: Modifier = Modifier, onTap: (() -> Unit)? = null) {
    val c = PosTheme.colors
    val type = PosTheme.type
    val t = rememberInfiniteTransition(label = "nfc")
    val pulse by t.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "nfcPulse",
    )
    PaymentCard(modifier, onClick = onTap) {
        Box(
            Modifier
                .width(220.dp)
                .aspectRatio(1f)
                .clip(CircleShape)
                .border(4.dp, c.primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Contactless,
                contentDescription = null,
                tint = c.primary.copy(alpha = pulse),
                modifier = Modifier.size(110.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("READY FOR TAP", style = type.headlineMd, color = c.primary)
        Text(
            "HOLD CARD OR DEVICE NEAR TERMINAL",
            style = type.labelSm,
            color = c.onSurface.copy(alpha = 0.6f),
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun PermissionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val c = PosTheme.colors
    val type = PosTheme.type
    Row(
        Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(c.surfaceContainerLow)
            .border(1.dp, c.primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(16.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = c.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = type.headlineMd,
            color = c.primary
        )
    }
}

@Composable
private fun ToggleButton(
    scanMode: ProximityScanMode,
    onClick: () -> Unit
) {
    val c = PosTheme.colors
    val type = PosTheme.type
    Row(
        Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(c.surfaceContainerLow)
            .border(1.dp, c.primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(16.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (scanMode == ProximityScanMode.NFC) Icons.Filled.QrCodeScanner else Icons.Filled.Nfc,
            contentDescription = null,
            tint = c.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            if (scanMode == ProximityScanMode.NFC) "Switch to QR Code" else "Switch to NFC",
            style = type.headlineMd,
            color = c.primary
        )
    }
}

@Composable
private fun PaymentCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = PosTheme.colors
    Column(
        modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(c.surfaceContainerLow)
            .border(1.dp, c.outlineVariant, RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun CheckoutFooter(onCancel: () -> Unit) {
    val c = PosTheme.colors
    val type = PosTheme.type
    Column(
        Modifier.fillMaxWidth().widthIn(max = 520.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Cancel
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(c.errorContainer)
                .border(1.dp, c.error.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .clickable(onClick = onCancel),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Cancel,
                contentDescription = null,
                tint = c.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text("CANCEL TRANSACTION", style = type.headlineMd, color = c.onErrorContainer)
        }
    }
}