package org.multipaz.getstarted.w3cdc

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.multipaz.asn1.ASN1Integer
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.compose.rememberUiBoundCoroutineScope
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.EcPublicKey
import org.multipaz.crypto.X500Name
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.multipaz.digitalcredentials.Default
import org.multipaz.digitalcredentials.DigitalCredentials
import org.multipaz.documenttype.DocumentCannedRequest
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.getstarted.getAppToAppOrigin
import org.multipaz.mdoc.util.MdocUtil
import org.multipaz.prompt.PromptModel
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.storage.StorageTable
import org.multipaz.util.Logger
import org.multipaz.verification.MdocApiDcResponse
import org.multipaz.verification.OpenID4VPDcResponse
import org.multipaz.verification.VerificationUtil
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

const val TAG = "W3CDCCredentialsRequestButton"

// Storage Configuration
// ---------------------
// Storage key names for persisting different cryptographic materials.
// These keys are used to store and retrieve the reader's private keys and certificates 
// across app sessions in the provided StorageTable.
// 
// The actual storage table is passed as a parameter to W3CDCCredentialsRequestButton.
// You can change these key names if needed, but ensure consistency across app sessions.

// Storage key names for persisting different cryptographic materials
private const val STORAGE_KEY_READER_ROOT_PRIVATE_KEY = "readerRootKey"
private const val STORAGE_KEY_READER_ROOT_CERT = "readerRootCert"
private const val STORAGE_KEY_READER_PRIVATE_KEY = "readerKey"
private const val STORAGE_KEY_READER_CERT = "readerCert"

// Certificate Configuration
// --------------------------
// Certificate validity dates (10-year validity period)
// These define how long the generated certificates are valid.
// For production, adjust these dates to match your security policies.
private const val CERT_VALID_FROM_DATE = "2024-12-01"
private const val CERT_VALID_UNTIL_DATE = "2034-12-01"

// The Common Name (CN) that appears in X.509 certificates
// This identifies your reader/verifier application.
// For production, change this to your organization's name (e.g., "CN=Acme Corp Verifier").
private const val CERT_SUBJECT_COMMON_NAME = "CN=OWF Multipaz Getting Started Reader Cert"

// The CRL (Certificate Revocation List) URL for certificate validation
// This URL is used to check if certificates have been revoked.
// Update this to your own CRL endpoint in production.
private const val CERT_CRL_URL =
    "https://github.com/openwallet-foundation-labs/identity-credential/crl"

// Cryptographic Configuration
// ---------------------------
// The number of bits for certificate serial numbers (128 bits = 16 bytes)
// This is a standard value for X.509 certificate serial numbers.
private const val CERT_SERIAL_NUMBER_BITS = 128

// The elliptic curve used for reader key generation (P-256 is industry standard)
private val READER_KEY_CURVE = EcCurve.P256

// The elliptic curve used for the bundled reader root key (P-384 provides higher security)
private val READER_ROOT_KEY_CURVE = EcCurve.P384

// Nonce size in bytes for request/response correlation
// 16 bytes (128 bits) provides sufficient entropy for security.
private const val NONCE_SIZE_BYTES = 16

// Response encryption configuration
private val RESPONSE_ENCRYPTION_CURVE = EcCurve.P256

// Descriptive labels used in response metadata for logging and analytics
private const val METADATA_ENGAGEMENT_TYPE = "OS-provided CredentialManager API"
private const val METADATA_TRANSFER_PROTOCOL_PREFIX = "W3C Digital Credentials"

/**
 * Bundled Reader Root Key Pair (P-384 Elliptic Curve)
 *
 * This is the root certificate authority key for the "reader" (verifier) side.
 * It's used to sign reader certificates that authenticate your app when requesting credentials.
 *
 * **Important Notes:**
 * - This key is hardcoded for TESTING/DEMO purposes only
 * - For PRODUCTION, generate your own key pair and keep the private key secure
 * - The public key should be shared with issuers who need to trust your verifier
 * - The private key should NEVER be exposed in production code
 *
 * **Key Details:**
 * - Algorithm: ECDSA with P-384 curve (NIST standard, 384-bit security)
 * - Format: PEM (Privacy Enhanced Mail) encoding
 * - Usage: Signs reader certificates to establish trust chain
 *
 * **How to generate your own:**
 * Use OpenSSL or similar tools:
 * ```
 * openssl ecparam -name secp384r1 -genkey -noout -out reader_root_key.pem
 * openssl ec -in reader_root_key.pem -pubout -out reader_root_pub.pem
 * ```
 */
val bundledReaderRootKey: EcPrivateKey by lazy {
    val readerRootKeyPub = EcPublicKey.fromPem(
        """
                    -----BEGIN PUBLIC KEY-----
                    MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE+QDye70m2O0llPXMjVjxVZz3m5k6agT+
                    wih+L79b7jyqUl99sbeUnpxaLD+cmB3HK3twkA7fmVJSobBc+9CDhkh3mx6n+YoH
                    5RulaSWThWBfMyRjsfVODkosHLCDnbPV
                    -----END PUBLIC KEY-----
                """.trimIndent().trim(),
        READER_ROOT_KEY_CURVE
    )
    EcPrivateKey.fromPem(
        """
                    -----BEGIN PRIVATE KEY-----
                    MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCcRuzXW3pW2h9W8pu5
                    /CSR6JSnfnZVATq+408WPoNC3LzXqJEQSMzPsI9U1q+wZ2yhZANiAAT5APJ7vSbY
                    7SWU9cyNWPFVnPebmTpqBP7CKH4vv1vuPKpSX32xt5SenFosP5yYHccre3CQDt+Z
                    UlKhsFz70IOGSHebHqf5igflG6VpJZOFYF8zJGOx9U4OSiwcsIOds9U=
                    -----END PRIVATE KEY-----
                """.trimIndent().trim(),
        readerRootKeyPub
    )
}

/**
 * W3C Digital Credentials Request Button
 *
 * This Button demonstrates how to request credentials using the W3C Digital Credentials API.
 * It handles:
 * 1. Initializing cryptographic keys and certificates
 * 2. Setting up Zero-Knowledge Proof circuits (optional)
 * 3. Requesting credentials from the OS credential manager
 * 4. Processing and logging the response
 *
 * **Flow:**
 * - Creates/retrieves reader root certificate (signs other certs)
 * - Creates/retrieves reader certificate (authenticates this app)
 * - Initializes ZKP circuits for privacy-preserving verification
 * - Makes a credential request using W3C Digital Credentials API
 * - Receives and decrypts the response
 *
 * **Use Case:**
 * This is typically used by "verifier" apps that need to request and verify
 * credentials (like driver's licenses, health cards, etc.) from a user's device.
 */

@OptIn(ExperimentalTime::class)
@Composable
fun W3CDCCredentialsRequestButton(
    storageTable: StorageTable,
    promptModel: PromptModel,
    text: String = "W3CDC Credentials Request",
    showResponse: (
        vpToken: JsonObject?,
        deviceResponse: DataItem?,
        sessionTranscript: DataItem,
        nonce: ByteString?,
        eReaderKey: EcPrivateKey?,
        metadata: ShowResponseMetadata
    ) -> Unit
) {
    val requestOptions = mutableListOf<RequestEntry>()
    val coroutineScope = rememberUiBoundCoroutineScope { promptModel }

    // Prepare request options from available document types
    LaunchedEffect(Unit) {
        val documentType = DrivingLicense.getDocumentType()
        documentType.cannedRequests.forEach { sampleRequest ->
            requestOptions.add(
                RequestEntry(
                    displayName = "${documentType.displayName}: ${sampleRequest.displayName}",
                    documentType = documentType,
                    sampleRequest = sampleRequest
                )
            )
        }
    }

    Button(onClick = {
        coroutineScope.launch {
            // Parse certificate validity dates from constants
            val certsValidFrom = LocalDate.parse(CERT_VALID_FROM_DATE).atStartOfDayIn(TimeZone.UTC)
            val certsValidUntil =
                LocalDate.parse(CERT_VALID_UNTIL_DATE).atStartOfDayIn(TimeZone.UTC)

            // Initialize the reader root key and certificate
            // This is the "root of trust" for your verifier application
            val readerRootKey = readerRootInit(
                keyStorage = storageTable,
                bundledReaderRootKey = bundledReaderRootKey,
                certsValidFrom = certsValidFrom,
                certsValidUntil = certsValidUntil
            )

            // Initialize the reader key and certificate
            // This is the operational key used to sign credential requests
            val readerKey = readerInit(
                keyStorage = storageTable,
                readerRootKey = readerRootKey,
                certsValidFrom = certsValidFrom,
                certsValidUntil = certsValidUntil
            )

            try {
                // Execute the credential request flow
                doDcRequestFlow(
                    appReaderKey = readerKey,
                    request = requestOptions.first().sampleRequest,
                    showResponse = showResponse
                )
            } catch (error: Throwable) {
                Logger.e(TAG, "Error requesting credentials", error)
            }
        }
    }) {
        Text(text = text)
    }
}

/**
 * Initialize Reader Certificate and Key
 *
 * Creates or retrieves the reader's operational key pair and certificate.
 * This key is used to authenticate the verifier app when requesting credentials.
 *
 * **Key Management:**
 * - Keys are generated once and stored persistently
 * - On first run: generates new P-256 key pair and certificate
 * - On subsequent runs: retrieves existing keys from storage
 *
 * **Certificate Chain:**
 * - Reader Cert (this function) → Reader Root Cert → Trust
 * - The reader cert is signed by the reader root cert
 *
 * **Why P-256?**
 * - Industry standard elliptic curve (NIST)
 * - Good balance of security and performance
 * - Widely supported across platforms
 *
 * @param keyStorage Storage table for persisting keys
 * @param readerRootKey The root key that signs this certificate
 * @param certsValidFrom Certificate validity start date
 * @param certsValidUntil Certificate validity end date
 * @return Certified key pair ready for signing requests
 */
@OptIn(ExperimentalTime::class)
private suspend fun readerInit(
    keyStorage: StorageTable,
    readerRootKey: AsymmetricKey.X509CertifiedExplicit,
    certsValidFrom: Instant,
    certsValidUntil: Instant
): AsymmetricKey.X509Certified {
    // Try to retrieve existing reader private key from storage
    // If not found, generate a new one
    val readerPrivateKey = keyStorage.get(STORAGE_KEY_READER_PRIVATE_KEY)
        ?.let { EcPrivateKey.fromDataItem(Cbor.decode(it.toByteArray())) }
        ?: run {
            // Generate new P-256 private key
            val key = Crypto.createEcPrivateKey(READER_KEY_CURVE)
            // Persist to storage using CBOR encoding
            keyStorage.insert(
                STORAGE_KEY_READER_PRIVATE_KEY,
                ByteString(Cbor.encode(key.toDataItem()))
            )
            key
        }

    // Try to retrieve existing reader certificate from storage
    // If not found, generate a new one signed by the root key
    val readerCert = keyStorage.get(STORAGE_KEY_READER_CERT)?.let {
        X509Cert.fromDataItem(Cbor.decode(it.toByteArray()))
    }
        ?: run {
            // Generate reader certificate signed by reader root
            val cert = MdocUtil.generateReaderCertificate(
                readerRootKey = readerRootKey,
                readerKey = readerPrivateKey.publicKey,
                subject = X500Name.fromName(CERT_SUBJECT_COMMON_NAME),
                serial = ASN1Integer.fromRandom(numBits = CERT_SERIAL_NUMBER_BITS),
                validFrom = certsValidFrom,
                validUntil = certsValidUntil,
            )
            // Persist certificate to storage
            keyStorage.insert(
                STORAGE_KEY_READER_CERT,
                ByteString(Cbor.encode(cert.toDataItem()))
            )
            cert
        }

    // Return the complete certificate chain: [Reader Cert, Reader Root Cert]
    return AsymmetricKey.X509CertifiedExplicit(
        certChain = X509CertChain(listOf(readerCert) + readerRootKey.certChain.certificates),
        privateKey = readerPrivateKey
    )
}

/**
 * Initialize Reader Root Certificate and Key
 *
 * Creates or retrieves the root certificate authority for the reader/verifier.
 * This is the "root of trust" that signs all other reader certificates.
 *
 * **Root Certificate vs Reader Certificate:**
 * - Root Cert: Top of trust chain, self-signed, long-lived
 * - Reader Cert: Operational cert, signed by root, can be rotated
 *
 * **Key Management:**
 * - Uses bundled key pair for consistency across deployments
 * - Stores in persistent storage after first use
 * - Generates self-signed root certificate
 *
 * **Security Note:**
 * - The bundled private key is only for testing/demo
 * - In production, generate unique keys per deployment
 * - Store private keys in secure hardware (HSM/TPM) if available
 *
 * @param keyStorage Storage table for persisting keys
 * @param bundledReaderRootKey Pre-generated root key pair (for testing)
 * @param certsValidFrom Certificate validity start date
 * @param certsValidUntil Certificate validity end date
 * @return Root certificate authority key pair
 */
@OptIn(ExperimentalTime::class)
private suspend fun readerRootInit(
    keyStorage: StorageTable,
    bundledReaderRootKey: EcPrivateKey,
    certsValidFrom: Instant,
    certsValidUntil: Instant
): AsymmetricKey.X509CertifiedExplicit {
    // Try to retrieve existing root private key
    // If not found, use the bundled key
    val readerRootPrivateKey = keyStorage.get(STORAGE_KEY_READER_ROOT_PRIVATE_KEY)
        ?.let { EcPrivateKey.fromDataItem(Cbor.decode(it.toByteArray())) }
        ?: run {
            // Store bundled key for future use
            keyStorage.insert(
                STORAGE_KEY_READER_ROOT_PRIVATE_KEY,
                ByteString(Cbor.encode(bundledReaderRootKey.toDataItem()))
            )
            bundledReaderRootKey
        }

    // Try to retrieve existing root certificate
    // If not found, generate self-signed root certificate
    val readerRootCert = keyStorage.get(STORAGE_KEY_READER_ROOT_CERT)
        ?.let { X509Cert.fromDataItem(Cbor.decode(it.toByteArray())) }
        ?: run {
            // Generate self-signed root certificate
            val bundledReaderRootCert = MdocUtil.generateReaderRootCertificate(
                readerRootKey = AsymmetricKey.anonymous(bundledReaderRootKey),
                subject = X500Name.fromName(CERT_SUBJECT_COMMON_NAME),
                serial = ASN1Integer.fromRandom(numBits = CERT_SERIAL_NUMBER_BITS),
                validFrom = certsValidFrom,
                validUntil = certsValidUntil,
                crlUrl = CERT_CRL_URL
            )
            // Persist root certificate
            keyStorage.insert(
                STORAGE_KEY_READER_ROOT_CERT,
                ByteString(Cbor.encode(bundledReaderRootCert.toDataItem()))
            )
            bundledReaderRootCert
        }

    println("readerRootCert: ${readerRootCert.toPem()}")

    return AsymmetricKey.X509CertifiedExplicit(
        certChain = X509CertChain(listOf(readerRootCert)),
        privateKey = readerRootPrivateKey
    )
}

/**
 * Execute W3C Digital Credentials Request Flow
 *
 * This function orchestrates the complete credential request and verification flow:
 *
 * **Request Phase:**
 * 1. Generate cryptographic nonce for request/response correlation
 * 2. Create response encryption key for secure communication
 * 3. Build request with specified claims and protocols
 * 4. Sign request with reader key (if protocol requires)
 * 5. Send request via W3C Digital Credentials API
 *
 * **Response Phase:**
 * 6. Receive encrypted response from OS credential manager
 * 7. Decrypt response using encryption key
 * 8. Parse response (either mDoc format or OpenID4VP format)
 * 9. Extract and validate credential data
 * 10. Invoke callback with verified data
 *
 * **Security Features:**
 * - Nonce prevents replay attacks
 * - Response encryption ensures confidentiality
 * - Reader authentication proves verifier identity
 * - Session transcript binds request/response
 *
 * **Supported Protocols:**
 * - ISO 18013-7 Annex C (mDoc API)
 * - OpenID4VP (OpenID for Verifiable Presentations)
 * - Hybrid combinations of both
 *
 * @param appReaderKey Reader's certified key for signing requests
 * @param request The credential request (what data to ask for)
 * @param showResponse Callback invoked with verified response data
 */
@OptIn(ExperimentalTime::class)
private suspend fun doDcRequestFlow(
    appReaderKey: AsymmetricKey.X509Compatible,
    request: DocumentCannedRequest,
    showResponse: (
        vpToken: JsonObject?,
        deviceResponse: DataItem?,
        sessionTranscript: DataItem,
        nonce: ByteString?,
        eReaderKey: EcPrivateKey?,
        metadata: ShowResponseMetadata
    ) -> Unit
) {


    require(request.mdocRequest != null) { "No ISO mdoc format in request" }


    // Generate random nonce for request/response correlation
    // This prevents replay attacks and binds request to response
    val nonce = ByteString(Random.Default.nextBytes(NONCE_SIZE_BYTES))

    // Generate ephemeral key for encrypting the response
    // This ensures response confidentiality (only we can decrypt)
    val responseEncryptionKey = Crypto.createEcPrivateKey(RESPONSE_ENCRYPTION_CURVE)

    // Get the app's origin identifier (required by W3C DC spec)
    val origin = getAppToAppOrigin()

    // Client ID is required for signed requests per OpenID4VP spec
    val clientId = "web-origin:$origin"


    val protocolDisplayName = "OpenID4VP 1.0"
    val exchangeProtocolNames = listOf("openid4vp-v1-signed")

    // Build list of claims (specific data elements) to request
    val claims = mutableListOf<MdocRequestedClaim>()
    request.mdocRequest!!.namespacesToRequest.forEach { namespaceRequest ->
        namespaceRequest.dataElementsToRequest.forEach { (mdocDataElement, intentToRetain) ->
            claims.add(
                MdocRequestedClaim(
                    namespaceName = namespaceRequest.namespace,
                    dataElementName = mdocDataElement.attribute.identifier,
                    intentToRetain = intentToRetain
                )
            )
        }
    }

    // Build the W3C Digital Credentials request object
    val dcRequestObject = VerificationUtil.generateDcRequestMdoc(
        exchangeProtocols = exchangeProtocolNames,
        docType = request.mdocRequest!!.docType,
        claims = claims,
        nonce = nonce,
        origin = origin,
        clientId = clientId,
        responseEncryptionKey = responseEncryptionKey.publicKey,
        readerAuthenticationKey = appReaderKey,
        zkSystemSpecs = emptyList()
    )

    // Log request details for debugging
    Logger.i(TAG, "clientId: $clientId")
    Logger.i(TAG, "origin: $origin")
    Logger.iJson(TAG, "Request", dcRequestObject)

    // Send request and measure response time
    val t0 = Clock.System.now()
    val dcResponseObject = DigitalCredentials.Default.request(dcRequestObject)
    Logger.iJson(TAG, "Response", dcResponseObject)

    // Decrypt the response using our encryption key
    val dcResponse = VerificationUtil.decryptDcResponse(
        response = dcResponseObject,
        nonce = nonce,
        origin = origin,
        responseEncryptionKey = AsymmetricKey.anonymous(
            privateKey = responseEncryptionKey,
            algorithm = responseEncryptionKey.curve.defaultKeyAgreementAlgorithm
        )
    )

    // Create metadata for analytics/logging
    val metadata = ShowResponseMetadata(
        engagementType = METADATA_ENGAGEMENT_TYPE,
        transferProtocol = "$METADATA_TRANSFER_PROTOCOL_PREFIX ($protocolDisplayName)",
        requestSize = Json.encodeToString(dcRequestObject).length.toLong(),
        responseSize = Json.encodeToString(dcResponseObject).length.toLong(),
        durationMsecNfcTapToEngagement = null,  // N/A for W3C DC (not NFC)
        durationMsecEngagementReceivedToRequestSent = null,  // N/A for W3C DC
        durationMsecRequestSentToResponseReceived = (Clock.System.now() - t0).inWholeMilliseconds
    )

    // Handle response based on protocol format
    when (dcResponse) {
        is MdocApiDcResponse -> {
            // ISO 18013-7 mDoc format response
            Logger.iCbor(TAG, "deviceResponse", dcResponse.deviceResponse)
            Logger.iCbor(TAG, "sessionTranscript", dcResponse.sessionTranscript)
            showResponse(
                /* vpToken = */ null,
                /* deviceResponse = */ dcResponse.deviceResponse,
                /* sessionTranscript = */ dcResponse.sessionTranscript,
                /* nonce = */ nonce,
                /* eReaderKey = */ null,
                /* metadata = */ metadata
            )
        }

        is OpenID4VPDcResponse -> {
            // OpenID4VP format response
            Logger.iJson(TAG, "vpToken", dcResponse.vpToken)
            Logger.iCbor(TAG, "sessionTranscript", dcResponse.sessionTranscript)
            showResponse(
                /* vpToken = */ dcResponse.vpToken,
                /* deviceResponse = */ null,
                /* sessionTranscript = */ dcResponse.sessionTranscript,
                /* nonce = */ nonce,
                /* eReaderKey = */ null,
                /* metadata = */ metadata
            )
        }
    }
}
