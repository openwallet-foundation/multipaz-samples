package org.multipaz.getstarted

import io.ktor.http.Url
import io.ktor.util.encodeBase64
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import multipazgettingstartedsample.composeapp.generated.resources.Res
import org.multipaz.asn1.OID
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.X509Cert
import org.multipaz.provisioning.openid4vci.OpenID4VCIBackend
import org.multipaz.provisioning.openid4vci.OpenID4VCIClientPreferences
import org.multipaz.securearea.KeyAttestation
import org.multipaz.util.toBase64Url
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * Imitate OpenID4VCI wallet back-end for the test app and provide support for the app links.
 *
 * In a real wallet app, the app should call its back-end server to implement [OpenID4VCIBackend]
 * interface, as keys that are used to sign various attestations and assertions must be kept
 * secret. For testing purposes the keys are embedded into the app itself - but such app can be
 * easily impersonated and therefore can never be trusted by a real-life provisioning server.
 */
class ProvisioningSupport : OpenID4VCIBackend {
    companion object Companion {

//      Custom URI Scheme - App-specific URLs. See AndroidManifest.xml Option #2
        const val APP_LINK_SERVER = "get-started-app"
        const val APP_LINK_BASE_URL = "$APP_LINK_SERVER://landing/"

//        Alternative HTTP App Links (more secure). See AndroidManifest.xml Option #1
//        const val APP_LINK_SERVER = "https://getstarted.multipaz.org"
//        const val APP_LINK_BASE_URL = "$APP_LINK_SERVER/landing/"

        private data class LocalClientAssertion(
            val jwk: kotlinx.serialization.json.JsonObject,
            val privateKey: EcPrivateKey,
            val kid: String
        )

        private val localClientAssertionMutex = Mutex()
        private var localClientAssertionCache: LocalClientAssertion? = null

        private suspend fun getLocalClientAssertion(): LocalClientAssertion {
            localClientAssertionCache?.let { return it }
            return localClientAssertionMutex.withLock {
                localClientAssertionCache?.let { return it }
                val jwk = Json.parseToJsonElement(
                    Res.readBytes("files/provisioning_local_assertion_jwk.json").decodeToString()
                ).jsonObject
                val privateKey = EcPrivateKey.fromJwk(jwk)
                val kid = jwk["kid"]!!.jsonPrimitive.content
                val v = LocalClientAssertion(jwk, privateKey, kid)
                localClientAssertionCache = v
                v
            }
        }

        private data class AttestationData(
            val cert: X509Cert,
            val privateKey: EcPrivateKey,
            val attestationId: String
        )

        private val attestationMutex = Mutex()
        private var attestationCache: AttestationData? = null

        private suspend fun getAttestationData(): AttestationData {
            attestationCache?.let { return it }
            return attestationMutex.withLock {
                attestationCache?.let { return it }
                val certPem = Res.readBytes("files/provisioning_attestation_certificate.pem").decodeToString()
                val cert = X509Cert.fromPem(certPem)
                val keyPem = Res.readBytes("files/provisioning_attestation_private_key.pem").decodeToString()
                val privateKey = EcPrivateKey.fromPem(keyPem, cert.ecPublicKey)
                val attestationId = cert.subject.components[OID.COMMON_NAME.oid]?.value
                    ?: throw IllegalStateException("No common name (CN) in certificate's subject")
                val v = AttestationData(cert, privateKey, attestationId)
                attestationCache = v
                v
            }
        }

        const val CLIENT_ID = "urn:uuid:418745b8-78a3-4810-88df-7898aff3ffb4"

        val OPENID4VCI_CLIENT_PREFERENCES = OpenID4VCIClientPreferences(
            clientId = CLIENT_ID,
            redirectUrl = APP_LINK_BASE_URL,
            locales = listOf("en-US"),
            signingAlgorithms = listOf(Algorithm.ESP256, Algorithm.ESP384, Algorithm.ESP512)
        )
    }

    private val lock = Mutex()
    private val pendingLinksByState = mutableMapOf<String, SendChannel<String>>()

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

    @OptIn(ExperimentalTime::class)
    override suspend fun createJwtClientAssertion(tokenUrl: String): String {
        val local = getLocalClientAssertion()
        val alg =
            local.privateKey.curve.defaultSigningAlgorithmFullySpecified.joseAlgorithmIdentifier
        val head = buildJsonObject {
            put("typ", "JWT")
            put("alg", alg)
            put("kid", local.kid)
        }.toString().encodeToByteArray().toBase64Url()

        // TODO: figure out what should be passed as `aud`.
        //  per 'https://datatracker.ietf.org/doc/html/rfc7523#page-5' tokenUrl is appropriate,
        //  but Openid validation suite does not seem to take that.
        val aud = if (tokenUrl.endsWith("/token")) {
            // A hack to get authorization url from token url; would not work in general case.
            tokenUrl.substring(0, tokenUrl.length - 5)
        } else {
            tokenUrl
        }

        val now = Clock.System.now()
        val expiration = now + 5.minutes
        val payload = buildJsonObject {
            put("jti", Random.Default.nextBytes(18).toBase64Url())
            put("iss", CLIENT_ID)
            put("sub", CLIENT_ID) // RFC 7523 Section 3, item 2.B
            put("exp", expiration.epochSeconds)
            put("iat", now.epochSeconds)
            put("aud", aud)
        }.toString().encodeToByteArray().toBase64Url()

        val message = "$head.$payload"
        val sig = Crypto.sign(
            key = local.privateKey,
            signatureAlgorithm = local.privateKey.curve.defaultSigningAlgorithm,
            message = message.encodeToByteArray()
        )
        val signature = sig.toCoseEncoded().toBase64Url()

        return "$message.$signature"
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun createJwtWalletAttestation(keyAttestation: KeyAttestation): String {
        // Implements this draft:
        // https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-04

        val att = getAttestationData()
        val signatureAlgorithm = att.privateKey.curve.defaultSigningAlgorithmFullySpecified
        val head = buildJsonObject {
            put("typ", "oauth-client-attestation+jwt")
            put("alg", signatureAlgorithm.joseAlgorithmIdentifier)
            put("x5c", buildJsonArray {
                add(att.cert.encodedCertificate.encodeBase64())
            })
        }.toString().encodeToByteArray().toBase64Url()

        val now = Clock.System.now()
        val notBefore = now - 1.seconds
        // Expiration here is only for the client assertion to be presented to the issuing server
        // in the given timeframe (which happens without user interaction). It does not imply that
        // the key becomes invalid at that point in time.
        val expiration = now + 5.minutes
        val payload = buildJsonObject {
            put("iss", att.attestationId)
            put("sub", CLIENT_ID)
            put("exp", expiration.epochSeconds)
            put("cnf", buildJsonObject {
                put(
                    "jwk",
                    keyAttestation.publicKey.toJwk(buildJsonObject { put("kid", CLIENT_ID) })
                )
            })
            put("nbf", notBefore.epochSeconds)
            put("iat", now.epochSeconds)
            put("wallet_name", "Multipaz Getting Started Sample")
            put("wallet_link", "https://multipaz.org")
        }.toString().encodeToByteArray().toBase64Url()

        val message = "$head.$payload"
        val sig = Crypto.sign(
            key = att.privateKey,
            signatureAlgorithm = signatureAlgorithm,
            message = message.encodeToByteArray()
        )
        val signature = sig.toCoseEncoded().toBase64Url()

        return "$message.$signature"
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun createJwtKeyAttestation(
        keyAttestations: List<KeyAttestation>,
        challenge: String
    ): String {
        val keyList = keyAttestations.map { it.publicKey }

        val att = getAttestationData()
        val alg = att.privateKey.curve.defaultSigningAlgorithm.joseAlgorithmIdentifier
        val head = buildJsonObject {
            put("typ", "keyattestation+jwt")
            put("alg", alg)
            put("x5c", buildJsonArray {
                add(att.cert.encodedCertificate.encodeBase64())
            })
        }.toString().encodeToByteArray().toBase64Url()

        val now = Clock.System.now()
        val notBefore = now - 1.seconds
        val expiration = now + 5.minutes
        val payload = buildJsonObject {
            put("iss", att.attestationId)
            put("attested_keys", JsonArray(keyList.map { it.toJwk() }))
            put("nonce", challenge)
            put("nbf", notBefore.epochSeconds)
            put("exp", expiration.epochSeconds)
            put("iat", now.epochSeconds)
        }.toString().encodeToByteArray().toBase64Url()

        val message = "$head.$payload"
        val sig = Crypto.sign(
            key = att.privateKey,
            signatureAlgorithm = att.privateKey.curve.defaultSigningAlgorithm,
            message = message.encodeToByteArray()
        )
        val signature = sig.toCoseEncoded().toBase64Url()

        return "$message.$signature"
    }
}