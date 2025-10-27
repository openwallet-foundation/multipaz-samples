package org.multipaz.samples.wallet.cmp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import io.ktor.utils.io.printStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.painterResource
import org.multipaz.cbor.Cbor
import org.multipaz.compose.permissions.rememberBluetoothEnabledState
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import org.multipaz.compose.presentment.MdocProximityQrPresentment
import org.multipaz.compose.presentment.MdocProximityQrSettings
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.compose.qrcode.generateQrCode
import org.multipaz.crypto.X509Cert
import org.multipaz.document.AbstractDocumentMetadata
import org.multipaz.document.DocumentMetadata
import org.multipaz.document.DocumentStore
import org.multipaz.document.buildDocumentStore
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.documenttype.knowntypes.LoyaltyID
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.mdoc.mso.MobileSecurityObjectParser
import org.multipaz.mdoc.mso.StaticAuthDataParser
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.models.digitalcredentials.DigitalCredentials
import org.multipaz.models.presentment.PresentmentModel
import org.multipaz.models.presentment.PresentmentSource
import org.multipaz.models.presentment.SimplePresentmentSource
import org.multipaz.models.provisioning.ProvisioningModel
import org.multipaz.provisioning.Display
import org.multipaz.sdjwt.SdJwt
import org.multipaz.sdjwt.credential.KeylessSdJwtVcCredential
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.storage.Storage
import org.multipaz.trustmanagement.TrustManager
import org.multipaz.trustmanagement.TrustManagerLocal
import org.multipaz.trustmanagement.TrustMetadata
import org.multipaz.trustmanagement.TrustPointAlreadyExistsException
import org.multipaz.util.Logger
import org.multipaz.util.Platform
import org.multipaz.util.UUID
import org.multipaz.util.fromBase64Url
import org.multipaz.util.toBase64Url
import utopiasample.composeapp.generated.resources.Res
import utopiasample.composeapp.generated.resources.profile
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.multipaz.securearea.CreateKeySettings as SA_CreateKeySettings


/**
 * Application singleton.
 *
 * Use [App.Companion.getInstance] to get an instance.
 */
class App() {
    val TAG = "APP"
    private val OID4VCI_CREDENTIAL_OFFER_URL_SCHEME = "openid-credential-offer://"
    private val HAIP_URL_SCHEME = "haip://"
    lateinit var storage: Storage
    lateinit var documentTypeRepository: DocumentTypeRepository
    lateinit var secureAreaRepository: SecureAreaRepository
    lateinit var secureArea: SecureArea
    lateinit var documentStore: DocumentStore
    lateinit var readerTrustManager: TrustManager
    lateinit var presentmentModel: PresentmentModel
    lateinit var presentmentSource: PresentmentSource
    lateinit var provisioningModel: ProvisioningModel

    private val credentialOffers = Channel<String>()

    val provisioningSupport = ProvisioningSupport()


    var display = false
    private val initLock = Mutex()
    private var initialized = false

    val appName = "UtopiaSample"
    val appIcon = Res.drawable.profile


    @OptIn(ExperimentalTime::class)
    suspend fun init() {
        initLock.withLock {
            if (initialized) {
                return
            }

            //TODO: initialize secureArea

            //TODO: initialize storage

            //TODO: initialize secureAreaRepository


            documentTypeRepository = DocumentTypeRepository().apply {
                addDocumentType(DrivingLicense.getDocumentType())
                addDocumentType(LoyaltyID.getDocumentType())
            }
            //TODO: initialize documentStore

            //TODO: add provisioningModel

            Logger.i(TAG, "init provisioningModel is $provisioningModel")

            if (documentStore.listDocuments().isEmpty()) {
                Logger.i(appName, "create document")
            } else {
                Logger.i(appName, "document already exists")
            }
            presentmentModel = PresentmentModel().apply { setPromptModel(promptModel) }

            val tm = TrustManagerLocal(storage = storage, identifier = "reader")
            try {
                tm.apply {
                    //TODO: Add X509Cert
                }
            } catch (e: TrustPointAlreadyExistsException) {
                e.printStack()
            }

            readerTrustManager = tm

            presentmentSource = SimplePresentmentSource(
                documentStore = documentStore,
                documentTypeRepository = documentTypeRepository,
                readerTrustManager = readerTrustManager,
                preferSignatureToKeyAgreement = true,
                // Match domains used when storing credentials via OpenID4VCI
                domainMdocSignature = TestAppUtils.CREDENTIAL_DOMAIN_MDOC_USER_AUTH,
                domainMdocKeyAgreement = TestAppUtils.CREDENTIAL_DOMAIN_MDOC_MAC_USER_AUTH,
                domainKeylessSdJwt = TestAppUtils.CREDENTIAL_DOMAIN_SDJWT_KEYLESS,
                domainKeyBoundSdJwt = TestAppUtils.CREDENTIAL_DOMAIN_SDJWT_USER_AUTH
            )

            if (DigitalCredentials.Default.available) {
                //The credentials will still exist in your document store and can be used for other presentation mechanisms like proximity sharing (NFC/BLE), but they won't be accessible through the standardized digital credentials infrastructure that Android provides.
                DigitalCredentials.Default.startExportingCredentials(
                    documentStore = documentStore,
                    documentTypeRepository = documentTypeRepository
                )
                Logger.i(TAG, "DigitalCredentials.Default.startExportingCredentials")
            }
            initialized = true
        }
    }

    // Add the document metadata initializer function
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

    @Composable
    fun Content() {
        var isInitialized = remember { mutableStateOf<Boolean>(false) }
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
            val navController = rememberNavController()

            // Stabilize dependencies to prevent unnecessary re-composition
            val stableProvisioningModel = remember(provisioningModel) { provisioningModel }
            val stableProvisioningSupport = remember(provisioningSupport) { provisioningSupport }

            // Use Compose Navigation instead of conditional rendering
            NavHost(navController = navController, startDestination = "main") {
                composable("main") {
                    Logger.i(TAG, "NavHost: Rendering 'main' route")
                    MainApp()
                }
                composable("provisioning") {
                    Logger.i(TAG, "NavHost: Rendering 'provisioning' route")
                    ProvisioningTestScreen(
                        this@App,
                        stableProvisioningModel,
                        stableProvisioningSupport,
                        onNavigateToMain = { navController.navigate("main") }
                    )
                }
            }

            // Use the working pattern from identity-credential project
            LaunchedEffect(true) {
                Logger.i(TAG, "LaunchedEffect: Credential processing loop started")
                while (true) {
                    Logger.i(TAG, "LaunchedEffect: Waiting for credential offer...")
                    val credentialOffer = credentialOffers.receive()
                    Logger.i(TAG, "LaunchedEffect: Received credential offer: $credentialOffer")
                    Logger.i(TAG, "LaunchedEffect: Launching OpenID4VCI provisioning...")
                    stableProvisioningModel.launchOpenID4VCIProvisioning(
                        offerUri = credentialOffer,
                        clientPreferences = ProvisioningSupport.OPENID4VCI_CLIENT_PREFERENCES,
                        backend = stableProvisioningSupport
                    )
                    Logger.i(
                        TAG,
                        "LaunchedEffect: Provisioning launched, navigating to provisioning"
                    )
                    navController.navigate("provisioning")
                    Logger.i(TAG, "LaunchedEffect: Navigation completed")
                }
            }

        }
    }

    @Composable
    private fun MainApp() {
        val selectedTab = remember { mutableStateOf(1) }
        val tabs = listOf("Explore", "Account")
        val deviceEngagement = remember { mutableStateOf<ByteString?>(null) }

        Scaffold(
            bottomBar = {
                TabRow(
                    selectedTabIndex = selectedTab.value,
                    modifier = Modifier.background(Color.White)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab.value == index,
                            onClick = { selectedTab.value = index },
                            text = { Text(title) },
                            icon = {
                                Icon(
                                    imageVector = if (index == 0) Icons.Default.Explore else Icons.Default.AccountCircle,
                                    contentDescription = title,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            when (selectedTab.value) {
                0 -> ExploreScreen(modifier = Modifier.padding(paddingValues))
                1 -> AccountScreen(
                    modifier = Modifier.padding(paddingValues),
                    deviceEngagement = deviceEngagement
                )
            }
        }
    }

    @Composable
    private fun AccountScreen(
        modifier: Modifier = Modifier,
        deviceEngagement: MutableState<ByteString?>
    ) {
        val hasCredentials = remember { mutableStateOf<Boolean?>(null) }
        val coroutineScope = rememberCoroutineScope { promptModel }
        
        // Check for usable credentials when the composable is first created
        LaunchedEffect(Unit) {
            val hasCred = hasAnyUsableCredential()
            hasCredentials.value = hasCred
            Logger.i(TAG, "AccountScreen: hasAnyUsableCredential: $hasCred")
        }
        
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PromptDialogs(promptModel)
            Spacer(modifier = Modifier.height(30.dp))
            MembershipCard()
            
            // Credential status indicator below the card
            Spacer(modifier = Modifier.height(16.dp))
            CredentialStatusIndicator(hasCredentials.value)
        }
        val blePermissionState = rememberBluetoothPermissionState()
        val bleEnabledState = rememberBluetoothEnabledState()

        if (!blePermissionState.isGranted) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            blePermissionState.launchPermissionRequest()
                        }
                    }
                ) {
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
        }else {
            val context = LocalPlatformContext.current
            val imageLoader = remember {
                ImageLoader.Builder(context).components { /* network loader omitted */ }.build()
            }

            val state = presentmentModel.state.collectAsState()
            val noCredentialDialog = remember { mutableStateOf(false) }
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
            if (noCredentialDialog.value) {
                AlertDialog(
                    onDismissRequest = { noCredentialDialog.value = false },
                    title = { Text("No credential available") },
                    text = { Text("Please add a credential before presenting.") },
                    confirmButton = {
                        TextButton(onClick = { noCredentialDialog.value = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun CredentialStatusIndicator(hasCredentials: Boolean?) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            when (hasCredentials) {
                true -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Credential Available",
                        tint = Color(0xFF4CAF50), // Green color
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Credential is available",
                        color = Color(0xFF4CAF50), // Green color
                        fontSize = 14.sp
                    )
                }
                false -> {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "No Credential",
                        tint = Color(0xFFF44336), // Red color
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Credential not available",
                        color = Color(0xFFF44336), // Red color
                        fontSize = 14.sp
                    )
                }
                null -> {
                    Text(
                        text = "Checking credential status...",
                        color = Color(0xFF666666), // Gray color
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    @Composable
    private fun ShowQrButton(onQrButtonClicked: (settings: MdocProximityQrSettings) -> Unit) {
        val hasCredentials = remember { mutableStateOf<Boolean?>(null) }
        val coroutineScope = rememberCoroutineScope { promptModel }
        val uriHandler = LocalUriHandler.current
        
        // Check for usable credentials when the composable is first created
        LaunchedEffect(Unit) {
            val hasCred = hasAnyUsableCredential()
            hasCredentials.value = hasCred
            Logger.i(TAG, "showQrButton: hasAnyUsableCredential: $hasCred")
        }
        
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Only show the button if we have usable credentials
            if (hasCredentials.value == true) {
                // TODO: show qr button when credentials are available
            } else if (hasCredentials.value == false) {
                // Show a message when no credentials are available
                Text(
                    text = "No usable credentials available.\nPlease add a credential first.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        Logger.i(TAG, "Opening issuer website: https://issuer.multipaz.org")
                        uriHandler.openUri("https://issuer.multipaz.org")
                    }
                ) {
                    Text("Get Credentials from Issuer")
                }
            } else {
                // Show loading state while checking credentials
                Text("Checking credentials...")
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
            Spacer(modifier = Modifier.height(330.dp))
            Text(text = "Present QR code to mdoc reader")
            //TODO: show QR code
            Button(
                onClick = {
                    presentmentModel.reset()
                }
            ) {
                Text("Cancel")
            }

        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun hasAnyUsableCredential(): Boolean {
        if (documentStore.listDocuments().isEmpty()) return false
        val documentId = documentStore.listDocuments().first()
        val document = documentStore.lookupDocument(documentId) ?: return false
        return document.hasUsableCredential()
    }


    /**
     * Handle a link (either a app link, universal link, or custom URL schema link).
     */
    fun handleUrl(url: String) {
        Logger.i(TAG, "handleUrl called with: $url")
        Logger.i(TAG, "handleuir provisioningModel sate: ${provisioningModel.state.value}")
        if (url.startsWith(OID4VCI_CREDENTIAL_OFFER_URL_SCHEME)
            || url.startsWith(HAIP_URL_SCHEME)
        ) {
            val queryIndex = url.indexOf('?')
            if (queryIndex >= 0) {
                Logger.i(TAG, "Starting OpenID4VCI provisioning with: $url")
                Logger.i(
                    TAG,
                    "OID4VCI_CREDENTIAL_OFFER_URL_SCHEME provisioningModel: $provisioningModel"
                )
                Logger.i(TAG, "handleUrl: Sending credential offer to channel...")
                CoroutineScope(Dispatchers.Default).launch {
                    Logger.i(TAG, "handleUrl: About to send to credentialOffers channel")
                    credentialOffers.send(url)
                    Logger.i(TAG, "handleUrl: Successfully sent to credentialOffers channel")
                }
                Logger.i(
                    TAG,
                    "handleUrl: Credential offer sent to channel, LaunchedEffect should process it"
                )
                // Navigate to ProvisioningTestScreen after starting provisioning (commented out, handled by LaunchedEffect)
            }
        } else if (url.startsWith(ProvisioningSupport.APP_LINK_BASE_URL)) {
            Logger.i(TAG, "APP_LINK_BASE_URL provisioningModel: $provisioningModel")
            Logger.i(TAG, "Processing app link invocation: $url")

            // Check if we have an active provisioning session (removed by assistant, re-added by user, then removed again)
            // if (!isProvisioningActive()) { ... } // This check is currently NOT in the user's latest code.

            CoroutineScope(Dispatchers.Default).launch {
                try {
                    Logger.i(TAG, "handleUrl: About to process app link invocation")
                    //TODO:    call processAppLinkInvocation(url)
                    Logger.i(TAG, "handleUrl: App link invocation processed successfully")
                } catch (e: Exception) {
                    Logger.e(TAG, "Error processing app link: ${e.message}", e)
                    // resetProvisioningModel() // This call is currently NOT in the user's latest code.
                }
            }
        }
    }

    /**
     * Store issued credentials returned by OpenID4VCI enrollment into the existing document.
     * Accepts raw credential bytes as returned by client.obtainCredentials().
     */
    @OptIn(ExperimentalTime::class)
    suspend fun storeIssuedCredentialsRaw(credentialBytesList: List<ByteArray>) {
        if (documentStore.listDocuments().isEmpty()) {
            Logger.w(appName, "No document available to store credentials")
            return
        }

        // If the first document already has any credentials, skip storing to avoid duplicates
        val documentId = documentStore.listDocuments().first()
        val document = documentStore.lookupDocument(documentId) ?: return
        if (document.getCredentials().isNotEmpty()) {
            Logger.i(appName, "Document $documentId already has credentials; skipping store")
            return
        }
        val domain = "openid4vci"

        // Build a normalized response-like Json for logging consistency
        val responseJson = buildJsonObject {
            put("credentials", buildJsonArray {
                credentialBytesList.forEach { rawBytes ->
                    val asString = try {
                        val text = rawBytes.decodeToString()
                        val dotCount = text.count { it == '.' }
                        val printable = text.all { ch ->
                            val c = ch.code
                            (c in 32..126) || ch == '\n' || ch == '\r' || ch == '\t'
                        }
                        if (printable && dotCount >= 2) text else rawBytes.toBase64Url()
                    } catch (_: Throwable) {
                        rawBytes.toBase64Url()
                    }
                    add(buildJsonObject { put("credential", JsonPrimitive(asString)) })
                }
            })
        }
        Logger.i(appName, "Issuer response: $responseJson")

        val jsonArray = (responseJson["credentials"] as JsonArray)
        Logger.i(appName, "Normalized credentials array size: ${jsonArray.size}")
        jsonArray.forEachIndexed { index, element ->
            Logger.i(appName, "credentials[$index]: $element")
        }
        var storedCount = 0
        jsonArray.forEach { item ->
            val credentialString = item.jsonObject["credential"]!!.jsonPrimitive.content
            // Try SD-JWT first
            val stored = runCatching {
                val sdJwt = SdJwt(credentialString)
                val vct = "unknown"
                val cred = KeylessSdJwtVcCredential.create(
                    document = document,
                    asReplacementForIdentifier = null,
                    domain = domain,
                    vct = vct
                )
                cred.certify(
                    issuerProvidedAuthenticationData = credentialString.encodeToByteArray(),
                    validFrom = sdJwt.validFrom ?: sdJwt.issuedAt ?: Clock.System.now(),
                    validUntil = sdJwt.validUntil ?: kotlin.time.Instant.DISTANT_FUTURE
                )
                true
            }.getOrElse {
                false
            }
            if (stored) {
                storedCount += 1
                return@forEach
            }

            // Try mdoc (IssuerSigned base64url)
            runCatching {
                val credentialBytes = credentialString.fromBase64Url()
                val staticAuth = StaticAuthDataParser(credentialBytes).parse()
                val issuerAuthCoseSign1 = Cbor.decode(staticAuth.issuerAuth).asCoseSign1
                val encodedMsoBytes = Cbor.decode(issuerAuthCoseSign1.payload!!)
                val encodedMso = Cbor.encode(encodedMsoBytes.asTaggedEncodedCbor)
                val mso = MobileSecurityObjectParser(encodedMso).parse()

                val mdocCred = MdocCredential.create(
                    document = document,
                    asReplacementForIdentifier = null,
                    domain = domain,
                    secureArea = secureArea,
                    docType = mso.docType,
                    createKeySettings = SA_CreateKeySettings(
                        nonce = "Enroll".encodeToByteString()
                    )
                )
                mdocCred.certify(
                    issuerProvidedAuthenticationData = credentialBytes,
                    validFrom = mso.validFrom,
                    validUntil = mso.validUntil
                )
                storedCount += 1
            }.onFailure { e ->
                Logger.w(appName, "Skipping unknown credential format: ${e.message}")
            }
        }
        Logger.i(appName, "Stored $storedCount credential(s) into document $documentId")
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
    }
}