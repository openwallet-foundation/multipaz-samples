package org.multipaz.getstarted

import io.ktor.http.Url
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.multipaz.crypto.Algorithm
import org.multipaz.provisioning.openid4vci.OpenID4VCIBackend
import org.multipaz.provisioning.openid4vci.OpenID4VCIClientPreferences
import org.multipaz.rpc.handler.RpcAuthClientSession
import org.multipaz.securearea.SecureArea
import org.multipaz.storage.Storage

/**
 * Imitate OpenID4VCI wallet back-end for the test app and provide support for the app links.
 *
 * In a real wallet app, the app should call its back-end server to implement [OpenID4VCIBackend]
 * interface, as keys that are used to sign various attestations and assertions must be kept
 * secret. For testing purposes the keys are embedded into the app itself - but such app can be
 * easily impersonated and therefore can never be trusted by a real-life provisioning server.
 */
class ProvisioningSupport(
    val storage: Storage,
    val secureArea: SecureArea,
) {
    companion object {
        // Custom URI Scheme - App-specific URLs. See AndroidManifest.xml Option #2
        const val APP_LINK_SERVER = "get-started-app"
        const val APP_LINK_BASE_URL = "$APP_LINK_SERVER://landing/"

        // Alternative HTTP App Links (more secure). See AndroidManifest.xml Option #1
        // const val APP_LINK_SERVER = "https://getstarted.multipaz.org"
        // const val APP_LINK_BASE_URL = "$APP_LINK_SERVER/landing/"
    }

    // Wait for wallet redirect: state is provided by the issuer during OAuth
    private val lock = Mutex()
    private val pendingLinksByState = mutableMapOf<String, SendChannel<String>>()

    // Instances of backend and client preferences used for provisioning
    private lateinit var backend: OpenID4VCIBackend
    private lateinit var preferences: OpenID4VCIClientPreferences

    suspend fun init() {
        this.backend = OpenID4VCILocalBackend()

        preferences = OpenID4VCIClientPreferences(
            clientId = withContext(RpcAuthClientSession()) {
                backend.getClientId()
            },
            redirectUrl = APP_LINK_BASE_URL,
            locales = listOf("en-US"),
            signingAlgorithms = listOf(Algorithm.ESP256, Algorithm.ESP384, Algorithm.ESP512)
        )
    }

    suspend fun processAppLinkInvocation(url: String) {
        val state = Url(url).parameters["state"] ?: ""
        lock.withLock {
            pendingLinksByState.remove(state)?.send(url)
        }
    }

    suspend fun waitForAppLinkInvocation(state: String): String {
        val channel = Channel<String>(Channel.RENDEZVOUS)
        lock.withLock {
            pendingLinksByState[state] = channel
        }
        return channel.receive()
    }

    fun getOpenID4VCIClientPreferences(): OpenID4VCIClientPreferences = preferences

    fun getOpenID4VCIBackend(): OpenID4VCIBackend = backend
}