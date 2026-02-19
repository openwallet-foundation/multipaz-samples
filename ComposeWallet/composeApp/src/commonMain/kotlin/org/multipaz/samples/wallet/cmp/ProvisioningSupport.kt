package org.multipaz.samples.wallet.cmp

import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.provisioning.openid4vci.OpenID4VCIClientPreferences
import org.multipaz.provisioning.openid4vci.OpenID4VCILocalBackend
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

    val backend = OpenID4VCILocalBackend(
        clientAssertionKey = AsymmetricKey.parseExplicit(
            """
                    {
                        "kty": "EC",
                        "alg": "ES256",
                        "kid": "895b72b9-0808-4fcc-bb19-960d14a9e28f",
                        "crv": "P-256",
                        "x": "nSmAFnZx-SqgTEyqqOSmZyLESdbiSUIYlRlLLoWy5uc",
                        "y": "FN1qcif7nyVX1MHN_YSbo7o7RgG2kPJUjg27YX6AKsQ",
                        "d": "TdQhxDqbAUpzMJN5XXQqLea7-6LvQu2GFKzj5QmFDCw"
                    }            
                    """.trimIndent()
        ),
        attestationKey = AsymmetricKey.parseExplicit(
            """
                    {
                        "kty": "EC",
                        "alg": "ES256",
                        "crv": "P-256",
                        "x": "CoLFZ9sJfTqax-GarKIyw7_fX8-L446AoCTSHKJnZGs",
                        "y": "ALEJB1_YQMO_0qSFQb3urFTxRfANN8-MSeWLHYU7MVI",
                        "d": "nJXw7FqLff14yQLBEAwu70mu1gzlfOONh9UuealdsVM",
                        "x5c": [
                            "MIIBtDCCATugAwIBAgIJAPosC/l8rotwMAoGCCqGSM49BAMCMDgxNjA0BgNVBAMTLXVybjp1dWlkOjYwZjhjMTE3LWI2OTItNGRlOC04ZjdmLTYzNmZmODUyYmFhNjAeFw0yNTA5MzAwMjUxNDRaFw0zNTA5MjgwMjUxNDRaMDgxNjA0BgNVBAMMLXVybjp1dWlkOjRjNDY0NzJiLTdlYjItNDRiNi04NTNhLWY3ZGZlMTEzYzU3NTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABAqCxWfbCX06msfhmqyiMsO/31/Pi+OOgKAk0hyiZ2RrALEJB1/YQMO/0qSFQb3urFTxRfANN8+MSeWLHYU7MVKjLjAsMB8GA1UdIwQYMBaAFPqAK5EjiQbxFAeWt//DCaWtC57aMAkGA1UdEwQCMAAwCgYIKoZIzj0EAwIDZwAwZAIwfDEviit5J188zK5qKjkzFWkPy3ljshUg650p2kNuQq7CiQvbKyVDIlCGgOhMZyy+AjBm6ehDicFMPVBEHLUEiXO4cHw7Ed6dFpPm/6GknWcADhax62KN1tIzExo6T1l06G4=",
                            "MIIBxTCCAUugAwIBAgIJAOQTL9qcQopZMAoGCCqGSM49BAMDMDgxNjA0BgNVBAMTLXVybjp1dWlkOjYwZjhjMTE3LWI2OTItNGRlOC04ZjdmLTYzNmZmODUyYmFhNjAeFw0yNDA5MjMyMjUxMzFaFw0zNDA5MjMyMjUxMzFaMDgxNjA0BgNVBAMTLXVybjp1dWlkOjYwZjhjMTE3LWI2OTItNGRlOC04ZjdmLTYzNmZmODUyYmFhNjB2MBAGByqGSM49AgEGBSuBBAAiA2IABN4D7fpNMAv4EtxyschbITpZ6iNH90rGapa6YEO/uhKnC6VpPt5RUrJyhbvwAs0edCPthRfIZwfwl5GSEOS0mKGCXzWdRv4GGX/Y0m7EYypox+tzfnRTmoVX3v6OxQiapKMhMB8wHQYDVR0OBBYEFPqAK5EjiQbxFAeWt//DCaWtC57aMAoGCCqGSM49BAMDA2gAMGUCMEO01fJKCy+iOTpaVp9LfO7jiXcXksn2BA22reiR9ahDRdGNCrH1E3Q2umQAssSQbQIxAIz1FTHbZPcEbA5uE5lCZlRG/DQxlZhk/rZrkPyXFhqEgfMnQ45IJ6f8Utlg+4Wiiw=="
                        ]
                    }
                    """.trimIndent()
        ),
        clientId = "urn:uuid:418745b8-78a3-4810-88df-7898aff3ffb4",
        walletName = "Multipaz Compose Wallet",
        walletLink = "https://apps.multipaz.org"
    )

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