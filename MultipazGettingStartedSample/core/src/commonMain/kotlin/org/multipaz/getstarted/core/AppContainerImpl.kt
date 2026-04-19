package org.multipaz.getstarted.core

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.bytestring.encodeToByteString
import multipazgettingstartedsample.core.generated.resources.Res
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
import org.multipaz.document.Document
import org.multipaz.document.DocumentStore
import org.multipaz.document.buildDocumentStore
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.mdoc.util.MdocUtil
import org.multipaz.presentment.PresentmentSource
import org.multipaz.presentment.SimplePresentmentSource
import org.multipaz.securearea.CreateKeySettings
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.storage.Storage
import org.multipaz.storage.StorageTable
import org.multipaz.storage.StorageTableSpec
import org.multipaz.trustmanagement.TrustEntryAlreadyExistsException
import org.multipaz.trustmanagement.TrustManager
import org.multipaz.trustmanagement.TrustMetadata
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class AppContainerImpl : AppContainer {

    override lateinit var storage: Storage
    override lateinit var storageTable: StorageTable
    override lateinit var secureArea: SecureArea
    override lateinit var secureAreaRepository: SecureAreaRepository

    override lateinit var documentTypeRepository: DocumentTypeRepository
    override lateinit var documentStore: DocumentStore

    override lateinit var presentmentSource: PresentmentSource

    override lateinit var readerTrustManager: TrustManager

    override var isInitialized = false

    @OptIn(ExperimentalTime::class)
    override suspend fun init() {
        if (isInitialized) return

        // Storage
        storage = org.multipaz.util.Platform.nonBackedUpStorage
        storageTable = storage.getTable(CredentialDomains.storageTableSpec)
        secureArea = org.multipaz.util.Platform.getSecureArea()
        secureAreaRepository = SecureAreaRepository.Builder().add(secureArea).build()

        // DocumentStore
        documentTypeRepository = DocumentTypeRepository().apply {
            addDocumentType(DrivingLicense.getDocumentType())
        }
        documentStore = buildDocumentStore(
            storage = storage,
            secureAreaRepository = secureAreaRepository
        ) {}

        val iacaCert =
            X509Cert.fromPem(Res.readBytes("files/iaca_certificate.pem").decodeToString())

        // Prepare Timestamps
        val now = Instant.fromEpochSeconds(Clock.System.now().epochSeconds)
        val signedAt = now
        val validFrom = now
        val validUntil = now + 365.days

        // Generate Document Signing (DS) Certificate
        val dsKey = Crypto.createEcPrivateKey(EcCurve.P256)
        val iacaKey = AsymmetricKey.X509CertifiedExplicit(
            certChain = X509CertChain(certificates = listOf(iacaCert)),
            privateKey = dsKey,
        )
        val dsCert = MdocUtil.generateDsCertificate(
            iacaKey = iacaKey,
            dsKey = dsKey.publicKey,
            subject = X500Name.fromName(name = "CN=Test DS Key"),
            serial = ASN1Integer.fromRandom(numBits = 128),
            validFrom = validFrom,
            validUntil = validUntil
        )

        // Creation of an mDoc
        if (documentStore.listDocuments().isEmpty()) {
            val document = documentStore.createDocument(
                displayName = CredentialDomains.SAMPLE_DOCUMENT_DISPLAY_NAME,
                typeDisplayName = CredentialDomains.SAMPLE_DOCUMENT_TYPE_DISPLAY_NAME,
            )
            DrivingLicense.getDocumentType().createMdocCredentialWithSampleData(
                document = document,
                secureArea = secureArea,
                createKeySettings = CreateKeySettings(
                    algorithm = Algorithm.ESP256,
                    nonce = "Challenge".encodeToByteString(),
                    userAuthenticationRequired = true
                ),
                dsKey = AsymmetricKey.X509CertifiedExplicit(
                    certChain = X509CertChain(certificates = listOf(dsCert)),
                    privateKey = dsKey,
                ),
                signedAt = signedAt,
                validFrom = validFrom,
                validUntil = validUntil,
                domain = CredentialDomains.MDOC_USER_AUTH
            )
        }

        // Initialize TrustManager
        readerTrustManager = TrustManager(storage = storage, identifier = "reader")

        try {
            readerTrustManager.addX509Cert(
                certificate = X509Cert.fromPem(
                    Res.readBytes("files/reader_root_cert_multipaz_testapp.pem")
                        .decodeToString()
                ),
                metadata = TrustMetadata(
                    displayName = "OWF Multipaz TestApp",
                    privacyPolicyUrl = "https://apps.multipaz.org"
                )
            )
        } catch (e: TrustEntryAlreadyExistsException) {
            e.printStackTrace()
        }

        try {
            readerTrustManager.addX509Cert(
                certificate = X509Cert.fromPem(
                    Res.readBytes("files/reader_root_cert_multipaz_identity_reader.pem")
                        .decodeToString()
                ),
                metadata = TrustMetadata(
                    displayName = "Multipaz Identity Reader",
                    privacyPolicyUrl = "https://verifier.multipaz.org/identityreaderbackend/"
                )
            )
        } catch (e: TrustEntryAlreadyExistsException) {
            e.printStackTrace()
        }

        try {
            readerTrustManager.addX509Cert(
                certificate = X509Cert.fromPem(
                    Res.readBytes("files/reader_root_cert_multipaz_identity_reader_untrusted.pem")
                        .decodeToString()
                ),
                metadata = TrustMetadata(
                    displayName = "Multipaz Identity Reader (Untrusted Devices)",
                    privacyPolicyUrl = "https://verifier.multipaz.org/identityreaderbackend/"
                )
            )
        } catch (e: TrustEntryAlreadyExistsException) {
            e.printStackTrace()
        }

        try {
            readerTrustManager.addX509Cert(
                certificate = X509Cert.fromPem(
                    Res.readBytes("files/reader_root_cert_multipaz_web_verifier.pem")
                        .decodeToString()
                ),
                metadata = TrustMetadata(
                    displayName = "Multipaz Verifier",
                    privacyPolicyUrl = "https://verifier.multipaz.org"
                )
            )
        } catch (e: TrustEntryAlreadyExistsException) {
            e.printStackTrace()
        }

        presentmentSource = SimplePresentmentSource(
            documentStore = documentStore,
            documentTypeRepository = documentTypeRepository,
            resolveTrustFn = { requester ->
                requester.certChain?.let { certChain ->
                    val trustResult = readerTrustManager.verify(certChain.certificates)

                    if (trustResult.isTrusted) {
                        return@SimplePresentmentSource trustResult.trustPoints.first().metadata
                    }
                }
                null
            },
            preferSignatureToKeyAgreement = true,
            domainMdocSignature = CredentialDomains.MDOC_USER_AUTH,
            domainMdocKeyAgreement = CredentialDomains.MDOC_MAC_USER_AUTH,
            domainKeylessSdJwt = CredentialDomains.SDJWT_KEYLESS,
            domainKeyBoundSdJwt = CredentialDomains.SDJWT_USER_AUTH
        )

        val digitalCredentials = DigitalCredentials.getDefault()
        if (digitalCredentials.registerAvailable) {
            try {
                digitalCredentials.register(
                    documentStore = documentStore,
                    documentTypeRepository = documentTypeRepository,
                )
            } catch (_: Throwable) {
            }

            CoroutineScope(Dispatchers.Default).launch {
                documentStore.eventFlow
                    .onEach { event ->
                        try {
                            digitalCredentials.register(
                                documentStore = documentStore,
                                documentTypeRepository = documentTypeRepository,
                            )
                        } catch (_: Throwable) {
                        }
                    }
                    .launchIn(this)
            }
        }

        isInitialized = true
    }

    override suspend fun listDocuments(): MutableList<Document> {
        val documents = mutableStateListOf<Document>()
        for (document in documentStore.listDocuments()) {
            if (!documents.contains(document)) {
                documents.add(document)
            }
        }
        return documents
    }
}
