package org.multipaz.transit.proximity

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.multipaz.cbor.DataItem
import org.multipaz.documenttype.ISO_18013_TRANSACTION_DATA_NAMESPACE
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.documenttype.knowntypes.EUPersonalID
import org.multipaz.documenttype.knowntypes.PaymentTransaction
import org.multipaz.documenttype.knowntypes.PhotoID
import org.multipaz.mdoc.nfc.MdocReaderNfcHandoverOptions
import org.multipaz.mdoc.nfc.ScanMdocReaderResult
import org.multipaz.mdoc.nfc.scanMdocReader
import org.multipaz.mdoc.request.DeviceRequest
import org.multipaz.mdoc.request.DeviceRequestInfo
import org.multipaz.mdoc.request.DocRequestInfo
import org.multipaz.mdoc.request.DocumentSet
import org.multipaz.mdoc.request.TransactionsInfo
import org.multipaz.mdoc.request.UseCase
import org.multipaz.mdoc.request.buildDeviceRequest
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.nfc.NfcTagReader
import org.multipaz.transit.Constants
import org.multipaz.transit.payment.RpcPaymentSettler
import org.multipaz.transit.ui.GhostButton
import org.multipaz.transit.ui.TransitTheme
import org.multipaz.transit.ui.type
import org.multipaz.util.Logger
import org.multipaz.util.Platform
import org.multipaz.utopia.knowntypes.DigitalPaymentCredential
import org.multipaz.verification.Iso18013PresentmentRecord

enum class GateTap { ENTRY, EXIT }

@Composable
fun ProximityScreen(
    proximityReaderModel: ProximityReaderModel,
    paymentSettler: RpcPaymentSettler,
    amountCents: Long? = null,
    gateTap: GateTap,
    onBackClicked: () -> Unit,
    onTransferComplete: suspend (presentmentRecord: Iso18013PresentmentRecord) -> Unit,
    onTransferError: (error: Throwable) -> Unit,
    onNfcHandover: (suspend (ScanMdocReaderResult) -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope { Platform.promptModel }

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
                    val deviceRequest = if (amountCents == null) {
                        createCheckInRequest(
                            sessionTranscript = sessionTranscript,
                        )
                    } else {
                        createCheckoutRequest(
                            paymentSettler = paymentSettler,
                            amountCents = amountCents,
                            sessionTranscript = sessionTranscript,
                        )
                    }

                    proximityReaderModel.setDeviceRequest(
                        deviceRequest = deviceRequest
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
                    onTransferComplete = onTransferComplete,
                    onTransferError = onTransferError,
                )
            }

            else -> {}
        }
    }

    val nfcTagReader = NfcTagReader.getReaders().firstOrNull()

    LaunchedEffect(Unit) {
        if (proximityReaderModel.state.value == ProximityReaderModel.State.IDLE && onNfcHandover != null) {
            if (nfcTagReader != null) {
                withContext(Platform.promptModel) {
                    while (isActive) {
                        try {
                            val scanResult = nfcTagReader.scanMdocReader(
                                message = if (nfcTagReader.dialogAlwaysShown) {
                                    "Hold your device near the terminal"
                                } else {
                                    null
                                },
                                options = MdocTransportOptions(),
                                handoverOptions = MdocReaderNfcHandoverOptions(
                                    useNfcV2 = true
                                ),
                                selectConnectionMethod = { connectionMethods ->
                                    connectionMethods.singleOrNull()
                                },
                                negotiatedHandoverConnectionMethods = emptyList(),
                                onHandover = { scanResult ->
                                    onNfcHandover(scanResult)
                                    scanResult
                                })
                            if (scanResult != null) {
                                break
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Logger.e(TAG, "Error scanning, stopping scan", e)
                            onTransferError(e)
                            break
                        }
                    }
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 26.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                proximityReaderModelState != ProximityReaderModel.State.IDLE -> ReadingState(gateTap)
                else -> TapAtGate(gateTap)
            }
        }

        CheckoutFooter(onCancel = onBackClicked)
    }
}

private suspend fun createCheckInRequest(
    sessionTranscript: DataItem,
): DeviceRequest {
    return buildDeviceRequest(
        sessionTranscript = sessionTranscript,
        deviceRequestInfo = DeviceRequestInfo.fromValues(
            useCases = listOf(
                // DPC
                UseCase(
                    mandatory = true,
                    documentSets = listOf(
                        DocumentSet(listOf(0))
                    ),
                    purposeHints = emptyMap()
                ),
                // + mdl OR photoid OR pid (optionally)
                UseCase(
                    mandatory = false,
                    documentSets = listOf(
                        DocumentSet(listOf(1)),
                        DocumentSet(listOf(2)),
                        DocumentSet(listOf(3))
                    ),
                    purposeHints = emptyMap()
                )
            )
        )
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
                ).associateWith { false }
            ),
        )
        addDocRequest(
            docType = DrivingLicense.MDL_DOCTYPE,
            nameSpaces = mapOf(
                DrivingLicense.MDL_NAMESPACE to listOf(
                    "age_over_18",
                    "age_over_65",
                    "age_in_years",
                    "birth_date"
                ).associateWith { false }
            )
        )
        addDocRequest(
            docType = PhotoID.PHOTO_ID_DOCTYPE,
            nameSpaces = mapOf(
                PhotoID.ISO_23220_2_NAMESPACE to listOf(
                    "age_over_18",
                    "age_over_65",
                    "age_in_years",
                    "birth_date"
                ).associateWith { false }
            )
        )
        addDocRequest(
            docType = EUPersonalID.EUPID_DOCTYPE,
            nameSpaces = mapOf(
                EUPersonalID.EUPID_NAMESPACE to listOf(
                    "age_over_18",
                    "age_in_years",
                    "birth_date"
                ).associateWith { false }
            )
        )
        // addReaderAuthAll(key)
    }
}

/**
 * Registers a transaction for [amountCents] with [paymentSettler] and builds the DeviceRequest which
 * asks the holder for a payment credential bound to it.
 *
 * The transaction is carried in the doc request so the holder signs over the amount and payee it is
 * about to authorize; [sessionTranscript] binds the request to this proximity session.
 */
private suspend fun createCheckoutRequest(
    paymentSettler: RpcPaymentSettler,
    amountCents: Long,
    sessionTranscript: DataItem,
): DeviceRequest {
    val transactionId =
        paymentSettler.createTransaction(amountCents = amountCents).transactionId

    val payload = PaymentTransaction.Payload(
        transactionId = transactionId,
        currency = Constants.TERMINAL_CURRENCY,
        amount = amountCents / 100.0,
        payee = PaymentTransaction.Payee(
            name = Constants.TERMINAL_PAYEE_NAME,
            id = Constants.TERMINAL_PAYEE_ID,
        ),
    )

    val transactionSerialized: DataItem = PaymentTransaction.serializeIso18013Request(payload)

    return buildDeviceRequest(
        sessionTranscript = sessionTranscript,
        deviceRequestInfo = DeviceRequestInfo.fromValues(
            useCases = listOf(
                // DPC
                UseCase(
                    mandatory = true,
                    documentSets = listOf(
                        DocumentSet(listOf(0))
                    ),
                    purposeHints = emptyMap()
                ),
            )
        )
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
                ).associateWith { false },
                ISO_18013_TRANSACTION_DATA_NAMESPACE to mapOf(PaymentTransaction.identifier to true)
            ),
            docRequestInfo =
                DocRequestInfo(
                    transactionData = TransactionsInfo(
                        data = mapOf(
                            PaymentTransaction.identifier to transactionSerialized
                        )
                    )
                )
        )
        // addReaderAuthAll(key)
    }
}


/**
 * Delivers the outcome of a completed proximity transfer, reporting the failure to
 * [onTransferError] or handing the resulting presentment record to [onTransferComplete]
 */
private suspend fun handleTransferOutcome(
    outcome: ProximityReaderOutcome?,
    onTransferComplete: suspend (presentmentRecord: Iso18013PresentmentRecord) -> Unit,
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
                onTransferComplete(presentmentRecord)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Logger.w(TAG, "Error completing transfer", e)
                onTransferError(e)
            }
        }
    }
}

/**
 * The resting state of the gate: a breathing NFC target with rings pulsing outward from it. The
 * whole point of the screen is that one tap covers both the age check and the fare, so that is
 * the only sentence on it.
 */
@Composable
private fun TapAtGate(gateTap: GateTap) {
    val c = TransitTheme.colors
    val type = TransitTheme.type
    val t = rememberInfiniteTransition(label = "tap")
    val ringA by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "ringA",
    )
    val ringB by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = LinearEasing),
            initialStartOffset = StartOffset(1200),
        ),
        label = "ringB",
    )
    val breathe by t.animateFloat(
        initialValue = 1f, targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "breathe",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(164.dp), contentAlignment = Alignment.Center) {
            PulseRing(ringA)
            PulseRing(ringB)
            Box(
                Modifier
                    .size(102.dp)
                    .graphicsLayer { scaleX = breathe; scaleY = breathe }
                    .clip(CircleShape)
                    .background(c.tapFill)
                    .border(1.dp, c.tapBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(c.primary))
            }
        }
        Spacer(Modifier.height(26.dp))
        Text(
            if (gateTap == GateTap.ENTRY) "Tap to check in" else "Tap to check out",
            style = type.headline,
            color = c.ink,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            if (gateTap == GateTap.ENTRY)
                "Tap at the entry gate to start the journey."
            else
                "Tap at the exit gate to calculate and pay the fare.",
            style = type.body,
            color = c.ink.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
        )
    }
}

/** One outward pulse: expands past the target and fades as it goes. */
@Composable
private fun BoxScope.PulseRing(progress: Float) {
    val c = TransitTheme.colors
    // Fades in over the first fifth of the travel, then out across the rest.
    val alpha = if (progress < 0.22f) {
        0.55f * (progress / 0.22f)
    } else {
        0.55f * (1f - (progress - 0.22f) / 0.78f)
    }
    val scale = 0.62f + (1.32f - 0.62f) * progress
    Box(
        Modifier
            .matchParentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .border(1.dp, c.primary, CircleShape)
    )
}

/** The wallet is answering: a filled target under a sweeping arc, plus a moving read bar. */
@Composable
private fun ReadingState(gateTap: GateTap) {
    val c = TransitTheme.colors
    val type = TransitTheme.type
    val t = rememberInfiniteTransition(label = "reading")
    val sweep by t.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "sweep",
    )
    val fill by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500)),
        label = "fill",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(164.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(134.dp).rotate(sweep)) {
                val stroke = 2.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawCircle(
                    color = c.tapBorder,
                    radius = (size.minDimension - stroke) / 2f,
                    style = Stroke(width = stroke),
                )
                drawArc(
                    color = c.primary,
                    startAngle = -90f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
            }
            Box(Modifier.size(102.dp).clip(CircleShape).background(c.primary))
        }
        Spacer(Modifier.height(26.dp))
        Text(
            if (gateTap == GateTap.ENTRY) "Checking in…" else "Checking out…",
            style = type.headline,
            color = c.ink,
        )
        Spacer(Modifier.height(7.dp))
        Text("Keep holding still", style = type.body, color = c.ink.copy(alpha = 0.55f))
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .width(160.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(c.track),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fill)
                    .background(c.primary)
            )
        }
    }
}

/** Quiet controls the rider only needs if the tap is not working out. */
@Composable
private fun CheckoutFooter(
    onCancel: () -> Unit,
) {
    val c = TransitTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GhostButton(
            label = "Cancel",
            color = c.error,
            modifier = Modifier.weight(1f),
            onClick = onCancel,
        )
    }
}
