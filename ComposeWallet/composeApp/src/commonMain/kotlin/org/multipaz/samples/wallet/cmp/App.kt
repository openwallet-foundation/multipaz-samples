package org.multipaz.samples.wallet.cmp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.bytestring.ByteString
import mpzcmpwallet.composeapp.generated.resources.Res
import mpzcmpwallet.composeapp.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource
import org.multipaz.compose.document.DocumentModel
import org.multipaz.compose.permissions.rememberBluetoothEnabledState
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import org.multipaz.compose.presentment.MdocProximityQrPresentment
import org.multipaz.compose.presentment.MdocProximityQrSettings
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.compose.provisioning.Provisioning
import org.multipaz.compose.qrcode.generateQrCode
import org.multipaz.crypto.X509Cert
import org.multipaz.digitalcredentials.Default
import org.multipaz.digitalcredentials.DigitalCredentials
import org.multipaz.document.AbstractDocumentMetadata
import org.multipaz.document.DocumentMetadata
import org.multipaz.document.DocumentStore
import org.multipaz.document.buildDocumentStore
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.presentment.model.PresentmentModel
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.presentment.model.SimplePresentmentSource
import org.multipaz.provisioning.Display
import org.multipaz.provisioning.ProvisioningModel
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.storage.Storage
import org.multipaz.storage.ephemeral.EphemeralStorage
import org.multipaz.trustmanagement.TrustManagerLocal
import org.multipaz.trustmanagement.TrustMetadata
import org.multipaz.util.Platform
import org.multipaz.util.UUID
import org.multipaz.util.fromHexByteString
import kotlin.time.ExperimentalTime

/**
 * Application singleton.
 *
 * Use [App.Companion.getInstance] to get an instance.
 */
class App() {
    lateinit var storage: Storage
    lateinit var documentTypeRepository: DocumentTypeRepository
    lateinit var secureAreaRepository: SecureAreaRepository
    lateinit var secureArea: SecureArea
    lateinit var documentStore: DocumentStore
    lateinit var documentModel: DocumentModel
    lateinit var readerTrustManager: TrustManagerLocal
    lateinit var presentmentModel: PresentmentModel
    lateinit var presentmentSource: PresentmentSource
    lateinit var provisioningModel: ProvisioningModel
    lateinit var provisioningSupport: ProvisioningSupport

    private val initLock = Mutex()
    private var initialized = false

    val appName = "MpzCmpWallet"
    val appIcon = Res.drawable.compose_multiplatform

    @OptIn(ExperimentalTime::class)
    suspend fun init() {
        initLock.withLock {
            if (initialized) {
                return
            }
            storage = Platform.nonBackedUpStorage
            secureArea = Platform.getSecureArea()
            secureAreaRepository = SecureAreaRepository.Builder().add(secureArea).build()
            documentTypeRepository = DocumentTypeRepository().apply {
                addDocumentType(DrivingLicense.getDocumentType())
            }
            documentStore = buildDocumentStore(storage = storage, secureAreaRepository = secureAreaRepository) {}
            documentModel = DocumentModel(documentStore = documentStore)
            presentmentModel = PresentmentModel().apply { setPromptModel(App.promptModel) }
            readerTrustManager = TrustManagerLocal(
                storage = EphemeralStorage()
            ).apply {
                // The Root CA baked into the Multipaz TestApp
                addX509Cert(
                        certificate = X509Cert
                            ("30820251308201d7a0030201020210a692991c8d623cddbdd0928403bf4ea7300a06082a8648ce3d040303302b3129302706035504030c204f5746204d756c746970617a20546573744170702052656164657220526f6f74301e170d3234313230313030303030305a170d3334313230313030303030305a302b3129302706035504030c204f5746204d756c746970617a20546573744170702052656164657220526f6f743076301006072a8648ce3d020106052b8104002203620004f900f27bbd26d8ed2594f5cc8d58f1559cf79b993a6a04fec2287e2fbf5bee3caa525f7db1b7949e9c5a2c3f9c981dc72b7b70900edf995252a1b05cfbd0838648779b1ea7f98a07e51ba569259385605f332463b1f54e0e4a2c1cb0839db3d5a381bf3081bc300e0603551d0f0101ff04040302010630120603551d130101ff040830060101ff02010030560603551d1f044f304d304ba049a047864568747470733a2f2f6769746875622e636f6d2f6f70656e77616c6c65742d666f756e646174696f6e2d6c6162732f6964656e746974792d63726564656e7469616c2f63726c301d0603551d0e04160414ab651be056c29053f1dd7f6ce487be68de60c9f5301f0603551d23041830168014ab651be056c29053f1dd7f6ce487be68de60c9f5300a06082a8648ce3d0403030368003065023100d37d594bc8d71b5942600a4b8fc3655c287ea2cd4592ff728f783b08b55c7e4b832881a52dc4f314b113d4833655558602302ad5cd044047340ec529e031280ceee3e3147f8833f23bf215a1341145654b6179b4045842453ab9d15e3ee715765a22".fromHexByteString()),
                        metadata = TrustMetadata(
                            displayName = "OWF Multipaz TestApp",
                            displayIconUrl = "https://www.multipaz.org/multipaz-logo-200x200.png",
                        )
                )
                // From https://verifier.multipaz.org/identityreaderbackend/
                addX509Cert(
                    certificate = X509Cert
                        ("30820261308201E7A00302010202103925792727AC38B28778373ED2A9ADB9300A06082A8648CE3D0403033033310B300906035504060C0255533124302206035504030C1B4D756C746970617A204964656E7469747920526561646572204341301E170D3235303730353132323032315A170D3330303730353132323032315A3033310B300906035504060C0255533124302206035504030C1B4D756C746970617A204964656E74697479205265616465722043413076301006072A8648CE3D020106052B81040022036200043E145F98DA6C32EE4688C4A7DAEC6640046CFF0872E8F7A8DE3005462AE9488E92850B30E2D46FEEFC620A279BEB09470AB20C9F66C584E396A9625BC3E90DFBA54197A3668D901AAA41F493C89E4AC20689794FED1352CD2086413965006C54A381BF3081BC300E0603551D0F0101FF04040302010630120603551D130101FF040830060101FF02010030560603551D1F044F304D304BA049A047864568747470733A2F2F6769746875622E636F6D2F6F70656E77616C6C65742D666F756E646174696F6E2D6C6162732F6964656E746974792D63726564656E7469616C2F63726C301D0603551D0E04160414CFA4AF87907312962E4D7A17646ACC1C45719B21301F0603551D23041830168014CFA4AF87907312962E4D7A17646ACC1C45719B21300A06082A8648CE3D040303036800306502310090FB8F814BCC87DB42957D22B54D20BF45F44CE0CF5734167ED27F5E3E0F5FB57505B797B894175D2BD98BF16CE726EA02305BA4F1ECB894A9DBE27B9BBF988F233C2E0BB0B4BADAA3EC5B3EA9D99C58DAD26128A4B363849E32626A9D5C3CE3E4DA".fromHexByteString()),
                    metadata = TrustMetadata(
                        displayName = "Multipaz Identity Reader",
                        displayIconUrl = "https://www.multipaz.org/multipaz-logo-200x200.png",
                    )
                )
                // Also from https://verifier.multipaz.org/identityreaderbackend/
                addX509Cert(
                    certificate = X509Cert
                        ("308202893082020FA003020102021041DFFB3D7133B2623E535E09D9C3B56E300A06082A8648CE3D0403033047310B300906035504060C0255533138303606035504030C2F4D756C746970617A204964656E74697479205265616465722043412028556E74727573746564204465766963657329301E170D3235303731393233303831345A170D3330303731393233303831345A3047310B300906035504060C0255533138303606035504030C2F4D756C746970617A204964656E74697479205265616465722043412028556E747275737465642044657669636573293076301006072A8648CE3D020106052B8104002203620004EA8A139ED395B79C877255FEF2138987262CFBB6CA1F72688D4E89F062C3CA05B2704531DAEC0304F93A007CD84F31A119F3794151306082C4D4352855A752F9C733D2FA32B4B462644769F2F7E53280F1AD519C443AE9462B923C64877EDF91A381BF3081BC300E0603551D0F0101FF04040302010630120603551D130101FF040830060101FF02010030560603551D1F044F304D304BA049A047864568747470733A2F2F6769746875622E636F6D2F6F70656E77616C6C65742D666F756E646174696F6E2D6C6162732F6964656E746974792D63726564656E7469616C2F63726C301D0603551D0E041604149BCFDAFD2059978E21869C7DD28AAF7481EBABC5301F0603551D230418301680149BCFDAFD2059978E21869C7DD28AAF7481EBABC5300A06082A8648CE3D0403030368003065023100A26AA37C97B6935EB64B959ACB7B04053723EFE0CFBDA2C972C96812C8FF1DA4E122C296A909502B180DBB5AC4FD7AF202307F1AAE9412B8162A5B29A7E2A9CEE00059A2A4F9B32370CE1A28E28E5378AD981FBD8D74D0DBDD0373C327595C1006CE".fromHexByteString()),
                    metadata = TrustMetadata(
                        displayName = "Multipaz Identity Reader (Untrusted Devices)",
                        displayIconUrl = "https://www.multipaz.org/multipaz-logo-200x200.png",
                    )
                )
            }
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
            provisioningModel = ProvisioningModel(
                documentStore = documentStore,
                secureArea = secureArea,
                httpClient = HttpClient(platformHttpClientEngineFactory()) {
                    followRedirects = false
                },
                promptModel = promptModel,
                documentMetadataInitializer = App::initializeDocumentMetadata
            )
            provisioningSupport = ProvisioningSupport()
            provisioningSupport.init()

            initialized = true
        }
    }

    @Composable
    fun Content() {
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
        val imageLoader = remember { ImageLoader.Builder(context).components { /* network loader omitted */ }.build() }
        val provisioningState = provisioningModel.state.collectAsState().value
        val documentInfos = documentModel.documentInfos.collectAsState().value

        MaterialTheme {
            val coroutineScope = rememberCoroutineScope { promptModel }
            val blePermissionState = rememberBluetoothPermissionState()
            val bleEnabledState = rememberBluetoothEnabledState()

            PromptDialogs(promptModel)

            if (provisioningState != ProvisioningModel.Idle &&
                provisioningState != ProvisioningModel.CredentialsIssued) {
                ProvisioningScreen()
            } else {
                if (!blePermissionState.isGranted) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = { coroutineScope.launch { blePermissionState.launchPermissionRequest() } }) {
                            Text("Request BLE permissions")
                        }
                    }
                } else if (!bleEnabledState.isEnabled) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = { coroutineScope.launch { bleEnabledState.enable() } }) {
                            Text("Enable BLE")
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(
                            space = 20.dp,
                            alignment = Alignment.CenterVertically
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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

                        HorizontalDivider()

                        Text("Number of documents in store: ${documentInfos.size}")
                        if (documentInfos.isNotEmpty()) {
                            val builder = StringBuilder("[")
                            documentInfos.onEachIndexed { index, (_, documentInfo) ->
                                if (index > 0) {
                                    builder.append(", ")
                                }
                                builder.append("${documentInfo.credentialInfos.size}")
                            }
                            builder.append("]")
                            Text("Number of credentials: $builder")
                        }
                        Button(onClick = {
                            coroutineScope.launch {
                                documentStore.listDocuments().forEach { id ->
                                    documentStore.deleteDocument(id)
                                }
                            }
                        }) {
                            Text("Clear DocumentStore")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ShowQrButton(
        onQrButtonClicked: (settings: MdocProximityQrSettings) -> Unit
    ) {
        Column(
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
                Text("Present mDL via QR")
            }
        }
    }

    @Composable
    private fun ShowQrCode(uri: String) {
        Column(
            modifier = Modifier.padding(16.dp),
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

    @Composable
    private fun ProvisioningScreen() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1.0f))
            Provisioning(
                provisioningModel = provisioningModel,
                waitForRedirectLinkInvocation = { state ->
                    provisioningSupport.waitForAppLinkInvocation(state)
                }
            )
            Spacer(modifier = Modifier.weight(1.0f))
            Button(onClick = {
                provisioningModel.cancel()
            }) {
                Text("Cancel Provisioning")
            }
            Spacer(modifier = Modifier.weight(1.0f))
        }
    }

    /**
     * Handle a link (either a app link, universal link, or custom URL schema link).
     */
    fun handleUrl(url: String) {
        if (url.startsWith("openid-credential-offer:") || url.startsWith("haip-vci")) {
            val queryIndex = url.indexOf('?')
            if (queryIndex >= 0) {
                try {
                    provisioningModel.launchOpenID4VCIProvisioning(
                        offerUri = url,
                        clientPreferences = provisioningSupport.preferences,
                        backend = provisioningSupport.backend
                    )
                } catch (_: IllegalStateException) {
                    // provisioning is already active
                }
            }
        } else if (url.startsWith(ProvisioningSupport.APP_LINK_BASE_URL)) {
            CoroutineScope(Dispatchers.Default).launch {
                provisioningSupport.processAppLinkInvocation(url)
            }
        }
    }

    companion object {
        val promptModel = Platform.promptModel
        
        private var app: App? = null
        fun getInstance(): App {
            if (app == null) {
                app = App()
            }
            return app!!
        }

        private suspend fun initializeDocumentMetadata(
            metadata: AbstractDocumentMetadata,
            credentialDisplay: Display,
            issuerDisplay: Display
        ) {
            (metadata as DocumentMetadata).setMetadata(
                displayName = credentialDisplay.text,  // TODO: customize after provisioning?
                typeDisplayName = credentialDisplay.text,
                cardArt = credentialDisplay.logo
                    ?: ByteString(Res.readBytes("drawable/card_generic.png")),
                issuerLogo = issuerDisplay.logo,
                other = null
            )
        }
    }
}
