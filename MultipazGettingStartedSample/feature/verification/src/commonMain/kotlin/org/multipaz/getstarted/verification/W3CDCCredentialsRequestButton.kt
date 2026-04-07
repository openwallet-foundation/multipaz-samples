package org.multipaz.getstarted.verification

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
import multipazgettingstartedsample.feature.verification.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.multipaz.asn1.ASN1Integer
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.compose.rememberUiBoundCoroutineScope
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.EcPublicKey
import org.multipaz.crypto.X500Name
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.multipaz.digitalcredentials.DigitalCredentials
import org.multipaz.digitalcredentials.getDefault
import org.multipaz.documenttype.DocumentCannedRequest
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.getstarted.core.getAppToAppOrigin
import org.multipaz.getstarted.verification.W3CDCConstants.Companion.CERT_CRL_URL
import org.multipaz.getstarted.verification.W3CDCConstants.Companion.CERT_SERIAL_NUMBER_BITS
import org.multipaz.getstarted.verification.W3CDCConstants.Companion.CERT_SUBJECT_COMMON_NAME
import org.multipaz.getstarted.verification.W3CDCConstants.Companion.CERT_VALID_FROM_DATE
import org.multipaz.getstarted.verification.W3CDCConstants.Companion.CERT_VALID_UNTIL_DATE
import org.multipaz.getstarted.verification.W3CDCConstants.Companion.METADATA_ENGAGEMENT_TYPE
import org.multipaz.getstarted.verification.W3CDCConstants.Companion.METADATA_TRANSFER_PROTOCOL_PREFIX
import org.multipaz.getstarted.verification.W3CDCConstants.Companion.NONCE_SIZE_BYTES
import org.multipaz.getstarted.verification.W3CDCConstants.Companion.READER_KEY_CURVE
import org.multipaz.getstarted.verification.W3CDCConstants.Companion.RESPONSE_ENCRYPTION_CURVE
import org.multipaz.getstarted.verification.W3CDCConstants.Companion.STORAGE_KEY_READER_CERT
import org.multipaz.getstarted.verification.W3CDCConstants.Companion.STORAGE_KEY_READER_PRIVATE_KEY
import org.multipaz.getstarted.verification.W3CDCConstants.Companion.STORAGE_KEY_READER_ROOT_CERT
import org.multipaz.getstarted.verification.W3CDCConstants.Companion.STORAGE_KEY_READER_ROOT_PRIVATE_KEY
import org.multipaz.mdoc.util.MdocUtil
import org.multipaz.prompt.PromptModel
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.storage.StorageTable
import org.multipaz.trustmanagement.TrustManagerLocal
import org.multipaz.trustmanagement.TrustMetadata
import org.multipaz.trustmanagement.TrustPointAlreadyExistsException
import org.multipaz.util.Logger
import org.multipaz.verification.MdocApiDcResponse
import org.multipaz.verification.OpenID4VPDcResponse
import org.multipaz.verification.VerificationUtil
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

const val TAG = "W3CDCCredentialsRequestButton"

@OptIn(ExperimentalTime::class)
@Composable
fun W3CDCCredentialsRequestButton(
    storageTable: StorageTable,
    promptModel: PromptModel,
    readerTrustManager: TrustManagerLocal,
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
            val certsValidFrom = LocalDate.parse(CERT_VALID_FROM_DATE).atStartOfDayIn(TimeZone.UTC)
            val certsValidUntil =
                LocalDate.parse(CERT_VALID_UNTIL_DATE).atStartOfDayIn(TimeZone.UTC)

            val readerRootKey = readerRootInit(
                keyStorage = storageTable,
                certsValidFrom = certsValidFrom,
                certsValidUntil = certsValidUntil
            )

            val readerKey = readerInit(
                keyStorage = storageTable,
                readerRootKey = readerRootKey,
                certsValidFrom = certsValidFrom,
                certsValidUntil = certsValidUntil
            )

            try {
                readerTrustManager.addX509Cert(
                    certificate = readerRootKey.certChain.certificates.first(),
                    metadata = TrustMetadata(
                        displayName = "Multipaz Getting Started Sample",
                        privacyPolicyUrl = "https://developer.multipaz.org"
                    )
                )
            } catch (e: TrustPointAlreadyExistsException) {
                e.printStackTrace()
            }

            try {
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

@OptIn(ExperimentalTime::class)
private suspend fun readerInit(
    keyStorage: StorageTable,
    readerRootKey: AsymmetricKey.X509CertifiedExplicit,
    certsValidFrom: Instant,
    certsValidUntil: Instant
): AsymmetricKey.X509Certified {
    val readerPrivateKey = keyStorage.get(STORAGE_KEY_READER_PRIVATE_KEY)
        ?.let { EcPrivateKey.fromDataItem(Cbor.decode(it.toByteArray())) }
        ?: run {
            val key = Crypto.createEcPrivateKey(READER_KEY_CURVE)
            keyStorage.insert(
                STORAGE_KEY_READER_PRIVATE_KEY,
                ByteString(Cbor.encode(key.toDataItem()))
            )
            key
        }

    val readerCert = keyStorage.get(STORAGE_KEY_READER_CERT)?.let {
        X509Cert.fromDataItem(Cbor.decode(it.toByteArray()))
    }
        ?: run {
            val cert = MdocUtil.generateReaderCertificate(
                readerRootKey = readerRootKey,
                readerKey = readerPrivateKey.publicKey,
                subject = X500Name.fromName(CERT_SUBJECT_COMMON_NAME),
                serial = ASN1Integer.fromRandom(numBits = CERT_SERIAL_NUMBER_BITS),
                validFrom = certsValidFrom,
                validUntil = certsValidUntil,
            )
            keyStorage.insert(
                STORAGE_KEY_READER_CERT,
                ByteString(Cbor.encode(cert.toDataItem()))
            )
            cert
        }

    return AsymmetricKey.X509CertifiedExplicit(
        certChain = X509CertChain(listOf(readerCert) + readerRootKey.certChain.certificates),
        privateKey = readerPrivateKey
    )
}

@OptIn(ExperimentalTime::class, ExperimentalResourceApi::class)
private suspend fun readerRootInit(
    keyStorage: StorageTable,
    certsValidFrom: Instant,
    certsValidUntil: Instant
): AsymmetricKey.X509CertifiedExplicit {
    val readerRootKey = loadBundledReaderRootKey()

    val readerRootPrivateKey = keyStorage.get(STORAGE_KEY_READER_ROOT_PRIVATE_KEY)
        ?.let { EcPrivateKey.fromDataItem(Cbor.decode(it.toByteArray())) }
        ?: run {
            keyStorage.insert(
                STORAGE_KEY_READER_ROOT_PRIVATE_KEY,
                ByteString(Cbor.encode(readerRootKey.toDataItem()))
            )
            readerRootKey
        }

    val readerRootCert = keyStorage.get(STORAGE_KEY_READER_ROOT_CERT)
        ?.let { X509Cert.fromDataItem(Cbor.decode(it.toByteArray())) }
        ?: run {
            val bundledReaderRootCert = MdocUtil.generateReaderRootCertificate(
                readerRootKey = AsymmetricKey.anonymous(readerRootKey),
                subject = X500Name.fromName(CERT_SUBJECT_COMMON_NAME),
                serial = ASN1Integer.fromRandom(numBits = CERT_SERIAL_NUMBER_BITS),
                validFrom = certsValidFrom,
                validUntil = certsValidUntil,
                crlUrl = CERT_CRL_URL
            )
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

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadBundledReaderRootKey(): EcPrivateKey {
    val publicKeyPem = Res.readBytes("files/reader_root_key_public.pem").decodeToString()
    val privateKeyPem = Res.readBytes("files/reader_root_key_private.pem").decodeToString()

    val readerRootKeyPub = EcPublicKey.fromPem(
        publicKeyPem
    )

    return EcPrivateKey.fromPem(
        privateKeyPem,
        readerRootKeyPub
    )
}

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

    val nonce = ByteString(Random.Default.nextBytes(NONCE_SIZE_BYTES))
    val responseEncryptionKey = Crypto.createEcPrivateKey(RESPONSE_ENCRYPTION_CURVE)
    val origin = getAppToAppOrigin()
    val clientId = "web-origin:$origin"

    val protocolDisplayName = "OpenID4VP 1.0"
    val exchangeProtocolNames = listOf("openid4vp-v1-signed")

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

    Logger.i(TAG, "clientId: $clientId")
    Logger.i(TAG, "origin: $origin")
    Logger.iJson(TAG, "Request", dcRequestObject)

    val t0 = Clock.System.now()
    val dcResponseObject = DigitalCredentials.getDefault().request(dcRequestObject)
    Logger.iJson(TAG, "Response", dcResponseObject)

    val dcResponse = VerificationUtil.decryptDcResponse(
        response = dcResponseObject,
        nonce = nonce,
        origin = origin,
        responseEncryptionKey = AsymmetricKey.anonymous(
            privateKey = responseEncryptionKey,
            algorithm = responseEncryptionKey.curve.defaultKeyAgreementAlgorithm
        )
    )

    val metadata = ShowResponseMetadata(
        engagementType = METADATA_ENGAGEMENT_TYPE,
        transferProtocol = "$METADATA_TRANSFER_PROTOCOL_PREFIX ($protocolDisplayName)",
        requestSize = Json.encodeToString(dcRequestObject).length.toLong(),
        responseSize = Json.encodeToString(dcResponseObject).length.toLong(),
        durationMsecNfcTapToEngagement = null,
        durationMsecEngagementReceivedToRequestSent = null,
        durationMsecRequestSentToResponseReceived = (Clock.System.now() - t0).inWholeMilliseconds
    )

    when (dcResponse) {
        is MdocApiDcResponse -> {
            Logger.iCbor(TAG, "deviceResponse", dcResponse.deviceResponse)
            Logger.iCbor(TAG, "sessionTranscript", dcResponse.sessionTranscript)
            showResponse(
                null,
                dcResponse.deviceResponse,
                dcResponse.sessionTranscript,
                nonce,
                null,
                metadata
            )
        }

        is OpenID4VPDcResponse -> {
            Logger.iJson(TAG, "vpToken", dcResponse.vpToken)
            Logger.iCbor(TAG, "sessionTranscript", dcResponse.sessionTranscript)
            showResponse(
                dcResponse.vpToken,
                null,
                dcResponse.sessionTranscript,
                nonce,
                null,
                metadata
            )
        }
    }
}
