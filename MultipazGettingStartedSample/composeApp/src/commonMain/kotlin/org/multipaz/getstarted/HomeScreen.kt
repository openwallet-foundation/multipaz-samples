package org.multipaz.getstarted

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.document.Document
import org.multipaz.facedetection.detectFaces
import org.multipaz.facematch.FaceEmbedding
import org.multipaz.facematch.getFaceEmbeddings
import org.multipaz.getstarted.w3cdc.ShowResponseMetadata
import org.multipaz.getstarted.w3cdc.W3CDCCredentialsRequestButton
import org.multipaz.getstarted.w3cdc.toDataItem
import org.multipaz.selfiecheck.SelfieCheck
import org.multipaz.selfiecheck.SelfieCheckViewModel
import org.multipaz.util.toBase64Url
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    app: App,
    navController: NavController,
    documents: List<Document>,
    imageLoader: ImageLoader,
    identityIssuer: String = "Multipaz Getting Started Sample",
    onDeleteDocument: (Document) -> Unit
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
                showQrButton = { onQrButtonClicked -> app.ShowQrButton(onQrButtonClicked) },
                showQrCode = { uri -> app.ShowQrCode(uri) }
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
                        if (index > 0) {
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
            W3CDCCredentialsRequestButton(
                promptModel = App.promptModel,
                storageTable = app.storageTable,
                zkSystemRepository = app.zkSystemRepository,

                showResponse = { vpToken: JsonObject?,
                                 deviceResponse: DataItem?,
                                 sessionTranscript: DataItem,
                                 nonce: ByteString?,
                                 eReaderKey: EcPrivateKey?,
                                 metadata: ShowResponseMetadata ->
                    val route = Destination.ShowResponseDestination.route +
                            "/${
                                vpToken?.let { Json.encodeToString(it) }?.encodeToByteArray()
                                    ?.toBase64Url() ?: "_"
                            }" +
                            "/${deviceResponse?.let { Cbor.encode(it).toBase64Url() } ?: "_"}" +
                            "/${Cbor.encode(sessionTranscript).toBase64Url()}" +
                            "/${nonce?.let { nonce.toByteArray().toBase64Url() } ?: "_"}" +
                            "/${
                                eReaderKey?.let {
                                    Cbor.encode(eReaderKey.toCoseKey().toDataItem()).toBase64Url()
                                } ?: "_"
                            }" +
                            "/${Cbor.encode(metadata.toDataItem()).toBase64Url()}"
                    navController.navigate(route)
                }
            )
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
                                    model = app.faceMatchLiteRtModel
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
                        similarity = 0f
                    } else if (faceCaptured.value != null) {
                        val faceImage =
                            app.extractFaceBitmap(
                                incomingVideoFrame,
                                faces[0], // assuming only one face exists for simplicity
                                app.faceMatchLiteRtModel.imageSquareSize
                            )

                        val faceInsetsForDetectedFace =
                            getFaceEmbeddings(faceImage, app.faceMatchLiteRtModel)

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
