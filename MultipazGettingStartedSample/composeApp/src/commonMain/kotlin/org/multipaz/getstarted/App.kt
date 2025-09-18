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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import multipazgettingstartedsample.composeapp.generated.resources.Res
import multipazgettingstartedsample.composeapp.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.multipaz.asn1.ASN1Integer
import org.multipaz.cbor.Simple
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
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

                val iacaCert = X509Cert.fromPem(
                    """
                        -----BEGIN CERTIFICATE-----
                        MIICYzCCAemgAwIBAgIQ36kOae8cfvOqQ+mO4YhnpDAKBggqhkjOPQQDAzAuMQswCQYDVQQGDAJV
                        UzEfMB0GA1UEAwwWT1dGIE11bHRpcGF6IFRFU1QgSUFDQTAeFw0yNTA3MjQxMTE3MTlaFw0zMDA3
                        MjQxMTE3MTlaMC4xCzAJBgNVBAYMAlVTMR8wHQYDVQQDDBZPV0YgTXVsdGlwYXogVEVTVCBJQUNB
                        MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEQQJf9BH+fJytVI4K4nQvHJAfzapvuT6jo+19fo+o9+zV
                        PFnOYtsbPXB5sPeuMMv5ZkQGmn9yWCgpbZHAS2pJ/eJXAcLp9uH8BGo6pYhkPomx9cwgMX0YUXoB
                        4wiO6w9eo4HLMIHIMA4GA1UdDwEB/wQEAwIBBjASBgNVHRMBAf8ECDAGAQH/AgEAMC0GA1UdEgQm
                        MCSGImh0dHBzOi8vaXNzdWVyLmV4YW1wbGUuY29tL3dlYnNpdGUwMwYDVR0fBCwwKjAooCagJIYi
                        aHR0cHM6Ly9pc3N1ZXIuZXhhbXBsZS5jb20vY3JsLmNybDAdBgNVHQ4EFgQUPbetw5QkxGKjazN0
                        qI9YfaexD+0wHwYDVR0jBBgwFoAUPbetw5QkxGKjazN0qI9YfaexD+0wCgYIKoZIzj0EAwMDaAAw
                        ZQIxAKizj2YexKf1+CTBCOV4ehyiUU5MSi9iPScW32+halSCVUtbmW63fpG+37obLGivegIwb38g
                        xhIRxDdIk1CBVsqANCFUvdBuSoORRV5928xo/B9he5ZFyb8b6UauJS70AMD8
                        -----END CERTIFICATE-----
                    """.trimIndent()
                )

                val iacaKey = EcPrivateKey.fromPem(
                    """
                        -----BEGIN PRIVATE KEY-----
                        MFcCAQAwEAYHKoZIzj0CAQYFK4EEACIEQDA+AgEBBDBEPQnb6xr3p0XKGucrf3iVI/sDF2fc55vs
                        T31kxam8x8ocKu4ETouTZM+DZKu0cD+gBwYFK4EEACI=
                        -----END PRIVATE KEY-----
                    """.trimIndent(),
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
                        """
                                -----BEGIN CERTIFICATE-----
                                MIICUTCCAdegAwIBAgIQppKZHI1iPN290JKEA79OpzAKBggqhkjOPQQDAzArMSkwJwYDVQQDDCBP
                                V0YgTXVsdGlwYXogVGVzdEFwcCBSZWFkZXIgUm9vdDAeFw0yNDEyMDEwMDAwMDBaFw0zNDEyMDEw
                                MDAwMDBaMCsxKTAnBgNVBAMMIE9XRiBNdWx0aXBheiBUZXN0QXBwIFJlYWRlciBSb290MHYwEAYH
                                KoZIzj0CAQYFK4EEACIDYgAE+QDye70m2O0llPXMjVjxVZz3m5k6agT+wih+L79b7jyqUl99sbeU
                                npxaLD+cmB3HK3twkA7fmVJSobBc+9CDhkh3mx6n+YoH5RulaSWThWBfMyRjsfVODkosHLCDnbPV
                                o4G/MIG8MA4GA1UdDwEB/wQEAwIBBjASBgNVHRMBAf8ECDAGAQH/AgEAMFYGA1UdHwRPME0wS6BJ
                                oEeGRWh0dHBzOi8vZ2l0aHViLmNvbS9vcGVud2FsbGV0LWZvdW5kYXRpb24tbGFicy9pZGVudGl0
                                eS1jcmVkZW50aWFsL2NybDAdBgNVHQ4EFgQUq2Ub4FbCkFPx3X9s5Ie+aN5gyfUwHwYDVR0jBBgw
                                FoAUq2Ub4FbCkFPx3X9s5Ie+aN5gyfUwCgYIKoZIzj0EAwMDaAAwZQIxANN9WUvI1xtZQmAKS4/D
                                ZVwofqLNRZL/co94Owi1XH5LgyiBpS3E8xSxE9SDNlVVhgIwKtXNBEBHNA7FKeAxKAzu4+MUf4gz
                                8jvyFaE0EUVlS2F5tARYQkU6udFePucVdloi
                                -----END CERTIFICATE-----
                            """.trimIndent().trim()
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
                        """
                                -----BEGIN CERTIFICATE-----
                                MIICYTCCAeegAwIBAgIQOSV5JyesOLKHeDc+0qmtuTAKBggqhkjOPQQDAzAzMQswCQYDVQQGDAJV
                                UzEkMCIGA1UEAwwbTXVsdGlwYXogSWRlbnRpdHkgUmVhZGVyIENBMB4XDTI1MDcwNTEyMjAyMVoX
                                DTMwMDcwNTEyMjAyMVowMzELMAkGA1UEBgwCVVMxJDAiBgNVBAMMG011bHRpcGF6IElkZW50aXR5
                                IFJlYWRlciBDQTB2MBAGByqGSM49AgEGBSuBBAAiA2IABD4UX5jabDLuRojEp9rsZkAEbP8Icuj3
                                qN4wBUYq6UiOkoULMOLUb+78Ygonm+sJRwqyDJ9mxYTjlqliW8PpDfulQZejZo2QGqpB9JPInkrC
                                Bol5T+0TUs0ghkE5ZQBsVKOBvzCBvDAOBgNVHQ8BAf8EBAMCAQYwEgYDVR0TAQH/BAgwBgEB/wIB
                                ADBWBgNVHR8ETzBNMEugSaBHhkVodHRwczovL2dpdGh1Yi5jb20vb3BlbndhbGxldC1mb3VuZGF0
                                aW9uLWxhYnMvaWRlbnRpdHktY3JlZGVudGlhbC9jcmwwHQYDVR0OBBYEFM+kr4eQcxKWLk16F2Rq
                                zBxFcZshMB8GA1UdIwQYMBaAFM+kr4eQcxKWLk16F2RqzBxFcZshMAoGCCqGSM49BAMDA2gAMGUC
                                MQCQ+4+BS8yH20KVfSK1TSC/RfRM4M9XNBZ+0n9ePg9ftXUFt5e4lBddK9mL8WznJuoCMFuk8ey4
                                lKnb4nubv5iPIzwuC7C0utqj7Fs+qdmcWNrSYSiks2OEnjJiap1cPOPk2g==
                                -----END CERTIFICATE-----
                           """.trimIndent().trim()
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
                        """
                                -----BEGIN CERTIFICATE-----
                                MIICiTCCAg+gAwIBAgIQQd/7PXEzsmI+U14J2cO1bjAKBggqhkjOPQQDAzBHMQswCQYDVQQGDAJV
                                UzE4MDYGA1UEAwwvTXVsdGlwYXogSWRlbnRpdHkgUmVhZGVyIENBIChVbnRydXN0ZWQgRGV2aWNl
                                cykwHhcNMjUwNzE5MjMwODE0WhcNMzAwNzE5MjMwODE0WjBHMQswCQYDVQQGDAJVUzE4MDYGA1UE
                                AwwvTXVsdGlwYXogSWRlbnRpdHkgUmVhZGVyIENBIChVbnRydXN0ZWQgRGV2aWNlcykwdjAQBgcq
                                hkjOPQIBBgUrgQQAIgNiAATqihOe05W3nIdyVf7yE4mHJiz7tsofcmiNTonwYsPKBbJwRTHa7AME
                                +ToAfNhPMaEZ83lBUTBggsTUNShVp1L5xzPS+jK0tGJkR2ny9+UygPGtUZxEOulGK5I8ZId+35Gj
                                gb8wgbwwDgYDVR0PAQH/BAQDAgEGMBIGA1UdEwEB/wQIMAYBAf8CAQAwVgYDVR0fBE8wTTBLoEmg
                                R4ZFaHR0cHM6Ly9naXRodWIuY29tL29wZW53YWxsZXQtZm91bmRhdGlvbi1sYWJzL2lkZW50aXR5
                                LWNyZWRlbnRpYWwvY3JsMB0GA1UdDgQWBBSbz9r9IFmXjiGGnH3Siq90geurxTAfBgNVHSMEGDAW
                                gBSbz9r9IFmXjiGGnH3Siq90geurxTAKBggqhkjOPQQDAwNoADBlAjEAomqjfJe2k162S5Way3sE
                                BTcj7+DPvaLJcsloEsj/HaThIsKWqQlQKxgNu1rE/XryAjB/Gq6UErgWKlspp+KpzuAAWaKk+bMj
                                cM4aKOKOU3itmB+9jXTQ290Dc8MnWVwQBs4=
                                -----END CERTIFICATE-----
                           """.trimIndent().trim()
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
            try {
                readerTrustManager.addX509Cert(
                    certificate = X509Cert(
                        "30820269308201efa0030201020210b7352f14308a2d40564006785270b0e7300a06082a8648ce3d0403033037310b300906035504060c0255533128302606035504030c1f76657269666965722e6d756c746970617a2e6f726720526561646572204341301e170d3235303631393232313633325a170d3330303631393232313633325a3037310b300906035504060c0255533128302606035504030c1f76657269666965722e6d756c746970617a2e6f7267205265616465722043413076301006072a8648ce3d020106052b81040022036200046baa02cc2f2b7c77f054e9907fcdd6c87110144f07acb2be371b2e7c90eb48580c5e3851bcfb777c88e533244069ff78636e54c7db5783edbc133cc1ff11bbabc3ff150f67392264c38710255743fee7cde7df6e55d7e9d5445d1bde559dcba8a381bf3081bc300e0603551d0f0101ff04040302010630120603551d130101ff040830060101ff02010030560603551d1f044f304d304ba049a047864568747470733a2f2f6769746875622e636f6d2f6f70656e77616c6c65742d666f756e646174696f6e2d6c6162732f6964656e746974792d63726564656e7469616c2f63726c301d0603551d0e04160414b18439852f4a6eeabfea62adbc51d081f7488729301f0603551d23041830168014b18439852f4a6eeabfea62adbc51d081f7488729300a06082a8648ce3d040303036800306502302a1f3bb0afdc31bcee73d3c5bf289245e76bd91a0fd1fb852b45fc75d3a98ba84430e6a91cbfc6b3f401c91382a43a64023100db22d2243644bb5188f2e0a102c0c167024fb6fe4a1d48ead55a6893af52367fb3cdbd66369aa689ecbeb5c84f063666".fromHex()
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

        MaterialTheme {
            // This ensures all prompts inherit the app's main style
            PromptDialogs(promptModel)

            val blePermissionState = rememberBluetoothPermissionState()
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
                } else {
                    val deviceEngagement = remember { mutableStateOf<ByteString?>(null) }
                    val state = presentmentModel.state.collectAsState()
                    when (state.value) {
                        PresentmentModel.State.IDLE -> {
                            ShowQrButton(deviceEngagement)
                        }

                        PresentmentModel.State.CONNECTING -> {
                            ShowQrCode(deviceEngagement)
                        }

                        PresentmentModel.State.WAITING_FOR_SOURCE,
                        PresentmentModel.State.PROCESSING,
                        PresentmentModel.State.WAITING_FOR_DOCUMENT_SELECTION,
                        PresentmentModel.State.WAITING_FOR_CONSENT,
                        PresentmentModel.State.COMPLETED -> {
                            Presentment(
                                appName = "Multipaz Getting Started Sample",
                                appIconPainter = painterResource(Res.drawable.compose_multiplatform),
                                presentmentModel = presentmentModel,
                                presentmentSource = presentmentSource,
                                documentTypeRepository = documentTypeRepository,
                                onPresentmentComplete = {
                                    presentmentModel.reset()
                                },
                            )
                        }
                    }
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
    private fun ShowQrButton(showQrCode: MutableState<ByteString?>) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                presentmentModel.reset()
                presentmentModel.setConnecting()
                presentmentModel.presentmentScope.launch() {
                    val connectionMethods = listOf(
                        MdocConnectionMethodBle(
                            supportsPeripheralServerMode = false,
                            supportsCentralClientMode = true,
                            peripheralServerModeUuid = null,
                            centralClientModeUuid = UUID.randomUUID(),
                        )
                    )
                    val eDeviceKey = Crypto.createEcPrivateKey(EcCurve.P256)
                    val advertisedTransports = connectionMethods.advertise(
                        role = MdocRole.MDOC,
                        transportFactory = MdocTransportFactory.Default,
                        options = MdocTransportOptions(bleUseL2CAP = true),
                    )
                    val engagementGenerator = EngagementGenerator(
                        eSenderKey = eDeviceKey.publicKey,
                        version = "1.0"
                    )
                    engagementGenerator.addConnectionMethods(advertisedTransports.map {
                        it.connectionMethod
                    })
                    val encodedDeviceEngagement = ByteString(engagementGenerator.generate())
                    showQrCode.value = encodedDeviceEngagement
                    val transport = advertisedTransports.waitForConnection(
                        eSenderKey = eDeviceKey.publicKey,
                        coroutineScope = presentmentModel.presentmentScope
                    )
                    presentmentModel.setMechanism(
                        MdocPresentmentMechanism(
                            transport = transport,
                            eDeviceKey = eDeviceKey,
                            encodedDeviceEngagement = encodedDeviceEngagement,
                            handover = Simple.NULL,
                            engagementDuration = null,
                            allowMultipleRequests = false
                        )
                    )
                    showQrCode.value = null
                }
            }) {
                Text("Present mDL via QR")
            }
        }
    }

    @Composable
    private fun ShowQrCode(deviceEngagement: MutableState<ByteString?>) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (deviceEngagement.value != null) {
                val mdocUrl = "mdoc:" + deviceEngagement.value!!.toByteArray().toBase64Url()
                val qrCodeBitmap = remember { generateQrCode(mdocUrl) }
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
}