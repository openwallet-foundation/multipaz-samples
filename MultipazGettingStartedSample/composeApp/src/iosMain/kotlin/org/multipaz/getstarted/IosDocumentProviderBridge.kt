package org.multipaz.getstarted

import kotlinx.io.bytestring.encodeToByteString
import org.multipaz.asn1.ASN1Integer
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.X500Name
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.multipaz.digitalcredentials.DigitalCredentials
import org.multipaz.digitalcredentials.getDefault
import org.multipaz.document.DocumentStore
import org.multipaz.document.buildDocumentStore
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.getstarted.core.CredentialDomains
import org.multipaz.mdoc.util.MdocUtil
import org.multipaz.presentment.SimplePresentmentSource
import org.multipaz.presentment.digitalCredentialsPresentment
import org.multipaz.prompt.promptModelSilentConsent
import org.multipaz.securearea.CreateKeySettings
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.util.Logger
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val TAG = "IosDocumentProvider"

// IACA certificate for the extension's sample credential (matches core module resources)
private const val IACA_CERT_PEM = """-----BEGIN CERTIFICATE-----
MIICrTCCAjOgAwIBAgIQ9BmVry0hKDk+MG8+h6pjfDAKBggqhkjOPQQDAzBAMQsw
CQYDVQQGDAJVUzExMC8GA1UEAwwoT1dGIE11bHRpcGF6IEdldHRpbmcgU3RhcnRl
ZCBTYW1wbGUgSUFDQTAeFw0yNTA5MTkxMzIyMDJaFw0zMDA5MTkxMzIyMDJaMEAx
CzAJBgNVBAYMAlVTMTEwLwYDVQQDDChPV0YgTXVsdGlwYXogR2V0dGluZyBTdGFy
dGVkIFNhbXBsZSBJQUNBMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE5Xnksv+TQc1w
UrACkPZG8FhRu4u7xbb4xhSJjYs4jrclG7JAVoAkCohm5ZoQF3/8uOGtuyaRO0F2
X/R7DXCkX+w/liIHWauweMGv9q4LXrbyijKrbUK/WuDcdVrAIsk3o4HxMIHuMA4G
A1UdDwEB/wQEAwIBBjASBgNVHRMBAf8ECDAGAQH/AgEAMD4GA1UdEgQ3MDWGM2h0
dHBzOi8vZGV2ZWxvcGVyLm11bHRpcGF6Lm9yZy9kb2NzL2dldHRpbmctc3RhcnRl
ZDBIBgNVHR8EQTA/MD2gO6A5hjdodHRwczovL2RldmVsb3Blci5tdWx0aXBhei5v
cmcvZG9jcy9nZXR0aW5nLXN0YXJ0ZWQ/Y3JsMB0GA1UdDgQWBBRDjkOcZIDVcy5a
Nb7pzE+7de2MsTAfBgNVHSMEGDAWgBRDjkOcZIDVcy5aNb7pzE+7de2MsTAKBggq
hkjOPQQDAwNoADBlAjEAobY/H1pT0ApbBPnT3YRRaa2qv5BaUw3i58EsVQlV9+s3
k47pqy+aRhi+aoOzUl9oAjBF6AY/9xNw9xCVJv0VeaYoAnnmyMjrcLQJTYOpG6C+
3lfFU74LexWLdIMMd1DhUwE=
-----END CERTIFICATE-----"""

private var initialized = false
private lateinit var bridgePresentmentSource: SimplePresentmentSource
private lateinit var bridgeDocumentStore: DocumentStore
private lateinit var bridgeDocumentTypeRepository: DocumentTypeRepository

@Suppress("FunctionName")
fun EnsureIosDocumentProviderInitialized() {
    // Initialization is performed lazily inside the suspend functions.
}

@Suppress("FunctionName")
suspend fun UpdateIosDocumentProviderRegistrations() {
    ensureBridgeInitialized()
    registerIosDigitalCredentials()
}

@Suppress("FunctionName")
suspend fun ProcessIosDocumentRequest(requestData: String, origin: String?): String {
    ensureBridgeInitialized()
    val effectiveOrigin = origin ?: ""
    Logger.i(TAG, "ProcessIosDocumentRequest start: requestChars=${requestData.length}, origin=$effectiveOrigin")
    val response = digitalCredentialsPresentment(
        protocol = "org-iso-mdoc",
        data = requestData,
        appId = null,
        origin = effectiveOrigin,
        preselectedDocuments = emptyList(),
        source = bridgePresentmentSource,
    )
    Logger.i(TAG, "ProcessIosDocumentRequest success: responseChars=${response.length}")
    return response
}


@OptIn(ExperimentalTime::class)
private suspend fun ensureBridgeInitialized() {
    if (initialized) return

    val storage = org.multipaz.util.Platform.nonBackedUpStorage
    val secureArea = org.multipaz.util.Platform.getSecureArea()
    val secureAreaRepository = SecureAreaRepository.Builder().add(secureArea).build()

    bridgeDocumentTypeRepository = DocumentTypeRepository().apply {
        addDocumentType(DrivingLicense.getDocumentType())
    }

    bridgeDocumentStore = buildDocumentStore(
        storage = storage,
        secureAreaRepository = secureAreaRepository
    ) {}

    // Recreate a known-good sample credential to avoid stale auth-gated keys from older runs.
    bridgeDocumentStore.listDocuments().forEach { existingDocument ->
        bridgeDocumentStore.deleteDocument(existingDocument.identifier)
    }

    val now = Instant.fromEpochSeconds(Clock.System.now().epochSeconds)
    val validFrom = now
    val validUntil = now + 365.days

    val iacaCert = X509Cert.fromPem(IACA_CERT_PEM)

    // Use a fresh DS key; iacaCert is included in the chain for issuer identification.
    val dsKey = Crypto.createEcPrivateKey(EcCurve.P256)
    val iacaSigningKey = AsymmetricKey.X509CertifiedExplicit(
        certChain = X509CertChain(certificates = listOf(iacaCert)),
        privateKey = dsKey,
    )
    val dsCert = MdocUtil.generateDsCertificate(
        iacaKey = iacaSigningKey,
        dsKey = dsKey.publicKey,
        subject = X500Name.fromName("CN=OWF Multipaz Getting Started Sample DS"),
        serial = ASN1Integer.fromRandom(numBits = 128),
        validFrom = validFrom,
        validUntil = validUntil,
    )

    val document = bridgeDocumentStore.createDocument(
        displayName = CredentialDomains.SAMPLE_DOCUMENT_DISPLAY_NAME,
        typeDisplayName = CredentialDomains.SAMPLE_DOCUMENT_TYPE_DISPLAY_NAME,
    )
    DrivingLicense.getDocumentType().createMdocCredentialWithSampleData(
        document = document,
        secureArea = secureArea,
        createKeySettings = CreateKeySettings(
            algorithm = Algorithm.ESP256,
            nonce = "Challenge".encodeToByteString(),
            // Extension flow can fail with request cancellation when auth-gated keys are required.
            userAuthenticationRequired = false
        ),
        dsKey = AsymmetricKey.X509CertifiedExplicit(
            certChain = X509CertChain(certificates = listOf(dsCert)),
            privateKey = dsKey,
        ),
        signedAt = now,
        validFrom = validFrom,
        validUntil = validUntil,
        domain = CredentialDomains.MDOC_USER_AUTH
    )

    bridgePresentmentSource = SimplePresentmentSource(
        documentStore = bridgeDocumentStore,
        documentTypeRepository = bridgeDocumentTypeRepository,
        resolveTrustFn = { null },
        showConsentPromptFn = ::promptModelSilentConsent,
        preferSignatureToKeyAgreement = true,
        domainMdocSignature = CredentialDomains.MDOC_USER_AUTH,
        domainMdocKeyAgreement = CredentialDomains.MDOC_MAC_USER_AUTH,
        domainKeylessSdJwt = CredentialDomains.SDJWT_KEYLESS,
        domainKeyBoundSdJwt = CredentialDomains.SDJWT_USER_AUTH,
    )

    initialized = true
}

private suspend fun registerIosDigitalCredentials() {
    val digitalCredentials = DigitalCredentials.getDefault()
    if (!digitalCredentials.registerAvailable) return
    try {
        digitalCredentials.register(
            documentStore = bridgeDocumentStore,
            documentTypeRepository = bridgeDocumentTypeRepository,
        )
    } catch (t: Throwable) {
        Logger.w(TAG, "Error registering with iOS W3C DC API", t)
    }
}
