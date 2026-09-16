package org.multipaz.pos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CancellationException
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.pos.payment.RpcPaymentSettler
import org.multipaz.pos.payment.SettlementResult
import org.multipaz.pos.payment.extractPaymentCard
import org.multipaz.pos.proximity.ProximityReaderModel
import org.multipaz.pos.proximity.VerificationProximityTransferScreen
import org.multipaz.pos.proximity.handleNfcHandover
import org.multipaz.pos.proximity.handleQrCodeScanned
import org.multipaz.pos.ui.AmountEntryScreen
import org.multipaz.pos.ui.PosScaffold
import org.multipaz.pos.ui.PosScreen
import org.multipaz.pos.ui.PosTheme
import org.multipaz.pos.ui.SettlementFailureScreen
import org.multipaz.pos.ui.SettlementSuccessScreen
import org.multipaz.pos.ui.rememberPosAppState
import org.multipaz.util.Logger
import org.multipaz.util.Platform
import kotlin.time.Clock

private const val TAG = "PosApp"

@Composable
@Preview
fun App() {
    val proximityReaderModel = remember { ProximityReaderModel() }
    val paymentSettler = remember { RpcPaymentSettler() }
    val state = rememberPosAppState()

    LaunchedEffect(Unit) {
        try {
            paymentSettler.init()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize payment settler", e)
            state.cancel(
                SettlementResult.Declined(
                    e.message ?: e.cause?.message ?: "Unknown Error Occurred"
                )
            )
        }
    }

    PosTheme {
        PromptDialogs(Platform.promptModel)

        Scaffold { paddingValues ->
            Column(
                modifier = Modifier.padding(paddingValues)
            ) {
                PosScaffold {
                    when (state.screen) {
                        PosScreen.AMOUNT_ENTRY ->
                            AmountEntryScreen(
                                onCheckout = { cents ->
                                    proximityReaderModel.reset()
                                    state.checkout(cents)
                                }
                            )

                        PosScreen.CHECKOUT -> {
                            VerificationProximityTransferScreen(
                                proximityReaderModel = proximityReaderModel,
                                paymentSettler = paymentSettler,
                                amountCents = state.settledCents,
                                promptModel = Platform.promptModel,
                                onNfcHandover = { scanResult ->
                                    handleNfcHandover(
                                        scanResult = scanResult,
                                        proximityReaderModel = proximityReaderModel
                                    )
                                },
                                onQrCodeScanned = { qrCode ->
                                    handleQrCodeScanned(
                                        mdocUrl = qrCode,
                                        proximityReaderModel = proximityReaderModel
                                    )
                                },
                                onBackClicked = {
                                    state.cancel(
                                        SettlementResult.Declined(
                                            "User Cancelled the Transaction"
                                        )
                                    )
                                },
                                onTransferComplete = { presentmentRecord, method ->
                                    try {
                                        val transactionId = paymentSettler.commit(presentmentRecord)
                                        val card = extractPaymentCard(presentmentRecord)
                                        state.settle(
                                            SettlementResult.Approved(
                                                transactionId = transactionId,
                                                amountCents = state.settledCents,
                                                method = method,
                                                timestampEpochMillis =
                                                    Clock.System.now().toEpochMilliseconds(),
                                                card = card,
                                            )
                                        )
                                    } catch (e: CancellationException) {
                                        Logger.w(TAG, "Settlement cancelled", e)
                                        state.cancel(
                                            SettlementResult.Declined(
                                                "User Cancelled the Transaction"
                                            )
                                        )
                                    } catch (e: Throwable) {
                                        Logger.w(TAG, "Settlement failed, declining sale", e)
                                        state.cancel(
                                            SettlementResult.Declined(
                                                e.message ?: e.cause?.message
                                                ?: "Unknown Error Occurred"
                                            )
                                        )
                                    }
                                },
                                onTransferError = { e ->
                                    Logger.w(TAG, "Proximity transfer failed", e)
                                    state.cancel(
                                        SettlementResult.Declined(
                                            e.message ?: e.cause?.message
                                            ?: "Unknown Error Occurred"
                                        )
                                    )
                                },
                            )
                        }

                        PosScreen.SETTLEMENT ->
                            SettlementSuccessScreen(
                                amountCents = state.settlementAmountCents,
                                onNewTransaction = state::newTransaction,
                                card = state.lastSettlement?.card,
                            )

                        PosScreen.FAILURE ->
                            SettlementFailureScreen(
                                failure = state.lastFailure
                                    ?: SettlementResult.Declined("Unknown Failure"),
                                amountCents = state.settledCents,
                                onNewTransaction = state::newTransaction,
                            )
                    }
                }
            }
        }
    }
}