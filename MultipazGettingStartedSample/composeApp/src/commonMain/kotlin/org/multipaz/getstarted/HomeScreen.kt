package org.multipaz.getstarted

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.ImageLoader
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.resources.painterResource
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.compose.camera.Camera
import org.multipaz.compose.camera.CameraCaptureResolution
import org.multipaz.compose.camera.CameraFrame
import org.multipaz.compose.camera.CameraSelection
import org.multipaz.compose.decodeImage
import org.multipaz.compose.permissions.rememberBluetoothEnabledState
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import org.multipaz.compose.permissions.rememberCameraPermissionState
import org.multipaz.compose.presentment.MdocProximityQrPresentment
import org.multipaz.compose.presentment.MdocProximityQrSettings
import org.multipaz.compose.qrcode.generateQrCode
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.document.Document
import org.multipaz.facedetection.detectFaces
import org.multipaz.facematch.FaceEmbedding
import org.multipaz.facematch.getFaceEmbeddings
import org.multipaz.getstarted.App.Companion.SAMPLE_DOCUMENT_DISPLAY_NAME
import org.multipaz.getstarted.w3cdc.ShowResponseMetadata
import org.multipaz.getstarted.w3cdc.W3CDCCredentialsRequestButton
import org.multipaz.getstarted.w3cdc.toDataItem
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.transport.MdocTransportOptions
import org.multipaz.selfiecheck.SelfieCheck
import org.multipaz.selfiecheck.SelfieCheckViewModel
import org.multipaz.util.UUID
import org.multipaz.util.toBase64Url
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    app: App,
    navController: NavController,
    documents: List<Document>,
    imageLoader: ImageLoader,
    identityIssuer: String = "Multipaz Getting Started Sample",
    onDeleteDocument: (Document) -> Unit,
    onCancel: () -> Unit
) {
    val selfieCheckViewModel: SelfieCheckViewModel =
        remember { SelfieCheckViewModel(identityIssuer) }

    val blePermissionState = rememberBluetoothPermissionState()
    val bleEnabledState = rememberBluetoothEnabledState()
    val coroutineScope = rememberCoroutineScope { App.promptModel }

    val uriHandler = LocalUriHandler.current

    val cameraPermissionState = rememberCameraPermissionState()

    var showCamera by remember { mutableStateOf(false) }
    val faceCaptured = remember { mutableStateOf<FaceEmbedding?>(null) }
    var showFaceMatching by remember { mutableStateOf(false) }
    var similarity by remember { mutableStateOf(0f) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .scrollable(
                scrollState,
                Orientation.Vertical
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            Button(
                onClick = { coroutineScope.launch { bleEnabledState.enable() } }) {
                Text("Enable Bluetooth")
            }
        } else {
            MdocProximityQrPresentment(
                modifier = Modifier.weight(1f),
                appName = app.appName,
                appIconPainter = painterResource(app.appIcon),
                presentmentModel = app.presentmentModel,
                presentmentSource = app.presentmentSource,
                promptModel = App.promptModel,
                documentTypeRepository = app.documentTypeRepository,
                imageLoader = imageLoader,
                allowMultipleRequests = false,
                showQrButton = { onQrButtonClicked -> ShowQrButton(onQrButtonClicked) },
                showQrCode = { uri ->
                    ShowQrCode(
                        uri,
                        onCancel = onCancel
                    )
                }
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


            if (documents.isNotEmpty()) {
                Text(
                    modifier = Modifier.padding(4.dp),
                    text = "${documents.size} Documents present:"
                )
                documents.forEachIndexed { index, document ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = document.metadata.displayName ?: document.identifier,
                            modifier = Modifier.padding(4.dp)
                        )
                        if (document.metadata.displayName != SAMPLE_DOCUMENT_DISPLAY_NAME) {
                            IconButton(
                                content = @Composable {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    coroutineScope.launch {
                                        app.documentStore.deleteDocument(document.identifier)
                                        onDeleteDocument(document)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                Text(text = "No documents found.")
            }
        }

        AnimatedVisibility(documents.isNotEmpty()) {
            // W3C Digital Credentials API is only available on Android
            if (isAndroid()) {
                W3CDCCredentialsRequestButton(
                    promptModel = App.promptModel,
                    storageTable = app.storageTable,
                    showResponse = { vpToken: JsonObject?,
                                     deviceResponse: DataItem?,
                                     sessionTranscript: DataItem,
                                     nonce: ByteString?,
                                     eReaderKey: EcPrivateKey?,
                                     metadata: ShowResponseMetadata ->
                        val vpTokenString =  vpToken
                            ?.let { Json.encodeToString(it) }
                            ?.encodeToByteArray()
                            ?.toBase64Url() ?: "_"

                        val deviceResponseString =
                            deviceResponse
                                ?.let { Cbor.encode(it).toBase64Url() }
                                ?: "_"

                        val sessionTranscriptString = Cbor.encode(sessionTranscript).toBase64Url()

                        val nonceString = nonce
                            ?.let { nonce.toByteArray().toBase64Url() }
                            ?: "_"

                        val eReaderKeyString =  eReaderKey
                            ?.let { Cbor.encode(eReaderKey.toCoseKey().toDataItem()).toBase64Url() }
                            ?: "_"

                        val metadataString = Cbor.encode(metadata.toDataItem()).toBase64Url()

                        navController.navigate(Destination.ShowResponseDestination(
                            vpResponse = vpTokenString,
                            deviceResponse = deviceResponseString,
                            sessionTranscript = sessionTranscriptString,
                            nonce = nonceString,
                            eReaderKey = eReaderKeyString,
                            metadata = metadataString
                        ))
                    }
                )
            }
        }

        when {
            !cameraPermissionState.isGranted -> {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    }
                ) {
                    Text("Grant Camera Permission for Selfie Check")
                }
            }

            faceCaptured.value == null -> {
                SelfieCheckFlow(
                    showCamera = showCamera,
                    onShowCameraChange = { showCamera = it },
                    selfieCheckViewModel = selfieCheckViewModel,
                    identityIssuer = identityIssuer,
                    onFaceCaptured = { embedding ->
                        faceCaptured.value = embedding
                    },
                    app = app
                )
            }

            else -> {
                FaceMatchingFlow(
                    showFaceMatching = showFaceMatching,
                    onShowFaceMatchingChange = { showFaceMatching = it },
                    similarity = similarity,
                    onSimilarityChange = { similarity = it },
                    faceCaptured = faceCaptured,
                    app = app
                )
            }
        }
    }
}

@Composable
private fun SelfieCheckFlow(
    showCamera: Boolean,
    onShowCameraChange: (Boolean) -> Unit,
    selfieCheckViewModel: SelfieCheckViewModel,
    identityIssuer: String,
    onFaceCaptured: (FaceEmbedding?) -> Unit,
    app: App
) {
    if (!showCamera) {
        Button(onClick = { onShowCameraChange(true) }) {
            Text("Selfie Check")
        }
    } else {
        SelfieCheck(
            modifier = Modifier.fillMaxWidth(),
            onVerificationComplete = {
                onShowCameraChange(false)
                if (selfieCheckViewModel.capturedFaceImage != null) {
                    val embedding = getFaceEmbeddings(
                        image = decodeImage(selfieCheckViewModel.capturedFaceImage!!.toByteArray()),
                        model = app.faceMatchLiteRtModel
                    )
                    onFaceCaptured(embedding)
                }
                selfieCheckViewModel.resetForNewCheck()
            },
            viewModel = selfieCheckViewModel,
            identityIssuer = identityIssuer
        )

        Button(
            onClick = {
                onShowCameraChange(false)
                selfieCheckViewModel.resetForNewCheck()
            }
        ) {
            Text("Close")
        }
    }
}

@Composable
private fun FaceMatchingFlow(
    showFaceMatching: Boolean,
    onShowFaceMatchingChange: (Boolean) -> Unit,
    similarity: Float,
    onSimilarityChange: (Float) -> Unit,
    faceCaptured: MutableState<FaceEmbedding?>,
    app: App
) {
    if (!showFaceMatching) {
        Button(onClick = { onShowFaceMatchingChange(true) }) {
            Text("Face Matching")
        }
    } else {
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

            when {
                faces.isNullOrEmpty() -> {
                    onSimilarityChange(0f)
                }

                faceCaptured.value != null -> {
                    val faceImage = app.extractFaceBitmap(
                        incomingVideoFrame,
                        faces[0], // assuming only one face exists for simplicity
                        app.faceMatchLiteRtModel.imageSquareSize
                    )

                    val faceEmbedding = getFaceEmbeddings(faceImage, app.faceMatchLiteRtModel)

                    if (faceEmbedding != null) {
                        val newSimilarity = faceCaptured.value!!.calculateSimilarity(faceEmbedding)
                        onSimilarityChange(newSimilarity)
                    }
                }
            }
        }

        Button(
            onClick = {
                onShowFaceMatchingChange(false)
                faceCaptured.value = null
            }
        ) {
            Text("Close")
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
fun ShowQrCode(
    uri: String,
    onCancel: () -> Unit
) {
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
                onCancel()
            }
        ) {
            Text("Cancel")
        }
    }
}
