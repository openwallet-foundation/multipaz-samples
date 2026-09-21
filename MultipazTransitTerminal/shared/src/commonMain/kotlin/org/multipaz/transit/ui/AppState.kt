package org.multipaz.transit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.multipaz.transit.journey.Journey
import org.multipaz.transit.payment.AgeDiscount
import org.multipaz.transit.payment.PresentedClaims
import org.multipaz.transit.payment.SettlementResult

/** The high-level screens of the faregate. */
enum class TransitScreen { CHECK_IN, SELECTION, CHECKOUT, SETTLEMENT, FAILURE }

/**
 * Drives the faregate flow: which [TransitScreen] is showing, the destination the rider picked
 * (and therefore the fare carried into settlement), and the settlement result. A plain Compose
 * state holder with no Android dependency, so the transitions can be unit-tested in commonTest.
 */
@Stable
class AppState {
    var screen by mutableStateOf(TransitScreen.CHECK_IN)
        private set

    /** The simulated destination, or null until the entry tap has completed. */
    var destination by mutableStateOf<Station?>(null)
        private set

    var claims by mutableStateOf<PresentedClaims?>(null)
        private set

    /** Fare entitlement captured when this journey was first created or restored. */
    var fareDiscount : AgeDiscount by mutableStateOf(AgeDiscount.None)
        private set

    var settledCents by mutableLongStateOf(0L)
        private set

    /** A short explanation shown when an existing backend journey is restored. */
    var journeyResumeNotice by mutableStateOf<String?>(null)
        private set

    /** The approved settlement backing the confirmation screen, or null before one exists. */
    var lastSettlement by mutableStateOf<SettlementResult.Approved?>(null)
        private set

    /** The decline backing the failure screen, or null while nothing has failed. */
    var lastFailure by mutableStateOf<SettlementResult.Declined?>(null)
        private set

    /** Amount shown on the settlement screen. */
    val settlementAmountCents: Long
        get() = lastSettlement?.amountCents ?: settledCents

    /** Entry tap complete → briefly simulate the rider's destination. */
    fun checkin(presentedClaims: PresentedClaims) {
        claims = presentedClaims
        fareDiscount = presentedClaims.discount
        journeyResumeNotice = null
        screen = TransitScreen.SELECTION
    }

    /** Simulation complete → wait for the same rider at the exit gate. */
    fun checkout(station: Station) {
        destination = station
        settledCents = station.fareCents
        screen = TransitScreen.CHECKOUT
    }

    /** Restores a checkout-ready journey persisted before this app instance was started. */
    fun resumeCheckout(
        presentedClaims: PresentedClaims,
        journey: Journey
    ) {
        claims = presentedClaims
        fareDiscount = AgeDiscount.fromFareClass(journey.fareClass)
        journeyResumeNotice = "Resuming an already existing journey. Please checkout."
        checkout(journey.checkoutStation())
    }

    /** Fare authorized → record the result and open the gate. */
    fun settle(result: SettlementResult.Approved) {
        lastSettlement = result
        settledCents = result.amountCents
        screen = TransitScreen.SETTLEMENT
    }

    /** Abort the tap and show why the gate stayed shut. */
    fun cancel(reason: SettlementResult.Declined) {
        lastFailure = reason
        screen = TransitScreen.FAILURE
    }

    /** Reset the faregate for the next rider's entry tap. */
    fun newTransaction() {
        destination = null
        claims = null
        fareDiscount = AgeDiscount.None
        settledCents = 0L
        journeyResumeNotice = null
        lastSettlement = null
        lastFailure = null
        screen = TransitScreen.CHECK_IN
    }
}


/** Rehydrates the display station while retaining the stored, server-calculated fare. */
private fun Journey.checkoutStation(): Station {
    val exitStation = requireNotNull(exitStation) { "Stored journey has no exit station" }
    val fareCents = requireNotNull(fareCents) { "Stored journey has no fare" }
    val station = getStationList(AgeDiscount.None).firstOrNull { it.name == exitStation }
        ?: throw IllegalStateException("Stored journey has an unknown exit station: $exitStation")
    return station.copy(fareCents = fareCents)
}

@Composable
fun rememberAppState(): AppState = remember { AppState() }
