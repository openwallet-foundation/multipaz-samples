package org.multipaz.photoididentityreader

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import multipazphotoididentityreader.composeapp.generated.resources.Res
import multipazphotoididentityreader.composeapp.generated.resources.nfc_icon
import multipazphotoididentityreader.composeapp.generated.resources.qr_icon
import multipazphotoididentityreader.composeapp.generated.resources.start_screen_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.multipaz.cbor.DataItem
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import org.multipaz.identityreader.BuildConfig
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethod
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodNfc
import org.multipaz.mdoc.nfc.scanNfcMdocReader
import org.multipaz.mdoc.transport.MdocTransport
import org.multipaz.mdoc.transport.MdocTransportFactory
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.nfc.nfcTagScanningSupported
import org.multipaz.nfc.nfcTagScanningSupportedWithoutDialog
import org.multipaz.prompt.PromptModel
import org.multipaz.util.Logger
import org.multipaz.util.UUID
import org.multipaz.util.toBase64Url

private const val TAG = "StartScreen"

private suspend fun signIn(
    explicitSignIn: Boolean,
    settingsModel: SettingsModel,
    readerBackendClient: ReaderBackendClient
) {
    Logger.i(TAG, "signIn, explicitSignIn = $explicitSignIn")
    val nonce = readerBackendClient.signInGetNonce()
    val (googleIdTokenString, signInData) = signInWithGoogle(
        explicitSignIn = explicitSignIn,
        serverClientId = BuildConfig.IDENTITY_READER_BACKEND_CLIENT_ID,
        nonce = nonce.toByteArray().toBase64Url(),
        httpClientEngineFactory = platformHttpClientEngineFactory(),
    )
    readerBackendClient.signIn(nonce, googleIdTokenString)
    settingsModel.signedIn.value = signInData
}

private suspend fun signOut(
    settingsModel: SettingsModel,
    readerBackendClient: ReaderBackendClient
) {
    Logger.i(TAG, "signOut()")
    settingsModel.explicitlySignedOut.value = true
    settingsModel.signedIn.value = null
    signInWithGoogleSignedOut()
    readerBackendClient.signOut()
    if (settingsModel.readerAuthMethod.value == ReaderAuthMethod.IDENTITY_FROM_GOOGLE_ACCOUNT ||
        settingsModel.readerAuthMethod.value == ReaderAuthMethod.STANDARD_READER_AUTH_WITH_GOOGLE_ACCOUNT_DETAILS
    ) {
        settingsModel.readerAuthMethod.value = ReaderAuthMethod.STANDARD_READER_AUTH
        settingsModel.readerAuthMethodGoogleIdentity.value = null
        try {
            readerBackendClient.getKey()
        } catch (e: Throwable) {
            Logger.w(TAG, "Error priming cache for standard reader auth", e)
        }
    }
}

private lateinit var snackbarHostState: SnackbarHostState

private fun showToast(message: String) {
    CoroutineScope(Dispatchers.Main).launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        when (snackbarHostState.showSnackbar(
            message = message,
            actionLabel = null,
            duration = SnackbarDuration.Short,
        )) {
            SnackbarResult.Dismissed -> {
            }

            SnackbarResult.ActionPerformed -> {
            }
        }
    }
}

@Composable
fun StartScreen(
    settingsModel: SettingsModel,
    readerBackendClient: ReaderBackendClient,
    promptModel: PromptModel,
    mdocTransportOptionsForNfcEngagement: MdocTransportOptions,
    onScanQrClicked: () -> Unit,
    onNfcHandover: suspend (
        transport: MdocTransport,
        encodedDeviceEngagement: ByteString,
        handover: DataItem,
        updateMessage: ((message: String) -> Unit)?
    ) -> Unit,
    onReaderIdentityClicked: () -> Unit,
    onTrustedIssuersClicked: () -> Unit,
    onDeveloperSettingsClicked: () -> Unit,
    onAboutClicked: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val blePermissionState = rememberBluetoothPermissionState()
    val showAccountDialog = remember { mutableStateOf(false) }
    val showSignInErrorDialog = remember { mutableStateOf<Throwable?>(null) }
    snackbarHostState = remember { SnackbarHostState() }

    showSignInErrorDialog.value?.let {
        AlertDialog(
            onDismissRequest = { showSignInErrorDialog.value = null },
            confirmButton = {
                TextButton(
                    onClick = { showSignInErrorDialog.value = null }
                ) {
                    Text(text = "Close")
                }
            },
            title = {
                Text(text = "Error signing in")
            },
            text = {
                Text(text = it.toString())
            }
        )
    }

    if (showAccountDialog.value) {
        SettingsDialog(
            settingsModel = settingsModel,
            onDismissed = { showAccountDialog.value = false },
            onUseWithoutGoogleAccountClicked = {
                coroutineScope.launch {
                    try {
                        showAccountDialog.value = false
                        signOut(
                            settingsModel = settingsModel,
                            readerBackendClient = readerBackendClient
                        )
                    } catch (e: Throwable) {
                        Logger.w(TAG, "Error signing out", e)
                    }
                }
            },
            onSignInToGoogleClicked = {
                coroutineScope.launch {
                    try {
                        showAccountDialog.value = false
                        signIn(
                            explicitSignIn = true,
                            settingsModel = settingsModel,
                            readerBackendClient = readerBackendClient
                        )
                    } catch (_: SignInWithGoogleDismissedException) {
                        // Do nothing
                    } catch (e: Throwable) {
                        showSignInErrorDialog.value = e
                    }
                }
            },
            onReaderIdentityClicked = {
                showAccountDialog.value = false
                onReaderIdentityClicked()
            },
            onTrustedIssuersClicked = {
                showAccountDialog.value = false
                onTrustedIssuersClicked()
            },
            onDeveloperSettingsClicked = {
                showAccountDialog.value = false
                onDeveloperSettingsClicked()
            },
            onAboutClicked = {
                showAccountDialog.value = false
                onAboutClicked()
            },
        )
    }

    // We can't do anything at all without Bluetooth permissions so make request those
    // upfront if they're not there...
    if (!blePermissionState.isGranted) {
        RequestBluetoothPermission(blePermissionState)
        return
    }

    var devModeNumTimesPressed by remember { mutableStateOf(0) }
    Scaffold(
        topBar = {
            AppBar(
                title = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(Res.string.start_screen_title))
                    }
                },
                onAccountPressed = {
                    showAccountDialog.value = true
                },
                settingsModel = settingsModel,
                titleClickable = true,
                onTitleClicked = {
                    if (settingsModel.devMode.value) {
                        showToast("Developer mode is already enabled")
                    } else {
                        if (devModeNumTimesPressed == 4) {
                            showToast("Developer mode is now enabled. See the Settings screen for details")
                            settingsModel.devMode.value = true
                        } else {
                            val tapsRemaining = 4 - devModeNumTimesPressed
                            if (tapsRemaining > 1) {
                                showToast("Tap $tapsRemaining more times to enable developer mode")
                            } else {
                                showToast("Tap 1 more time to enable developer mode")
                            }
                            devModeNumTimesPressed += 1
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            StartScreenWithPermissions(
                settingsModel = settingsModel,
                promptModel = promptModel,
                mdocTransportOptionsForNfcEngagement = mdocTransportOptionsForNfcEngagement,
                onScanQrClicked = onScanQrClicked,
                onNfcHandover = onNfcHandover,
                onOpportunisticSignInToGoogle = {
                    // Only opportunistically try to sign in the user except
                    //  - they explicitly signed out
                    //  - they dismissed the dialog for an opportunistic sign-in attempt
                    if (settingsModel.signedIn.value == null && !settingsModel.explicitlySignedOut.value) {
                        coroutineScope.launch {
                            try {
                                signIn(
                                    explicitSignIn = false,
                                    settingsModel = settingsModel,
                                    readerBackendClient = readerBackendClient
                                )
                            } catch (e: SignInWithGoogleDismissedException) {
                                // If the user explicitly dismissed this, don't try to sign them in again
                                settingsModel.explicitlySignedOut.value = true
                            } catch (e: Throwable) {
                                Logger.e(TAG, "Error signing into Google", e)
                            }
                        }
                    }
                },
                onReaderIdentityClicked = onReaderIdentityClicked
            )
        }
    }
}

@Composable
private fun StartScreenWithPermissions(
    settingsModel: SettingsModel,
    promptModel: PromptModel,
    mdocTransportOptionsForNfcEngagement: MdocTransportOptions,
    onScanQrClicked: () -> Unit,
    onNfcHandover: suspend (
        transport: MdocTransport,
        encodedDeviceEngagement: ByteString,
        handover: DataItem,
        updateMessage: ((message: String) -> Unit)?
    ) -> Unit,
    onOpportunisticSignInToGoogle: () -> Unit,
    onReaderIdentityClicked: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val selectedQueryName = remember { mutableStateOf(
        ReaderQuery.valueOf(settingsModel.selectedQueryName.value).name
    )}
    val coroutineScope = rememberCoroutineScope { promptModel }
    val nfcComposition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            if (isDarkTheme) {
                Res.readBytes("files/nfc_animation_dark.json").decodeToString()
            } else {
                Res.readBytes("files/nfc_animation.json").decodeToString()
            }
        )
    }
    val nfcProgress by animateLottieCompositionAsState(
        composition = nfcComposition,
        iterations = Compottie.IterateForever
    )

    // On Platforms that support NFC scanning without a dialog, start scanning as soon
    // as we enter this screen. We'll get canceled when switched away because `coroutineScope`
    // will get canceled.
    //
    LaunchedEffect(Unit) {
        if (nfcTagScanningSupportedWithoutDialog) {
            coroutineScope.launch {
                try {
                    val negotiatedHandoverConnectionMethods = mutableListOf<MdocConnectionMethod>()
                    val bleUuid = UUID.randomUUID()

                    // Add BLE Central Client Mode
                    negotiatedHandoverConnectionMethods.add(
                        MdocConnectionMethodBle(
                            supportsPeripheralServerMode = false,
                            supportsCentralClientMode = true,
                            peripheralServerModeUuid = null,
                            centralClientModeUuid = bleUuid,
                        )
                    )

                    // Add BLE Peripheral Server Mode
                    negotiatedHandoverConnectionMethods.add(
                        MdocConnectionMethodBle(
                            supportsPeripheralServerMode = true,
                            supportsCentralClientMode = false,
                            peripheralServerModeUuid = bleUuid,
                            centralClientModeUuid = null,
                        )
                    )

                    // Add NFC Data Transfer support
                    negotiatedHandoverConnectionMethods.add(
                        MdocConnectionMethodNfc(
                            commandDataFieldMaxLength = 0xffff,
                            responseDataFieldMaxLength = 0x10000
                        )
                    )

                    // In multipaz 0.94.0, scanNfcMdocReader returns a result instead of using a callback
                    val result = scanNfcMdocReader(
                        message = null,
                        options = mdocTransportOptionsForNfcEngagement,
                        transportFactory = MdocTransportFactory.Default,
                        selectConnectionMethod = { connectionMethods -> connectionMethods.first() },
                        negotiatedHandoverConnectionMethods = negotiatedHandoverConnectionMethods
                    )

                    // Handle the result if not null (user didn't cancel)
                    result?.let {
                        onNfcHandover(
                            it.transport,
                            it.encodedDeviceEngagement,
                            it.handover,
                            null  // updateMessage not available in background scanning
                        )
                    }
                } catch (e: Throwable) {
                    Logger.e(TAG, "NFC engagement failed", e)
                    showToast("NFC engagement failed: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        onOpportunisticSignInToGoogle()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppUpdateCard()

        Spacer(modifier = Modifier.weight(0.1f))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(
                16.dp,
                alignment = Alignment.CenterHorizontally
            )
        ) {
            for (query in ReaderQuery.entries) {
                FilterChip(
                    selected = query.name == selectedQueryName.value,
                    onClick = {
                        selectedQueryName.value = query.name
                        settingsModel.selectedQueryName.value = query.name
                    },
                    label = { Text(text = query.displayName) },
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.2f))

        if (!nfcTagScanningSupported) {
            // This is for phones that don't support NFC scanning
            Text(
                text = "Scan QR code from Wallet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        } else {
            Image(
                painter = rememberLottiePainter(
                    composition = nfcComposition,
                    progress = { nfcProgress },
                ),
                contentDescription = null,
                modifier = Modifier.size(200.dp)
            )
            if (nfcTagScanningSupportedWithoutDialog) {
                // This is for phones that support NFC scanning w/o dialog (Android)
                Text(
                    text = "Hold to Wallet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            } else {
                // This is for phones that requires a dialog for NFC scanning (iOS)
                Text(
                    text = "Scan NFC or QR from Wallet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.2f))
        RequestingData(
            settingsModel = settingsModel,
            onClicked = onReaderIdentityClicked
        )
        Spacer(modifier = Modifier.weight(0.3f))

        // Only show the "Scan NFC" button on platforms which require the system NFC Scan dialog (iOS)
        // and if the device actually supports NFC scanning functionality.
        if (nfcTagScanningSupported && !nfcTagScanningSupportedWithoutDialog) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val negotiatedHandoverConnectionMethods = mutableListOf<MdocConnectionMethod>()
                            val bleUuid = UUID.randomUUID()

                            // Add BLE Central Client Mode
                            negotiatedHandoverConnectionMethods.add(
                                MdocConnectionMethodBle(
                                    supportsPeripheralServerMode = false,
                                    supportsCentralClientMode = true,
                                    peripheralServerModeUuid = null,
                                    centralClientModeUuid = bleUuid,
                                )
                            )

                            // Add BLE Peripheral Server Mode
                            negotiatedHandoverConnectionMethods.add(
                                MdocConnectionMethodBle(
                                    supportsPeripheralServerMode = true,
                                    supportsCentralClientMode = false,
                                    peripheralServerModeUuid = bleUuid,
                                    centralClientModeUuid = null,
                                )
                            )

                            // Add NFC Data Transfer support
                            negotiatedHandoverConnectionMethods.add(
                                MdocConnectionMethodNfc(
                                    commandDataFieldMaxLength = 0xffff,
                                    responseDataFieldMaxLength = 0x10000
                                )
                            )

                            // In multipaz 0.94.0, scanNfcMdocReader returns a result instead of using a callback
                            val result = scanNfcMdocReader(
                                message = "Hold to Wallet",
                                options = MdocTransportOptions(bleUseL2CAP = true),
                                transportFactory = MdocTransportFactory.Default,
                                selectConnectionMethod = { connectionMethods -> connectionMethods.first() },
                                negotiatedHandoverConnectionMethods = negotiatedHandoverConnectionMethods
                            )

                            // Handle the result if not null (user didn't cancel)
                            result?.let {
                                onNfcHandover(
                                    it.transport,
                                    it.encodedDeviceEngagement,
                                    it.handover,
                                    null  // updateMessage not available when using return value pattern
                                )
                            }
                        } catch (e: Throwable) {
                            Logger.e(TAG, "NFC engagement failed", e)
                            showToast("NFC engagement failed: ${e.message}")
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(
                        Res.drawable
                            .nfc_icon),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                    text = "Scan NFC"
                )
            }
        }

        OutlinedButton(
            onClick = { onScanQrClicked() }
        ) {
            Icon(
                painter = painterResource(Res.drawable.qr_icon),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(
                text = "Scan QR Code"
            )
        }
    }
}
