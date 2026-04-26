package org.multipaz.getstarted

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonObject
import org.multipaz.cbor.DataItem
import org.multipaz.compose.document.DocumentCarousel
import org.multipaz.compose.document.DocumentInfo
import org.multipaz.compose.document.DocumentModel
import org.multipaz.compose.permissions.rememberCameraPermissionState
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.document.Document
import org.multipaz.document.DocumentStore
import org.multipaz.facematch.FaceEmbedding
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

@OptIn(ExperimentalMaterial3Api::class)
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
            .verticalScroll(scrollState),
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

        val documentModel by produceState<DocumentModel?>(null, container) {
            value = DocumentModel.create(
                documentStore = container.documentStore,
                documentTypeRepository = container.documentTypeRepository,
            )
        }

        var selectedDocumentId by remember { mutableStateOf<String?>(null) }

        documentModel?.let { model ->
            DocumentCarousel(
                documentModel = model,
                onDocumentClicked = { documentInfo: DocumentInfo ->
                    selectedDocumentId = documentInfo.document.identifier
                }
            )

            selectedDocumentId?.let { id ->
                ModalBottomSheet(
                    onDismissRequest = { selectedDocumentId = null },
                ) {
                    DocumentDetails(
                        documentModel = model,
                        documentStore = container.documentStore,
                        documentId = id,
                        onDocumentDeleted = { selectedDocumentId = null },
                    )
                }
            }
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

@Composable
private fun DocumentDetails(
    documentModel: DocumentModel,
    documentStore: DocumentStore,
    documentId: String,
    onDocumentDeleted: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val documentInfo = documentModel.documentInfos.collectAsState().value
        .find { it.document.identifier == documentId }

    if (documentInfo == null) {
        Text("No document for identifier $documentId")
        return
    }
    val document = documentInfo.document

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            modifier = Modifier.height(200.dp),
            contentScale = ContentScale.FillHeight,
            bitmap = documentInfo.cardArt,
            contentDescription = null,
        )
        Text(
            text = document.typeDisplayName ?: "(typeDisplayName not set)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )

        KeyValuePair("Provisioned", if (document.provisioned) "Yes" else "No")
        KeyValuePair("Document Type", document.typeDisplayName ?: "(typeDisplayName not set)")
        KeyValuePair("Document Name", document.displayName ?: "(displayName not set)")

        if (document.displayName != CredentialDomains.SAMPLE_DOCUMENT_DISPLAY_NAME)
            Button(
                onClick = {
                    coroutineScope.launch { documentStore.deleteDocument(documentId) }
                    onDocumentDeleted()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White,
                ),
            ) {
                Text("Delete document")
            }
    }
}

@Composable
private fun KeyValuePair(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = key, fontWeight = FontWeight.Bold)
        Text(text = value)
    }
}
