package org.multipaz.getstarted

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import multipazgettingstartedsample.composeapp.generated.resources.Res
import multipazgettingstartedsample.composeapp.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.multipaz.asn1.ASN1Integer
import org.multipaz.cbor.Simple
import org.multipaz.compose.permissions.rememberBluetoothEnabledState
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import org.multipaz.compose.presentment.MdocProximityQrPresentment
import org.multipaz.compose.presentment.MdocProximityQrSettings
import org.multipaz.compose.presentment.Presentment
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.compose.qrcode.generateQrCode
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.X500Name
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.multipaz.document.DocumentStore
import org.multipaz.document.buildDocumentStore
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.engagement.EngagementGenerator
import org.multipaz.mdoc.role.MdocRole
import org.multipaz.mdoc.transport.MdocTransportFactory
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.mdoc.transport.advertise
import org.multipaz.mdoc.transport.waitForConnection
import org.multipaz.mdoc.util.MdocUtil
import org.multipaz.models.digitalcredentials.DigitalCredentials
import org.multipaz.models.presentment.MdocPresentmentMechanism
import org.multipaz.models.presentment.PresentmentModel
import org.multipaz.models.presentment.PresentmentSource
import org.multipaz.models.presentment.SimplePresentmentSource
import org.multipaz.securearea.CreateKeySettings
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.storage.Storage
import org.multipaz.trustmanagement.TrustManagerLocal
import org.multipaz.trustmanagement.TrustMetadata
import org.multipaz.trustmanagement.TrustPointAlreadyExistsException
import org.multipaz.util.UUID
import org.multipaz.util.fromHex
import org.multipaz.util.toBase64Url
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

    val appName = "Multipaz Getting Started Sample"
    val appIcon = Res.drawable.compose_multiplatform

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

                val iacaKey = EcPrivateKey.fromPem(
                    Res.readBytes("files/iaca_private_key.pem").decodeToString(),
                    iacaCert.ecPublicKey
                )

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
                    iacaCert = iacaCert,
                    iacaKey = iacaKey,
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
                    dsKey = dsKey,
                    dsCertChain = X509CertChain(listOf(dsCert)),
                    signedAt = signedAt,
                    validFrom = validFrom,
                    validUntil = validUntil,
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
                domainMdocSignature = "mdoc",
            )

            if (DigitalCredentials.Default.available) {
                DigitalCredentials.Default.startExportingCredentials(
                    documentStore = documentStore,
                    documentTypeRepository = documentTypeRepository
                )
            }

            isInitialized = true
        }
    }

    @Composable
    @Preview
    fun Content() {

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

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // todo: add button to list docs
                // todo: add button for deletion

                // Bluetooth Permission
                if (!blePermissionState.isGranted) {
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
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = { coroutineScope.launch { bleEnabledState.enable() } }) {
                            Text("Enable Bluetooth")
                        }
                    }
                } else {
                    MdocProximityQrPresentment(
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
                }
            }
        }
    }

    companion object {
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
}