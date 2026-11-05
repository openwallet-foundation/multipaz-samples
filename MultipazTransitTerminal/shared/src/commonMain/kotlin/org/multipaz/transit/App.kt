package org.multipaz.transit

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.transit.journey.RpcJourneyStore
import org.multipaz.transit.journey.Journey
import org.multipaz.transit.payment.AgeDiscount
import org.multipaz.transit.payment.RpcPaymentSettler
import org.multipaz.transit.payment.SettlementResult
import org.multipaz.transit.payment.extractPresentedClaims
import org.multipaz.transit.proximity.GateTap
import org.multipaz.transit.proximity.ProximityReaderModel
import org.multipaz.transit.proximity.ProximityScreen
import org.multipaz.transit.proximity.handleNfcHandover
import org.multipaz.transit.ui.DocPhase
import org.multipaz.transit.ui.SettlementFailureScreen
import org.multipaz.transit.ui.SettlementSuccessScreen
import org.multipaz.transit.ui.Station
import org.multipaz.transit.ui.StationSelectionScreen
import org.multipaz.transit.ui.TransitScaffold
import org.multipaz.transit.ui.TransitScreen
import org.multipaz.transit.ui.TransitTheme
import org.multipaz.transit.ui.getStationList
import org.multipaz.transit.ui.rememberAppState
import org.multipaz.util.Logger
import org.multipaz.util.Platform
import kotlin.time.Clock

private const val TAG = "TransitTerminal"

@Composable
@Preview
fun App() {
    val proximityReaderModel = remember { ProximityReaderModel() }
    val paymentSettler = remember { RpcPaymentSettler() }
    val journeyStore = remember { RpcJourneyStore() }
    val state = rememberAppState()
    val coroutineScope = rememberCoroutineScope()
    val readerState by proximityReaderModel.state.collectAsState()

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

    // The document chips track the reader, so both credentials light up the moment the
    // wallet starts answering and turn green together once the fare settles.
    val docPhase = when (state.screen) {
        TransitScreen.CHECK_IN -> if (readerState == ProximityReaderModel.State.IDLE) DocPhase.IDLE else DocPhase.READING
        TransitScreen.SELECTION -> DocPhase.IDLE
        TransitScreen.CHECKOUT ->
            if (readerState == ProximityReaderModel.State.IDLE) DocPhase.IDLE else DocPhase.READING

        TransitScreen.SETTLEMENT -> DocPhase.DONE
        TransitScreen.FAILURE -> DocPhase.FAILED
    }

    TransitTheme {
        PromptDialogs(Platform.promptModel)

        Scaffold { paddingValues ->
            TransitScaffold(
                destination = state.destination,
                discount = state.fareDiscount,
                docPhase = docPhase,
                journeyResumeNotice = state.journeyResumeNotice,
                modifier = Modifier.padding(paddingValues),
            ) {
                when (state.screen) {
                    TransitScreen.CHECK_IN ->
                        ProximityScreen(
                            proximityReaderModel = proximityReaderModel,
                            paymentSettler = paymentSettler,
                            amountCents = null, // non amount-binding requst
                            gateTap = GateTap.ENTRY,
                            onNfcHandover = { scanResult ->
                                handleNfcHandover(
                                    scanResult = scanResult,
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
                            onTransferComplete = { presentmentRecord ->
                                try {
                                    val claims = extractPresentedClaims(presentmentRecord)
                                    val paymentInstrumentId = requireNotNull(
                                        claims.card?.paymentInstrumentId
                                    ) { "The payment credential did not provide an instrument id" }
                                    val journey = journeyStore.checkIn(
                                        paymentInstrumentId = paymentInstrumentId,
                                        entryStation = Constants.ENTRY_STATION,
                                        fareClass = claims.discount.fareClass,
                                    )
                                    when (journey.state) {
                                        "OPEN" -> state.checkin(claims)
                                        "READY_FOR_CHECKOUT" -> {
                                            proximityReaderModel.reset()
                                            state.resumeCheckout(
                                                presentedClaims = claims,
                                                journey = journey
                                            )
                                        }

                                        else -> throw IllegalStateException(
                                            "Journey cannot resume from state ${journey.state}"
                                        )
                                    }
                                } catch (e: CancellationException) {
                                    Logger.w(TAG, "Settlement cancelled", e)
                                    state.cancel(
                                        SettlementResult.Declined(
                                            "User Cancelled the Transaction"
                                        )
                                    )
                                } catch (e: Throwable) {
                                    Logger.w(
                                        TAG,
                                        "Settlement failed, declining sale",
                                        e
                                    )
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

                    TransitScreen.SELECTION ->
                        StationSelectionScreen(
                            discount = state.fareDiscount,
                            onStationSelected = { station ->
                                // The selection callback is not suspend, so the next screen is
                                // entered only after the backend has durably recorded the fare.
                                coroutineScope.launch {
                                    try {
                                        val paymentInstrumentId = requireNotNull(
                                            state.claims?.card?.paymentInstrumentId
                                        ) { "The payment credential did not provide an instrument id" }

                                        journeyStore.selectExit(
                                            paymentInstrumentId = paymentInstrumentId,
                                            exitStation = station.name,
                                            fareCents = station.fareCents,
                                        )
                                        proximityReaderModel.reset()
                                        state.checkout(station)
                                    } catch (e: CancellationException) {
                                        Logger.w(TAG, "Settlement cancelled", e)
                                        state.cancel(
                                            SettlementResult.Declined(
                                                "User Cancelled the Transaction"
                                            )
                                        )
                                    } catch (e: Throwable) {
                                        Logger.w(TAG, "Could not store journey destination", e)
                                        state.cancel(
                                            SettlementResult.Declined(
                                                e.message ?: "Could not store journey"
                                            )
                                        )
                                    }
                                }
                            }
                        )

                    TransitScreen.CHECKOUT -> {
                        ProximityScreen(
                            proximityReaderModel = proximityReaderModel,
                            paymentSettler = paymentSettler,
                            amountCents = state.settledCents,
                            gateTap = GateTap.EXIT,
                            onNfcHandover = { scanResult ->
                                handleNfcHandover(
                                    scanResult = scanResult,
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
                            onTransferComplete = { presentmentRecord ->
                                try {
                                    val claims = extractPresentedClaims(presentmentRecord)
                                    val exitCard = requireNotNull(claims.card) {
                                        "The payment credential did not provide an instrument id"
                                    }
                                    val paymentInstrumentId =
                                        requireNotNull(exitCard.paymentInstrumentId) {
                                            "The payment credential did not provide an instrument id"
                                        }

                                    if (paymentInstrumentId != state.claims?.card?.paymentInstrumentId)
                                        throw IllegalStateException("Cards doesn't match!")

                                    journeyStore.beginCheckout(paymentInstrumentId)

                                    val transactionId = paymentSettler.commit(presentmentRecord)
                                    journeyStore.settle(paymentInstrumentId)
                                    state.settle(
                                        SettlementResult.Approved(
                                            transactionId = transactionId,
                                            amountCents = state.settledCents,
                                            timestampEpochMillis =
                                                Clock.System.now().toEpochMilliseconds(),
                                            card = exitCard,
                                            age = state.fareDiscount,
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
                                    Logger.w(
                                        TAG,
                                        "Settlement failed, declining sale",
                                        e
                                    )
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

                    TransitScreen.SETTLEMENT ->
                        SettlementSuccessScreen(
                            amountCents = state.settlementAmountCents,
                            destination = state.destination,
                            onNewTransaction = {
                                proximityReaderModel.reset()
                                state.newTransaction()
                            },
                            card = state.lastSettlement?.card,
                            age = state.lastSettlement?.age
                                ?: AgeDiscount.None,
                        )

                    TransitScreen.FAILURE ->
                        SettlementFailureScreen(
                            failure = state.lastFailure
                                ?: SettlementResult.Declined("Unknown Failure"),
                            amountCents = state.settledCents,
                            destination = state.destination,
                            onNewTransaction = {
                                proximityReaderModel.reset()
                                state.newTransaction()
                            },
                        )
                }
            }
        }
    }
}
