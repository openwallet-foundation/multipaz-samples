package org.multipaz.pos.proximity

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.multipaz.cbor.DataItem
import org.multipaz.mdoc.nfc.MdocHandoverType
import org.multipaz.mdoc.request.DeviceRequest
import org.multipaz.mdoc.transport.MdocTransport
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.util.Logger
import kotlin.time.Duration

const val TAG = "ProximityReaderModel"

/**
 * Drives a single mdoc reader session through its states, from an engagement to an outcome.
 *
 * The session is set up in steps - [setMdocTransportOptions] and [setConnectionEndpoint] with the
 * engagement, then [setDeviceRequest] with a request built against [sessionTranscript] - and run by
 * [start], which leaves the result in [outcome]. The exchange itself lives in [runReaderFlow]; this
 * class owns the state, not the protocol.
 */
class ProximityReaderModel {
    enum class State {
        IDLE,
        WAITING_FOR_DEVICE_REQUEST,
        WAITING_FOR_START,
        CONNECTING,
        COMPLETED,
    }

    private var sessionJob: Job? = null
    private var session: ProximityReaderSession? = null
    private var deviceRequest: DeviceRequest? = null
    private var pendingTransportOptions: MdocTransportOptions? = null
    private var _outcome: ProximityReaderOutcome? = null

    private val _state = MutableStateFlow<State>(State.IDLE)

    /**
     * The current state.
     */
    val state = _state.asStateFlow()

    /**
     * The outcome of the reader session, either a [ProximityReaderOutcome.Success] or one of the
     * [ProximityReaderOutcome.Failure] cases.
     *
     * This is `null` until the model reaches [State.COMPLETED], and stays `null` if the session was
     * canceled before it produced an outcome.
     */
    val outcome: ProximityReaderOutcome?
        get() = _outcome

    /**
     * The session transcript of the session being set up, which a device request must be built
     * against.
     *
     * This should only be read in states which aren't [State.IDLE] and [State.COMPLETED]. It will
     * throw [IllegalStateException] if this is not the case.
     */
    val sessionTranscript: DataItem
        get() = activeSession().sessionTranscript

    private fun activeSession(): ProximityReaderSession {
        check(_state.value != State.IDLE && _state.value != State.COMPLETED) {
            "There is no session in state ${_state.value}"
        }
        return checkNotNull(session)
    }

    /**
     * Runs the reader flow in [scope], moving to [State.CONNECTING] and then, once the session ends
     * for any reason, to [State.COMPLETED] with [outcome] set.
     */
    fun start(
        scope: CoroutineScope,
    ) {
        check(_state.value == State.WAITING_FOR_START)
        val session = checkNotNull(session)
        val deviceRequest = checkNotNull(deviceRequest)

        _state.value = State.CONNECTING
        Logger.i(TAG, "Starting...")

        sessionJob = scope.launch {
            val currentJob = coroutineContext[Job]
            try {
                _outcome = runReaderFlow(session, deviceRequest)
            } catch (e: CancellationException) {
                Logger.i(TAG, "Reader flow cancelled")
                throw e
            } catch (e: Throwable) {
                Logger.w(TAG, "Error doing reader flow", e)
                _outcome = ProximityReaderOutcome.Failed(e)
            } finally {
                // A newer session, or a reset(), has already taken over the model - leave it alone.
                if (this@ProximityReaderModel.sessionJob === currentJob) {
                    completeSession()
                }
            }
        }
    }

    /**
     * Sets the options to create a transport with, for the next call to [setConnectionEndpoint].
     */
    fun setMdocTransportOptions(options: MdocTransportOptions) {
        pendingTransportOptions = options
    }

    /**
     * Sets up a session for the holder described by [deviceEngagement] and moves to
     * [State.WAITING_FOR_DEVICE_REQUEST].
     *
     * See [ProximityReaderSession] for what the parameters mean.
     */
    suspend fun setConnectionEndpoint(
        deviceEngagement: DataItem,
        handover: DataItem,
        existingTransport: MdocTransport? = null,
        nfcHandoverType: MdocHandoverType? = null,
        durationNfcTapToEngagement: Duration? = null,
    ) {
        check(_state.value == State.IDLE)
        val session = ProximityReaderSession.create(
            encodedDeviceEngagement = deviceEngagement,
            handover = handover,
            existingTransport = existingTransport,
            nfcHandoverType = nfcHandoverType,
            durationNfcTapToEngagement = durationNfcTapToEngagement,
            transportOptions = pendingTransportOptions ?: MdocTransportOptions(),
        )
        this.session = session
        Logger.dCbor(TAG, "sessionTranscript", session.sessionTranscript)
        _state.value = State.WAITING_FOR_DEVICE_REQUEST
    }

    /**
     * Sets the request to send to the holder and moves to [State.WAITING_FOR_START].
     */
    fun setDeviceRequest(
        deviceRequest: DeviceRequest
    ) {
        check(_state.value == State.WAITING_FOR_DEVICE_REQUEST)
        this.deviceRequest = deviceRequest
        _state.value = State.WAITING_FOR_START
    }


    /**
     * Releases the session which just ended, keeping its [outcome].
     */
    private fun completeSession() {
        session = null
        deviceRequest = null
        if (_state.value != State.IDLE) {
            Logger.i(TAG, "Setting state to COMPLETED")
            _state.value = State.COMPLETED
        }
        sessionJob = null
    }

    /**
     * Cancels any session in progress and returns the model to [State.IDLE], discarding the outcome
     * of a previous session.
     */
    fun reset() {
        sessionJob?.cancel(CancellationException("ReaderModel reset"))
        sessionJob = null
        session = null
        deviceRequest = null
        pendingTransportOptions = null
        _outcome = null
        _state.value = State.IDLE
    }
}
