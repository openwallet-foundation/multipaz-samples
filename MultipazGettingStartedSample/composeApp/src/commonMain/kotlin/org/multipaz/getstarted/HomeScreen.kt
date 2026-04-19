package org.multipaz.getstarted

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonObject
import org.multipaz.cbor.DataItem
import org.multipaz.compose.permissions.rememberCameraPermissionState
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.document.Document
import org.multipaz.getstarted.biometrics.FaceExtractor
import org.multipaz.getstarted.biometrics.FaceMatchingSection
import org.multipaz.getstarted.biometrics.SelfieCheckSection
import org.multipaz.getstarted.core.AppContainer
import org.multipaz.getstarted.core.CredentialDomains
import org.multipaz.getstarted.core.isAndroid
import org.multipaz.getstarted.presentment.PresentmentHomeSection
import org.multipaz.getstarted.verification.ShowResponseMetadata
import org.multipaz.getstarted.verification.W3CDCCredentialsRequestButton
import org.multipaz.getstarted.verification.buildShowResponseDestination
import org.multipaz.facematch.FaceEmbedding

@Composable
fun HomeScreen(
    container: AppContainer,
    navController: NavController,
    documents: List<Document>,
    identityIssuer: String = "Multipaz Getting Started Sample",
    onDeleteDocument: (Document) -> Unit
) {
    val coroutineScope = rememberCoroutineScope { AppContainer.promptModel }
    val uriHandler = LocalUriHandler.current
    val cameraPermissionState = rememberCameraPermissionState()

    val faceExtractor = remember { FaceExtractor() }
    var faceExtractorReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        faceExtractor.init()
        faceExtractorReady = true
    }

    val faceCaptured = remember { mutableStateOf<FaceEmbedding?>(null) }
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
        PresentmentHomeSection(
            presentmentSource = container.presentmentSource,
            promptModel = AppContainer.promptModel,
            modifier = Modifier.weight(1f)
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
                        text = document.displayName ?: document.identifier,
                        modifier = Modifier.padding(4.dp)
                    )
                    if (document.displayName != CredentialDomains.SAMPLE_DOCUMENT_DISPLAY_NAME) {
                        IconButton(
                            content = @Composable {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                coroutineScope.launch {
                                    container.documentStore.deleteDocument(document.identifier)
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

        // W3C Digital Credentials API is only available on Android
        if (isAndroid() && documents.isNotEmpty()) {
            W3CDCCredentialsRequestButton(
                promptModel = AppContainer.promptModel,
                storageTable = container.storageTable,
                readerTrustManager = container.readerTrustManager,
                showResponse = { vpToken: JsonObject?,
                                 deviceResponse: DataItem?,
                                 sessionTranscript: DataItem,
                                 nonce: ByteString?,
                                 eReaderKey: EcPrivateKey?,
                                 metadata: ShowResponseMetadata ->
                    navController.navigate(
                        buildShowResponseDestination(
                            vpToken = vpToken,
                            deviceResponse = deviceResponse,
                            sessionTranscript = sessionTranscript,
                            nonce = nonce,
                            eReaderKey = eReaderKey,
                            metadata = metadata,
                        )
                    )
                }
            )
        }

        if (faceExtractorReady) {
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
                    SelfieCheckSection(
                        faceExtractor = faceExtractor,
                        identityIssuer = identityIssuer,
                        onFaceCaptured = { embedding ->
                            faceCaptured.value = embedding
                        }
                    )
                }

                else -> {
                    FaceMatchingSection(
                        faceExtractor = faceExtractor,
                        faceCaptured = faceCaptured
                    )
                }
            }
        }
    }
}
