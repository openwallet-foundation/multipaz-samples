package org.multipaz.samples.wallet.cmp

import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.multipaz.crypto.Algorithm
import org.multipaz.provisioning.openid4vci.OpenID4VCIBackend
import org.multipaz.provisioning.openid4vci.OpenID4VCIClientPreferences
import org.multipaz.rpc.handler.RpcAuthClientSession
import org.multipaz.util.Logger
import kotlin.time.ExperimentalTime

/**
 * Helper class to integrate [org.multipaz.provisioning.ProvisioningModel].
 */
@OptIn(ExperimentalTime::class)
class ProvisioningSupport() {
    companion object {
        const val TAG = "ProvisioningSupport"
        const val APP_LINK_SERVER = "https://apps.multipaz.org"
        const val APP_LINK_BASE_URL = "$APP_LINK_SERVER/redirect/org.multipaz.samples.wallet.cmp"
    }

    private val lock = Mutex()
    private val pendingLinksByState = mutableMapOf<String, SendChannel<String>>()

    val backend: OpenID4VCIBackend = OpenID4VCILocalBackend()
    lateinit var preferences: OpenID4VCIClientPreferences
        private set

    suspend fun init() {
        preferences = OpenID4VCIClientPreferences(
            clientId = withContext(RpcAuthClientSession()) {
                backend.getClientId()
            },
            redirectUrl = APP_LINK_BASE_URL,
            locales = listOf("en-US"),
            signingAlgorithms = listOf(Algorithm.ESP256, Algorithm.ESP384, Algorithm.ESP512)
        )
    }

    /**
     * Should be called when an applink with the given URL is passed from the browser to the app.
     */
    suspend fun processAppLinkInvocation(url: String) {
        val state = Url(url).parameters["state"] ?: ""
        lock.withLock {
            pendingLinksByState.remove(state)?.send(url)
        }
    }

    /**
     * Wait until an applink with the given state parameter is handled by the app.
     *
     * @return url that was passed to the app by the brower.
     */
    suspend fun waitForAppLinkInvocation(state: String): String {
        Logger.e(TAG, "Waiting for redirect state '$state'")
        val channel = Channel<String>(Channel.RENDEZVOUS)
        lock.withLock {
            pendingLinksByState[state] = channel
        }
        try {
            return channel.receive()
        } catch (err: CancellationException) {
            Logger.e(TAG, "Cancelled waiting for redirect state '$state'")
            throw err
        }
    }
}