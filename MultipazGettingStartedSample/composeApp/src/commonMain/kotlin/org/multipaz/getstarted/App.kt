package org.multipaz.getstarted

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import multipazgettingstartedsample.composeapp.generated.resources.Res
import multipazgettingstartedsample.composeapp.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.multipaz.asn1.ASN1Integer
import org.multipaz.compose.camera.Camera
import org.multipaz.compose.camera.CameraCaptureResolution
import org.multipaz.compose.camera.CameraFrame
import org.multipaz.compose.camera.CameraSelection
import org.multipaz.compose.cropRotateScaleImage
import org.multipaz.compose.decodeImage
import org.multipaz.compose.permissions.rememberBluetoothEnabledState
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import org.multipaz.compose.permissions.rememberCameraPermissionState
import org.multipaz.compose.presentment.MdocProximityQrPresentment
import org.multipaz.compose.presentment.MdocProximityQrSettings
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.compose.provisioning.Provisioning
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
import org.multipaz.facedetection.detectFaces
import org.multipaz.facematch.FaceEmbedding
import org.multipaz.facematch.FaceMatchLiteRtModel
import org.multipaz.facematch.getFaceEmbeddings
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.mdoc.util.MdocUtil
import org.multipaz.presentment.model.PresentmentModel
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.presentment.model.SimplePresentmentSource
import org.multipaz.provisioning.Display
import org.multipaz.provisioning.ProvisioningModel
import org.multipaz.securearea.CreateKeySettings
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.selfiecheck.SelfieCheck
import org.multipaz.selfiecheck.SelfieCheckViewModel
import org.multipaz.storage.Storage
import org.multipaz.trustmanagement.TrustManagerLocal
import org.multipaz.trustmanagement.TrustMetadata
import org.multipaz.trustmanagement.TrustPointAlreadyExistsException
import org.multipaz.util.Platform.promptModel
import org.multipaz.util.UUID
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

class App {

    // Storage
    lateinit var storage: Storage
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

    val appName = "Multipaz Getting Started Sample"
    val appIcon = Res.drawable.compose_multiplatform

    lateinit var faceMatchLiteRtModel: FaceMatchLiteRtModel

    var isInitialized = false

    @OptIn(ExperimentalTime::class)
    suspend fun init() {
        if (!isInitialized) {

            // Storage
            storage = org.multipaz.util.Platform.nonBackedUpStorage
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

            // Creation of an mDoc
            if (documentStore.listDocuments().isEmpty()) {

                // Creating a Document
                val document = documentStore.createDocument(
                    displayName = "Erika's Driving License",
                    typeDisplayName = "Utopia Driving License",
                )

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
                val dsCert = MdocUtil.generateDsCertificate(
                    iacaKey = AsymmetricKey.X509CertifiedExplicit(
                        certChain = X509CertChain(certificates = listOf(iacaCert)),
                        privateKey = dsKey,
                    ),
                    dsKey = dsKey.publicKey,
                    subject = X500Name.fromName(name = "CN=Test DS Key"),
                    serial = ASN1Integer.fromRandom(numBits = 128),
                    validFrom = validFrom,
                    validUntil = validUntil
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

            isInitialized = true
        }
    }

    @Composable
    @Preview
    fun Content() {

        val identityIssuer = "Multipaz Getting Started Sample"
        val selfieCheckViewModel: SelfieCheckViewModel =
            remember { SelfieCheckViewModel(identityIssuer) }

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

        MaterialTheme {
            // This ensures all prompts inherit the app's main style
            PromptDialogs(promptModel)

            val blePermissionState = rememberBluetoothPermissionState()
            val bleEnabledState = rememberBluetoothEnabledState()
            val coroutineScope = rememberCoroutineScope { promptModel }

            var isProvisioning by remember { mutableStateOf(false) }
            val provisioningState = provisioningModel.state.collectAsState().value
            val uriHandler = LocalUriHandler.current

            // Use the working pattern from identity-credential project
            LaunchedEffect(true) {
                if (!provisioningModel.isActive)  {
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

            val cameraPermissionState = rememberCameraPermissionState()

            var showCamera by remember { mutableStateOf(false) }
            val faceCaptured = remember { mutableStateOf<FaceEmbedding?>(null) }
            var showFaceMatching by remember { mutableStateOf(false) }
            var similarity by remember { mutableStateOf(0f) }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if (isProvisioning) {
                    Provisioning(
                        provisioningModel = provisioningModel,
                        waitForRedirectLinkInvocation = { state ->
                            provisioningSupport.waitForAppLinkInvocation(state)
                        }
                    )
                    Button(onClick = {
                        provisioningModel.cancel();
                        isProvisioning = false
                    }) {
                        Text(
                            if (provisioningState is ProvisioningModel.CredentialsIssued)
                                "Go Back"
                            else if (provisioningState is ProvisioningModel.Error)
                                "An Error Occurred\nTry Again"
                            else
                                "Cancel"
                        )
                    }
                    // Bluetooth Permission
                } else if (!blePermissionState.isGranted) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                blePermissionState.launchPermissionRequest()
                            }
                        }
                    ) {
                        Text("Request BLE permissions")
                    }
                } else if (!bleEnabledState.isEnabled) {
                    Button(
                        onClick = { coroutineScope.launch { bleEnabledState.enable() } }) {
                        Text("Enable Bluetooth")
                    }
                } else {
                    MdocProximityQrPresentment(
                        modifier = Modifier.weight(1f),
                        appName = appName,
                        appIconPainter = painterResource(appIcon),
                        presentmentModel = presentmentModel,
                        presentmentSource = presentmentSource,
                        promptModel = promptModel,
                        documentTypeRepository = documentTypeRepository,
                        imageLoader = imageLoader,
                        allowMultipleRequests = false,
                        showQrButton = { onQrButtonClicked -> ShowQrButton(onQrButtonClicked) },
                        showQrCode = { uri -> ShowQrCode(uri) }
                    )

                    Button(
                        modifier = Modifier.padding(16.dp),
                        onClick = {
                            uriHandler.openUri("https://issuer.multipaz.org")
                        }) {
                        Text(
                            buildAnnotatedString {
                                withStyle(style = SpanStyle(fontSize = 14.sp)) {
                                    append("Issue an mDoc from the server")
                                }
                                withStyle(style = SpanStyle(fontSize = 12.sp)) {
                                    append("\nhttps://issuer.multipaz.org")
                                }
                            },
                            textAlign = TextAlign.Center
                        )
                    }

                    var documents = remember { mutableStateListOf<Document>() }

                    LaunchedEffect(isInitialized.value, documents) {
                        if (isInitialized.value) {
                            documents.addAll(listDocuments())
                        }
                    }

                    if (documents.isNotEmpty()) {
                        Text(
                            modifier = Modifier.padding(4.dp),
                            text = "${documents.size} Documents present:"
                        )
                        for (document in documents) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = document.metadata.displayName ?: document.identifier,
                                    modifier = Modifier.padding(4.dp)
                                )
                                IconButton(
                                    content = @Composable {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        coroutineScope.launch {
                                            documentStore.deleteDocument(document.identifier)
                                            documents.remove(document)
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        Text(text = "No documents found.")
                    }
                }

                if (!cameraPermissionState.isGranted)
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        }) {
                        Text("Grant Camera Permission for Selfie Check")
                    }
                else if (faceCaptured.value == null) {
                    if (!showCamera)
                        Button(
                            onClick = { showCamera = true }) {
                            Text("Selfie Check")
                        }
                    else {
                        SelfieCheck(
                            modifier = Modifier.fillMaxWidth(),
                            onVerificationComplete = {
                                showCamera = false
                                if (selfieCheckViewModel.capturedFaceImage != null)
                                    faceCaptured.value =
                                        getFaceEmbeddings(
                                            image = decodeImage(selfieCheckViewModel.capturedFaceImage!!.toByteArray()),
                                            model = faceMatchLiteRtModel
                                        )

                                selfieCheckViewModel.resetForNewCheck()
                            },
                            viewModel = selfieCheckViewModel,
                            identityIssuer = identityIssuer
                        )
                        Button(
                            onClick = {
                                showCamera = false
                                selfieCheckViewModel.resetForNewCheck()
                            }) {
                            Text("Close")
                        }
                    }
                } else {
                    if (!showFaceMatching)
                        Button(
                            onClick = {
                                showFaceMatching = true
                            }) {
                            Text("Face Matching")
                        }
                    else {
                        Text("Similarity: ${(similarity * 100).roundToInt()}%")

                        Camera(
                            modifier = Modifier
                                .fillMaxSize(0.5f)
                                .padding(64.dp),
                            cameraSelection = CameraSelection.DEFAULT_FRONT_CAMERA,
                            captureResolution = CameraCaptureResolution.MEDIUM,
                            showCameraPreview = true,
                        ) { incomingVideoFrame: CameraFrame ->

                            val faces = detectFaces(incomingVideoFrame)

                            if (faces.isNullOrEmpty()) {
                                similarity = 0f;
                            } else if (faceCaptured.value != null) {
                                val faceImage =
                                    extractFaceBitmap(
                                        incomingVideoFrame,
                                        faces[0], // assuming only one face exists for simplicity
                                        faceMatchLiteRtModel.imageSquareSize
                                    )

                                val faceInsetsForDetectedFace =
                                    getFaceEmbeddings(faceImage, faceMatchLiteRtModel)

                                if (faceInsetsForDetectedFace != null) {
                                    similarity = faceCaptured.value!!.calculateSimilarity(
                                        faceInsetsForDetectedFace
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                showFaceMatching = false
                                faceCaptured.value = null
                            }) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }

    companion object {
        // OID4VCI url scheme used for filtering OID4VCI Urls from all incoming URLs (deep links or QR)
        private const val OID4VCI_CREDENTIAL_OFFER_URL_SCHEME = "openid-credential-offer://"
        private const val HAIP_URL_SCHEME = "haip://"

        // Domains used for MdocCredential & SdJwtVcCredential
        private const val CREDENTIAL_DOMAIN_MDOC_USER_AUTH = "mdoc_user_auth"
        private const val CREDENTIAL_DOMAIN_MDOC_MAC_USER_AUTH = "mdoc_mac_user_auth"
        private const val CREDENTIAL_DOMAIN_SDJWT_USER_AUTH = "sdjwt_user_auth"
        private const val CREDENTIAL_DOMAIN_SDJWT_KEYLESS = "sdjwt_keyless"

        val promptModel = org.multipaz.util.Platform.promptModel

        private var app: App? = null
        fun getInstance(): App {
            if (app == null) {
                app = App()
            }
            return app!!
        }
    }

    @Composable
    private fun ShowQrButton(onQrButtonClicked: (settings: MdocProximityQrSettings) -> Unit) {
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
    private fun ShowQrCode(uri: String) {
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
    private fun extractFaceBitmap(
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
}