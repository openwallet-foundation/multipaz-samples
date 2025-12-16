package org.multipaz.getstarted

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import multipazgettingstartedsample.composeapp.generated.resources.Res
import multipazgettingstartedsample.composeapp.generated.resources.compose_multiplatform
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.multipaz.asn1.ASN1Integer
import org.multipaz.cbor.Cbor
import org.multipaz.compose.camera.CameraFrame
import org.multipaz.compose.cropRotateScaleImage
import org.multipaz.compose.presentment.MdocProximityQrSettings
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.compose.qrcode.generateQrCode
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.X500Name
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.multipaz.digitalcredentials.Default
import org.multipaz.digitalcredentials.DigitalCredentials
import org.multipaz.document.AbstractDocumentMetadata
import org.multipaz.document.Document
import org.multipaz.document.DocumentMetadata
import org.multipaz.document.DocumentStore
import org.multipaz.document.buildDocumentStore
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.facedetection.DetectedFace
import org.multipaz.facedetection.FaceLandmarkType
import org.multipaz.facematch.FaceMatchLiteRtModel
import org.multipaz.getstarted.w3cdc.ShowResponseMetadata
import org.multipaz.getstarted.w3cdc.fromDataItem
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.mdoc.util.MdocUtil
import org.multipaz.mdoc.vical.SignedVical
import org.multipaz.mdoc.zkp.ZkSystemRepository
import org.multipaz.mdoc.zkp.longfellow.LongfellowZkSystem
import org.multipaz.presentment.model.PresentmentModel
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.presentment.model.SimplePresentmentSource
import org.multipaz.provisioning.Display
import org.multipaz.provisioning.ProvisioningModel
import org.multipaz.securearea.CreateKeySettings
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.storage.Storage
import org.multipaz.storage.StorageTable
import org.multipaz.storage.StorageTableSpec
import org.multipaz.storage.ephemeral.EphemeralStorage
import org.multipaz.trustmanagement.CompositeTrustManager
import org.multipaz.trustmanagement.TrustManagerLocal
import org.multipaz.trustmanagement.TrustMetadata
import org.multipaz.trustmanagement.TrustPointAlreadyExistsException
import org.multipaz.trustmanagement.VicalTrustManager
import org.multipaz.util.UUID
import org.multipaz.util.fromBase64Url
import org.multipaz.util.toBase64Url
import kotlin.collections.addAll
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

class App {

    // Storage
    lateinit var storage: Storage
    lateinit var storageTable: StorageTable
    lateinit var secureArea: SecureArea
    lateinit var secureAreaRepository: SecureAreaRepository

    // DocumentStore
    lateinit var documentTypeRepository: DocumentTypeRepository
    lateinit var documentStore: DocumentStore

    lateinit var presentmentModel: PresentmentModel
    lateinit var presentmentSource: PresentmentSource

    lateinit var readerTrustManager: TrustManagerLocal

    lateinit var provisioningModel: ProvisioningModel
    lateinit var provisioningSupport: ProvisioningSupport
    private val credentialOffers = Channel<String>()

    lateinit var iacaKey: AsymmetricKey.X509Certified
    lateinit var issuerTrustManager: CompositeTrustManager
    lateinit var zkSystemRepository: ZkSystemRepository

    val appName = "Multipaz Getting Started Sample"
    val appIcon = Res.drawable.compose_multiplatform

    lateinit var faceMatchLiteRtModel: FaceMatchLiteRtModel

    var isInitialized = false

    @OptIn(ExperimentalTime::class)
    suspend fun init() {
        if (!isInitialized) {

            // Storage
            storage = org.multipaz.util.Platform.nonBackedUpStorage
            storageTable = storage.getTable(
                StorageTableSpec(
                    name = STORAGE_TABLE_NAME,
                    supportPartitions = false,  // Simple key-value storage
                    supportExpiration = false    // Keys don't auto-expire
                )
            )
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

            println("------- IACA PEM -------")
            println(iacaCert.toPem().toString())
            println("------- IACA PEM -------")

            // 1. Prepare Timestamps
            val now = Clock.System.now()
            val signedAt = now
            val validFrom = now
            val validUntil = now + 365.days

            // 3. Generate Document Signing (DS) Certificate
            val dsKey = Crypto.createEcPrivateKey(EcCurve.P256)
            iacaKey = AsymmetricKey.X509CertifiedExplicit(
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

                // Creating a Document
                val document = documentStore.createDocument(
                    displayName = SAMPLE_DOCUMENT_DISPLAY_NAME,
                    typeDisplayName = SAMPLE_DOCUMENT_TYPE_DISPLAY_NAME,
                )
                // 4. Create the mDoc Credential
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
                    domain = CREDENTIAL_DOMAIN_MDOC_USER_AUTH
                )
            }

            // Initialize TrustManager
            // Three certificates are configured to handle different verification scenarios:
            // 1. OWF Multipaz TestApp - for testing with the Multipaz test application
            // 2. Multipaz Identity Reader - for APK downloaded from https://apps.multipaz.org/ (production devices with secure boot)
            //    Certificate available from: https://verifier.multipaz.org/identityreaderbackend/readerRootCert
            // 3. Multipaz Identity Reader (Untrusted Devices) - for app compiled from source code at https://github.com/davidz25/MpzIdentityReader
            //    Certificate available from: https://verifier.multipaz.org/identityreaderbackend/readerRootCertUntrustedDevices
            readerTrustManager = TrustManagerLocal(storage = storage, identifier = "reader")

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
            } catch (e: TrustPointAlreadyExistsException) {
                e.printStackTrace()
            }

            // Certificate for APK downloaded from https://apps.multipaz.org/
            // This should be used for production devices with secure boot (GREEN state)
            // Certificate source: https://verifier.multipaz.org/identityreaderbackend/readerRootCert
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
            } catch (e: TrustPointAlreadyExistsException) {
                e.printStackTrace()
            }

            // Certificate for app compiled from source code at https://github.com/davidz25/MpzIdentityReader
            // This should be used for development/testing devices or devices with unlocked bootloaders
            // Certificate source: https://verifier.multipaz.org/identityreaderbackend/readerRootCertUntrustedDevices
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
            } catch (e: TrustPointAlreadyExistsException) {
                e.printStackTrace()
            }

            // This is for https://verifier.multipaz.org website.
            // Certificate source: https://verifier.multipaz.org/verifier/readerRootCert
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
            } catch (e: TrustPointAlreadyExistsException) {
                e.printStackTrace()
            }


            val builtInIssuerTrustManager = TrustManagerLocal(
                storage = EphemeralStorage(),
                partitionId = "BuiltInTrustedIssuers",
                identifier = "Built-in Trusted Issuers"
            )
            builtInIssuerTrustManager.addX509Cert(
                certificate = iacaKey.certChain.certificates.first(),
                metadata = TrustMetadata(displayName = "OWF Multipaz TestApp Issuer"),
            )
            val signedVical =
                SignedVical.parse(Res.readBytes("files/ISO_SC17WG10_Wellington_Test_Event_Nov_2025.vical"))
            val vicalTrustManager = VicalTrustManager(signedVical)
            issuerTrustManager =
                CompositeTrustManager(listOf(builtInIssuerTrustManager, vicalTrustManager))

            presentmentModel = PresentmentModel().apply { setPromptModel(promptModel) }
            presentmentSource = SimplePresentmentSource(
                documentStore = documentStore,
                documentTypeRepository = documentTypeRepository,
                readerTrustManager = readerTrustManager,
                preferSignatureToKeyAgreement = true,
                // Match domains used when storing credentials via OpenID4VCI
                domainMdocSignature = CREDENTIAL_DOMAIN_MDOC_USER_AUTH,
                domainMdocKeyAgreement = CREDENTIAL_DOMAIN_MDOC_MAC_USER_AUTH,
                domainKeylessSdJwt = CREDENTIAL_DOMAIN_SDJWT_KEYLESS,
                domainKeyBoundSdJwt = CREDENTIAL_DOMAIN_SDJWT_USER_AUTH
            )

            val modelData = ByteString(*Res.readBytes("files/facenet_512.tflite"))
            faceMatchLiteRtModel =
                FaceMatchLiteRtModel(modelData, imageSquareSize = 160, embeddingsArraySize = 512)

            if (DigitalCredentials.Default.available) {
                DigitalCredentials.Default.startExportingCredentials(
                    documentStore = documentStore,
                    documentTypeRepository = documentTypeRepository
                )
            }

            provisioningModel = ProvisioningModel(
                documentStore = documentStore,
                secureArea = secureArea,
                httpClient = HttpClient() {
                    followRedirects = false
                },
                promptModel = promptModel,
                documentMetadataInitializer = ::initializeDocumentMetadata
            )
            provisioningSupport = ProvisioningSupport(
                storage = storage,
                secureArea = secureArea,
            )
            provisioningSupport.init()

            zkSystemRepository = zkSystemRepositoryInit()

            isInitialized = true
        }
    }

    @Composable
    @Preview
    fun Content() {
        val navController = rememberNavController()
        val identityIssuer = "Multipaz Getting Started Sample"

        // to track initialization
        val isInitialized = remember { mutableStateOf(false) }

        if (!isInitialized.value) {
            CoroutineScope(Dispatchers.Main).launch {
                init()
                isInitialized.value = true
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Initializing...")
            }
            return
        }

        val context = LocalPlatformContext.current
        val imageLoader = remember {
            ImageLoader.Builder(context).components { /* network loader omitted */ }.build()
        }

        var isProvisioning by remember { mutableStateOf(false) }
        val provisioningState = provisioningModel.state.collectAsState().value

        val documents = remember { mutableStateListOf<Document>() }

        LaunchedEffect(navController.currentDestination) {
            val currentDocuments = listDocuments()
            if (currentDocuments.size != documents.size) {
                documents.apply {
                    clear()
                    addAll(currentDocuments)
                }
            }
        }

        LaunchedEffect(isProvisioning) {
            if (isProvisioning) {
                navController.navigate(Destination.ProvisioningDestination.route)
            }
        }

        MaterialTheme {
            // This ensures all prompts inherit the app's main style
            PromptDialogs(promptModel)

            // Use the working pattern from identity-credential project
            LaunchedEffect(true) {
                if (!provisioningModel.isActive) {
                    while (true) {
                        val credentialOffer = credentialOffers.receive()
                        provisioningModel.launchOpenID4VCIProvisioning(
                            offerUri = credentialOffer,
                            clientPreferences = provisioningSupport.getOpenID4VCIClientPreferences(),
                            backend = provisioningSupport.getOpenID4VCIBackend()
                        )
                        isProvisioning = true
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = Destination.HomeDestination.route,
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            ) {
                composable(route = Destination.HomeDestination.route) {
                    HomeScreen(
                        app = this@App,
                        navController = navController,
                        imageLoader = imageLoader,
                        identityIssuer = identityIssuer,
                        documents = documents,
                        onDeleteDocument = {
                            documents.remove(it)
                        }
                    )
                }
                composable(
                    route = Destination.ShowResponseDestination.routeWithArgs,
                    arguments = Destination.ShowResponseDestination.arguments
                ) { backStackEntry ->
                    val vpToken =
                        backStackEntry.arguments?.getString(Destination.ShowResponseDestination.VP_TOKEN)
                            ?.let {
                                if (it != "_") Json.decodeFromString<JsonObject>(
                                    it.fromBase64Url().decodeToString()
                                ) else null
                            }
                    val deviceResponse =
                        backStackEntry.arguments?.getString(Destination.ShowResponseDestination.DEVICE_RESPONSE)
                            ?.let { if (it != "_") Cbor.decode(it.fromBase64Url()) else null }
                    val sessionTranscript =
                        backStackEntry.arguments!!.getString(Destination.ShowResponseDestination.SESSION_TRANSCRIPT)!!
                            .fromBase64Url().let { Cbor.decode(it) }
                    val nonce =
                        backStackEntry.arguments?.getString(Destination.ShowResponseDestination.NONCE)
                            ?.let { if (it != "_") ByteString(it.fromBase64Url()) else null }
                    val eReaderKey =
                        backStackEntry.arguments?.getString(Destination.ShowResponseDestination.EREADERKEY)
                            ?.let { if (it != "_") Cbor.decode(it.fromBase64Url()).asCoseKey.ecPrivateKey else null }
                    val metadata =
                        backStackEntry.arguments!!.getString(Destination.ShowResponseDestination.METADATA)!!
                            .fromBase64Url()
                            .let { ShowResponseMetadata.Companion.fromDataItem(Cbor.decode(it)) }
                    ShowResponseScreen(
                        vpToken = vpToken,
                        deviceResponse = deviceResponse,
                        sessionTranscript = sessionTranscript,
                        nonce = nonce,
                        eReaderKey = eReaderKey,
                        metadata = metadata,
                        issuerTrustManager = issuerTrustManager,
                        documentTypeRepository = documentTypeRepository,
                        zkSystemRepository = zkSystemRepository,
                        onViewCertChain = { certChain ->
                            val encodedCertificateData =
                                Cbor.encode(certChain.toDataItem()).toBase64Url()
                            navController.navigate(Destination.CertificateViewerDestination.route + "/${encodedCertificateData}")
                        }
                    )
                }

                composable(
                    route = Destination.CertificateViewerDestination.routeWithArgs,
                    arguments = Destination.CertificateViewerDestination.arguments
                ) { backStackEntry ->
                    val certData = backStackEntry.arguments?.getString(
                        Destination.CertificateViewerDestination.CERTIFICATE_DATA
                    )!!
                    CertificateScreen(certData)
                }

                composable(
                    route = Destination.ProvisioningDestination.route
                ) {
                    ProvisioningScreen(
                        provisioningModel = provisioningModel,
                        provisioningSupport = provisioningSupport,
                        provisioningState = provisioningState,
                        goBack = {
                            isProvisioning = false
                            provisioningModel.cancel()
                            presentmentModel
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun ShowQrButton(onQrButtonClicked: (settings: MdocProximityQrSettings) -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                val connectionMethods = listOf(
                    MdocConnectionMethodBle(
                        supportsPeripheralServerMode = false,
                        supportsCentralClientMode = true,
                        peripheralServerModeUuid = null,
                        centralClientModeUuid = UUID.randomUUID(),
                    )
                )
                onQrButtonClicked(
                    MdocProximityQrSettings(
                        availableConnectionMethods = connectionMethods,
                        createTransportOptions = MdocTransportOptions(bleUseL2CAP = true)
                    )
                )
            }) {
                Text("Present mDL via QR Code")
            }
        }
    }

    @Composable
    fun ShowQrCode(uri: String) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val qrCodeBitmap = remember { generateQrCode(uri) }
            Text(text = "Present QR code to mdoc reader")
            Image(
                modifier = Modifier.fillMaxWidth(),
                bitmap = qrCodeBitmap,
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
            Button(
                onClick = {
                    presentmentModel.reset()
                }
            ) {
                Text("Cancel")
            }
        }
    }

    private suspend fun initializeDocumentMetadata(
        metadata: AbstractDocumentMetadata,
        credentialDisplay: Display,
        issuerDisplay: Display
    ) {
        (metadata as DocumentMetadata).setMetadata(
            displayName = credentialDisplay.text,
            typeDisplayName = credentialDisplay.text,
            cardArt = credentialDisplay.logo
                ?: ByteString(Res.readBytes("drawable/profile.png")),
            issuerLogo = issuerDisplay.logo,
            other = null
        )
    }

    /**
     * Handle a link (either a app link, universal link, or custom URL schema link).
     */
    fun handleUrl(url: String) {
        if (url.startsWith(OID4VCI_CREDENTIAL_OFFER_URL_SCHEME)
            || url.startsWith(HAIP_URL_SCHEME)
        ) {
            val queryIndex = url.indexOf('?')
            if (queryIndex >= 0) {
                CoroutineScope(Dispatchers.Default).launch {
                    credentialOffers.send(url)
                }
            }
        } else if (url.startsWith(ProvisioningSupport.APP_LINK_BASE_URL)) {
            CoroutineScope(Dispatchers.Default).launch {
                provisioningSupport.processAppLinkInvocation(url)
            }
        }
    }

    suspend fun listDocuments(): MutableList<Document> {
        val documents = mutableStateListOf<Document>()
        for (documentId in documentStore.listDocuments()) {
            documentStore.lookupDocument(documentId)?.let { document ->
                if (!documents.contains(document)) {
                    documents.add(document)
                }
            }
        }
        return documents
    }

    /** Cut out the face square, rotate it to level eyes line, scale to the smaller size for face matching tasks. */
    fun extractFaceBitmap(
        frameData: CameraFrame,
        face: DetectedFace,
        targetSize: Int
    ): ImageBitmap {
        val leftEye = face.landmarks.find { it.type == FaceLandmarkType.LEFT_EYE }
        val rightEye = face.landmarks.find { it.type == FaceLandmarkType.RIGHT_EYE }
        val mouthPosition = face.landmarks.find { it.type == FaceLandmarkType.MOUTH_BOTTOM }

        if (leftEye == null || rightEye == null || mouthPosition == null) {
            return frameData.cameraImage.toImageBitmap()
        }

        // Heuristic multiplier to fit the face normalized to the eyes pupilar distance.
        val faceCropFactor = 4f

        // Heuristic multiplier to offset vertically so the face is better centered within the rectangular crop.
        val faceVerticalOffsetFactor = 0.25f

        var faceCenterX = (leftEye.position.x + rightEye.position.x) / 2
        var faceCenterY = (leftEye.position.y + rightEye.position.y) / 2
        val eyeOffsetX = leftEye.position.x - rightEye.position.x
        val eyeOffsetY = leftEye.position.y - rightEye.position.y
        val eyeDistance = sqrt(eyeOffsetX * eyeOffsetX + eyeOffsetY * eyeOffsetY)
        val faceWidth = eyeDistance * faceCropFactor
        val faceVerticalOffset = eyeDistance * faceVerticalOffsetFactor
        if (frameData.isLandscape) {
            /** Required for iOS capable of upside-down face detection. */
            faceCenterY += faceVerticalOffset * (if (leftEye.position.y < mouthPosition.position.y) 1 else -1)
        } else {
            /** Required for iOS capable of upside-down face detection. */
            faceCenterX -= faceVerticalOffset * (if (leftEye.position.x < mouthPosition.position.x) -1 else 1)
        }
        val eyesAngleRad = atan2(eyeOffsetY, eyeOffsetX)
        val eyesAngleDeg = eyesAngleRad * 180.0 / PI // Convert radians to degrees
        val totalRotationDegrees = 180 - eyesAngleDeg

        // Call platform dependent bitmap transformation.
        return cropRotateScaleImage(
            frameData = frameData, // Platform-specific image data.
            cx = faceCenterX.toDouble(), // Point between eyes
            cy = faceCenterY.toDouble(), // Point between eyes
            angleDegrees = totalRotationDegrees, //includes the camera rotation and eyes rotation.
            outputWidthPx = faceWidth.toInt(), // Expected face width for cropping *before* final scaling.
            outputHeightPx = faceWidth.toInt(),// Expected face height for cropping *before* final scaling.
            targetWidthPx = targetSize, // Final square image size (for database saving and face matching tasks).
        )
    }

    /**
     * Initialize Zero-Knowledge Proof System Repository
     *
     * Loads Longfellow ZKP circuits that enable privacy-preserving credential verification.
     *
     * **What are ZKP Circuits?**
     * - Pre-computed cryptographic circuits for zero-knowledge proofs
     * - Allow proving properties about credentials without revealing the data
     * - Example: Prove "age > 21" without revealing exact birthdate
     *
     * **Circuit Files:**
     * - Each file is ~300KB and contains circuit parameters
     * - Named with complexity parameters and a hash for integrity
     * - Loaded from app resources at runtime
     *
     * **When to use:**
     * - Enable for privacy-sensitive verification scenarios
     * - Disable if you only need standard credential verification (saves ~1.2MB memory)
     *
     * @return Initialized ZkSystemRepository with Longfellow circuits
     */
    private suspend fun zkSystemRepositoryInit(): ZkSystemRepository {
        val longfellowSystem = LongfellowZkSystem()

        // Load each circuit file from resources
        for (circuit in LONGFELLOW_CIRCUIT_FILES) {
            val circuitBytes = Res.readBytes(circuit)
            val pathParts = circuit.split("/")
            val circuitName = pathParts[pathParts.size - 1]  // Extract filename from path
            longfellowSystem.addCircuit(circuitName, ByteString(circuitBytes))
        }

        val zkSystemRepository = ZkSystemRepository().apply {
            add(longfellowSystem)
        }

        return zkSystemRepository
    }

    companion object {

        const val SAMPLE_DOCUMENT_DISPLAY_NAME = "Erika's Driving License"
        private const val SAMPLE_DOCUMENT_TYPE_DISPLAY_NAME = "Utopia Driving License"

        // OID4VCI url scheme used for filtering OID4VCI Urls from all incoming URLs (deep links or QR)
        private const val OID4VCI_CREDENTIAL_OFFER_URL_SCHEME = "openid-credential-offer://"
        private const val HAIP_URL_SCHEME = "haip://"

        // Domains used for MdocCredential & SdJwtVcCredential
        private const val CREDENTIAL_DOMAIN_MDOC_USER_AUTH = "mdoc_user_auth"
        private const val CREDENTIAL_DOMAIN_MDOC_MAC_USER_AUTH = "mdoc_mac_user_auth"
        private const val CREDENTIAL_DOMAIN_SDJWT_USER_AUTH = "sdjwt_user_auth"
        private const val CREDENTIAL_DOMAIN_SDJWT_KEYLESS = "sdjwt_keyless"

        private const val STORAGE_TABLE_NAME = "TestAppKeys"

        // Zero-Knowledge Proof (ZKP) Configuration - Longfellow Circuits
// ---------------------------------------------------------------
// These are pre-computed cryptographic circuits for Zero-Knowledge Proofs.
// Longfellow enables privacy-preserving credential verification (e.g., proving age > 21
// without revealing exact birthdate).
//
// The circuits are named: {params}_{hash} where:
// - params describe circuit complexity (e.g., "6_1_4096_2945")
// - hash is a SHA-256 identifier for integrity verification
//
// These files are bundled in: composeApp/src/commonMain/composeResources/files/longfellow-libzk-v1/
// If you don't need ZKP features, you can skip loading these circuits.
        private val LONGFELLOW_CIRCUIT_FILES = listOf(
            "files/longfellow-libzk-v1/6_1_4096_2945_137e5a75ce72735a37c8a72da1a8a0a5df8d13365c2ae3d2c2bd6a0e7197c7c6",
            "files/longfellow-libzk-v1/6_2_4025_2945_b4bb6f01b7043f4f51d8302a30b36e3d4d2d0efc3c24557ab9212ad524a9764e",
            "files/longfellow-libzk-v1/6_3_4121_2945_b2211223b954b34a1081e3fbf71b8ea2de28efc888b4be510f532d6ba76c2010",
            "files/longfellow-libzk-v1/6_4_4283_2945_c70b5f44a1365c53847eb8948ad5b4fdc224251a2bc02d958c84c862823c49d6",
        )


        val promptModel = org.multipaz.util.Platform.promptModel

        private var app: App? = null
        fun getInstance(): App {
            if (app == null) {
                app = App()
            }
            return app!!
        }
    }
}